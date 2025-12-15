package io.github.ulalax.zmq.core;

import java.io.*;
import java.nio.file.*;

/**
 * Loads the native libzmq library.
 * Extracts the platform-specific shared library from the JAR and loads it.
 */
public final class NativeLoader {

    private static volatile boolean loaded = false;
    private static final Object lock = new Object();

    private NativeLoader() {
        // Prevent instantiation
    }

    /**
     * Loads the native libzmq library.
     * This method is thread-safe and idempotent.
     * @throws UnsatisfiedLinkError if the library cannot be loaded
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
     * Detects the operating system.
     * @return "windows", "linux", or "macos"
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
     * Detects the CPU architecture.
     * @return "x86_64" or "aarch64"
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
     * Gets the library file name for the platform.
     * @param os Operating system
     * @return Library file name
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
     * Extracts the native library from JAR to a temporary file.
     * @param resourcePath Path to resource in JAR
     * @param libraryName Name of the library
     * @return Temporary file containing the library
     * @throws IOException if extraction fails
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
     * Checks if the native library has been loaded.
     * @return true if loaded
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
