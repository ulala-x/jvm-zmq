package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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

    @Nested
    @DisplayName("TrySend and TryRecv with Errno Checking")
    class TrySendAndTryRecvWithErrnoChecking {

        @Test
        @DisplayName("Should trySend with SEND_MORE flag (multipart transmission)")
        void should_TrySend_With_SendMore_Flag() throws Exception {
            // Given: A PUSH-PULL pair (simpler than ROUTER-DEALER for multipart testing)
            try (Context ctx = new Context();
                 Socket push = new Socket(ctx, SocketType.PUSH);
                 Socket pull = new Socket(ctx, SocketType.PULL)) {

                push.bind("inproc://test-sendmore");
                pull.connect("inproc://test-sendmore");
                Thread.sleep(100);

                // When: Send multipart using trySend with SEND_MORE flag
                boolean sent1 = push.trySend("part1".getBytes(StandardCharsets.UTF_8), SendFlags.SEND_MORE);
                boolean sent2 = push.trySend("part2".getBytes(StandardCharsets.UTF_8), SendFlags.NONE);

                // Then: Both sends should succeed
                assertThat(sent1).as("First part sent").isTrue();
                assertThat(sent2).as("Second part sent").isTrue();

                Thread.sleep(50);

                // And: Verify reception
                byte[] buffer = new byte[256];
                int n1 = pull.recv(buffer);
                assertThat(new String(buffer, 0, n1, StandardCharsets.UTF_8))
                        .as("First part received")
                        .isEqualTo("part1");
                assertThat(pull.hasMore())
                        .as("Has more parts after first")
                        .isTrue();

                int n2 = pull.recv(buffer);
                assertThat(new String(buffer, 0, n2, StandardCharsets.UTF_8))
                        .as("Second part received")
                        .isEqualTo("part2");
                assertThat(pull.hasMore())
                        .as("No more parts after second")
                        .isFalse();
            }
        }

        @Test
        @DisplayName("Should tryRecv return -1 for EAGAIN (no exception thrown)")
        void should_TryRecv_Return_Negative_One_For_EAGAIN() throws Exception {
            // Given: A PULL socket with no incoming messages
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.PULL)) {

                socket.bind("inproc://test-eagain");

                // When: Try to receive with no data
                byte[] buffer = new byte[256];
                int result = socket.tryRecv(buffer, RecvFlags.NONE);

                // Then: Should return -1, not throw exception
                assertThat(result)
                        .as("tryRecv should return -1 for EAGAIN")
                        .isEqualTo(-1);
            }
        }

        @Test
        @DisplayName("Should tryRecvString return null for EAGAIN")
        void should_TryRecvString_Return_Null_For_EAGAIN() throws Exception {
            // Given: A PULL socket with no incoming messages
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.PULL)) {

                socket.bind("inproc://test-eagain-string");

                // When: Try to receive with no data
                String result = socket.tryRecvString(RecvFlags.NONE);

                // Then: Should return null, not throw exception
                assertThat(result)
                        .as("tryRecvString should return null for EAGAIN")
                        .isNull();
            }
        }

        @Test
        @DisplayName("Should tryRecvBytes return null for EAGAIN")
        void should_TryRecvBytes_Return_Null_For_EAGAIN() throws Exception {
            // Given: A PULL socket with no incoming messages
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.PULL)) {

                socket.bind("inproc://test-eagain-bytes");

                // When: Try to receive with no data
                byte[] result = socket.tryRecvBytes(RecvFlags.NONE);

                // Then: Should return null, not throw exception
                assertThat(result)
                        .as("tryRecvBytes should return null for EAGAIN")
                        .isNull();
            }
        }

        @Test
        @DisplayName("Should tryRecvMultipart return null for EAGAIN")
        void should_TryRecvMultipart_Return_Null_For_EAGAIN() throws Exception {
            // Given: A PULL socket with no incoming messages
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.PULL)) {

                socket.bind("inproc://test-eagain-multipart");

                // When: Try to receive with no data
                MultipartMessage result = socket.tryRecvMultipart();

                // Then: Should return null, not throw exception
                assertThat(result)
                        .as("tryRecvMultipart should return null for EAGAIN")
                        .isNull();
            }
        }

        @Test
        @DisplayName("Should trySend and tryRecv work with flags")
        void should_TrySend_And_TryRecv_Work_With_Flags() throws Exception {
            // Given: A PUSH-PULL pair
            try (Context ctx = new Context();
                 Socket push = new Socket(ctx, SocketType.PUSH);
                 Socket pull = new Socket(ctx, SocketType.PULL)) {

                push.bind("inproc://test-flags");
                pull.connect("inproc://test-flags");
                Thread.sleep(100);

                // When: Send with NONE flags
                boolean sent = push.trySend("test".getBytes(StandardCharsets.UTF_8), SendFlags.NONE);
                assertThat(sent).as("Message sent").isTrue();

                Thread.sleep(50);

                // And: Receive with NONE flags
                byte[] buffer = new byte[256];
                int bytesReceived = pull.tryRecv(buffer, RecvFlags.NONE);

                // Then: Should receive the message successfully
                assertThat(bytesReceived)
                        .as("Bytes received")
                        .isGreaterThan(0);
                assertThat(new String(buffer, 0, bytesReceived, StandardCharsets.UTF_8))
                        .as("Received message content")
                        .isEqualTo("test");
            }
        }
    }
}
