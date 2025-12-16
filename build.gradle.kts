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
