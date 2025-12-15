# Project Setup - jvm-zmq

This document provides complete Gradle multi-module configuration for the jvm-zmq project using JDK 22 FFM API.

---

## Table of Contents
1. [Project Structure](#project-structure)
2. [Root Configuration](#root-configuration)
3. [zmq-core Module](#zmq-core-module)
4. [zmq Module](#zmq-module)
5. [Native Library Bundling](#native-library-bundling)
6. [Build Commands](#build-commands)

---

## Project Structure

```
jvm-zmq/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── zmq-core/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── io/github/ulalax/zmq/core/
│       │   │       ├── LibZmq.java
│       │   │       ├── ZmqConstants.java
│       │   │       ├── ZmqStructs.java
│       │   │       ├── ZmqException.java
│       │   │       └── NativeLoader.java
│       │   └── resources/
│       │       └── native/
│       │           ├── windows/
│       │           │   └── x86_64/
│       │           │       └── libzmq.dll
│       │           ├── linux/
│       │           │   ├── x86_64/
│       │           │   │   └── libzmq.so
│       │           │   └── aarch64/
│       │           │       └── libzmq.so
│       │           └── macos/
│       │               ├── x86_64/
│       │               │   └── libzmq.dylib
│       │               └── aarch64/
│       │                   └── libzmq.dylib
│       └── test/
│           └── java/
│               └── io/github/ulalax/zmq/core/
└── zmq/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   └── java/
        │       └── io/github/ulalax/zmq/
        │           ├── Context.java
        │           ├── Socket.java
        │           ├── Message.java
        │           ├── Poller.java
        │           ├── PollItem.java
        │           ├── SocketType.java
        │           ├── SendFlags.java
        │           ├── RecvFlags.java
        │           ├── PollEvents.java
        │           ├── ContextOption.java
        │           ├── SocketOption.java
        │           ├── MessageProperty.java
        │           ├── SocketMonitorEvent.java
        │           ├── Curve.java
        │           ├── Z85.java
        │           ├── Proxy.java
        │           └── MultipartMessage.java
        └── test/
            └── java/
                └── io/github/ulalax/zmq/
```

---

## Root Configuration

### settings.gradle.kts

Complete root settings file:

```kotlin
rootProject.name = "jvm-zmq"

include("zmq-core")
include("zmq")
```

### build.gradle.kts (Root)

Complete root build file:

```kotlin
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
        // Enable preview features for FFM (if needed)
        // options.compilerArgs.addAll(listOf("--enable-preview"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs(
            // Enable native access for FFM
            "--enable-native-access=ALL-UNNAMED"
            // If using preview features:
            // "--enable-preview"
        )
    }

    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            // If using preview features:
            // addBooleanOption("-enable-preview", true)
            // addStringOption("-release", "22")
        }
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
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
```

### gradle.properties

```properties
# Project settings
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true

# Java version
javaVersion=22

# Enable native access for FFM
org.gradle.jvmargs=--enable-native-access=ALL-UNNAMED
```

---

## zmq-core Module

This module contains the low-level FFM bindings to libzmq.

### zmq-core/build.gradle.kts

```kotlin
plugins {
    `java-library`
}

description = "Low-level ZeroMQ bindings using Java FFM API"

dependencies {
    // No external dependencies - only JDK FFM
}

tasks {
    processResources {
        // Ensure native libraries are included
        from("src/main/resources") {
            include("native/**/*")
        }
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
```

### Module Info (Optional - Java 9+)

Create `zmq-core/src/main/java/module-info.java`:

```java
module io.github.ulalax.zmq.core {
    exports io.github.ulalax.zmq.core;

    // No requires - only uses java.base
}
```

---

## zmq Module

This module contains the high-level ZeroMQ API.

### zmq/build.gradle.kts

```kotlin
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
```

### Module Info (Optional - Java 9+)

Create `zmq/src/main/java/module-info.java`:

```java
module io.github.ulalax.zmq {
    requires transitive io.github.ulalax.zmq.core;

    exports io.github.ulalax.zmq;
}
```

---

## Native Library Bundling

### Directory Structure

Native libraries must be organized by OS and architecture:

```
zmq-core/src/main/resources/native/
├── windows/
│   └── x86_64/
│       └── libzmq.dll
├── linux/
│   ├── x86_64/
│   │   └── libzmq.so
│   └── aarch64/
│       └── libzmq.so
└── macos/
    ├── x86_64/
    │   └── libzmq.dylib
    └── aarch64/
        └── libzmq.dylib
```

### Obtaining Native Libraries

Download from: https://github.com/ulala-x/libzmq-native/releases

For each platform:
1. Download the release archive
2. Extract the shared library
3. Place in appropriate `native/{os}/{arch}/` directory

### Platform Detection

The `NativeLoader.java` will detect platform using:

```java
String os = System.getProperty("os.name").toLowerCase();
String arch = System.getProperty("os.arch").toLowerCase();

String osName;
if (os.contains("win")) {
    osName = "windows";
} else if (os.contains("nux") || os.contains("nix")) {
    osName = "linux";
} else if (os.contains("mac")) {
    osName = "macos";
} else {
    throw new UnsupportedOperationException("Unsupported OS: " + os);
}

String archName;
if (arch.contains("amd64") || arch.contains("x86_64")) {
    archName = "x86_64";
} else if (arch.contains("aarch64") || arch.contains("arm64")) {
    archName = "aarch64";
} else {
    throw new UnsupportedOperationException("Unsupported architecture: " + arch);
}

String libraryName;
if (osName.equals("windows")) {
    libraryName = "libzmq.dll";
} else if (osName.equals("linux")) {
    libraryName = "libzmq.so";
} else { // macos
    libraryName = "libzmq.dylib";
}

String resourcePath = "/native/" + osName + "/" + archName + "/" + libraryName;
```

### Loading Strategy

1. **Check System Property**: Allow override via `-Dzmq.library.path=/path/to/libzmq`
2. **Extract from JAR**: Extract to temp directory
3. **Load**: Use `System.load(absolutePath)`
4. **Cleanup**: Delete temp file on JVM shutdown (optional, temp dir cleaned by OS)

---

## Build Commands

### Build All Modules

```bash
./gradlew build
```

### Build Specific Module

```bash
./gradlew :zmq-core:build
./gradlew :zmq:build
```

### Run Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :zmq-core:test
./gradlew :zmq:test
```

### Generate Javadoc

```bash
./gradlew javadoc
```

Output: `build/docs/javadoc/`

### Publish to Local Maven

```bash
./gradlew publishToMavenLocal
```

Installs to: `~/.m2/repository/io/github/ulalax/`

### Create Distribution

```bash
./gradlew build
```

JARs will be in:
- `zmq-core/build/libs/zmq-core-1.0.0-SNAPSHOT.jar`
- `zmq/build/libs/zmq-1.0.0-SNAPSHOT.jar`

### Clean

```bash
./gradlew clean
```

---

## JDK 22 FFM Configuration

### Enable Native Access

The FFM API requires native access to be enabled. This is configured in the root `build.gradle.kts`:

```kotlin
tasks.withType<Test> {
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED"
    )
}
```

For running applications:

```bash
java --enable-native-access=ALL-UNNAMED -jar myapp.jar
```

### Java Toolchain

The project uses Java 22 toolchain:

```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}
```

Gradle will automatically download JDK 22 if not available.

### Compiler Options

```kotlin
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(22)
}
```

---

## Dependencies

### zmq-core

No external dependencies. Uses only:
- JDK 22 FFM API (`java.lang.foreign.*`)
- JDK 22 Core APIs

### zmq

Depends on:
- `zmq-core` (api dependency - transitively available to consumers)

### Test Dependencies

Both modules use JUnit 5:

```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

---

## IDE Configuration

### IntelliJ IDEA

1. **Import Project**: File → Open → Select `build.gradle.kts`
2. **Set JDK**: File → Project Structure → Project SDK → Add JDK 22
3. **Enable Native Access**:
   - Run → Edit Configurations → Add VM options: `--enable-native-access=ALL-UNNAMED`

### Eclipse

1. **Import Gradle Project**: File → Import → Gradle → Existing Gradle Project
2. **Set JDK**: Project Properties → Java Build Path → Add JDK 22
3. **Enable Native Access**: Run Configurations → Arguments → VM arguments: `--enable-native-access=ALL-UNNAMED`

### VS Code

1. **Open Folder**: Open `jvm-zmq` folder
2. **Install Extensions**:
   - Extension Pack for Java
   - Gradle for Java
3. **Configure JDK**: `.vscode/settings.json`:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-22",
         "path": "/path/to/jdk-22",
         "default": true
       }
     ]
   }
   ```

---

## Troubleshooting

### FFM Native Access Denied

**Error**: `java.lang.IllegalCallerException: Illegal native access`

**Solution**: Add `--enable-native-access=ALL-UNNAMED` to JVM arguments

### Unsupported Class File Version

**Error**: `java.lang.UnsupportedClassFileFormatError: Unsupported major.minor version`

**Solution**: Ensure JDK 22 is being used. Check:
```bash
java -version
./gradlew --version
```

### Native Library Not Found

**Error**: `UnsatisfiedLinkError: no libzmq in java.library.path`

**Solution**:
1. Verify native libraries are in `zmq-core/src/main/resources/native/`
2. Check `processResources` task includes them
3. Verify JAR contains them: `jar -tf zmq-core/build/libs/zmq-core-*.jar | grep native`

### Platform Detection Issues

**Error**: `UnsupportedOperationException: Unsupported OS`

**Solution**: Check system properties:
```java
System.out.println("os.name: " + System.getProperty("os.name"));
System.out.println("os.arch: " + System.getProperty("os.arch"));
```

---

## Next Steps

After project setup:

1. Implement `NativeLoader.java` (see `02-ffm-bindings.md` Section 5)
2. Implement `LibZmq.java` (see `02-ffm-bindings.md` Section 1)
3. Implement constants and structures (see `02-ffm-bindings.md` Sections 2-3)
4. Continue with high-level API (see `03-high-level-api.md`)

---

## Reference

- **JDK 22 FFM API**: https://openjdk.org/jeps/454
- **Gradle Kotlin DSL**: https://docs.gradle.org/current/userguide/kotlin_dsl.html
- **JUnit 5**: https://junit.org/junit5/docs/current/user-guide/
- **Maven Publishing**: https://docs.gradle.org/current/userguide/publishing_maven.html
