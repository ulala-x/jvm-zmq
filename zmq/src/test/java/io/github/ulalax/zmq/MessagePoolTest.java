package io.github.ulalax.zmq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for MessagePool functionality.
 * Tests bucket selection, rent/return cycle, reuse, overflow, concurrency, and statistics.
 */
class MessagePoolTest {

    @BeforeEach
    void setUp() {
        // Clear the pool before each test to ensure test isolation
        // This prevents state carryover from previous tests
        MessagePool.clear();
    }

    @Test
    @DisplayName("Test bucket selection for various sizes")
    void testBucketSelection() {
        // Test exact bucket sizes
        assertEquals(0, MessagePool.selectBucket(16), "16 bytes should map to bucket 0");
        assertEquals(1, MessagePool.selectBucket(32), "32 bytes should map to bucket 1");
        assertEquals(6, MessagePool.selectBucket(1024), "1KB should map to bucket 6");
        assertEquals(18, MessagePool.selectBucket(4194304), "4MB should map to bucket 18");

        // Test in-between sizes (should use next larger bucket)
        assertEquals(1, MessagePool.selectBucket(17), "17 bytes should use 32-byte bucket");
        assertEquals(2, MessagePool.selectBucket(33), "33 bytes should use 64-byte bucket");
        assertEquals(7, MessagePool.selectBucket(1025), "1025 bytes should use 2KB bucket");

        // Test boundary cases
        assertEquals(0, MessagePool.selectBucket(1), "1 byte should use 16-byte bucket");
        assertEquals(0, MessagePool.selectBucket(15), "15 bytes should use 16-byte bucket");

        // Test oversized (should return -1)
        assertEquals(-1, MessagePool.selectBucket(4194305), "4MB + 1 should not be poolable");
        assertEquals(-1, MessagePool.selectBucket(10000000), "10MB should not be poolable");
    }

    @Test
    @DisplayName("Test basic rent and return cycle")
    void testRentReturn() {
        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();

        // Rent a segment
        MessagePool.PooledSegment pooled = MessagePool.rent(1024);
        assertNotNull(pooled, "Pooled segment should not be null");
        assertNotNull(pooled.segment, "Memory segment should not be null");
        assertEquals(6, pooled.bucketIndex, "1KB should use bucket 6");
        assertTrue(pooled.segment.byteSize() >= 1024, "Segment should be at least 1KB");

        // Return to pool
        MessagePool.returnToPool(pooled);

        // Verify statistics changed
        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        assertTrue(statsAfter.totalRents > statsBefore.totalRents, "Rents should increase");
        assertTrue(statsAfter.totalReturns > statsBefore.totalReturns, "Returns should increase");
    }

    @Test
    @DisplayName("Test pool reuse - renting increases hit rate")
    void testPoolReuse() {
        int testSize = 2048; // 2KB
        int bucketIndex = MessagePool.selectBucket(testSize);
        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();
        long hitsBefore = statsBefore.buckets[bucketIndex].hits;

        // First rent and return - add to pool
        MessagePool.PooledSegment pooled1 = MessagePool.rent(testSize);
        assertNotNull(pooled1);
        assertEquals(bucketIndex, pooled1.bucketIndex, "Should use correct bucket");
        MessagePool.returnToPool(pooled1);

        // Second rent - should be a hit (reuse from pool)
        MessagePool.PooledSegment pooled2 = MessagePool.rent(testSize);
        assertNotNull(pooled2);
        assertEquals(bucketIndex, pooled2.bucketIndex, "Should use same bucket");
        MessagePool.returnToPool(pooled2);

        // Check hit rate improved for this bucket
        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        long hitsAfter = statsAfter.buckets[bucketIndex].hits;
        assertTrue(hitsAfter > hitsBefore, "Hits should increase on reuse");
    }

    @Test
    @DisplayName("Test bucket overflow - per-bucket max limits")
    void testBucketOverflow() {
        // Use 64-byte bucket (max 1000) and 1MB bucket (max 50)
        int testSize1 = 64; // Max 1000
        int testSize2 = 1048576; // 1MB, Max 50
        int bucketIndex1 = MessagePool.selectBucket(testSize1);
        int bucketIndex2 = MessagePool.selectBucket(testSize2);

        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();
        long overflowsBefore1 = statsBefore.buckets[bucketIndex1].overflows;
        long overflowsBefore2 = statsBefore.buckets[bucketIndex2].overflows;

        // Test 64-byte bucket: Rent and return 1010 segments (exceeds 1000)
        MessagePool.PooledSegment[] segments1 = new MessagePool.PooledSegment[1010];
        for (int i = 0; i < 1010; i++) {
            segments1[i] = MessagePool.rent(testSize1);
        }
        for (int i = 0; i < 1010; i++) {
            MessagePool.returnToPool(segments1[i]);
        }

        // Test 1MB bucket: Rent and return 60 segments (exceeds 50)
        MessagePool.PooledSegment[] segments2 = new MessagePool.PooledSegment[60];
        for (int i = 0; i < 60; i++) {
            segments2[i] = MessagePool.rent(testSize2);
        }
        for (int i = 0; i < 60; i++) {
            MessagePool.returnToPool(segments2[i]);
        }

        // Check overflow occurred
        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        long overflowsAfter1 = statsAfter.buckets[bucketIndex1].overflows;
        long overflowsAfter2 = statsAfter.buckets[bucketIndex2].overflows;

        assertTrue(overflowsAfter1 > overflowsBefore1,
            "Overflows should occur when 64-byte bucket exceeds 1000");
        assertTrue(statsAfter.buckets[bucketIndex1].poolSize <= 1000,
            "64-byte pool size should not exceed 1000");

        assertTrue(overflowsAfter2 > overflowsBefore2,
            "Overflows should occur when 1MB bucket exceeds 50");
        assertTrue(statsAfter.buckets[bucketIndex2].poolSize <= 50,
            "1MB pool size should not exceed 50");
    }

