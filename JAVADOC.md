# Javadoc Documentation Setup

This document describes the Javadoc configuration and deployment setup for jvm-zmq.

## Overview

The project uses Gradle to generate aggregate Javadoc for the `zmq-core` and `zmq` modules. The documentation is automatically deployed to GitHub Pages on every push to the `main` branch.

## Project Structure

```
jvm-zmq/
├── .github/
│   └── workflows/
│       └── javadoc.yml          # GitHub Actions workflow for deployment
├── src/
│   └── docs/
│       └── overview.html        # Main overview page
├── zmq/
│   └── src/main/java/io/github/ulalax/zmq/
│       ├── package-info.java   # High-level API package documentation
│       └── *.java              # API classes with Javadoc comments
├── zmq-core/
│   └── src/main/java/io/github/ulalax/zmq/core/
│       ├── package-info.java   # Low-level FFM bindings package documentation
│       └── *.java              # Core classes with Javadoc comments
└── build.gradle.kts            # Aggregate Javadoc task configuration
```

## Generating Javadoc Locally

### Prerequisites
- Java 22 or later
- Gradle (wrapper included)

### Commands

Generate aggregate Javadoc for all API modules:
```bash
./gradlew aggregateJavadoc
```

The generated documentation will be located at:
```
build/docs/javadoc/index.html
```

Open the generated Javadoc in your default browser:
```bash
./gradlew openJavadoc
```

Clean and regenerate:
```bash
./gradlew clean aggregateJavadoc
```

## Javadoc Configuration

### Key Features

1. **Aggregate Documentation**: Combines documentation from `zmq-core` and `zmq` modules (excludes `zmq-samples`)

2. **Java 22 API Links**: Automatic linking to Java 22 API documentation

3. **Package Grouping**:
   - High-Level API: `io.github.ulalax.zmq`
   - Low-Level FFM Bindings: `io.github.ulalax.zmq.core`

4. **Custom Overview**: Rich overview page with quick start guide and examples

5. **HTML5 Output**: Modern HTML5-based documentation

6. **Custom Tags**: Support for `@apiNote`, `@implSpec`, and `@implNote` tags

### Configuration Location

The Javadoc task is configured in the root `build.gradle.kts`:

```kotlin
val aggregateJavadoc by tasks.registering(Javadoc::class) {
    // Configuration details...
}
```

## GitHub Pages Deployment

### Setup

1. **Enable GitHub Pages**:
   - Go to repository Settings → Pages
   - Under "Build and deployment", select "GitHub Actions" as the source

2. **Workflow**: The `.github/workflows/javadoc.yml` workflow automatically:
   - Builds the project
   - Generates Javadoc
   - Deploys to GitHub Pages

### Deployment Triggers

- Automatic: On every push to `main` branch
- Manual: Via GitHub Actions "workflow_dispatch" event

### Accessing Documentation

Once deployed, the documentation will be available at:
```
https://ulala-x.github.io/jvm-zmq/
```

## Documentation Guidelines

### Package-Level Documentation

Each package should have a `package-info.java` file with:
- Package overview and purpose
- Key features and components
- Usage examples
- Links to related documentation

### Class-Level Documentation

Each public class should include:
- Clear description of purpose and responsibility
- Usage examples in `<pre>{@code ...}</pre>` blocks
- `@see` tags for related classes
- Thread safety notes where applicable

### Method-Level Documentation

Each public method should document:
- Purpose and behavior
- `@param` descriptions for all parameters
- `@return` description (if not void)
- `@throws` for all checked and important unchecked exceptions
- Usage examples for complex methods

### Code Examples

Use the following format for code examples:
```java
/**
 * <p>Usage:</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket socket = new Socket(ctx, SocketType.REP)) {
 *     socket.bind("tcp://*:5555");
 *     // Use socket...
 * }
 * }</pre>
 */
```

### HTML Escaping

Remember to escape HTML special characters in documentation:
- `<` → `&lt;`
- `>` → `&gt;`
- `&` → `&amp;`

## Maintenance

### Updating Javadoc

1. Add or update Javadoc comments in source files
2. Test locally: `./gradlew aggregateJavadoc`
3. Review output in `build/docs/javadoc/`
4. Commit and push changes
5. GitHub Actions will automatically deploy updates

### Fixing Warnings

Run with verbose output to see all warnings:
```bash
./gradlew aggregateJavadoc --warning-mode all
```

Common issues:
- Missing `@param` or `@return` tags
- Unescaped HTML characters
- Broken `@link` references
- Unknown custom tags

## Advanced Configuration

### Adding Custom Stylesheets

To add a custom stylesheet:

1. Create `src/docs/javadoc-stylesheet.css`
2. Update `build.gradle.kts`:
   ```kotlin
   (options as StandardJavadocDocletOptions).apply {
       stylesheetFile = file("src/docs/javadoc-stylesheet.css")
   }
   ```

### Adding More Modules

To include additional modules in the aggregate Javadoc:

```kotlin
val apiProjects = subprojects.filter {
    it.name in listOf("zmq-core", "zmq", "new-module")
}
```

### Linking to External Documentation

Add external API links:
```kotlin
links(
    "https://docs.oracle.com/en/java/javase/22/docs/api/",
    "https://netty.io/4.1/api/"
)
```

## Troubleshooting

### Build Failures

**Error: "option --source cannot be used together with --release"**
- Solution: Use `source = "22"` instead of `addStringOption("-release", "22")`

**Error: "The -footer option is no longer supported"**
- Solution: Remove `footer` option (deprecated in Java 22+)

### Missing Classes in Javadoc

- Ensure module is included in `apiProjects` filter
- Verify classes are public
- Check that source sets are correctly configured

### Broken Links

- Verify package names in `@link` tags
- Ensure linked classes are in the same aggregate documentation
- Check external link URLs are accessible

## Reference Documentation

- [Javadoc Tool Guide](https://docs.oracle.com/en/java/javase/22/docs/specs/javadoc/doc-comment-spec.html)
- [Gradle Javadoc Task](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.javadoc.Javadoc.html)
- [GitHub Pages Documentation](https://docs.github.com/en/pages)
- [GitHub Actions for Pages](https://github.com/actions/deploy-pages)

## License

The documentation is part of the jvm-zmq project and is licensed under the MIT License.
