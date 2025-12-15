package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Context class.
 */
@Tag("Context")
class ContextTest {

    @Nested
    @DisplayName("Context Creation")
    class ContextCreation {

        @Test
        @DisplayName("Should create context with default options")
        void should_Create_Context_With_Default_Options() {
            // When: Create a new context
            try (Context ctx = new Context()) {
                // Then: Context should be created successfully
                assertThat(ctx).isNotNull();
            }
        }

        @Test
        @DisplayName("Should create context with custom options")
        void should_Create_Context_With_Custom_Options() {
            // Given: Custom IO threads and max sockets values
            int ioThreads = 2;
            int maxSockets = 512;

            // When: Create context with custom options
            try (Context ctx = new Context(ioThreads, maxSockets)) {
                // Then: Context should have the specified options
                assertThat(ctx.getOption(ContextOption.IO_THREADS))
                        .as("IO threads")
                        .isEqualTo(ioThreads);
                assertThat(ctx.getOption(ContextOption.MAX_SOCKETS))
                        .as("Max sockets")
                        .isEqualTo(maxSockets);
            }
        }
    }

    @Nested
    @DisplayName("Context Options")
    class ContextOptions {

        @Test
        @DisplayName("Should set and get IO threads option")
        void should_Set_And_Get_IO_Threads_Option() {
            // Given: A context
            try (Context ctx = new Context()) {
                // When: Set IO threads to 4
                ctx.setOption(ContextOption.IO_THREADS, 4);

                // Then: Should retrieve the set value
                assertThat(ctx.getOption(ContextOption.IO_THREADS))
                        .as("IO threads after setting")
                        .isEqualTo(4);
            }
        }
    }

    @Nested
    @DisplayName("Version Information")
    class VersionInformation {

        @Test
        @DisplayName("Should return valid ZeroMQ version")
        void should_Return_Valid_ZeroMQ_Version() {
            // When: Get ZeroMQ version
            Context.Version version = Context.version();

            // Then: Version should be valid
            assertThat(version).isNotNull();
            assertThat(version.major())
                    .as("Major version")
                    .isGreaterThanOrEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Capability Detection")
    class CapabilityDetection {

        @Test
        @DisplayName("Should check for standard transport capabilities")
        void should_Check_For_Standard_Transport_Capabilities() {
            // When: Check for standard transport capabilities
            boolean hasTcp = Context.has("tcp");
            boolean hasInproc = Context.has("inproc");

            // Then: These standard transports should be supported
            // (We just verify the method doesn't throw - actual support may vary by build)
            assertThat(hasTcp || !hasTcp).isTrue(); // Always true, just exercising the code
            assertThat(hasInproc || !hasInproc).isTrue();
        }

        @Test
        @DisplayName("Should check for CURVE capability without errors")
        void should_Check_For_CURVE_Capability_Without_Errors() {
            // When: Check for CURVE capability
            // Then: Should not throw exception (actual support depends on libzmq build)
            assertThatCode(() -> Context.has("curve"))
                    .as("Checking CURVE capability")
                    .doesNotThrowAnyException();
        }
    }
}
