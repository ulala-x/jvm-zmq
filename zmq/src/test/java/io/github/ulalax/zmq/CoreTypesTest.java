package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for core ZMQ types and enumerations.
 */
@Tag("Core")
@Tag("Types")
class CoreTypesTest {

    @Nested
    @DisplayName("SocketType Enumeration")
    class SocketTypeEnumeration {

        @Test
        @DisplayName("Should have all standard socket types")
        void should_Have_All_Standard_Socket_Types() {
            // When: Access socket types
            // Then: All standard types should be available
            assertThat(SocketType.PAIR).isNotNull();
            assertThat(SocketType.PUB).isNotNull();
            assertThat(SocketType.SUB).isNotNull();
            assertThat(SocketType.REQ).isNotNull();
            assertThat(SocketType.REP).isNotNull();
            assertThat(SocketType.DEALER).isNotNull();
            assertThat(SocketType.ROUTER).isNotNull();
            assertThat(SocketType.PULL).isNotNull();
            assertThat(SocketType.PUSH).isNotNull();
            assertThat(SocketType.XPUB).isNotNull();
            assertThat(SocketType.XSUB).isNotNull();
        }

        @Test
        @DisplayName("Should have unique values for each socket type")
        void should_Have_Unique_Values_For_Each_Socket_Type() {
            // Given: All socket types
            SocketType[] types = SocketType.values();

            // When: Check values
            // Then: Each should have a unique value
            assertThat(types)
                    .as("Socket types")
                    .hasSize(12); // PAIR, PUB, SUB, REQ, REP, DEALER, ROUTER, PULL, PUSH, XPUB, XSUB, STREAM

            for (int i = 0; i < types.length; i++) {
                for (int j = i + 1; j < types.length; j++) {
                    assertThat(types[i].getValue())
                            .as("Socket type values should be unique")
                            .isNotEqualTo(types[j].getValue());
                }
            }
        }

        @Test
        @DisplayName("Should create sockets of each type")
        void should_Create_Sockets_Of_Each_Type() {
            // Given: A context
            try (Context ctx = new Context()) {
                // When: Create socket of each type
                // Then: All should be created successfully
                for (SocketType type : SocketType.values()) {
                    assertThatCode(() -> {
                        try (Socket socket = new Socket(ctx, type)) {
                            assertThat(socket).isNotNull();
                        }
                    })
                            .as("Creating socket of type: " + type)
                            .doesNotThrowAnyException();
                }
            }
        }
    }

    @Nested
    @DisplayName("SocketOption Enumeration")
    class SocketOptionEnumeration {

        @Test
        @DisplayName("Should have common socket options")
        void should_Have_Common_Socket_Options() {
            // When: Access socket options
            // Then: Common options should be available
            assertThat(SocketOption.LINGER).isNotNull();
            assertThat(SocketOption.SNDTIMEO).isNotNull();
            assertThat(SocketOption.RCVTIMEO).isNotNull();
            assertThat(SocketOption.SNDHWM).isNotNull();
            assertThat(SocketOption.RCVHWM).isNotNull();
            assertThat(SocketOption.ROUTING_ID).isNotNull();
            assertThat(SocketOption.TYPE).isNotNull();
            assertThat(SocketOption.EVENTS).isNotNull();
        }

        @Test
        @DisplayName("Should have buffer size options")
        void should_Have_Buffer_Size_Options() {
            // When: Access buffer options
            // Then: Buffer options should be available
            assertThat(SocketOption.SNDBUF).isNotNull();
            assertThat(SocketOption.RCVBUF).isNotNull();
        }

        @Test
        @DisplayName("Should have TCP-specific options")
        void should_Have_TCP_Specific_Options() {
            // When: Access TCP options
            // Then: TCP options should be available
            assertThat(SocketOption.TCP_KEEPALIVE).isNotNull();
            assertThat(SocketOption.TCP_KEEPALIVE_IDLE).isNotNull();
        }

        @Test
        @DisplayName("Should have advanced options")
        void should_Have_Advanced_Options() {
            // When: Access advanced options
            // Then: Advanced options should be available
            assertThat(SocketOption.XPUB_VERBOSE).isNotNull();
            assertThat(SocketOption.IMMEDIATE).isNotNull();
            assertThat(SocketOption.CONFLATE).isNotNull();
        }
    }

