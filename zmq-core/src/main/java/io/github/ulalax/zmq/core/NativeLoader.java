package io.github.ulalax.zmq.core;

import java.io.*;
import java.nio.file.*;

/**
 * Utility class for loading the native libzmq library.
 * <p>
 * This class handles the automatic extraction and loading of platform-specific
 * native libraries (DLL, SO, DYLIB) bundled within the JAR file. It detects the
 * operating system and architecture at runtime and loads the appropriate library.
 * </p>
 *
 * <h2>Supported Platforms:</h2>
 * <ul>
 *   <li>Windows (x86_64, aarch64/ARM64)</li>
 *   <li>Linux (x86_64, aarch64/ARM64)</li>
 *   <li>macOS (x86_64, aarch64/Apple Silicon)</li>
 * </ul>
 *
 * <h2>Custom Library Path:</h2>
 * <p>
 * You can specify a custom library path using the system property {@code zmq.library.path}:
 * </p>
 * <pre>{@code
 * System.setProperty("zmq.library.path", "/path/to/libzmq.so");
 * NativeLoader.load();
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is thread-safe. The {@link #load()} method uses double-checked locking
 * to ensure the library is loaded exactly once, even when called from multiple threads.
 * </p>
 *
 * @see LibZmq
 * @since 1.0.0
 */
public final class NativeLoader {

    private static volatile boolean loaded = false;
    private static final Object lock = new Object();

    private NativeLoader() {
        // Prevent instantiation
    }

    /**
     * Loads the native libzmq library.
     * <p>
     * This method automatically detects the platform and loads the appropriate
     * native library. If the library has already been loaded, this method does nothing.
     * </p>
     * <p>
     * The loading process:
     * </p>
     * <ol>
     *   <li>Check if already loaded (thread-safe)</li>
     *   <li>Check for custom library path via {@code zmq.library.path} system property</li>
     *   <li>Detect OS and architecture</li>
     *   <li>Extract library from JAR to temporary directory</li>
     *   <li>Load the library using {@link System#load(String)}</li>
     * </ol>
     *
     * <p>
     * This method is thread-safe and idempotent - calling it multiple times has no effect
     * after the first successful call.
     * </p>
     *
     * @throws UnsatisfiedLinkError if the library cannot be loaded (unsupported platform,
     *                              missing library in JAR, or extraction failure)
     * @see #isLoaded()
     */
    public static void load() {
        if (loaded) {
            return;
        }

        synchronized (lock) {
            if (loaded) {
                return;
            }

            // Check for user-specified library path
            String userPath = System.getProperty("zmq.library.path");
            if (userPath != null && !userPath.isEmpty()) {
                System.load(userPath);
                loaded = true;
                return;
            }

            // Detect platform
            String os = detectOS();
            String arch = detectArch();
            String libraryName = getLibraryName(os);

            // Resource path in JAR
            String resourcePath = "/native/" + os + "/" + arch + "/" + libraryName;

            // Extract and load
            try {
                File tempFile = extractLibrary(resourcePath, libraryName);
                System.load(tempFile.getAbsolutePath());
                loaded = true;
            } catch (IOException e) {
                throw new UnsatisfiedLinkError("Failed to load native library: " + e.getMessage());
            }
        }
    }

    /**
     * Detects the operating system from the {@code os.name} system property.
     *
     * @return the normalized OS name: "windows", "linux", or "macos"
     * @throws UnsupportedOperationException if the operating system is not supported
     */
    private static String detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("nux") || os.contains("nix")) {
            return "linux";
        } else if (os.contains("mac")) {
            return "macos";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }
    }

    /**
     * Detects the CPU architecture from the {@code os.arch} system property.
     *
     * @return the normalized architecture name: "x86_64" or "aarch64"
     * @throws UnsupportedOperationException if the architecture is not supported
     */
    private static String detectArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            return "x86_64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + arch);
        }
    }

    /**
     * Gets the platform-specific library file name.
     *
     * @param os the operating system ("windows", "linux", or "macos")
     * @return the library file name (e.g., "libzmq.dll", "libzmq.so", or "libzmq.dylib")
     * @throws IllegalArgumentException if the OS is unknown
     */
    private static String getLibraryName(String os) {
        switch (os) {
            case "windows":
                return "libzmq.dll";
            case "linux":
                return "libzmq.so";
            case "macos":
                return "libzmq.dylib";
            default:
                throw new IllegalArgumentException("Unknown OS: " + os);
        }
    }

    /**
     * Extracts the native library from the JAR to a temporary file.
     * <p>
     * This method reads the library from the JAR's resources, creates a temporary
     * file in the system temp directory under a "jvm-zmq" subdirectory, and writes
     * the library content to that file. The file is marked for deletion on JVM exit.
     * </p>
     *
     * @param resourcePath the path to the resource in the JAR (e.g., "/native/linux/x86_64/libzmq.so")
     * @param libraryName the name of the library file
     * @return a {@link File} object representing the extracted temporary library file
     * @throws IOException if the resource is not found or extraction fails
     */
    private static File extractLibrary(String resourcePath, String libraryName) throws IOException {
        // Get resource as stream
        InputStream in = NativeLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new FileNotFoundException("Native library not found in JAR: " + resourcePath);
        }

        // Create temp file
        // Use a unique name to avoid conflicts
        String tempFileName = "jvm-zmq-" + System.currentTimeMillis() + "-" + libraryName;
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "jvm-zmq");
        Files.createDirectories(tempDir);

        File tempFile = tempDir.resolve(tempFileName).toFile();
        tempFile.deleteOnExit();

        // Copy to temp file
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            in.close();
        }

        // Make executable (Unix/macOS)
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            tempFile.setExecutable(true, false);
            tempFile.setReadable(true, false);
        }

        return tempFile;
    }

    /**
     * Checks if the native library has been successfully loaded.
     *
     * @return {@code true} if the library has been loaded, {@code false} otherwise
     * @see #load()
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
