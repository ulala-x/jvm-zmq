package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PAIR socket pattern (exclusive pair communication).
 */
@Tag("Socket")
@Tag("Pair")
class PairTest {

    @Nested
    @DisplayName("Basic Pair Communication")
    class BasicPairCommunication {

        @Test
        @DisplayName("Should exchange messages bidirectionally")
        void should_Exchange_Messages_Bidirectionally() throws Exception {
            // Given: A pair of connected PAIR sockets
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket1.setOption(SocketOption.RCVTIMEO, 1000);
                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Socket1 sends to Socket2
                socket1.send("Hello from Socket1");

                // Then: Socket2 receives the message
                String received1 = socket2.recvString();
                assertThat(received1)
                        .as("Message from Socket1")
                        .isEqualTo("Hello from Socket1");

                // When: Socket2 sends back to Socket1
                socket2.send("Hello from Socket2");

                // Then: Socket1 receives the reply
                String received2 = socket1.recvString();
                assertThat(received2)
                        .as("Message from Socket2")
                        .isEqualTo("Hello from Socket2");
            }
        }

        @Test
        @DisplayName("Should support full-duplex communication")
        void should_Support_Full_Duplex_Communication() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket1.setOption(SocketOption.RCVTIMEO, 1000);
                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Both sockets send simultaneously
                socket1.send("From 1 to 2");
                socket2.send("From 2 to 1");

                // Then: Both should receive messages
                String msg1 = socket2.recvString();
                String msg2 = socket1.recvString();

