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

    // Include patterns
    includes.add(".*Benchmark.*")

    // JVM arguments for FFM native access
    jvmArgs.add("--enable-native-access=ALL-UNNAMED")
}
