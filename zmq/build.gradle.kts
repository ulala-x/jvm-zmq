plugins {
    `java-library`
    id("me.champeau.jmh")
}

description = "High-level ZeroMQ API for Java"

dependencies {
    // Depend on zmq-core
    api(project(":zmq-core"))

    // JMH dependencies
    jmhImplementation("org.openjdk.jmh:jmh-core:1.37")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // Netty for PooledByteBufAllocator (benchmark only)
    jmhImplementation("io.netty:netty-buffer:4.1.100.Final")
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

    // Benchmark parameters
    warmupIterations.set(3)
    iterations.set(5)
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

        output.appendLine("\n" + "=".repeat(72))
        output.appendLine("Router-to-Router Throughput Benchmark Results")
        output.appendLine("=".repeat(72))

        // Table header
        output.appendLine("Message Size | Throughput (msg/sec) |  Latency (μs)  | JMH Score (ops/s)")
        output.appendLine("-".repeat(72))

        // Group by message size and sort
        val groupedBySize = json.groupBy {
            (it["params"] as? Map<String, String>)?.get("messageSize")
        }

        // Data class to hold benchmark metrics for each message size
        data class BenchmarkMetrics(
            val messageSize: Int,
            val throughput: Long,
            val throughputError: Long,
            val latencyUs: Double,
            val latencyError: Double,
            val jmhScore: Double,
            val jmhScoreError: Double
        )

        val metrics = mutableListOf<BenchmarkMetrics>()

        groupedBySize.toSortedMap(compareBy { it?.toInt() ?: 0 }).forEach { (size, results) ->
            results.forEach { result ->
                val mode = result["mode"] as String
                val metric = result["primaryMetric"] as Map<String, Any>
                val score = (metric["score"] as Number).toDouble()
                val error = (metric["scoreError"] as Number).toDouble()
                val params = result["params"] as? Map<String, String>
                val messageCount = params?.get("messageCount")?.toIntOrNull() ?: 1
                val sizeInt = size?.toInt() ?: 0

                when (mode) {
                    "thrpt" -> {
                        // Calculate actual message throughput
                        // JMH reports ops/s, each op processes messageCount messages
                        val msgPerSec = (score * messageCount).toLong()
                        val errorPerSec = (error * messageCount).toLong()

                        // Calculate latency from throughput
                        // latency_us = 1,000,000 / throughput_msg_per_sec
                        val latencyUs = 1_000_000.0 / msgPerSec
                        val latencyError = (1_000_000.0 / msgPerSec) * (errorPerSec.toDouble() / msgPerSec.toDouble())

                        metrics.add(BenchmarkMetrics(
                            messageSize = sizeInt,
                            throughput = msgPerSec,
                            throughputError = errorPerSec,
                            latencyUs = latencyUs,
                            latencyError = latencyError,
                            jmhScore = score,
                            jmhScoreError = error
                        ))
                    }
                }
            }
        }

        // Format table rows
        metrics.forEach { m ->
            // Format message size with appropriate unit
            val sizeStr = when {
                m.messageSize >= 1024 -> String.format("%,7d B", m.messageSize)
                else -> String.format("%7d B", m.messageSize)
            }

            // Format throughput with K suffix for errors
            val throughputStr = if (m.throughputError >= 1000) {
                String.format("%,10d (±%dK)", m.throughput, m.throughputError / 1000)
            } else {
                String.format("%,10d (±%d)", m.throughput, m.throughputError)
            }

            // Format latency with appropriate precision
            val latencyStr = String.format("%6.2f (±%.2f)", m.latencyUs, m.latencyError)

            // Format JMH score
            val jmhScoreStr = String.format("%8.2f (±%.2f)", m.jmhScore, m.jmhScoreError)

            output.appendLine(String.format("%s | %s | %s | %s",
                sizeStr,
                throughputStr,
                latencyStr,
                jmhScoreStr
            ))
        }

        output.appendLine("=".repeat(72))
        output.appendLine("Legend:")
        output.appendLine("  Throughput: Messages per second (higher is better)")
        output.appendLine("  Latency:    One-way send time in microseconds (lower is better)")
        output.appendLine("              Calculated as: 1,000,000 / Throughput")
        output.appendLine("  JMH Score:  Benchmark iterations per second (each = 10,000 messages)")
        output.appendLine("=".repeat(72) + "\n")

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
