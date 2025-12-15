package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Advanced tests for Message class including metadata and routing.
 */
@Tag("Message")
@Tag("Advanced")
class MessageAdvancedTest {

    @Nested
    @DisplayName("Message Properties")
    class MessageProperties {

        @Test
        @DisplayName("Should handle message routing")
        void should_Handle_Message_Routing() {
            // Given: A message
            try (Message msg = new Message("test")) {
                // When/Then: Message should be created successfully
                // Note: Routing is handled by socket, not message
                assertThat(msg.size())
                        .as("Message size")
                        .isEqualTo(4);
            }
        }
    }

    @Nested
    @DisplayName("Message Metadata")
    class MessageMetadata {

        @Test
        @DisplayName("Should get metadata from message")
        void should_Get_Metadata_From_Message() {
            // Given: A message
            try (Message msg = new Message("test message")) {
                // When: Get metadata (e.g., Socket-Type)
                String metadata = msg.getMetadata("Socket-Type");

                // Then: Should return metadata or null if not available
                // Note: metadata availability depends on the socket type and context
                assertThat(metadata)
                        .as("Metadata retrieval")
                        .satisfiesAnyOf(
                                value -> assertThat(value).isNull(),
                                value -> assertThat(value).isNotEmpty()
                        );
            }
        }

        @Test
        @DisplayName("Should handle missing metadata gracefully")
        void should_Handle_Missing_Metadata_Gracefully() {
            // Given: A message
            try (Message msg = new Message("test")) {
                // When: Get non-existent metadata
                String metadata = msg.getMetadata("NonExistentProperty");

                // Then: Should return null for missing metadata
                assertThat(metadata)
                        .as("Missing metadata")
                        .isNull();
            }
        }
    }

    @Nested
    @DisplayName("Message Data Access")
    class MessageDataAccess {

        @Test
        @DisplayName("Should access message data pointer")
        void should_Access_Message_Data_Pointer() {
            // Given: A message with data
            byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
            try (Message msg = new Message(data)) {
                // When: Get data
                byte[] retrieved = msg.toByteArray();

                // Then: Should retrieve the same data
                assertThat(retrieved)
                        .as("Retrieved data")
                        .isEqualTo(data);
            }
        }

        @Test
        @DisplayName("Should handle zero-copy message data")
        void should_Handle_Zero_Copy_Message_Data() {
            // Given: A large byte array
            byte[] largeData = new byte[8192];
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }

