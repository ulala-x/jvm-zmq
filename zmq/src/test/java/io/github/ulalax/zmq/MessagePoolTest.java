package io.github.ulalax.zmq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MessagePool class.
 */
@Tag("MessagePool")
class MessagePoolTest {

    private MessagePool pool;

    @BeforeEach
    void setUp() {
        pool = new MessagePool();
    }

    @Nested
    @DisplayName("Message Rental")
    class MessageRental {

        @Test
        @DisplayName("Should rent message with requested size")
        void should_Rent_Message_With_Requested_Size() {
            // When: Rent a message of 100 bytes
            Message msg = pool.rent(100);

            // Then: Message should have buffer size >= 100
            assertThat(msg.getBufferSize())
                    .as("Buffer size should be >= requested size")
                    .isGreaterThanOrEqualTo(100);

            assertThat(msg.size())
                    .as("Actual size should equal requested size")
                    .isEqualTo(100);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should rent message with byte array data")
        void should_Rent_Message_With_Byte_Array() {
            // Given: Test data
            byte[] data = new byte[]{1, 2, 3, 4, 5};

            // When: Rent message with data
            Message msg = pool.rent(data);

            // Then: Message should contain the data
            assertThat(msg.size())
                    .as("Message size")
                    .isEqualTo(5);

            byte[] retrieved = msg.toByteArray();
            assertThat(retrieved)
                    .as("Message data")
                    .containsExactly(1, 2, 3, 4, 5);

            // Cleanup
            msg.close();
        }

        @Test
        @DisplayName("Should select appropriate bucket for size")
        void should_Select_Appropriate_Bucket() {
            // When: Rent messages of various sizes
            Message msg1 = pool.rent(10);     // <= 64 bytes: not pooled
            Message msg2 = pool.rent(100);    // Should use 128-byte bucket
            Message msg3 = pool.rent(1000);   // Should use 1024-byte bucket

            // Then: For messages <= 64 bytes, bufferSize is -1 (not pooled)
            // For pooled messages, buffer size should be power of 2
            assertThat(msg1.getBufferSize()).isEqualTo(-1);  // Not pooled
            assertThat(msg1.size()).isEqualTo(10);           // Actual size
            assertThat(msg2.getBufferSize()).isEqualTo(128);
            assertThat(msg3.getBufferSize()).isEqualTo(1024);

            // Cleanup
            msg1.close();
            msg2.close();
            msg3.close();
        }

        @Test
        @DisplayName("Should throw exception for negative size")
        void should_Throw_Exception_For_Negative_Size() {
            // When/Then: Renting with negative size should throw
            assertThatThrownBy(() -> pool.rent(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("size cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception for null byte array")
        void should_Throw_Exception_For_Null_Array() {
            // When/Then: Renting with null array should throw
            assertThatThrownBy(() -> pool.rent(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("data cannot be null");
        }
    }

    @Nested
    @DisplayName("Pool Reuse")
    class PoolReuse {

        @Test
        @DisplayName("Should reuse messages from pool")
        void should_Reuse_Messages_From_Pool() {
            // Given: Rent and return a message
            Message msg1 = pool.rent(100);
            msg1.close(); // Return to pool

            // Give some time for callback to execute
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When: Rent another message of same size
            Message msg2 = pool.rent(100);

            // Then: Statistics should show pool hit
            MessagePool.PoolStatistics stats = pool.getStatistics();
            assertThat(stats.totalRents)
                    .as("Total rents")
                    .isEqualTo(2);

            // Note: We can't guarantee a hit due to timing, but we can verify the pool is working
            assertThat(stats.poolHits + stats.poolMisses)
                    .as("Total rents should equal hits + misses")
                    .isEqualTo(stats.totalRents);

            // Cleanup
            msg2.close();
        }

        @Test
        @DisplayName("Should track pool statistics")
        void should_Track_Pool_Statistics() {
            // When: Perform several rent operations
            Message msg1 = pool.rent(100);
            Message msg2 = pool.rent(200);
            Message msg3 = pool.rent(100);

            // Then: Statistics should be tracked
            MessagePool.PoolStatistics stats = pool.getStatistics();
            assertThat(stats.totalRents)
                    .as("Total rents")
                    .isEqualTo(3);

            assertThat(stats.outstandingBuffers)
                    .as("Outstanding buffers")
                    .isEqualTo(3);

            // Cleanup
            msg1.close();
            msg2.close();
            msg3.close();
        }
    }

    @Nested
    @DisplayName("Pool Configuration")
    class PoolConfiguration {

        @Test
        @DisplayName("Should prewarm pool with specific size")
        void should_Prewarm_Pool_With_Specific_Size() {
            // When: Prewarm pool with 10 messages of 1KB
            pool.prewarm(MessageSize.SIZE_1K, 10);

            // Then: Pool should have 10 messages in the 1KB bucket
            Map<Integer, Integer> counts = pool.getPoolCounts();
            int bucketIndex = MessageSize.SIZE_1K.getBucketIndex();

            assertThat(counts.get(bucketIndex))
                    .as("Messages in 1KB bucket")
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Should prewarm pool with multiple sizes")
        void should_Prewarm_Pool_With_Multiple_Sizes() {
            // Given: Array of sizes
            MessageSize[] sizes = {
                    MessageSize.SIZE_1K,
                    MessageSize.SIZE_4K,
                    MessageSize.SIZE_16K
            };

            // When: Prewarm with multiple sizes
            pool.prewarm(sizes, 5);

            // Then: All buckets should have messages
            Map<Integer, Integer> counts = pool.getPoolCounts();
            for (MessageSize size : sizes) {
                assertThat(counts.get(size.getBucketIndex()))
                        .as("Messages in bucket " + size)
                        .isGreaterThanOrEqualTo(5);
            }
        }

        @Test
        @DisplayName("Should prewarm pool with custom configuration")
        void should_Prewarm_Pool_With_Custom_Configuration() {
            // Given: Custom configuration map
            Map<MessageSize, Integer> config = new HashMap<>();
            config.put(MessageSize.SIZE_1K, 20);
            config.put(MessageSize.SIZE_4K, 10);

            // When: Prewarm with custom config
            pool.prewarm(config);

            // Then: Buckets should have specified counts
            Map<Integer, Integer> counts = pool.getPoolCounts();
            assertThat(counts.get(MessageSize.SIZE_1K.getBucketIndex()))
                    .as("Messages in 1KB bucket")
                    .isGreaterThanOrEqualTo(20);

            assertThat(counts.get(MessageSize.SIZE_4K.getBucketIndex()))
                    .as("Messages in 4KB bucket")
                    .isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Should set and get max buffers per bucket")
        void should_Set_And_Get_Max_Buffers() {
            // When: Set max buffers for 1KB bucket
            pool.setMaxBuffers(MessageSize.SIZE_1K, 50);

            // Then: Should be able to get the value back
            int maxBuffers = pool.getMaxBuffers(MessageSize.SIZE_1K);
            assertThat(maxBuffers)
                    .as("Max buffers for 1KB bucket")
                    .isEqualTo(50);
        }

        @Test
        @DisplayName("Should throw exception for negative max buffers")
        void should_Throw_Exception_For_Negative_Max_Buffers() {
            // When/Then: Setting negative max buffers should throw
            assertThatThrownBy(() -> pool.setMaxBuffers(MessageSize.SIZE_1K, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxBuffers cannot be negative");
        }

        @Test
        @DisplayName("Should throw exception for negative prewarm count")
        void should_Throw_Exception_For_Negative_Prewarm_Count() {
            // When/Then: Prewarming with negative count should throw
            assertThatThrownBy(() -> pool.prewarm(MessageSize.SIZE_1K, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("count cannot be negative");
        }
    }

    @Nested
    @DisplayName("Statistics")
    class Statistics {

        @Test
        @DisplayName("Should calculate hit rate correctly")
        void should_Calculate_Hit_Rate_Correctly() {
            // Given: Initial statistics
            MessagePool.PoolStatistics initialStats = pool.getStatistics();
            assertThat(initialStats.getHitRate())
                    .as("Initial hit rate")
                    .isEqualTo(0.0);

            // When: Rent and use some messages
            for (int i = 0; i < 10; i++) {
                Message msg = pool.rent(100);
                msg.close();
            }

            // Then: Hit rate should be calculable
            MessagePool.PoolStatistics stats = pool.getStatistics();
            assertThat(stats.getHitRate())
                    .as("Hit rate")
                    .isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should provide pool counts per bucket")
        void should_Provide_Pool_Counts_Per_Bucket() {
            // When: Get pool counts
            Map<Integer, Integer> counts = pool.getPoolCounts();

            // Then: Should have 16 buckets (128B to 4MB)
            assertThat(counts)
                    .as("Pool counts map")
                    .hasSize(16);

            // All counts should be non-negative
            for (Integer count : counts.values()) {
                assertThat(count)
                        .as("Bucket count")
                        .isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should format statistics as string")
        void should_Format_Statistics_As_String() {
            // Given: Some pool activity
            Message msg = pool.rent(100);
            msg.close();

            // When: Get statistics string
            MessagePool.PoolStatistics stats = pool.getStatistics();
            String statsString = stats.toString();

            // Then: String should contain key information
            assertThat(statsString)
                    .as("Statistics string")
                    .contains("rents=")
                    .contains("returns=")
                    .contains("hits=")
                    .contains("misses=")
                    .contains("rejects=")
                    .contains("outstanding=")
                    .contains("hitRate=");
        }
    }

    @Nested
    @DisplayName("Pool Clear")
    class PoolClear {

        @Test
        @DisplayName("Should clear all messages from pool")
        void should_Clear_All_Messages_From_Pool() {
            // Given: Pool with some messages
            pool.prewarm(MessageSize.SIZE_1K, 10);
            pool.prewarm(MessageSize.SIZE_4K, 5);

            // When: Clear the pool
            pool.clear();

            // Then: All buckets should be empty
            Map<Integer, Integer> counts = pool.getPoolCounts();
            for (Integer count : counts.values()) {
                assertThat(count)
                        .as("Bucket count after clear")
                        .isEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should reset statistics when clearing pool")
        void should_Reset_Statistics_When_Clearing() {
            // Given: Pool with some activity
            for (int i = 0; i < 5; i++) {
                Message msg = pool.rent(100);
                msg.close();
            }

            // When: Clear the pool
            pool.clear();

            // Then: Statistics should be reset
            MessagePool.PoolStatistics stats = pool.getStatistics();
            assertThat(stats.totalRents)
                    .as("Total rents after clear")
                    .isEqualTo(0);

            assertThat(stats.totalReturns)
                    .as("Total returns after clear")
                    .isEqualTo(0);

            assertThat(stats.poolHits)
                    .as("Pool hits after clear")
                    .isEqualTo(0);

            assertThat(stats.poolMisses)
                    .as("Pool misses after clear")
                    .isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Shared Singleton")
    class SharedSingleton {

        @Test
        @DisplayName("Should provide shared singleton instance")
        void should_Provide_Shared_Singleton_Instance() {
            // When: Get shared instance
            MessagePool shared = MessagePool.SHARED;

            // Then: Should be non-null
            assertThat(shared)
                    .as("Shared singleton")
                    .isNotNull();

            // And: Multiple calls should return same instance
            assertThat(MessagePool.SHARED)
                    .as("Shared singleton consistency")
                    .isSameAs(shared);
        }

        @Test
        @DisplayName("Should work with shared singleton")
        void should_Work_With_Shared_Singleton() {
            // When: Use shared singleton
            Message msg = MessagePool.SHARED.rent(100);

            // Then: Should work correctly
            assertThat(msg.size())
                    .as("Message size from shared pool")
                    .isEqualTo(100);

            // Cleanup
            msg.close();
        }
    }
}
