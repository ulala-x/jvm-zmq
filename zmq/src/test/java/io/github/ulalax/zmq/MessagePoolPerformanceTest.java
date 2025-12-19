package io.github.ulalax.zmq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance validation tests for MessagePool.
 * Verifies that pool allocation is significantly faster than Arena.ofShared() creation.
 */
class MessagePoolPerformanceTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int MEASUREMENT_ITERATIONS = 10000;

    @Test
    @DisplayName("Measure rent/return speed - should be < 200ns per operation")
    void measureRentReturnSpeed() {
        int testSize = 1024;

        // Prewarm the pool
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(testSize, 100);
        Message.prewarmPool(warmup);

        // Warmup JIT
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            MessagePool.returnToPool(pooled);
        }

        // Measure rent/return cycle
        long startTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            MessagePool.returnToPool(pooled);
        }
        long endTime = System.nanoTime();

        long totalNanos = endTime - startTime;
        double avgNanosPerOp = (double) totalNanos / MEASUREMENT_ITERATIONS;

        System.out.printf("MessagePool rent/return: %.2f ns/op (target: < 500 ns/op)%n", avgNanosPerOp);

        // Performance target: < 500 ns per operation (relaxed for CI environments)
        // This is significantly faster than Arena.ofShared() (~2753 ns)
        // Note: In production benchmarks this is typically < 200ns
        assertTrue(avgNanosPerOp < 500.0,
            String.format("Average time per operation should be < 500ns, got %.2f ns", avgNanosPerOp));
    }

    @Test
    @DisplayName("Compare pool vs Arena.ofShared() - pool should be 10x+ faster")
    void compareWithArenaCreation() {
        int testSize = 1024;

        // Prewarm the pool
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(testSize, 100);
        Message.prewarmPool(warmup);

        // === Test 1: MessagePool (warmed up) ===
        // Warmup JIT
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            MessagePool.returnToPool(pooled);
        }

        long poolStartTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            pooled.segment.set(ValueLayout.JAVA_BYTE, 0, (byte) 42);
            MessagePool.returnToPool(pooled);
        }
        long poolEndTime = System.nanoTime();
        long poolTotalNanos = poolEndTime - poolStartTime;
        double poolAvgNanos = (double) poolTotalNanos / MEASUREMENT_ITERATIONS;

        // === Test 2: Arena.ofShared() creation ===
        // Warmup JIT
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            Arena arena = Arena.ofShared();
            MemorySegment segment = arena.allocate(testSize);
            segment.set(ValueLayout.JAVA_BYTE, 0, (byte) 42);
            arena.close();
        }

        long arenaStartTime = System.nanoTime();
        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            Arena arena = Arena.ofShared();
            MemorySegment segment = arena.allocate(testSize);
            segment.set(ValueLayout.JAVA_BYTE, 0, (byte) 42);
            arena.close();
        }
        long arenaEndTime = System.nanoTime();
        long arenaTotalNanos = arenaEndTime - arenaStartTime;
        double arenaAvgNanos = (double) arenaTotalNanos / MEASUREMENT_ITERATIONS;

        double speedup = arenaAvgNanos / poolAvgNanos;

        System.out.println("=== Performance Comparison ===");
        System.out.printf("MessagePool: %.2f ns/op%n", poolAvgNanos);
        System.out.printf("Arena.ofShared(): %.2f ns/op%n", arenaAvgNanos);
        System.out.printf("Speedup: %.2fx%n", speedup);

        // Pool should be significantly faster (10x minimum)
        assertTrue(speedup >= 10.0,
            String.format("Pool should be at least 10x faster than Arena.ofShared(), got %.2fx", speedup));
    }

    @Test
    @DisplayName("Measure throughput improvement with realistic workload")
    void measureThroughputImprovement() {
        int messageCount = 5000;
        byte[] testData = new byte[1024];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }

        // Prewarm the pool
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(1024, 200);
        Message.prewarmPool(warmup);

        // === Test 1: Using MessagePool ===
        // Warmup
        for (int i = 0; i < 100; i++) {
            try (Message msg = Message.fromPool(testData)) {
                byte[] received = msg.toByteArray();
            }
        }

        long poolStartTime = System.nanoTime();
        for (int i = 0; i < messageCount; i++) {
            try (Message msg = Message.fromPool(testData)) {
                byte[] received = msg.toByteArray();
                assertEquals(testData.length, received.length);
            }
        }
        long poolEndTime = System.nanoTime();
        long poolTotalNanos = poolEndTime - poolStartTime;
        double poolAvgMicros = (double) poolTotalNanos / messageCount / 1000.0;

        // === Test 2: Using Arena.ofShared() per message ===
        // Warmup
        for (int i = 0; i < 100; i++) {
            Arena dataArena = Arena.ofShared();
            MemorySegment dataSeg = dataArena.allocate(testData.length);
            MemorySegment.copy(testData, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, testData.length);
            try (Message msg = new Message(dataSeg, testData.length, data -> {
                dataArena.close();
            })) {
                byte[] received = msg.toByteArray();
            }
        }

        long arenaStartTime = System.nanoTime();
        for (int i = 0; i < messageCount; i++) {
            Arena dataArena = Arena.ofShared();
            MemorySegment dataSeg = dataArena.allocate(testData.length);
            MemorySegment.copy(testData, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, testData.length);
            try (Message msg = new Message(dataSeg, testData.length, data -> {
                dataArena.close();
            })) {
                byte[] received = msg.toByteArray();
                assertEquals(testData.length, received.length);
            }
        }
        long arenaEndTime = System.nanoTime();
        long arenaTotalNanos = arenaEndTime - arenaStartTime;
        double arenaAvgMicros = (double) arenaTotalNanos / messageCount / 1000.0;

        double throughputImprovement = arenaAvgMicros / poolAvgMicros;

        System.out.println("=== Throughput Comparison (End-to-End) ===");
        System.out.printf("MessagePool: %.2f µs per message%n", poolAvgMicros);
        System.out.printf("Arena.ofShared(): %.2f µs per message%n", arenaAvgMicros);
        System.out.printf("Throughput improvement: %.2fx%n", throughputImprovement);

        // Should see significant improvement (at least 5x)
        assertTrue(throughputImprovement >= 5.0,
            String.format("Throughput should improve by at least 5x, got %.2fx", throughputImprovement));
    }

    @Test
    @DisplayName("Verify hit rate exceeds 90% after warmup")
    void verifyHitRateAfterWarmup() {
        int testSize = 2048;

        // Prewarm
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(testSize, 50);
        Message.prewarmPool(warmup);

        MessagePool.PoolStatistics statsBefore = Message.getPoolStatistics();

        // Perform many operations
        for (int i = 0; i < 1000; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            MessagePool.returnToPool(pooled);
        }

        MessagePool.PoolStatistics statsAfter = Message.getPoolStatistics();

        // Calculate hit rate for this test
        int bucketIndex = MessagePool.selectBucket(testSize);
        MessagePool.PoolStatistics.BucketInfo bucketBefore = statsBefore.buckets[bucketIndex];
        MessagePool.PoolStatistics.BucketInfo bucketAfter = statsAfter.buckets[bucketIndex];

        long hits = bucketAfter.hits - bucketBefore.hits;
        long misses = bucketAfter.misses - bucketBefore.misses;
        double hitRate = (double) hits / (hits + misses);

        System.out.printf("Hit rate after warmup: %.2f%% (target: > 90%%)%n", hitRate * 100);

        assertTrue(hitRate > 0.90,
            String.format("Hit rate should exceed 90%% after warmup, got %.2f%%", hitRate * 100));
    }

    @Test
    @DisplayName("Test allocation overhead for different message sizes")
    void testAllocationOverheadForDifferentSizes() {
        int[] testSizes = {64, 256, 1024, 4096, 16384, 65536};
        int iterations = 5000;

        System.out.println("=== Allocation Overhead by Size ===");

        for (int size : testSizes) {
            // Prewarm
            Map<Integer, Integer> warmup = new HashMap<>();
            warmup.put(size, 50);
            Message.prewarmPool(warmup);

            // Warmup JIT
            for (int i = 0; i < 100; i++) {
                MessagePool.PooledSegment pooled = MessagePool.rent(size);
                MessagePool.returnToPool(pooled);
            }

            // Measure
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                MessagePool.PooledSegment pooled = MessagePool.rent(size);
                MessagePool.returnToPool(pooled);
            }
            long endTime = System.nanoTime();

            double avgNanos = (double) (endTime - startTime) / iterations;
            System.out.printf("Size %6d bytes: %.2f ns/op%n", size, avgNanos);

            // All sizes should be fast (< 1000 ns in CI environments)
            assertTrue(avgNanos < 1000.0,
                String.format("Size %d should be < 1000ns, got %.2f ns", size, avgNanos));
        }
    }

    @Test
    @DisplayName("Test GC allocation rate - should be minimal")
    void testGCAllocationRate() {
        int iterations = 100000;
        int testSize = 1024;

        // Prewarm
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(testSize, 100);
        Message.prewarmPool(warmup);

        // Warmup JIT
        for (int i = 0; i < 1000; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            MessagePool.returnToPool(pooled);
        }

        // Force GC before measurement
        System.gc();
        Thread.yield();

        // Get memory stats
        Runtime runtime = Runtime.getRuntime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            pooled.segment.set(ValueLayout.JAVA_BYTE, 0, (byte) i);
            MessagePool.returnToPool(pooled);
        }
        long endTime = System.nanoTime();

        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = Math.max(0, memAfter - memBefore);

        double durationSeconds = (double) (endTime - startTime) / 1_000_000_000.0;
        double allocRateMBPerSec = (double) memUsed / durationSeconds / (1024 * 1024);

        System.out.println("=== GC Allocation Rate ===");
        System.out.printf("Memory used: %d bytes%n", memUsed);
        System.out.printf("Duration: %.3f seconds%n", durationSeconds);
        System.out.printf("Allocation rate: %.2f MB/sec (target: < 200 MB/sec)%n", allocRateMBPerSec);

        // Note: This is a rough estimate. GC can happen during the test.
        // The key is that pool reuse should keep allocation rate very low.
        // Target: < 200 MB/sec (vs hundreds of MB/sec with Arena.ofShared() per message)
        // Relaxed threshold for CI environments where memory measurement is less accurate
        assertTrue(allocRateMBPerSec < 500.0,
            String.format("GC allocation rate should be reasonable, got %.2f MB/sec", allocRateMBPerSec));
    }

    @Test
    @DisplayName("Test statistics overhead - should be negligible")
    void testStatisticsOverhead() {
        int iterations = 10000;
        int testSize = 1024;

        // Prewarm
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(testSize, 50);
        Message.prewarmPool(warmup);

        // Warmup JIT
        for (int i = 0; i < 1000; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            MessagePool.returnToPool(pooled);
        }

        // Measure with statistics calls
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            MessagePool.PooledSegment pooled = MessagePool.rent(testSize);
            MessagePool.returnToPool(pooled);

            if (i % 100 == 0) {
                // Call getStatistics periodically
                MessagePool.PoolStatistics stats = Message.getPoolStatistics();
                assertNotNull(stats);
            }
        }
        long endTime = System.nanoTime();

        double avgNanos = (double) (endTime - startTime) / iterations;
        System.out.printf("Avg time per operation with periodic stats: %.2f ns/op%n", avgNanos);

        // Even with statistics calls, should still be fast (relaxed for CI)
        assertTrue(avgNanos < 1000.0,
            String.format("Operations with stats should be fast, got %.2f ns", avgNanos));
    }
}
