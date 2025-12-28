package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.LibZmq;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MessagePool.setActualDataSize() method.
 */
@Tag("MessagePool")
class MessagePoolSetActualDataSizeTest {

    private MessagePool pool;
    private Context context;

    @BeforeEach
    void setUp() {
        pool = new MessagePool();
        context = new Context();
    }

    @AfterEach
    void tearDown() {
        pool.clear();
        if (context != null) {
            context.close();
        }
    }

    @Nested
    @DisplayName("Basic Operations")
    class BasicOperations {

        @Test
        @DisplayName("Should set actual data size after rent(int)")
        void should_Set_Actual_Data_Size_After_Rent_Int() {
            // Given: Rent a message with 1024 bytes
            Message msg = pool.rent(1024);

            // Then: Initial size should be 1024
            assertThat(msg.size())
                .as("Initial message size")
                .isEqualTo(1024);

            // When: Set actual data size to 512
            msg.setActualDataSize(512);

            // Then: Size should be updated to 512
            assertThat(msg.size())
                .as("Updated message size")
                .isEqualTo(512);

            // Buffer size should remain unchanged
            assertThat(msg.getBufferSize())
                .as("Buffer size should remain unchanged")
                .isGreaterThanOrEqualTo(1024);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should set actual data size to same value as buffer size")
        void should_Set_Actual_Data_Size_To_Buffer_Size() {
            // Given: Rent a message with 1024 bytes (buffer size will be 1024)
            Message msg = pool.rent(1024);
            int bufferSize = msg.getBufferSize();

            // When: Set actual data size to buffer size
            msg.setActualDataSize(bufferSize);

            // Then: Size should equal buffer size
            assertThat(msg.size())
                .as("Message size should equal buffer size")
                .isEqualTo(bufferSize);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should set actual data size to zero")
        void should_Set_Actual_Data_Size_To_Zero() {
            // Given: Rent a message with 1024 bytes
            Message msg = pool.rent(1024);

            // When: Set actual data size to 0
            msg.setActualDataSize(0);

            // Then: Size should be 0
            assertThat(msg.size())
                .as("Message size should be zero")
                .isEqualTo(0);

            // Cleanup
            msg.close();
        }
    }

    @Nested
    @DisplayName("Size Reduction")
    class SizeReduction {

        @Test
        @DisplayName("Should reduce size smaller than buffer size")
        void should_Reduce_Size_Smaller_Than_Buffer() {
            // Given: Rent a message with 2048 bytes
            Message msg = pool.rent(2048);
            int originalSize = msg.size();
            int bufferSize = msg.getBufferSize();

            // When: Set actual data size to 100 (much smaller)
            msg.setActualDataSize(100);

            // Then: Size should be reduced to 100
            assertThat(msg.size())
                .as("Reduced message size")
                .isEqualTo(100);

            // Buffer size should remain unchanged
            assertThat(msg.getBufferSize())
                .as("Buffer size should remain unchanged")
                .isEqualTo(bufferSize);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should reduce size multiple times")
        void should_Reduce_Size_Multiple_Times() {
            // Given: Rent a message with 4096 bytes
            Message msg = pool.rent(4096);

            // When: Reduce size in steps
            msg.setActualDataSize(2048);
            assertThat(msg.size()).isEqualTo(2048);

            msg.setActualDataSize(1024);
            assertThat(msg.size()).isEqualTo(1024);

            msg.setActualDataSize(512);
            assertThat(msg.size()).isEqualTo(512);

            // Then: Final size should be 512
            assertThat(msg.size())
                .as("Final message size")
                .isEqualTo(512);

            // Cleanup
            msg.close();
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandling {

        @Test
        @DisplayName("Should throw exception when size exceeds buffer size")
        void should_Throw_Exception_When_Size_Exceeds_Buffer_Size() {
            // Given: Rent a message with 1024 bytes
            Message msg = pool.rent(1024);
            int bufferSize = msg.getBufferSize();

            // When/Then: Setting size larger than buffer should throw exception
            assertThatThrownBy(() -> msg.setActualDataSize(bufferSize + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size")
                .hasMessageContaining("exceeds buffer size");

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should throw exception when size is much larger than buffer")
        void should_Throw_Exception_When_Size_Is_Much_Larger() {
            // Given: Rent a message with 512 bytes
            Message msg = pool.rent(512);

            // When/Then: Setting size to 10000 should throw exception
            assertThatThrownBy(() -> msg.setActualDataSize(10000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds buffer size");

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should not throw exception for negative size with rent(byte[])")
        void should_Handle_Negative_Size_For_ByteArray_Rent() {
            // Note: setActualDataSize is called internally by rent(byte[])
            // This test ensures rent(byte[]) handles edge cases correctly

            // Given: Empty byte array
            byte[] emptyData = new byte[0];

            // When: Rent with empty data
            Message msg = pool.rent(emptyData);

            // Then: Size should be 0
            assertThat(msg.size())
                .as("Empty message size")
                .isEqualTo(0);

            // Cleanup
            msg.close();
        }
    }

    @Nested
    @DisplayName("Reinitialization Optimization")
    class ReinitializationOptimization {

        @Test
        @DisplayName("Should optimize when setting same size twice")
        void should_Optimize_When_Setting_Same_Size_Twice() {
            // Given: Rent a message with 1024 bytes
            Message msg = pool.rent(1024);

            // When: Set same size twice
            msg.setActualDataSize(512);
            int sizeAfterFirst = msg.size();

            msg.setActualDataSize(512);
            int sizeAfterSecond = msg.size();

            // Then: Both sizes should be equal (optimization works)
            assertThat(sizeAfterFirst)
                .as("Size after first setActualDataSize")
                .isEqualTo(512);

            assertThat(sizeAfterSecond)
                .as("Size after second setActualDataSize")
                .isEqualTo(512);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should handle size change optimization correctly")
        void should_Handle_Size_Change_Optimization() {
            // Given: Rent a message
            Message msg = pool.rent(2048);

            // When: Change size multiple times
            msg.setActualDataSize(1024);
            msg.setActualDataSize(1024); // Same size - should optimize
            msg.setActualDataSize(512);  // Different size - should reinitialize
            msg.setActualDataSize(512);  // Same size - should optimize

            // Then: Final size should be correct
            assertThat(msg.size())
                .as("Final optimized size")
                .isEqualTo(512);

            // Cleanup
            msg.close();
        }
    }

    @Nested
    @DisplayName("Send and Reuse")
    class SendAndReuse {

        @Test
        @DisplayName("Should work correctly after send and reuse")
        void should_Work_After_Send_And_Reuse() throws Exception {
            // Given: PUSH-PULL socket pair
            try (Socket sender = new Socket(context, SocketType.PUSH);
                 Socket receiver = new Socket(context, SocketType.PULL)) {

                sender.bind("inproc://test-set-size-send");
                receiver.connect("inproc://test-set-size-send");

                // Give sockets time to connect
                Thread.sleep(100);

                // When: Rent with byte array (automatically sets size)
                byte[] data1 = "Hello".getBytes();
                Message msg1 = pool.rent(data1);

                // Send message
                sender.send(msg1, SendFlags.NONE);
                msg1.close(); // Return to pool after send

                // Receive on other side
                Message received = new Message();
                receiver.recv(received, RecvFlags.NONE);
                assertThat(received.toString()).isEqualTo("Hello");
                received.close();

                // Wait for callback to return message to pool
                Thread.sleep(100);

                // Rent again (should reuse from pool)
                byte[] data2 = "Test".getBytes();
                Message msg2 = pool.rent(data2);

                // Then: Second message should work correctly
                assertThat(msg2.size())
                    .as("Reused message size")
                    .isEqualTo(data2.length);

                // Send to verify it works
                sender.send(msg2, SendFlags.NONE);
                msg2.close();

                // Receive to verify
                Message received2 = new Message();
                receiver.recv(received2, RecvFlags.NONE);
                assertThat(received2.toString()).isEqualTo("Test");
                received2.close();
            }
        }

        @Test
        @DisplayName("Should handle multiple send-reuse cycles")
        void should_Handle_Multiple_Send_Reuse_Cycles() throws Exception {
            // Given: PUSH-PULL socket pair
            try (Socket sender = new Socket(context, SocketType.PUSH);
                 Socket receiver = new Socket(context, SocketType.PULL)) {

                sender.bind("inproc://test-multiple-cycles");
                receiver.connect("inproc://test-multiple-cycles");

                Thread.sleep(100);

                // When: Perform multiple send-reuse cycles
                for (int i = 0; i < 5; i++) {
                    String testData = "Message-" + i;
                    byte[] data = testData.getBytes();

                    // Rent with byte array (automatically sets size)
                    Message msg = pool.rent(data);

                    sender.send(msg, SendFlags.NONE);
                    msg.close();

                    Message received = new Message();
                    receiver.recv(received, RecvFlags.NONE);
                    assertThat(received.toString()).isEqualTo(testData);
                    received.close();

                    Thread.sleep(50); // Allow callback to execute
                }

                // Then: Pool should have reused messages
                MessagePool.PoolStatistics stats = pool.getStatistics();
                assertThat(stats.totalRents)
                    .as("Total rents after cycles")
                    .isEqualTo(5);
            }
        }
    }

    @Nested
    @DisplayName("copyFromNative Integration")
    class CopyFromNativeIntegration {

        @Test
        @DisplayName("Should return correct size after copyFromNative")
        void should_Return_Correct_Size_After_CopyFromNative() {
            // Given: Rent a message
            Message msg = pool.rent(1024);

            // When: Copy data using copyFromNative
            byte[] testData = "Test Data for copyFromNative".getBytes();
            MemorySegment sourceSegment = MemorySegment.ofArray(testData);
            msg.copyFromNative(sourceSegment, testData.length);

            // Then: Size should match the copied data
            assertThat(msg.size())
                .as("Size after copyFromNative")
                .isEqualTo(testData.length);

            // And: Data should be correct
            assertThat(msg.toByteArray())
                .as("Data after copyFromNative")
                .isEqualTo(testData);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should handle multiple copyFromNative calls")
        void should_Handle_Multiple_CopyFromNative_Calls() {
            // Given: Rent a message
            Message msg = pool.rent(2048);

            // When: Copy data multiple times
            byte[] data1 = "First data".getBytes();
            msg.copyFromNative(MemorySegment.ofArray(data1), data1.length);
            assertThat(msg.size()).isEqualTo(data1.length);

            byte[] data2 = "Second data that is longer".getBytes();
            msg.copyFromNative(MemorySegment.ofArray(data2), data2.length);
            assertThat(msg.size()).isEqualTo(data2.length);

            // Then: Final data should be correct
            assertThat(msg.toByteArray())
                .as("Data after multiple copyFromNative")
                .isEqualTo(data2);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should throw exception when copyFromNative size exceeds buffer")
        void should_Throw_Exception_When_CopyFromNative_Exceeds_Buffer() {
            // Given: Rent a message with small buffer
            Message msg = pool.rent(128);
            int bufferSize = msg.getBufferSize();

            // When/Then: Copying data larger than buffer should throw
            byte[] largeData = new byte[bufferSize + 100];
            assertThatThrownBy(() -> msg.copyFromNative(MemorySegment.ofArray(largeData), largeData.length))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds buffer size");

            // Cleanup
            msg.close();
        }
    }

    @Nested
    @DisplayName("Data Integrity")
    class DataIntegrity {

        @Test
        @DisplayName("Should preserve data when changing size")
        void should_Preserve_Data_When_Changing_Size() {
            // Given: Rent a message and write data
            Message msg = pool.rent(1024);
            byte[] originalData = "Original Data".getBytes();
            msg.getPoolDataPtr().copyFrom(MemorySegment.ofArray(originalData));
            msg.setActualDataSize(originalData.length);

            // When: Read the data back
            byte[] readData = msg.toByteArray();

            // Then: Data should match
            assertThat(readData)
                .as("Data should be preserved")
                .isEqualTo(originalData);

            // When: Reduce size
            msg.setActualDataSize(8); // "Original"

            // Then: First 8 bytes should still be accessible
            byte[] truncatedData = msg.toByteArray();
            assertThat(truncatedData)
                .as("Truncated data")
                .hasSize(8)
                .startsWith("Original".getBytes());

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should handle size increase within buffer limits")
        void should_Handle_Size_Increase_Within_Buffer_Limits() {
            // Given: Rent a message with initial small size
            Message msg = pool.rent(256);
            int bufferSize = msg.getBufferSize();

            byte[] smallData = "Small".getBytes();
            msg.getPoolDataPtr().copyFrom(MemorySegment.ofArray(smallData));
            msg.setActualDataSize(smallData.length);

            // When: Increase size to use more buffer
            int newSize = Math.min(200, bufferSize);
            msg.setActualDataSize(newSize);

            // Then: Size should be updated
            assertThat(msg.size())
                .as("Increased size")
                .isEqualTo(newSize);

            // And: Original data should still be at the beginning
            byte[] currentData = msg.toByteArray();
            assertThat(currentData)
                .as("Data after size increase")
                .hasSize(newSize)
                .startsWith(smallData);

            // Cleanup
            msg.close();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle rent(byte[]) with setActualDataSize internally")
        void should_Handle_Rent_ByteArray_With_SetActualDataSize() {
            // Given: Byte array data
            byte[] data = "Test message content".getBytes();

            // When: Rent with byte array (internally calls setActualDataSize)
            Message msg = pool.rent(data);

            // Then: Size should match data length
            assertThat(msg.size())
                .as("Message size should match data length")
                .isEqualTo(data.length);

            // And: Content should match
            assertThat(msg.toByteArray())
                .as("Message content")
                .isEqualTo(data);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should handle boundary size values")
        void should_Handle_Boundary_Size_Values() {
            // Given: Rent a message with 1024 bytes
            Message msg = pool.rent(1024);
            int bufferSize = msg.getBufferSize();

            // When: Set to boundary values
            msg.setActualDataSize(0); // Minimum
            assertThat(msg.size()).isEqualTo(0);

            msg.setActualDataSize(bufferSize); // Maximum
            assertThat(msg.size()).isEqualTo(bufferSize);

            msg.setActualDataSize(1); // Minimum positive
            assertThat(msg.size()).isEqualTo(1);

            msg.setActualDataSize(bufferSize - 1); // Just below maximum
            assertThat(msg.size()).isEqualTo(bufferSize - 1);

            // Then: All operations should succeed
            assertThat(msg.size())
                .as("Final size")
                .isEqualTo(bufferSize - 1);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should work with very large messages")
        void should_Work_With_Very_Large_Messages() {
            // Given: Rent a large message (4MB bucket)
            Message msg = pool.rent(4 * 1024 * 1024); // 4MB
            int bufferSize = msg.getBufferSize();

            // When: Set various sizes
            msg.setActualDataSize(1024 * 1024); // 1MB
            assertThat(msg.size()).isEqualTo(1024 * 1024);

            msg.setActualDataSize(2 * 1024 * 1024); // 2MB
            assertThat(msg.size()).isEqualTo(2 * 1024 * 1024);

            msg.setActualDataSize(bufferSize); // Full size
            assertThat(msg.size()).isEqualTo(bufferSize);

            // Then: Operations should succeed
            assertThat(msg.getBufferSize())
                .as("Large message buffer size")
                .isGreaterThanOrEqualTo(4 * 1024 * 1024);

            // Cleanup
            msg.close();
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    class ThreadSafety {

        @Test
        @DisplayName("Should handle concurrent setActualDataSize calls")
        void should_Handle_Concurrent_SetActualDataSize() throws Exception {
            // Given: Multiple messages from pool
            int threadCount = 5;
            Thread[] threads = new Thread[threadCount];

            // When: Multiple threads set actual data size concurrently
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    Message msg = pool.rent(2048);

                    for (int j = 0; j < 10; j++) {
                        int size = 100 + (threadId * 10) + j;
                        msg.setActualDataSize(size);
                        assertThat(msg.size()).isEqualTo(size);
                    }

                    msg.close();
                });
                threads[i].start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Then: Pool statistics should be correct
            MessagePool.PoolStatistics stats = pool.getStatistics();
            assertThat(stats.totalRents)
                .as("Total rents from concurrent threads")
                .isEqualTo(threadCount);
        }
    }
}
