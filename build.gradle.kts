plugins {
    `java-library`
    `maven-publish`
}

allprojects {
    group = "io.github.ulalax"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        // Enable preview features for FFM API (Java 21)
        options.compilerArgs.addAll(listOf("--enable-preview"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs(
            // Enable native access for FFM
            "--enable-native-access=ALL-UNNAMED",
            // Enable preview features for FFM API
            "--enable-preview"
        )
    }

    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            // Enable preview features for FFM API
            addBooleanOption("-enable-preview", true)
            addStringOption("-release", "21")
        }
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.assertj:assertj-core:3.24.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                pom {
                    name.set("${project.group}:${project.name}")
                    description.set("ZeroMQ Java bindings using Foreign Function & Memory API")
                    url.set("https://github.com/ulala-x/jvm-zmq")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("ulala-x")
                            name.set("Ulala X")
                            email.set("ulala.x@example.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/ulala-x/jvm-zmq.git")
                        developerConnection.set("scm:git:ssh://github.com/ulala-x/jvm-zmq.git")
                        url.set("https://github.com/ulala-x/jvm-zmq")
                    }
                }
            }
        }
    }
}
