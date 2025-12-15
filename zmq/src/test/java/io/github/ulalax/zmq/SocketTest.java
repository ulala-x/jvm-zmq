package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Socket class.
 */
@Tag("Socket")
class SocketTest {

    @Nested
    @DisplayName("Socket Creation")
    class SocketCreation {

        @Test
        @DisplayName("Should create socket with specified type")
        void should_Create_Socket_With_Specified_Type() {
            // Given: A context
            try (Context ctx = new Context();
                 // When: Create a REQ socket
                 Socket socket = new Socket(ctx, SocketType.REQ)) {

                // Then: Socket should be created successfully
                assertThat(socket).isNotNull();
            }
        }

        @Test
        @DisplayName("Should create all socket types successfully")
        void should_Create_All_Socket_Types_Successfully() {
            // Given: A context
            try (Context ctx = new Context()) {
                // When: Create sockets of all types
                // Then: All socket types should be created without errors
                for (SocketType type : SocketType.values()) {
                    assertThatCode(() -> {
                        try (Socket socket = new Socket(ctx, type)) {
                            assertThat(socket).isNotNull();
                        }
                    })
                            .as("Creating socket type: " + type)
                            .doesNotThrowAnyException();
                }
            }
        }
    }

    @Nested
    @DisplayName("Socket Options")
    class SocketOptions {

        @Test
        @DisplayName("Should set and get linger option")
        void should_Set_And_Get_Linger_Option() {
            // Given: A socket
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.REQ)) {

                // When: Set linger to 0
                socket.setOption(SocketOption.LINGER, 0);

                // Then: Should retrieve the set value
                assertThat(socket.getOption(SocketOption.LINGER))
                        .as("Linger option")
                        .isEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should set and get send timeout")
        void should_Set_And_Get_Send_Timeout() {
            // Given: A socket
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.REQ)) {

                // When: Set send timeout to 1000ms
                socket.setOption(SocketOption.SNDTIMEO, 1000);

                // Then: Should retrieve the set value
                assertThat(socket.getOption(SocketOption.SNDTIMEO))
                        .as("Send timeout")
                        .isEqualTo(1000);
            }
        }

        @Test
        @DisplayName("Should set and get receive timeout")
        void should_Set_And_Get_Receive_Timeout() {
            // Given: A socket
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.REQ)) {

                // When: Set receive timeout to 1000ms
                socket.setOption(SocketOption.RCVTIMEO, 1000);

                // Then: Should retrieve the set value
                assertThat(socket.getOption(SocketOption.RCVTIMEO))
                        .as("Receive timeout")
                        .isEqualTo(1000);
            }
        }
    }

    @Nested
    @DisplayName("Socket Binding")
    class SocketBinding {

        @Test
        @DisplayName("Should bind socket to endpoint")
        void should_Bind_Socket_To_Endpoint() {
            // Given: A REP socket
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.REP)) {

                // When: Bind to inproc endpoint
                // Then: Should not throw exception
                assertThatCode(() -> socket.bind("inproc://test-bind"))
                        .as("Binding to endpoint")
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should unbind socket from endpoint")
        void should_Unbind_Socket_From_Endpoint() {
            // Given: A bound socket
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.REP)) {

                socket.bind("inproc://test-unbind");

                // When: Unbind from endpoint
                // Then: Should not throw exception
                assertThatCode(() -> socket.unbind("inproc://test-unbind"))
                        .as("Unbinding from endpoint")
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("Socket Connection")
    class SocketConnection {

        @Test
        @DisplayName("Should connect socket to endpoint")
        void should_Connect_Socket_To_Endpoint() {
            // Given: A server socket bound to an endpoint
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.bind("inproc://test-connect");

                // When: Connect client to endpoint
                // Then: Should not throw exception
                assertThatCode(() -> client.connect("inproc://test-connect"))
                        .as("Connecting to endpoint")
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should disconnect socket from endpoint")
        void should_Disconnect_Socket_From_Endpoint() throws Exception {
            // Given: A connected socket
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.bind("inproc://test-disconnect");
                client.connect("inproc://test-disconnect");

                // Give time for connection
                Thread.sleep(50);

                // When: Disconnect from endpoint
                // Then: Should not throw exception
                assertThatCode(() -> client.disconnect("inproc://test-disconnect"))
                        .as("Disconnecting from endpoint")
                        .doesNotThrowAnyException();
            }
        }
    }
}