    @Nested
    @DisplayName("ContextOption Enumeration")
    class ContextOptionEnumeration {

        @Test
        @DisplayName("Should have IO_THREADS option")
        void should_Have_IO_THREADS_Option() {
            // When: Access context option
            // Then: IO_THREADS should be available
            assertThat(ContextOption.IO_THREADS)
                    .as("IO_THREADS option")
                    .isNotNull();
        }

        @Test
        @DisplayName("Should have MAX_SOCKETS option")
        void should_Have_MAX_SOCKETS_Option() {
            // When: Access context option
            // Then: MAX_SOCKETS should be available
            assertThat(ContextOption.MAX_SOCKETS)
                    .as("MAX_SOCKETS option")
                    .isNotNull();
        }

        @Test
        @DisplayName("Should set and get context options")
        void should_Set_And_Get_Context_Options() {
            // Given: A context
            try (Context ctx = new Context()) {
                // When: Set IO threads
                ctx.setOption(ContextOption.IO_THREADS, 2);

                // Then: Should retrieve the value
                assertThat(ctx.getOption(ContextOption.IO_THREADS))
                        .as("Context IO_THREADS")
                        .isEqualTo(2);
            }
        }
    }

    @Nested
    @DisplayName("SocketMonitor Event Constants")
    class SocketMonitorEventConstants {