    @Test
    @DisplayName("Test oversized messages (> 4MB) are handled correctly")
    void testOversizedMessages() {
        int oversizedLength = 5 * 1024 * 1024; // 5MB

        MessagePool.PooledSegment pooled = MessagePool.rent(oversizedLength);
        assertNotNull(pooled, "Oversized allocation should succeed");
        assertEquals(-1, pooled.bucketIndex, "Oversized should have bucketIndex -1");
        assertTrue(pooled.segment.byteSize() >= oversizedLength,
            "Segment should be large enough");

        // Return should be no-op for oversized
        MessagePool.returnToPool(pooled); // Should not crash
    }

    @Test
    @DisplayName("Test concurrent access from multiple threads")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Use different sizes to test different buckets
                        int size = 128 * (1 + (threadId % 4)); // 128, 256, 384, 512

                        MessagePool.PooledSegment pooled = MessagePool.rent(size);
                        assertNotNull(pooled);

                        // Simulate some work
                        pooled.segment.set(ValueLayout.JAVA_BYTE, 0, (byte) threadId);

                        MessagePool.returnToPool(pooled);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(0, errors.get(), "No errors should occur during concurrent access");

        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        assertEquals(threadCount * operationsPerThread,
            statsAfter.totalRents - statsBefore.totalRents,
            "Total rents should match operations");
    }

    @Test
    @DisplayName("Test fromPool factory method with byte array")
    void testFromPoolByteArray() throws Exception {
        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.PAIR)) {

            socket.bind("inproc://test-frompool-bytearray");

            byte[] data = "Hello MessagePool".getBytes();
            MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();

            try (Message msg = Message.fromPool(data)) {
                assertNotNull(msg, "Message should not be null");
                assertEquals(data.length, msg.size(), "Message size should match data length");

                // Verify data was copied correctly
                byte[] received = msg.toByteArray();
                assertArrayEquals(data, received, "Data should match");
            }

            // Note: Statistics may not change immediately as callback runs on ZMQ thread
            // This test just verifies the message is created correctly
        }
    }

    @Test
    @DisplayName("Test fromPool factory method with byte array slice")
    void testFromPoolByteArraySlice() {
        byte[] data = "0123456789".getBytes();
        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();

        try (Message msg = Message.fromPool(data, 2, 5)) {
            assertNotNull(msg, "Message should not be null");
            assertEquals(5, msg.size(), "Message size should be 5");
            assertEquals("23456", msg.toString(), "Data should be correct slice");
        }

        // Verify statistics
        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        assertTrue(statsAfter.totalRents > statsBefore.totalRents,
            "Rents should increase");
    }

    @Test
    @DisplayName("Test fromPool factory method with MemorySegment")
    void testFromPoolMemorySegment() {
        byte[] sourceData = "Test MemorySegment".getBytes();
        MemorySegment source = MemorySegment.ofArray(sourceData);

        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();

        try (Message msg = Message.fromPool(source, sourceData.length)) {
            assertNotNull(msg, "Message should not be null");
            assertEquals(sourceData.length, msg.size(), "Size should match");
            assertArrayEquals(sourceData, msg.toByteArray(), "Data should match");
        }

        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        assertTrue(statsAfter.totalRents > statsBefore.totalRents,
            "Rents should increase");
    }

    @Test
    @DisplayName("Test statistics tracking")
    void testStatistics() {
        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();

        // Perform some operations
        int testSize = 512;
        int bucketIndex = MessagePool.selectBucket(testSize);

        // First rent - should be a miss
        MessagePool.PooledSegment pooled1 = MessagePool.rent(testSize);
        MessagePool.returnToPool(pooled1);

        // Second rent - should be a hit
        MessagePool.PooledSegment pooled2 = MessagePool.rent(testSize);
        MessagePool.returnToPool(pooled2);

        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();

        // Verify overall statistics
        assertTrue(statsAfter.totalRents >= statsBefore.totalRents + 2,
            "Total rents should increase by at least 2");
        assertTrue(statsAfter.totalReturns >= statsBefore.totalReturns + 2,
            "Total returns should increase by at least 2");
        assertTrue(statsAfter.totalHits > statsBefore.totalHits,
            "Total hits should increase");

        // Verify bucket statistics
        MessagePool.PoolStatistics.BucketInfo bucketBefore = statsBefore.buckets[bucketIndex];
        MessagePool.PoolStatistics.BucketInfo bucketAfter = statsAfter.buckets[bucketIndex];

        assertTrue(bucketAfter.rents > bucketBefore.rents,
            "Bucket rents should increase");
        assertTrue(bucketAfter.returns > bucketBefore.returns,
            "Bucket returns should increase");
        assertTrue(bucketAfter.hits > bucketBefore.hits,
            "Bucket hits should increase on reuse");

        // Hit rate should be meaningful
        assertTrue(statsAfter.hitRate >= 0.0 && statsAfter.hitRate <= 1.0,
            "Hit rate should be between 0 and 1");
    }

    @Test
    @DisplayName("Test no memory leak with 10,000 message cycle")
    void testNoMemoryLeak() throws InterruptedException {
        int iterations = 10000;
        int testSize = 1024;

        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();
        long outstandingBefore = statsBefore.totalOutstanding;

        // Rent and return 10,000 times
        for (int i = 0; i < iterations; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            // Write some data to ensure segment is usable
            pooled.segment.set(ValueLayout.JAVA_BYTE, 0, (byte) (i % 256));
            MessagePool.returnToPool(pooled);
        }

        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        long outstandingAfter = statsAfter.totalOutstanding;

        // Outstanding should remain stable (within reason, considering other tests)
        long outstandingDelta = Math.abs(outstandingAfter - outstandingBefore);
        assertTrue(outstandingDelta < 100,
            "Outstanding buffers should remain stable, delta: " + outstandingDelta);

        // Verify operations completed
        assertEquals(iterations, statsAfter.totalRents - statsBefore.totalRents,
            "All rents should be recorded");
        assertEquals(iterations, statsAfter.totalReturns - statsBefore.totalReturns,
            "All returns should be recorded");

        // Hit rate should be high after warm-up
        int bucketIndex = MessagePool.selectBucket(testSize);
        MessagePool.PoolStatistics.BucketInfo bucket = statsAfter.buckets[bucketIndex];
        assertTrue(bucket.hitRate > 0.90, "Hit rate should exceed 90% after warm-up");
    }

    @Test
    @DisplayName("Test prewarm functionality")
    void testPrewarm() {
        Map<Integer, Integer> warmupSizes = new HashMap<>();
        warmupSizes.put(128, 50);  // Pre-allocate 50 segments of 128 bytes
        warmupSizes.put(1024, 100); // Pre-allocate 100 segments of 1KB

        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();
        int bucket128Before = statsBefore.buckets[MessagePool.selectBucket(128)].poolSize;
        int bucket1024Before = statsBefore.buckets[MessagePool.selectBucket(1024)].poolSize;

        // Prewarm
        Message.prewarmPool(warmupSizes);

        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();
        int bucket128After = statsAfter.buckets[MessagePool.selectBucket(128)].poolSize;
        int bucket1024After = statsAfter.buckets[MessagePool.selectBucket(1024)].poolSize;

        // Pool sizes should increase
        assertTrue(bucket128After >= bucket128Before + 50,
            "128-byte bucket should have at least 50 more segments");
        assertTrue(bucket1024After >= bucket1024Before + 100,
            "1KB bucket should have at least 100 more segments");

        // Verify pre-warmed segments work correctly
        MessagePool.PooledSegment pooled128 = MessagePool.rent(128);
        assertNotNull(pooled128, "Should rent from pre-warmed pool");
        MessagePool.returnToPool(pooled128);

        MessagePool.PooledSegment pooled1024 = MessagePool.rent(1024);
        assertNotNull(pooled1024, "Should rent from pre-warmed pool");
        MessagePool.returnToPool(pooled1024);
    }

    @Test
    @DisplayName("Test fromPool with invalid arguments")
    void testFromPoolInvalidArguments() {
        // Null byte array
        assertThrows(IllegalArgumentException.class, () -> {
            Message.fromPool((byte[]) null);
        }, "Should throw on null byte array");

        // Null MemorySegment
        assertThrows(IllegalArgumentException.class, () -> {
            Message.fromPool((MemorySegment) null, 10);
        }, "Should throw on null MemorySegment");

        // Invalid offset/length
        byte[] data = new byte[10];
        assertThrows(IllegalArgumentException.class, () -> {
            Message.fromPool(data, -1, 5);
        }, "Should throw on negative offset");

        assertThrows(IllegalArgumentException.class, () -> {
            Message.fromPool(data, 0, -1);
        }, "Should throw on negative length");

        assertThrows(IllegalArgumentException.class, () -> {
            Message.fromPool(data, 5, 10);
        }, "Should throw on offset+length > data.length");

        // Invalid size for MemorySegment
        MemorySegment seg = MemorySegment.ofArray(new byte[10]);
        assertThrows(IllegalArgumentException.class, () -> {
            Message.fromPool(seg, -1);
        }, "Should throw on negative size");

        assertThrows(IllegalArgumentException.class, () -> {
            Message.fromPool(seg, 20);
        }, "Should throw on size > segment size");
    }
}
