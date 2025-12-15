package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Message class.
 */
@Tag("Message")
class MessageTest {

    @Nested
    @DisplayName("Message Construction")
    class MessageConstruction {

        @Test
        @DisplayName("Should create empty message with default constructor")
        void should_Create_Empty_Message_With_Default_Constructor() {
            // When: Create an empty message
            try (Message msg = new Message()) {
                // Then: Message should have size 0
                assertThat(msg.size())
                        .as("Empty message size")
                        .isEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should create message with specified size")
        void should_Create_Message_With_Specified_Size() {
            // When: Create a message with size 100
            try (Message msg = new Message(100)) {
                // Then: Message should have size 100
                assertThat(msg.size())
                        .as("Message size")
                        .isEqualTo(100);
            }
        }

        @Test
        @DisplayName("Should create message from byte array")
        void should_Create_Message_From_Byte_Array() {
            // Given: Byte array data
            byte[] data = new byte[]{1, 2, 3, 4, 5};

            // When: Create message from byte array
            try (Message msg = new Message(data)) {
                // Then: Message should contain the data
                assertThat(msg.size()).isEqualTo(5);
                assertThat(msg.toByteArray())
                        .as("Message data")
                        .isEqualTo(data);
            }
        }

        @Test
        @DisplayName("Should create message from string")
        void should_Create_Message_From_String() {
            // Given: A text string
            String text = "Hello, World!";

            // When: Create message from string
            try (Message msg = new Message(text)) {
                // Then: Message should contain the UTF-8 encoded string
                assertThat(msg.toString())
                        .as("Message string content")
                        .isEqualTo(text);
            }
        }

        @Test
        @DisplayName("Should throw exception for negative size")
        void should_Throw_Exception_For_Negative_Size() {
            // When: Try to create message with negative size
            // Then: Should throw IllegalArgumentException
            assertThatThrownBy(() -> new Message(-1))
                    .as("Creating message with negative size")
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Message Rebuilding")
    class MessageRebuilding {

        @Test
        @DisplayName("Should rebuild message with new size")
        void should_Rebuild_Message_With_New_Size() {
            // Given: A message with size 10
            try (Message msg = new Message(10)) {
                // When: Rebuild with size 20
                msg.rebuild(20);

                // Then: Message should have new size
                assertThat(msg.size())
                        .as("Rebuilt message size")
                        .isEqualTo(20);
            }
        }

        @Test
        @DisplayName("Should rebuild message as empty")
        void should_Rebuild_Message_As_Empty() {
            // Given: A message with size 50
            try (Message msg = new Message(50)) {
                // When: Rebuild as empty
                msg.rebuild();

                // Then: Message should have size 0
                assertThat(msg.size())
                        .as("Empty rebuilt message size")
                        .isEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should handle multiple rebuilds")
        void should_Handle_Multiple_Rebuilds() {
            // Given: A message
            try (Message msg = new Message(10)) {
                // When: Rebuild multiple times
                msg.rebuild(20);
                assertThat(msg.size()).isEqualTo(20);

                msg.rebuild(5);
                assertThat(msg.size()).isEqualTo(5);

                msg.rebuild();
                // Then: Final size should be 0
                assertThat(msg.size())
                        .as("Size after multiple rebuilds")
                        .isEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should throw exception for negative rebuild size")
        void should_Throw_Exception_For_Negative_Rebuild_Size() {
            // Given: A message
            try (Message msg = new Message()) {
                // When: Try to rebuild with negative size
                // Then: Should throw IllegalArgumentException
                assertThatThrownBy(() -> msg.rebuild(-5))
                        .as("Rebuilding with negative size")
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Nested
    @DisplayName("Message Copy Operations")
    class MessageCopyOperations {

        @Test
        @DisplayName("Should copy message data to another message")
        void should_Copy_Message_Data_To_Another_Message() {
            // Given: A source message with data
            Message source = new Message("Test data");
            Message dest = new Message();

            try {
                // When: Copy source to destination
                dest.copy(source);

                // Then: Destination should have same data
                assertThat(dest.toString())
                        .as("Copied message content")
                        .isEqualTo("Test data");
                assertThat(dest.size())
                        .as("Copied message size")
                        .isEqualTo(source.size());
            } finally {
                source.close();
                dest.close();
            }
        }

        @Test
        @DisplayName("Should move message data to another message")
        void should_Move_Message_Data_To_Another_Message() {
            // Given: A source message with data
            Message source = new Message("Move test");
            Message dest = new Message();
            int originalSize = source.size();

            try {
                // When: Move source to destination
                dest.move(source);

                // Then: Destination should have the data
                assertThat(dest.size())
                        .as("Moved message size")
                        .isEqualTo(originalSize);
                assertThat(dest.toString())
                        .as("Moved message content")
                        .isEqualTo("Move test");

                // And: Source should be empty after move
                assertThat(source.size())
                        .as("Source size after move")
                        .isEqualTo(0);
            } finally {
                source.close();
                dest.close();
            }
        }
    }

    @Nested
    @DisplayName("Message Data Conversion")
    class MessageDataConversion {

        @Test
        @DisplayName("Should convert message to byte array")
        void should_Convert_Message_To_Byte_Array() {
            // Given: A message with byte data
            byte[] original = new byte[]{10, 20, 30, 40, 50};
            try (Message msg = new Message(original)) {
                // When: Convert to byte array
                byte[] result = msg.toByteArray();

                // Then: Should return copy of data
                assertThat(result)
                        .as("Converted byte array")
                        .isEqualTo(original)
                        .isNotSameAs(original); // Should be a copy, not same reference
            }
        }

        @Test
        @DisplayName("Should convert message to UTF-8 string")
        void should_Convert_Message_To_UTF8_String() {
            // Given: A message with UTF-8 text including multibyte characters
            String expected = "한글 테스트 UTF-8";
            try (Message msg = new Message(expected)) {
                // When: Convert to string
                String result = msg.toString();

                // Then: Should correctly decode UTF-8
                assertThat(result)
                        .as("Converted string")
                        .isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("Should handle empty message conversions")
        void should_Handle_Empty_Message_Conversions() {
            // Given: An empty message
            try (Message msg = new Message()) {
                // When: Convert to byte array and string
                // Then: Should return empty results
                assertThat(msg.size()).isEqualTo(0);
                assertThat(msg.toByteArray())
                        .as("Empty message byte array")
                        .isEqualTo(new byte[0]);
                assertThat(msg.toString())
                        .as("Empty message string")
                        .isEqualTo("");
            }
        }
    }

    @Nested
    @DisplayName("Large Message Handling")
    class LargeMessageHandling {

        @Test
        @DisplayName("Should handle large message data")
        void should_Handle_Large_Message_Data() {
            // Given: Large byte array (10KB)
            byte[] largeData = new byte[10_000];
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }

            // When: Create message with large data
            try (Message msg = new Message(largeData)) {
                // Then: Should handle the data correctly
                assertThat(msg.size())
                        .as("Large message size")
                        .isEqualTo(10_000);
                assertThat(msg.toByteArray())
                        .as("Large message data")
                        .isEqualTo(largeData);
            }
        }
    }
}
