package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.ZmqException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for multipart message handling across various socket patterns.
 */
@Tag("Message")
@Tag("Multipart")
class MultipartMessageTest {

    @Nested
    @DisplayName("Basic Multipart Operations")
    class BasicMultipartOperations {

        @Test
        @DisplayName("Should send and receive two-part message")
        void should_Send_And_Receive_Two_Part_Message() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-two-part");
                sender.connect("inproc://test-two-part");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send two-part message
                try (Message part1 = new Message("Header");
                     Message part2 = new Message("Body")) {

                    sender.send(part1, SendFlags.SEND_MORE);
                    sender.send(part2, SendFlags.NONE);

                    // Then: Should receive both parts
                    try (Message recv1 = new Message();
                         Message recv2 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(recv1.toString())
                                .as("First part")
                                .isEqualTo("Header");
                        assertThat(receiver.hasMore())
                                .as("First part has more")
                                .isTrue();

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(recv2.toString())
                                .as("Second part")
                                .isEqualTo("Body");
                        assertThat(receiver.hasMore())
                                .as("Second part has more")
                                .isFalse();
                    }
                }
            }
        }

        @Test
        @DisplayName("Should send and receive multi-frame message")
        void should_Send_And_Receive_Multi_Frame_Message() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-multi-frame");
                sender.connect("inproc://test-multi-frame");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send five-part message
                String[] parts = {"Part1", "Part2", "Part3", "Part4", "Part5"};
                for (int i = 0; i < parts.length; i++) {
                    try (Message msg = new Message(parts[i])) {
                        SendFlags flags = (i < parts.length - 1) ? SendFlags.SEND_MORE : SendFlags.NONE;
                        sender.send(msg, flags);
                    }
                }

                // Then: Should receive all five parts
                List<String> received = new ArrayList<>();
                for (int i = 0; i < parts.length; i++) {
                    try (Message msg = new Message()) {
                        receiver.recv(msg, RecvFlags.NONE);
                        received.add(msg.toString());

                        boolean expectedMore = (i < parts.length - 1);
                        assertThat(receiver.hasMore())
                                .as("Part " + i + " has more flag")
                                .isEqualTo(expectedMore);
                    }
                }

                assertThat(received)
                        .as("All parts received")
                        .containsExactly(parts);
            }
        }
    }

    @Nested
    @DisplayName("Multipart with Different Patterns")
    class MultipartWithDifferentPatterns {

        @Test
        @DisplayName("Should handle multipart messages in REQ-REP pattern")
        void should_Handle_Multipart_Messages_In_REQ_REP_Pattern() throws Exception {
            // Given: A REQ-REP connection
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.bind("inproc://test-reqrep-multipart");
                client.connect("inproc://test-reqrep-multipart");
                Thread.sleep(50);

                server.setOption(SocketOption.RCVTIMEO, 1000);
                client.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Client sends multipart request
                try (Message reqPart1 = new Message("RequestID");
                     Message reqPart2 = new Message("RequestData")) {

                    client.send(reqPart1, SendFlags.SEND_MORE);
                    client.send(reqPart2, SendFlags.NONE);

                    // Server receives multipart request
                    try (Message recvPart1 = new Message();
                         Message recvPart2 = new Message()) {

                        server.recv(recvPart1, RecvFlags.NONE);
                        assertThat(recvPart1.toString()).isEqualTo("RequestID");
                        assertThat(server.hasMore()).isTrue();

                        server.recv(recvPart2, RecvFlags.NONE);
                        assertThat(recvPart2.toString()).isEqualTo("RequestData");
                        assertThat(server.hasMore()).isFalse();

                        // Then: Server sends multipart reply
                        try (Message repPart1 = new Message("ResponseID");
                             Message repPart2 = new Message("ResponseData")) {

                            server.send(repPart1, SendFlags.SEND_MORE);
                            server.send(repPart2, SendFlags.NONE);

                            // Client receives multipart reply
                            try (Message clientRecv1 = new Message();
                                 Message clientRecv2 = new Message()) {

                                client.recv(clientRecv1, RecvFlags.NONE);
                                assertThat(clientRecv1.toString()).isEqualTo("ResponseID");

                                client.recv(clientRecv2, RecvFlags.NONE);
                                assertThat(clientRecv2.toString()).isEqualTo("ResponseData");
                            }
                        }
                    }
                }
            }
        }

        @Test
        @DisplayName("Should handle multipart messages in PUSH-PULL pattern")
        void should_Handle_Multipart_Messages_In_PUSH_PULL_Pattern() throws Exception {
            // Given: A PUSH-PULL connection
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-pushpull-multipart");
                pusher.connect("inproc://test-pushpull-multipart");
                Thread.sleep(50);

                puller.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Push multipart work item
                try (Message header = new Message("WorkHeader");
                     Message payload = new Message("WorkPayload")) {

                    pusher.send(header, SendFlags.SEND_MORE);
                    pusher.send(payload, SendFlags.NONE);

                    // Then: Pull should receive complete multipart message
                    try (Message recvHeader = new Message();
                         Message recvPayload = new Message()) {

                        puller.recv(recvHeader, RecvFlags.NONE);
                        assertThat(recvHeader.toString()).isEqualTo("WorkHeader");
                        assertThat(puller.hasMore()).isTrue();

                        puller.recv(recvPayload, RecvFlags.NONE);
                        assertThat(recvPayload.toString()).isEqualTo("WorkPayload");
                        assertThat(puller.hasMore()).isFalse();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Multipart Message Integrity")
    class MultipartMessageIntegrity {

        @Test
        @DisplayName("Should preserve message boundaries in multipart")
        void should_Preserve_Message_Boundaries_In_Multipart() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-boundaries");
                sender.connect("inproc://test-boundaries");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send two separate multipart messages
                try (Message msg1p1 = new Message("Message1-Part1");
                     Message msg1p2 = new Message("Message1-Part2");
                     Message msg2p1 = new Message("Message2-Part1");
                     Message msg2p2 = new Message("Message2-Part2")) {

                    // First multipart message
                    sender.send(msg1p1, SendFlags.SEND_MORE);
                    sender.send(msg1p2, SendFlags.NONE);

                    // Second multipart message
                    sender.send(msg2p1, SendFlags.SEND_MORE);
                    sender.send(msg2p2, SendFlags.NONE);

                    // Then: Should receive both messages with correct boundaries
                    try (Message recv1p1 = new Message();
                         Message recv1p2 = new Message();
                         Message recv2p1 = new Message();
                         Message recv2p2 = new Message()) {

                        // First message
                        receiver.recv(recv1p1, RecvFlags.NONE);
                        assertThat(recv1p1.toString()).isEqualTo("Message1-Part1");
                        assertThat(receiver.hasMore()).isTrue();

                        receiver.recv(recv1p2, RecvFlags.NONE);
                        assertThat(recv1p2.toString()).isEqualTo("Message1-Part2");
                        assertThat(receiver.hasMore()).isFalse();

                        // Second message
                        receiver.recv(recv2p1, RecvFlags.NONE);
                        assertThat(recv2p1.toString()).isEqualTo("Message2-Part1");
                        assertThat(receiver.hasMore()).isTrue();

                        receiver.recv(recv2p2, RecvFlags.NONE);
                        assertThat(recv2p2.toString()).isEqualTo("Message2-Part2");
                        assertThat(receiver.hasMore()).isFalse();
                    }
                }
            }
        }

        @Test
        @DisplayName("Should handle empty parts in multipart message")
        void should_Handle_Empty_Parts_In_Multipart_Message() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-empty-parts");
                sender.connect("inproc://test-empty-parts");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send multipart with empty frames
                try (Message part1 = new Message("");
                     Message part2 = new Message("Data");
                     Message part3 = new Message("")) {

                    sender.send(part1, SendFlags.SEND_MORE);
                    sender.send(part2, SendFlags.SEND_MORE);
                    sender.send(part3, SendFlags.NONE);

                    // Then: Should receive all parts including empty ones
                    try (Message recv1 = new Message();
                         Message recv2 = new Message();
                         Message recv3 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(recv1.size()).isEqualTo(0);
                        assertThat(receiver.hasMore()).isTrue();

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(recv2.toString()).isEqualTo("Data");
                        assertThat(receiver.hasMore()).isTrue();

                        receiver.recv(recv3, RecvFlags.NONE);
                        assertThat(recv3.size()).isEqualTo(0);
                        assertThat(receiver.hasMore()).isFalse();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Binary Multipart Messages")
    class BinaryMultipartMessages {

        @Test
        @DisplayName("Should handle binary data in multipart frames")
        void should_Handle_Binary_Data_In_Multipart_Frames() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-binary-multipart");
                sender.connect("inproc://test-binary-multipart");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send multipart with binary data
                byte[] binaryPart1 = new byte[]{0x00, 0x01, 0x02, (byte) 0xFF};
                byte[] binaryPart2 = new byte[]{0x10, 0x20, 0x30, 0x40};

                try (Message msg1 = new Message(binaryPart1);
                     Message msg2 = new Message(binaryPart2)) {

                    sender.send(msg1, SendFlags.SEND_MORE);
                    sender.send(msg2, SendFlags.NONE);

                    // Then: Should receive exact binary data
                    try (Message recv1 = new Message();
                         Message recv2 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(recv1.toByteArray()).isEqualTo(binaryPart1);
                        assertThat(receiver.hasMore()).isTrue();

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(recv2.toByteArray()).isEqualTo(binaryPart2);
                        assertThat(receiver.hasMore()).isFalse();
                    }
                }
            }
        }

        @Test
        @DisplayName("Should handle mixed text and binary parts")
        void should_Handle_Mixed_Text_And_Binary_Parts() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-mixed-parts");
                sender.connect("inproc://test-mixed-parts");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Send mixed text and binary parts
                String textPart = "TextHeader";
                byte[] binaryPart = new byte[]{0x01, 0x02, 0x03, 0x04};

                try (Message msg1 = new Message(textPart);
                     Message msg2 = new Message(binaryPart)) {

                    sender.send(msg1, SendFlags.SEND_MORE);
                    sender.send(msg2, SendFlags.NONE);

                    // Then: Should receive both correctly
                    try (Message recv1 = new Message();
                         Message recv2 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(recv1.toString()).isEqualTo(textPart);

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(recv2.toByteArray()).isEqualTo(binaryPart);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Multipart Error Handling")
    class MultipartErrorHandling {

        @Test
        @org.junit.jupiter.api.Disabled("Timing issue - incomplete multipart detection is implementation-specific")
        @DisplayName("Should detect incomplete multipart message")
        void should_Detect_Incomplete_Multipart_Message() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-incomplete");
                sender.connect("inproc://test-incomplete");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 500);

                // When: Send complete multipart, then incomplete multipart
                try (Message part1 = new Message("Complete-Part1");
                     Message part2 = new Message("Complete-Part2")) {
                    // First send a complete multipart message
                    sender.send(part1, SendFlags.SEND_MORE);
                    sender.send(part2, SendFlags.NONE);

                    // Then: Receiver should receive both parts
                    try (Message recv1 = new Message();
                         Message recv2 = new Message()) {
                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(receiver.hasMore())
                                .as("First part has more flag")
                                .isTrue();

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(receiver.hasMore())
                                .as("Last part has no more flag")
                                .isFalse();
                    }
                }

                // Now send an incomplete multipart and close sender to trigger error
                try (Message incomplete = new Message("Incomplete-Part1")) {
                    sender.send(incomplete, SendFlags.SEND_MORE);
                    sender.close();

                    // Receiver detects incomplete message
                    try (Message recv = new Message()) {
                        receiver.recv(recv, RecvFlags.NONE);
                        assertThat(receiver.hasMore())
                                .as("Incomplete message has more flag")
                                .isTrue();

                        // Trying to receive next part should timeout or fail
                        try (Message recv2 = new Message()) {
                            assertThatThrownBy(() -> receiver.recv(recv2, RecvFlags.NONE))
                                    .as("Receiving incomplete message part should fail")
                                    .isInstanceOf(ZmqException.class);
                        }
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Large Multipart Messages")
    class LargeMultipartMessages {

        @Test
        @DisplayName("Should handle many parts in single message")
        void should_Handle_Many_Parts_In_Single_Message() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-many-parts");
                sender.connect("inproc://test-many-parts");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 2000);

                int partCount = 50;

                // When: Send message with many parts
                for (int i = 0; i < partCount; i++) {
                    try (Message part = new Message("Part-" + i)) {
                        SendFlags flags = (i < partCount - 1) ? SendFlags.SEND_MORE : SendFlags.NONE;
                        sender.send(part, flags);
                    }
                }

                // Then: Should receive all parts
                for (int i = 0; i < partCount; i++) {
                    try (Message recv = new Message()) {
                        receiver.recv(recv, RecvFlags.NONE);
                        assertThat(recv.toString()).isEqualTo("Part-" + i);

                        boolean expectedMore = (i < partCount - 1);
                        assertThat(receiver.hasMore()).isEqualTo(expectedMore);
                    }
                }
            }
        }

        @Test
        @DisplayName("Should handle large data in multipart frames")
        void should_Handle_Large_Data_In_Multipart_Frames() throws Exception {
            // Given: A PAIR socket connection
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                receiver.bind("inproc://test-large-frames");
                sender.connect("inproc://test-large-frames");
                Thread.sleep(50);

                receiver.setOption(SocketOption.RCVTIMEO, 2000);

                // When: Send multipart message with large frames (100KB each)
                byte[] largePart1 = new byte[100 * 1024];
                byte[] largePart2 = new byte[100 * 1024];

                for (int i = 0; i < largePart1.length; i++) {
                    largePart1[i] = (byte) (i % 256);
                    largePart2[i] = (byte) ((i + 128) % 256);
                }

                try (Message msg1 = new Message(largePart1);
                     Message msg2 = new Message(largePart2)) {

                    sender.send(msg1, SendFlags.SEND_MORE);
                    sender.send(msg2, SendFlags.NONE);

                    // Then: Should receive complete large frames
                    try (Message recv1 = new Message();
                         Message recv2 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(recv1.size()).isEqualTo(largePart1.length);
                        assertThat(recv1.toByteArray()).isEqualTo(largePart1);

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(recv2.size()).isEqualTo(largePart2.length);
                        assertThat(recv2.toByteArray()).isEqualTo(largePart2);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Multipart with PUB-SUB")
    class MultipartWithPubSub {

        @Test
        @DisplayName("Should handle multipart messages in PUB-SUB pattern")
        void should_Handle_Multipart_Messages_In_PUB_SUB_Pattern() throws Exception {
            // Given: A PUB-SUB connection
            try (Context ctx = new Context();
                 Socket pub = new Socket(ctx, SocketType.PUB);
                 Socket sub = new Socket(ctx, SocketType.SUB)) {

                pub.bind("inproc://test-pubsub-multipart");
                sub.subscribe("");
                sub.connect("inproc://test-pubsub-multipart");
                Thread.sleep(100);

                sub.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Publish multipart message
                try (Message topic = new Message("topic.test");
                     Message payload = new Message("Payload data")) {

                    pub.send(topic, SendFlags.SEND_MORE);
                    pub.send(payload, SendFlags.NONE);

                    // Then: Subscriber should receive complete multipart message
                    try (Message recvTopic = new Message();
                         Message recvPayload = new Message()) {

                        sub.recv(recvTopic, RecvFlags.NONE);
                        assertThat(recvTopic.toString()).isEqualTo("topic.test");
                        assertThat(sub.hasMore()).isTrue();

                        sub.recv(recvPayload, RecvFlags.NONE);
                        assertThat(recvPayload.toString()).isEqualTo("Payload data");
                        assertThat(sub.hasMore()).isFalse();
                    }
                }
            }
        }
    }
}
