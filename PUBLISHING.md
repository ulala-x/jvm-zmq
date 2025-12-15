# Publishing Guide

This guide explains how to publish the jvm-zmq library to Maven repositories.

## Prerequisites

- Java 21 or later
- Gradle 8.x
- GitHub account with appropriate permissions (for GitHub Packages)

## Project Structure

The project consists of two modules:
- `zmq-core`: Low-level ZeroMQ bindings using Java FFM API
- `zmq`: High-level ZeroMQ API for Java

## Artifact Configuration

- **Group ID**: `io.github.ulalax`
- **Version**: `1.0.0-SNAPSHOT`
- **Artifacts**:
  - Main JAR
  - Sources JAR (`-sources.jar`)
  - Javadoc JAR (`-javadoc.jar`)

## Publishing to Local Maven Repository

To publish artifacts to your local Maven repository (`~/.m2/repository`):

```bash
./gradlew publishToMavenLocal
```

This will publish:
- `io.github.ulalax:zmq-core:1.0.0-SNAPSHOT`
- `io.github.ulalax:zmq:1.0.0-SNAPSHOT`

## Publishing to GitHub Packages

### Setup

GitHub Packages requires authentication. You have two options:

#### Option 1: Environment Variables (Recommended for CI/CD)

```bash
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=your-github-personal-access-token
```

#### Option 2: gradle.properties

Create or edit `~/.gradle/gradle.properties`:

```properties
gpr.user=your-github-username
gpr.token=your-github-personal-access-token
```

### Generate GitHub Token

1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token with the following permissions:
   - `write:packages` (Upload packages to GitHub Package Registry)
   - `read:packages` (Download packages from GitHub Package Registry)

### Publish

```bash
./gradlew publishAllPublicationsToGitHubPackagesRepository
```

Or simply:

```bash
./gradlew publish
```

This will publish to both local Maven repository and GitHub Packages.

## Using Published Artifacts

### From Local Maven Repository

Add to your `build.gradle.kts`:

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("io.github.ulalax:zmq:1.0.0-SNAPSHOT")
}
```

### From GitHub Packages

Add to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/ulala-x/jvm-zmq")
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("gpr.user") as String?
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("gpr.token") as String?
        }
    }
}

dependencies {
    implementation("io.github.ulalax:zmq:1.0.0-SNAPSHOT")
}
```

## Available Gradle Tasks

List all publishing tasks:

```bash
./gradlew tasks --group=publishing
```

Key tasks:
- `publishToMavenLocal` - Publish to local Maven repository
- `publishMavenPublicationToGitHubPackagesRepository` - Publish to GitHub Packages
- `publish` - Publish to all configured repositories
- `generatePomFileForMavenPublication` - Generate POM file

## Version Management

To change the version, edit the root `build.gradle.kts`:

```kotlin
allprojects {
    group = "io.github.ulalax"
    version = "1.0.0"  // Remove -SNAPSHOT for releases
}
```

## POM Metadata

The published POM includes:

- **Description**: ZeroMQ Java bindings using Foreign Function & Memory API
- **URL**: https://github.com/ulala-x/jvm-zmq
- **License**: MIT License
- **SCM**: GitHub repository information
- **Developers**: Project maintainers

## Troubleshooting

### Authentication Failed

Ensure your GitHub token has the correct permissions:
- `write:packages`
- `read:packages`

### Module Not Found

Check that:
1. The artifact was published successfully
2. Repository credentials are correct
3. Repository URL is correct in the consuming project

### Build Cache Issues

Clean and rebuild:

```bash
./gradlew clean build publishToMavenLocal
```

## CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Publish Package

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3

      - name: Publish package
        run: ./gradlew publish
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GITHUB_ACTOR: ${{ github.actor }}
```

## Additional Resources

- [Maven Publish Plugin Documentation](https://docs.gradle.org/current/userguide/publishing_maven.html)
- [GitHub Packages Documentation](https://docs.github.com/en/packages)
- [Java FFM API Documentation](https://openjdk.org/jeps/454)
