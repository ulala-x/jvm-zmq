plugins {
    `java-library`
    id("me.champeau.jmh")
}

description = "High-level ZeroMQ API for Java"

dependencies {
    // Depend on zmq-core
    api(project(":zmq-core"))

    // Netty for ByteBuf support
    api("io.netty:netty-buffer:4.1.100.Final")

    // JMH dependencies
    jmhImplementation("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.jar {
    archiveBaseName.set("zmq")

    manifest {
        attributes(
            "Implementation-Title" to "ZMQ",
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "io.github.ulalax.zmq"
        )
    }
}

// JMH configuration
jmh {
    // JMH version
    jmhVersion.set("1.37")

    // Benchmark parameters (use annotation values by default)
    warmupIterations.set(1)
    iterations.set(1)
    fork.set(1)
    threads.set(1)

    // Output format
    resultsFile.set(project.file("${project.layout.buildDirectory.get()}/reports/jmh/results.json"))
    resultFormat.set("JSON")

    // Include patterns - support runtime override via -PjmhIncludes
    val jmhIncludesPattern = project.findProperty("jmhIncludes") as String? ?: ".*Benchmark.*"
    includes.add(jmhIncludesPattern)

    // JVM arguments for FFM native access
    jvmArgs.add("--enable-native-access=ALL-UNNAMED")

    // Enable GC profiling
    profilers.add("gc")
}

// Format JMH benchmark results in human-readable format
tasks.register("formatJmhResults") {
    description = "Format JMH benchmark results into human-readable format"
    group = "verification"

    doLast {
        val resultsFile = file("build/reports/jmh/results.json")
        if (!resultsFile.exists()) {
            println("No JMH results found at: ${resultsFile.absolutePath}")
            return@doLast
        }

        val json = groovy.json.JsonSlurper().parse(resultsFile) as List<Map<String, Any>>
        val output = StringBuilder()

        output.appendLine("\n" + "=".repeat(155))
        output.appendLine("JMH Benchmark Results")
        output.appendLine("=".repeat(155))

        // Table header
        output.appendLine(String.format("%-45s | %9s | %22s | %10s | %12s | %12s | %10s | %8s",
            "Benchmark", "Size", "Throughput (msg/s)", "Latency", "ops/s", "Alloc/op", "GC Rate", "GC Count"))
        output.appendLine("=".repeat(155))

        // Data class to hold benchmark metrics
        data class BenchmarkMetrics(
            val benchmarkName: String,
            val messageSize: Int,
            val throughput: Long,
            val throughputError: Long,
            val latencyUs: Double,
            val jmhScore: Double,
            val jmhScoreError: Double,
            val gcAllocRate: Double,
            val gcAllocNorm: Double,
            val gcCount: Int,
            val gcTime: Int
        )

        val metrics = mutableListOf<BenchmarkMetrics>()

        json.forEach { result ->
            val mode = result["mode"] as String
            if (mode != "thrpt") return@forEach

            val benchmarkFull = result["benchmark"] as String
            val benchmarkMethod = benchmarkFull.substringAfterLast(".")
            val metric = result["primaryMetric"] as Map<String, Any>
            val score = (metric["score"] as? Number)?.toDouble() ?: 0.0
            val error = when (val scoreError = metric["scoreError"]) {
                is Number -> scoreError.toDouble()
                is String -> if (scoreError == "NaN") Double.NaN else 0.0
                else -> 0.0
            }
            val params = result["params"] as? Map<String, String> ?: emptyMap()
            val messageCount = params["messageCount"]?.toIntOrNull() ?: 1
            val messageSize = params["messageSize"]?.toIntOrNull() ?: 0
            val receiveMode = params["receiveMode"]

            // Get GC metrics
            val secondaryMetrics = result["secondaryMetrics"] as? Map<String, Any> ?: emptyMap()
            val gcAllocRateMap = secondaryMetrics["gc.alloc.rate"] as? Map<String, Any>
            val gcAllocRate = (gcAllocRateMap?.get("score") as? Number)?.toDouble() ?: 0.0
            val gcAllocNormMap = secondaryMetrics["gc.alloc.rate.norm"] as? Map<String, Any>
            val gcAllocNorm = (gcAllocNormMap?.get("score") as? Number)?.toDouble() ?: 0.0
            val gcCountMap = secondaryMetrics["gc.count"] as? Map<String, Any>
            val gcCount = (gcCountMap?.get("score") as? Number)?.toInt() ?: 0
            val gcTimeMap = secondaryMetrics["gc.time"] as? Map<String, Any>
            val gcTime = (gcTimeMap?.get("score") as? Number)?.toInt() ?: 0

            // Build benchmark name with optional receiveMode
            val displayName = if (receiveMode != null) {
                "$benchmarkMethod [$receiveMode]"
            } else {
                benchmarkMethod
            }

            // Calculate actual message throughput
            val msgPerSec = (score * messageCount).toLong()
            val errorPerSec = if (error.isNaN()) 0L else (error * messageCount).toLong()

            // Calculate latency from throughput
            val latencyUs = if (msgPerSec > 0) 1_000_000.0 / msgPerSec else 0.0

            metrics.add(BenchmarkMetrics(
                benchmarkName = displayName,
                messageSize = messageSize,
                throughput = msgPerSec,
                throughputError = errorPerSec,
                latencyUs = latencyUs,
                jmhScore = score,
                jmhScoreError = if (error.isNaN()) 0.0 else error,
                gcAllocRate = gcAllocRate,
                gcAllocNorm = gcAllocNorm,
                gcCount = gcCount,
                gcTime = gcTime
            ))
        }

        // Sort by benchmark name, then by message size
        metrics.sortedWith(compareBy({ it.benchmarkName }, { it.messageSize })).forEach { m ->
            // Format size string
            val sizeStr = String.format("%,d B", m.messageSize)

            // Format throughput with K suffix for errors
            val throughputStr = if (m.throughputError >= 1000) {
                String.format("%,d (±%dK)", m.throughput, m.throughputError / 1000)
            } else {
                String.format("%,d (±%d)", m.throughput, m.throughputError)
            }

            // Format latency (μs)
            val latencyStr = String.format("%.2f μs", m.latencyUs)

            // Format JMH score
            val opsStr = String.format("%.1f (±%.1f)", m.jmhScore, m.jmhScoreError)

            // Format allocation per operation (B/op)
            val allocNormStr = when {
                m.gcAllocNorm >= 1_000_000 -> String.format("%.1f MB", m.gcAllocNorm / 1_000_000)
                m.gcAllocNorm >= 1_000 -> String.format("%.1f KB", m.gcAllocNorm / 1_000)
                else -> String.format("%.0f B", m.gcAllocNorm)
            }

            // Format GC rate (MB/s)
            val gcRateStr = String.format("%.1f MB/s", m.gcAllocRate)

            // Format GC count
            val gcCountStr = String.format("%d (%dms)", m.gcCount, m.gcTime)

            output.appendLine(String.format("%-45s | %9s | %22s | %10s | %12s | %12s | %10s | %8s",
                m.benchmarkName,
                sizeStr,
                throughputStr,
                latencyStr,
                opsStr,
                allocNormStr,
                gcRateStr,
                gcCountStr
            ))
        }

        output.appendLine("=".repeat(155))
        output.appendLine("Legend:")
        output.appendLine("  Throughput: Messages per second (higher is better)")
        output.appendLine("  Latency:    One-way send time in microseconds (lower is better)")
        output.appendLine("  ops/s:      JMH benchmark iterations per second")
        output.appendLine("  Alloc/op:   Memory allocated per operation (lower is better)")
        output.appendLine("  GC Rate:    GC allocation rate in MB/sec (lower is better)")
        output.appendLine("  GC Count:   Total GC count and time during benchmark")
        output.appendLine("=".repeat(155) + "\n")

        val formattedOutput = output.toString()
        println(formattedOutput)

        // Save to file
        val outputFile = file("build/reports/jmh/results-formatted.txt")
        outputFile.writeText(formattedOutput)
        println("Formatted results saved to: ${outputFile.absolutePath}")
    }
}

// Connect formatJmhResults to run automatically after jmh task
tasks.named("jmh") {
    finalizedBy("formatJmhResults")
}