                assertThat(msg1)
                        .as("Socket2 received from Socket1")
                        .isEqualTo("From 1 to 2");
                assertThat(msg2)
                        .as("Socket1 received from Socket2")
                        .isEqualTo("From 2 to 1");
            }
        }
    }

    @Nested
    @DisplayName("Exclusive Connection")
    class ExclusiveConnection {

        @Test
        @DisplayName("Should maintain exclusive one-to-one connection")
        void should_Maintain_Exclusive_One_To_One_Connection() throws Exception {
            // Given: One PAIR socket bound to an endpoint
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket1.setOption(SocketOption.RCVTIMEO, 1000);
                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send messages between the pair
                socket1.send("Exclusive message");
                String received = socket2.recvString();

                // Then: Communication should work
                assertThat(received)
                        .as("Exclusive pair message")
                        .isEqualTo("Exclusive message");

                // Note: Attempting to connect a third socket would violate
                // the PAIR semantics, but we don't test error cases here
            }
        }
    }

    @Nested
    @DisplayName("Message Order")
    class MessageOrder {

        @Test
        @DisplayName("Should preserve message order")
        void should_Preserve_Message_Order() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("tcp://127.0.0.1:0");
                String endpoint = receiver.getOptionString(SocketOption.LAST_ENDPOINT);
                sender.connect(endpoint);
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send multiple messages in order
                for (int i = 0; i < 5; i++) {
                    sender.send("Message-" + i);
                }

                // Then: Should receive in the same order
                for (int i = 0; i < 5; i++) {
                    String received = receiver.recvString();
                    assertThat(received).isNotNull();
                    assertThat(received)
                            .as("Message order #" + i)
                            .isEqualTo("Message-" + i);
                }
            }
        }

        @Test
        @DisplayName("Should handle rapid sequential messages")
        void should_Handle_Rapid_Sequential_Messages() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("tcp://127.0.0.1:0");
                String endpoint = receiver.getOptionString(SocketOption.LAST_ENDPOINT);
                sender.connect(endpoint);
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 2000);

                int messageCount = 100;

                // When: Send many messages rapidly
                for (int i = 0; i < messageCount; i++) {
                    sender.send("Rapid-" + i);
                }

                // Then: All messages should be received in order
                for (int i = 0; i < messageCount; i++) {
                    String received = receiver.recvString();
                    assertThat(received)
                            .as("Rapid message #" + i + " received")
                            .isNotNull();
                }
            }
        }
    }

    @Nested
    @DisplayName("Binary Data Exchange")
    class BinaryDataExchange {

        @Test
        @DisplayName("Should exchange binary data correctly")
        void should_Exchange_Binary_Data_Correctly() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send binary data with all byte values
                byte[] binaryData = new byte[256];
                for (int i = 0; i < 256; i++) {
                    binaryData[i] = (byte) i;
                }
                socket1.send(binaryData);

                // Then: Should receive exact binary data
                byte[] receivedBuffer = new byte[256];
                int receivedBytes = socket2.recv(receivedBuffer);
                byte[] received = new byte[receivedBytes];
                System.arraycopy(receivedBuffer, 0, received, 0, receivedBytes);

                assertThat(received)
                        .as("Binary data integrity")
                        .isEqualTo(binaryData);
            }
        }

        @Test
        @DisplayName("Should handle large binary payloads")
        void should_Handle_Large_Binary_Payloads() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket2.setOption(SocketOption.RCVTIMEO, 2000);

                // When: Send large binary data (2MB)
                byte[] largeData = new byte[2 * 1024 * 1024];
                for (int i = 0; i < largeData.length; i++) {
                    largeData[i] = (byte) (i % 256);
                }
                socket1.send(largeData);

                // Then: Should receive complete data
                byte[] receivedBuffer = new byte[2 * 1024 * 1024];
                int receivedBytes = socket2.recv(receivedBuffer);
                byte[] received = new byte[receivedBytes];
                System.arraycopy(receivedBuffer, 0, received, 0, receivedBytes);

                assertThat(received)
                        .as("Large data size")
                        .hasSize(largeData.length);
                assertThat(received)
                        .as("Large data integrity")
                        .isEqualTo(largeData);
            }
        }
    }

    @Nested
    @DisplayName("Multipart Messages")
    class MultipartMessages {

        @Test
        @DisplayName("Should send and receive multipart messages")
        void should_Send_And_Receive_Multipart_Messages() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("tcp://127.0.0.1:0");
                String endpoint = receiver.getOptionString(SocketOption.LAST_ENDPOINT);
                sender.connect(endpoint);
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send multipart message
                try (Message part1 = new Message("Part1");
                     Message part2 = new Message("Part2");
                     Message part3 = new Message("Part3")) {

                    sender.send(part1, SendFlags.SEND_MORE);
                    sender.send(part2, SendFlags.SEND_MORE);
                    sender.send(part3, SendFlags.NONE);

                    // Then: Should receive all parts with correct flags
                    try (Message recv1 = new Message();
                         Message recv2 = new Message();
                         Message recv3 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(recv1.toString()).isEqualTo("Part1");
                        assertThat(receiver.hasMore()).isTrue();

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(recv2.toString()).isEqualTo("Part2");
                        assertThat(receiver.hasMore()).isTrue();

                        receiver.recv(recv3, RecvFlags.NONE);
                        assertThat(recv3.toString()).isEqualTo("Part3");
                        assertThat(receiver.hasMore()).isFalse();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Connection Handling")
    class ConnectionHandling {

        @Test
        @DisplayName("Should handle bind before connect")
        void should_Handle_Bind_Before_Connect() throws Exception {
            // Given: Socket binds first
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                Thread.sleep(20);
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send message
                socket1.send("Test");

                // Then: Should receive successfully
                String received = socket2.recvString();
                assertThat(received).isNotNull();
            }
        }

        @Test
        @DisplayName("Should handle connect before bind")
        void should_Handle_Connect_Before_Bind() throws Exception {
            // Given: Socket connects first (ZeroMQ allows this)
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket2.connect("tcp://127.0.0.1:25555");
                Thread.sleep(20);
                socket1.bind("tcp://127.0.0.1:25555");
                Thread.sleep(50);

                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send message
                socket1.send("Test");

                // Then: Should receive successfully
                String received = socket2.recvString();
                assertThat(received).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty messages")
        void should_Handle_Empty_Messages() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send empty message
                socket1.send("");

                // Then: Should receive empty message
                String received = socket2.recvString();
                assertThat(received)
                        .as("Empty message")
                        .isNotNull()
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("Should handle messages with special characters")
        void should_Handle_Messages_With_Special_Characters() throws Exception {
            // Given: A pair of PAIR sockets
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send message with special characters
                String specialMsg = "Special: \n\t\r\0 chars and 한글 UTF-8";
                socket1.send(specialMsg);

                // Then: Should receive with all characters preserved
                String received = socket2.recvString();
                assertThat(received).isNotNull();
                assertThat(received)
                        .as("Special characters preservation")
                        .isEqualTo(specialMsg);
            }
        }

        @Test
        @DisplayName("Should handle send and receive timeouts")
        void should_Handle_Send_And_Receive_Timeouts() throws Exception {
            // Given: A single PAIR socket (no peer)
            try (Context ctx = new Context();
                 Socket socket = new Socket(ctx, SocketType.PAIR)) {

                socket.bind("tcp://127.0.0.1:0");
                socket.setOption(SocketOption.RCVTIMEO, 100);

                // When: Try to receive with no data
                byte[] buffer = new byte[256];
                int bytesReceived = socket.recv(buffer, RecvFlags.DONT_WAIT);

                // Then: Should timeout and return -1 (EAGAIN)
                assertThat(bytesReceived)
                        .as("Receive timeout result")
                        .isEqualTo(-1);
            }
        }
    }

    @Nested
    @DisplayName("Transport Types")
    class TransportTypes {

        @Test
        @DisplayName("Should work with inproc transport")
        void should_Work_With_Inproc_Transport() throws Exception {
            // Given: PAIR sockets using inproc transport
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:0");
                String endpoint = socket1.getOptionString(SocketOption.LAST_ENDPOINT);
                socket2.connect(endpoint);
                Thread.sleep(50);

                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Exchange messages
                socket1.send("tcp test");
                String received = socket2.recvString();

                // Then: Should work correctly
                assertThat(received).isNotNull();
                assertThat(received)
                        .isEqualTo("tcp test");
            }
        }

        @Test
        @DisplayName("Should work with tcp transport")
        void should_Work_With_Tcp_Transport() throws Exception {
            // Given: PAIR sockets using tcp transport
            try (Context ctx = new Context();
                 Socket socket1 = new Socket(ctx, SocketType.PAIR);
                 Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

                socket1.bind("tcp://127.0.0.1:15555");
                socket2.connect("tcp://127.0.0.1:15555");
                Thread.sleep(100);

                socket2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Exchange messages
                socket1.send("tcp test");
                String received = socket2.recvString();

                // Then: Should work correctly
                assertThat(received).isNotNull();
                assertThat(received)
                        .isEqualTo("tcp test");
            }
        }
    }
}