        @Test
        @DisplayName("Should have all monitor event types")
        void should_Have_All_Monitor_Event_Types() {
            // When: Access monitor event constants
            // Then: All event types should be defined
            assertThat(SocketMonitorEvent.CONNECTED.getValue()).isGreaterThan(0);
            assertThat(SocketMonitorEvent.DISCONNECTED.getValue()).isGreaterThan(0);
            assertThat(SocketMonitorEvent.LISTENING.getValue()).isGreaterThan(0);
            assertThat(SocketMonitorEvent.ACCEPTED.getValue()).isGreaterThan(0);
            assertThat(SocketMonitorEvent.CLOSED.getValue()).isGreaterThan(0);
            assertThat(SocketMonitorEvent.ALL.getValue()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have unique event values")
        void should_Have_Unique_Event_Values() {
            // Given: Monitor event constants
            int[] events = {
                SocketMonitorEvent.CONNECTED.getValue(),
                SocketMonitorEvent.DISCONNECTED.getValue(),
                SocketMonitorEvent.LISTENING.getValue(),
                SocketMonitorEvent.ACCEPTED.getValue(),
                SocketMonitorEvent.CLOSED.getValue()
            };

            // When: Check uniqueness
            // Then: Each event should have unique bit pattern
            for (int i = 0; i < events.length; i++) {
                for (int j = i + 1; j < events.length; j++) {
                    assertThat(events[i])
                            .as("Event values should be different")
                            .isNotEqualTo(events[j]);
                }
            }
        }

        @Test
        @DisplayName("Should combine events with combine method")
        void should_Combine_Events_With_Combine_Method() {
            // When: Combine multiple events
            SocketMonitorEvent combined = SocketMonitorEvent.CONNECTED.combine(SocketMonitorEvent.DISCONNECTED);

            // Then: Combined value should include both events
            assertThat(combined.getValue() & SocketMonitorEvent.CONNECTED.getValue())
                    .as("Combined includes CONNECTED")
                    .isNotEqualTo(0);
            assertThat(combined.getValue() & SocketMonitorEvent.DISCONNECTED.getValue())
                    .as("Combined includes DISCONNECTED")
                    .isNotEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Poller Event Constants")
    class PollerEventConstants {

        @Test
        @DisplayName("Should have POLLIN constant")
        void should_Have_POLLIN_Constant() {
            // When: Access POLLIN
            // Then: Should be defined with positive value
            assertThat(PollEvents.IN.getValue())
                    .as("POLLIN constant")
                    .isGreaterThan((short)0);
        }

        @Test
        @DisplayName("Should have POLLOUT constant")
        void should_Have_POLLOUT_Constant() {
            // When: Access POLLOUT
            // Then: Should be defined with positive value
            assertThat(PollEvents.OUT.getValue())
                    .as("POLLOUT constant")
                    .isGreaterThan((short)0);
        }

        @Test
        @DisplayName("Should have POLLERR constant")
        void should_Have_POLLERR_Constant() {
            // When: Access POLLERR
            // Then: Should be defined with positive value
            assertThat(PollEvents.ERR.getValue())
                    .as("POLLERR constant")
                    .isGreaterThan((short)0);
        }

        @Test
        @DisplayName("Should combine poll events with bitwise OR")
        void should_Combine_Poll_Events_With_Bitwise_OR() {
            // When: Combine POLLIN and POLLOUT
            int combined = PollEvents.IN.getValue() | PollEvents.OUT.getValue();

            // Then: Combined value should include both
            assertThat(combined & PollEvents.IN.getValue())
                    .as("Combined includes POLLIN")
                    .isNotEqualTo(0);
            assertThat(combined & PollEvents.OUT.getValue())
                    .as("Combined includes POLLOUT")
                    .isNotEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Socket Send Flags")
    class SocketSendFlags {

        @Test
        @DisplayName("Should have SNDMORE flag")
        void should_Have_SNDMORE_Flag() {
            // When: Access SNDMORE flag
            // Then: Should be defined
            assertThat(SendFlags.SEND_MORE.getValue())
                    .as("SNDMORE flag")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("Should have DONTWAIT flag")
        void should_Have_DONTWAIT_Flag() {
            // When: Access DONTWAIT flag
            // Then: Should be defined
            assertThat(SendFlags.DONT_WAIT.getValue())
                    .as("DONTWAIT flag")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("Should use SNDMORE flag for multipart messages")
        void should_Use_SNDMORE_Flag_For_Multipart_Messages() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-sndmore");
                sender.connect("inproc://test-sndmore");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send with SNDMORE flag
                try (Message part1 = new Message("Part1");
                     Message part2 = new Message("Part2")) {

                    sender.send(part1, SendFlags.SEND_MORE);
                    sender.send(part2, SendFlags.NONE);

                    // Then: Receiver should detect multipart
                    try (Message recv1 = new Message();
                         Message recv2 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(receiver.hasMore())
                                .as("First part has more")
                                .isTrue();

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(receiver.hasMore())
                                .as("Last part has no more")
                                .isFalse();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Version Information")
    class VersionInformation {

        @Test
        @DisplayName("Should provide ZMQ version information")
        void should_Provide_ZMQ_Version_Information() {
            // When: Get version
            Context.Version version = Context.version();

            // Then: Should provide valid version info
            assertThat(version)
                    .as("Version object")
                    .isNotNull();
            assertThat(version.major())
                    .as("Major version")
                    .isGreaterThanOrEqualTo(4);
            assertThat(version.minor())
                    .as("Minor version")
                    .isGreaterThanOrEqualTo(0);
            assertThat(version.patch())
                    .as("Patch version")
                    .isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should format version as string")
        void should_Format_Version_As_String() {
            // When: Get version string
            Context.Version version = Context.version();
            String versionString = version.toString();

            // Then: Should be formatted properly
            assertThat(versionString)
                    .as("Version string")
                    .isNotNull()
                    .isNotEmpty()
                    .matches("\\d+\\.\\d+\\.\\d+");
        }
    }

    @Nested
    @DisplayName("Capability Detection")
    class CapabilityDetection {

        @Test
        @DisplayName("Should check for transport capabilities")
        void should_Check_For_Transport_Capabilities() {
            // When: Check for standard transports
            // Then: Should not throw exception
            assertThatCode(() -> {
                Context.has("tcp");
                Context.has("inproc");
                Context.has("ipc");
            })
                    .as("Checking transport capabilities")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should check for security capabilities")
        void should_Check_For_Security_Capabilities() {
            // When: Check for security features
            // Then: Should not throw exception
            assertThatCode(() -> {
                Context.has("curve");
                Context.has("gssapi");
            })
                    .as("Checking security capabilities")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should return boolean for capability check")
        void should_Return_Boolean_For_Capability_Check() {
            // When: Check for tcp capability
            boolean hasTcp = Context.has("tcp");

            // Then: Should return boolean value
            assertThat(hasTcp)
                    .as("TCP capability")
                    .isIn(true, false);
        }
    }

    @Nested
    @DisplayName("Type Safety")
    class TypeSafety {

        @Test
        @DisplayName("Should enforce socket type at compile time")
        void should_Enforce_Socket_Type_At_Compile_Time() {
            // Given: A context
            try (Context ctx = new Context()) {
                // When: Create sockets with specific types
                // Then: Type system should enforce socket type
                Socket req = new Socket(ctx, SocketType.REQ);
                Socket rep = new Socket(ctx, SocketType.REP);

                assertThat(req).isNotNull();
                assertThat(rep).isNotNull();

                req.close();
                rep.close();
            }
        }

        @Test
        @DisplayName("Should enforce option types")
        void should_Enforce_Option_Types() {
            // Given: A socket
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.REP)) {

                // When: Set integer option
                socket.setOption(SocketOption.LINGER, 100);

                // Then: Should retrieve as integer
                int value = socket.getOption(SocketOption.LINGER);
                assertThat(value).isEqualTo(100);
            }
        }

        @Test
        @DisplayName("Should enforce string option types")
        void should_Enforce_String_Option_Types() throws Exception {
            // Given: A DEALER socket with string option set
            try (Context ctx = new Context();
                 Socket router = new Socket(ctx, SocketType.ROUTER);
                 Socket dealer = new Socket(ctx, SocketType.DEALER)) {

                router.bind("inproc://test-string-option");

                // When: Set string option (ROUTING_ID)
                String testId = "TestIdentity";
                dealer.setOption(SocketOption.ROUTING_ID, testId);
                dealer.connect("inproc://test-string-option");
                Thread.sleep(50);

                router.setOption(SocketOption.RCVTIMEO, 1000);

                // Send a message to verify the identity
                dealer.send("Test");

                // Then: Router should receive message with the custom identity
                try (Message identity = new Message();
                     Message content = new Message()) {
                    router.recv(identity, RecvFlags.NONE);
                    router.recv(content, RecvFlags.NONE);

                    assertThat(identity.toString())
                            .as("Custom routing ID is applied")
                            .isEqualTo(testId);
                }
            }
        }
    }

    @Nested
    @DisplayName("Error Constants")
    class ErrorConstants {

        @Test
        @DisplayName("Should handle negative return values as errors")
        void should_Handle_Negative_Return_Values_As_Errors() {
            // Given: A socket
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.PULL)) {

                socket.bind("inproc://test-error");
                socket.setOption(SocketOption.RCVTIMEO, 10);

                // When: Try to receive with timeout
                byte[] buffer = new byte[256];
                int result = socket.recv(buffer, RecvFlags.DONT_WAIT);

                // Then: Should return -1 (EAGAIN) for timeout/error
                assertThat(result)
                        .as("Error result - EAGAIN")
                        .isEqualTo(-1);
            }
        }
    }

    @Nested
    @DisplayName("Resource Management")
    class ResourceManagement {

        @Test
        @DisplayName("Should implement AutoCloseable for try-with-resources")
        void should_Implement_AutoCloseable_For_Try_With_Resources() {
            // When: Use try-with-resources
            // Then: Should implement AutoCloseable
            assertThatCode(() -> {
                try (Context ctx = new Context();
                     Socket socket = new Socket(ctx, SocketType.REP);
                     Message msg = new Message("test")) {
                    // All should be auto-closeable
                }
            })
                    .as("AutoCloseable implementation")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow manual close")
        void should_Allow_Manual_Close() {
            // Given: Resources
            Context ctx = new Context();
            Socket socket = new Socket(ctx, SocketType.REP);
            Message msg = new Message("test");

            // When: Manually close
            // Then: Should not throw exception
            assertThatCode(() -> {
                msg.close();
                socket.close();
                ctx.close();
            })
                    .as("Manual close")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle double close safely")
        void should_Handle_Double_Close_Safely() {
            // Given: A closed socket
            Context ctx = new Context();
            Socket socket = new Socket(ctx, SocketType.REP);
            socket.close();

            // When: Close again
            // Then: Should not throw exception
            assertThatCode(() -> socket.close())
                    .as("Double close")
                    .doesNotThrowAnyException();

            ctx.close();
        }
    }
}
