plugins {
    `java-library`
    id("me.champeau.jmh")
}

description = "JMH Benchmarks for ZeroMQ Java Bindings"

dependencies {
    // Depend on zmq module
    implementation(project(":zmq"))

    // JMH dependencies - need to be in implementation scope for compilation
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(22)
}

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

    // Include patterns
    includes.add(".*Benchmark.*")

    // JVM arguments for FFM native access
    jvmArgs.add("--enable-native-access=ALL-UNNAMED")

    // Profilers (uncomment to use)
    // profilers.add("gc")
    // profilers.add("stack")
}
