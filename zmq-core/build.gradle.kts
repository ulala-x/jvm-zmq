plugins {
    `java-library`
}

description = "Low-level ZeroMQ bindings using Java FFM API"

dependencies {
    // No external dependencies - only JDK FFM
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

// Configure JAR to include native libraries
tasks.jar {
    archiveBaseName.set("zmq-core")

    manifest {
        attributes(
            "Implementation-Title" to "ZMQ Core",
            "Implementation-Version" to project.version,
            "Automatic-Module-Name" to "io.github.ulalax.zmq.core"
        )
    }
}
