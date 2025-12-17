plugins {
    `java-library`
    idea
}

allprojects {
    apply(plugin = "idea")

    group = "io.github.ulalax"
    version = "0.1"

    repositories {
        mavenCentral()
    }

    // IntelliJ 모듈 이름을 단순하게 설정
    the<org.gradle.plugins.ide.idea.model.IdeaModel>().module {
        name = project.name
    }
}

// Aggregate Javadoc for all API modules (excluding samples)
val aggregateJavadoc by tasks.registering(Javadoc::class) {
    group = "documentation"
    description = "Generates aggregate Javadoc API documentation for zmq-core and zmq modules"

    // Include only zmq-core and zmq modules
    val apiProjects = subprojects.filter { it.name in listOf("zmq-core", "zmq") }

    // Collect source from all API modules
    source(apiProjects.map { it.the<SourceSetContainer>()["main"].allJava })

    // Collect classpath from all API modules
    classpath = files(apiProjects.map { it.the<SourceSetContainer>()["main"].compileClasspath })

    // Set destination directory
    setDestinationDir(file("${layout.buildDirectory.get()}/docs/javadoc"))

    // Configure Javadoc options
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        docEncoding = "UTF-8"
        charSet = "UTF-8"

        // Link to Java 22 API documentation
        links(
            "https://docs.oracle.com/en/java/javase/22/docs/api/"
        )

        // Set window and doc title
        windowTitle = "jvm-zmq ${project.version} API"
        docTitle = "jvm-zmq ${project.version} API Documentation"

        // Add header (footer is deprecated in Java 22+)
        header = "<b>jvm-zmq ${project.version}</b>"
        bottom = "Copyright &copy; 2024-2025 ulala-x. Licensed under MIT License."

        // Configure styling and options
        addStringOption("Xdoclint:none", "-quiet")
        addBooleanOption("html5", true)

        // Use source instead of release for Javadoc
        source = "22"

        // Group packages for better navigation
        group("High-Level API", listOf(
            "io.github.ulalax.zmq"
        ))
        group("Low-Level FFM Bindings", listOf(
            "io.github.ulalax.zmq.core"
        ))

        // Add overview documentation
        overview = file("src/docs/overview.html").takeIf { it.exists() }?.absolutePath

        // Enable author and version tags
        author(true)
        version(true)

        // Add tags for better documentation
        tags(
            "apiNote:a:API Note:",
            "implSpec:a:Implementation Requirements:",
            "implNote:a:Implementation Note:"
        )
    }

    // Ensure this task depends on all subproject compilation
    dependsOn(apiProjects.map { it.tasks.named("classes") })
}

// Convenience task to open generated Javadoc in browser
val openJavadoc by tasks.registering {
    group = "documentation"
    description = "Opens the generated Javadoc in the default web browser"

    dependsOn(aggregateJavadoc)

    doLast {
        val indexFile = file("${layout.buildDirectory.get()}/docs/javadoc/index.html")
        if (indexFile.exists()) {
            val os = System.getProperty("os.name").lowercase()
            val command = when {
                os.contains("windows") -> listOf("cmd", "/c", "start", indexFile.absolutePath)
                os.contains("mac") -> listOf("open", indexFile.absolutePath)
                else -> listOf("xdg-open", indexFile.absolutePath)
            }

            try {
                ProcessBuilder(command).start()
                println("Opening Javadoc in browser: ${indexFile.absolutePath}")
            } catch (e: Exception) {
                println("Unable to open browser automatically. Open this file manually:")
                println(indexFile.absolutePath)
            }
        } else {
            println("Javadoc not found. Run './gradlew aggregateJavadoc' first.")
        }
    }
}

// java-library 플러그인이 적용된 서브프로젝트 공통 설정
subprojects {
    plugins.withType<JavaLibraryPlugin> {
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(22))
            }
        }

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release.set(22)
        }

        tasks.withType<Test> {
            useJUnitPlatform()
            jvmArgs(
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
    }
}
