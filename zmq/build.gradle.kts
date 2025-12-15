plugins {
    `java-library`
}

description = "High-level ZeroMQ API for Java"

dependencies {
    // Depend on zmq-core
    api(project(":zmq-core"))
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
