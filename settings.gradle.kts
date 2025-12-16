pluginManagement {
    plugins {
        id("me.champeau.jmh") version "0.7.2"
    }
}

rootProject.name = "jvm-zmq"

include("zmq-core")
include("zmq")
include("zmq-benchmark")