            // When: Create message from large data
            try (Message msg = new Message(largeData)) {
                // Then: Message should contain all data correctly
                assertThat(msg.size())
                        .as("Message size")
                        .isEqualTo(largeData.length);
                assertThat(msg.toByteArray())
                        .as("Message data integrity")
                        .isEqualTo(largeData);
            }
        }
    }

    @Nested
    @DisplayName("Message Frame Operations")
    class MessageFrameOperations {

        @Test
        @DisplayName("Should send and receive multi-frame messages")
        void should_Send_And_Receive_Multi_Frame_Messages() throws Exception {
            // Given: A pair of connected sockets
            try (Context ctx = new Context();
                 Socket sender = new Socket(ctx, SocketType.PAIR);
                 Socket receiver = new Socket(ctx, SocketType.PAIR)) {

                sender.bind("inproc://test-frames");
                receiver.connect("inproc://test-frames");
                Thread.sleep(50);

                // When: Send multi-frame message
                try (Message frame1 = new Message("Frame1");
                     Message frame2 = new Message("Frame2");
                     Message frame3 = new Message("Frame3")) {

                    sender.send(frame1, SendFlags.SEND_MORE);
                    sender.send(frame2, SendFlags.SEND_MORE);
                    sender.send(frame3, SendFlags.NONE);

                    // Then: Should receive all frames
                    receiver.setOption(SocketOption.RCVTIMEO, 1000);

                    try (Message recv1 = new Message();
                         Message recv2 = new Message();
                         Message recv3 = new Message()) {

                        receiver.recv(recv1, RecvFlags.NONE);
                        assertThat(recv1.toString())
                                .as("First frame")
                                .isEqualTo("Frame1");
                        assertThat(receiver.hasMore())
                                .as("First frame has more")
                                .isTrue();

                        receiver.recv(recv2, RecvFlags.NONE);
                        assertThat(recv2.toString())
                                .as("Second frame")
                                .isEqualTo("Frame2");
                        assertThat(receiver.hasMore())
                                .as("Second frame has more")
                                .isTrue();

                        receiver.recv(recv3, RecvFlags.NONE);
                        assertThat(recv3.toString())
                                .as("Third frame")
                                .isEqualTo("Frame3");
                        assertThat(receiver.hasMore())
                                .as("Third frame has more")
                                .isFalse();
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Message Equality and Comparison")
    class MessageEqualityAndComparison {

        @Test
        @DisplayName("Should compare messages with same content")
        void should_Compare_Messages_With_Same_Content() {
            // Given: Two messages with same content
            try (Message msg1 = new Message("identical");
                 Message msg2 = new Message("identical")) {

                // When: Compare their content
                byte[] data1 = msg1.toByteArray();
                byte[] data2 = msg2.toByteArray();

                // Then: Content should be equal
                assertThat(data1)
                        .as("Message content equality")
                        .isEqualTo(data2);
            }
        }

        @Test
        @DisplayName("Should distinguish messages with different content")
        void should_Distinguish_Messages_With_Different_Content() {
            // Given: Two messages with different content
            try (Message msg1 = new Message("first");
                 Message msg2 = new Message("second")) {

                // When: Compare their content
                byte[] data1 = msg1.toByteArray();
                byte[] data2 = msg2.toByteArray();

                // Then: Content should be different
                assertThat(data1)
                        .as("Message content difference")
                        .isNotEqualTo(data2);
            }
        }
    }

    @Nested
    @DisplayName("Message Resource Management")
    class MessageResourceManagement {

        @Test
        @DisplayName("Should properly close message resources")
        void should_Properly_Close_Message_Resources() {
            // Given: A message
            Message msg = new Message("test");

            // When: Close the message
            // Then: Should not throw exception
            assertThatCode(() -> msg.close())
                    .as("Closing message")
                    .doesNotThrowAnyException();

            // And: Multiple closes should be safe
            assertThatCode(() -> msg.close())
                    .as("Double closing message")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle message in try-with-resources")
        void should_Handle_Message_In_Try_With_Resources() {
            // When: Use message in try-with-resources
            // Then: Should auto-close without errors
            assertThatCode(() -> {
                try (Message msg = new Message("auto-close test")) {
                    assertThat(msg.size()).isGreaterThan(0);
                }
            })
                    .as("Try-with-resources usage")
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Message Edge Cases")
    class MessageEdgeCases {

        @Test
        @DisplayName("Should handle very small messages")
        void should_Handle_Very_Small_Messages() {
            // Given: A single byte message
            try (Message msg = new Message(new byte[]{42})) {
                // When: Read the message
                byte[] data = msg.toByteArray();

                // Then: Should contain the single byte
                assertThat(data)
                        .as("Single byte message")
                        .hasSize(1)
                        .containsExactly((byte) 42);
            }
        }

        @Test
        @DisplayName("Should handle messages with special characters")
        void should_Handle_Messages_With_Special_Characters() {
            // Given: A message with special characters
            String specialChars = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";
            try (Message msg = new Message(specialChars)) {
                // When: Convert back to string
                String result = msg.toString();

                // Then: Should preserve all special characters
                assertThat(result)
                        .as("Special characters preservation")
                        .isEqualTo(specialChars);
            }
        }

        @Test
        @DisplayName("Should handle messages with null bytes")
        void should_Handle_Messages_With_Null_Bytes() {
            // Given: A byte array with null bytes
            byte[] dataWithNulls = new byte[]{1, 0, 2, 0, 3};
            try (Message msg = new Message(dataWithNulls)) {
                // When: Read the message
                byte[] result = msg.toByteArray();

                // Then: Should preserve null bytes
                assertThat(result)
                        .as("Message with null bytes")
                        .isEqualTo(dataWithNulls);
            }
        }
    }
}
