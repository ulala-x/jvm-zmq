package io.github.ulalax.zmq.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for NativeLoader class.
 * Verifies native library loading functionality.
 */
@Tag("Core")
@Tag("NativeLoader")
class NativeLoaderTest {

    @Nested
    @DisplayName("Library Loading")
    class LibraryLoading {

        @Test
        @DisplayName("Should load native library without errors")
        void should_Load_Native_Library_Without_Errors() {
            // Given: Native library should be available in resources

            // When: Load is called
            // Then: Should not throw any exception
            assertThatCode(() -> NativeLoader.load())
                    .as("Native library loading")
                    .doesNotThrowAnyException();

            // And: Library should be loaded successfully - verify by checking version
            int[] version = LibZmq.version();
            assertThat(version).isNotNull()
                    .hasSize(3);
            assertThat(version[0]).as("Major version").isGreaterThanOrEqualTo(4);
        }

        @Test
        @DisplayName("Should handle multiple load calls safely")
        void should_Handle_Multiple_Load_Calls_Safely() {
            // Given: Library is already loaded
            assertThatCode(() -> NativeLoader.load())
                    .doesNotThrowAnyException();

            // When: Load is called again
            // Then: Should not throw exception (idempotent behavior)
            assertThatCode(() -> NativeLoader.load())
                    .as("Multiple load calls should be safe")
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Platform Detection")
    class PlatformDetection {

        @Test
        @DisplayName("Should recognize supported platform")
        void should_Recognize_Supported_Platform() {
            // Given: Running on a supported platform
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();

            // Then: Should run on supported operating system
            assertThat(os)
                    .as("Operating system")
                    .satisfiesAnyOf(
                            name -> assertThat(name).contains("linux"),
                            name -> assertThat(name).contains("mac"),
                            name -> assertThat(name).contains("windows")
                    );

            // And: Should run on supported architecture
            assertThat(arch)
                    .as("Architecture")
                    .satisfiesAnyOf(
                            a -> assertThat(a).contains("amd64"),
                            a -> assertThat(a).contains("x86_64"),
                            a -> assertThat(a).contains("aarch64")
                    );
        }
    }
}
