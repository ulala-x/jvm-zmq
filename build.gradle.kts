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
            languageVersion.set(JavaLanguageVersion.of(22))
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(22)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs(
            // Enable native access for FFM
            "--enable-native-access=ALL-UNNAMED"
        )
    }

    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("-release", "22")
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

        repositories {
            // Publish to local Maven repository
            mavenLocal()

            // Publish to GitHub Packages
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/ulala-x/jvm-zmq")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String? ?: ""
                    password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.token") as String? ?: ""
                }
            }
        }
    }
}
