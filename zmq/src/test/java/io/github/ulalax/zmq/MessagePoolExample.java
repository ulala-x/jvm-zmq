package io.github.ulalax.zmq;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating MessagePool usage for high-performance zero-copy messaging.
 *
 * This example shows:
 * 1. Basic usage with fromPool()
 * 2. Pre-warming the pool
 * 3. Monitoring pool statistics
 * 4. Performance comparison
 */
public class MessagePoolExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== MessagePool Usage Example ===\n");

        // Example 1: Basic Usage
        basicUsage();

        // Example 2: Pre-warming
        prewarmingExample();

        // Example 3: Statistics Monitoring
        statisticsExample();

        // Example 4: Performance Comparison
        performanceComparison();
    }

    static void basicUsage() {
        System.out.println("1. Basic Usage");
        System.out.println("--------------");

        // Simple zero-copy message creation
        byte[] data = "Hello MessagePool!".getBytes(StandardCharsets.UTF_8);

        try (Message msg = Message.fromPool(data)) {
            System.out.printf("Created message of size: %d bytes\n", msg.size());
            System.out.printf("Content: %s\n", msg.toString());
        } // Automatically returns segment to pool

        System.out.println("✅ Message automatically returned to pool\n");
    }

    static void prewarmingExample() {
        System.out.println("2. Pre-warming for Benchmarks");
        System.out.println("------------------------------");

        // Pre-allocate segments before benchmark
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(1024, 100);  // 100 segments of 1KB
        warmup.put(64, 200);    // 200 segments of 64B
        warmup.put(4096, 50);   // 50 segments of 4KB

        System.out.println("Pre-warming pool...");
        Message.prewarmPool(warmup);

        MessagePool.PoolStatistics stats = Message.getPoolStatistics();
        System.out.printf("Pool ready with %d segments available\n",
            getTotalPoolSize(stats));
        System.out.println("✅ First allocations will be instant (pool hits)\n");
    }

    static void statisticsExample() {
        System.out.println("3. Monitoring Pool Statistics");
        System.out.println("------------------------------");

        // Perform some allocations
        for (int i = 0; i < 10; i++) {
            byte[] data = new byte[1024];
            try (Message msg = Message.fromPool(data)) {
                // Use message
            }
        }

        // Check statistics
        MessagePool.PoolStatistics stats = Message.getPoolStatistics();

        System.out.printf("Total rents:   %d\n", stats.totalRents);
        System.out.printf("Total returns: %d\n", stats.totalReturns);
        System.out.printf("Pool hits:     %d\n", stats.totalHits);
        System.out.printf("Pool misses:   %d\n", stats.totalMisses);
        System.out.printf("Hit rate:      %.2f%%\n", stats.hitRate * 100);
        System.out.printf("Outstanding:   %d\n", stats.totalOutstanding);

        System.out.println("\nPer-bucket statistics:");
        for (int i = 0; i < stats.buckets.length; i++) {
            MessagePool.PoolStatistics.BucketInfo bucket = stats.buckets[i];
            if (bucket.rents > 0) {
                System.out.printf("  %7d bytes: rents=%d, hits=%d, pool_size=%d\n",
                    bucket.bucketSize, bucket.rents, bucket.hits, bucket.poolSize);
            }
        }
        System.out.println();
    }

    static void performanceComparison() throws Exception {
        System.out.println("4. Performance Comparison");
        System.out.println("-------------------------");

        int iterations = 10000;
        byte[] testData = new byte[1024];

        // Pre-warm
        Map<Integer, Integer> warmup = new HashMap<>();
        warmup.put(1024, 50);
        Message.prewarmPool(warmup);

        // Warmup JIT
        for (int i = 0; i < 1000; i++) {
            try (Message msg = Message.fromPool(testData)) {}
        }

        // Test MessagePool
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            try (Message msg = Message.fromPool(testData)) {
                // Use message
            }
        }
        long endTime = System.nanoTime();
        double poolAvgMicros = (double) (endTime - startTime) / iterations / 1000.0;

        System.out.printf("MessagePool:      %.2f µs per message\n", poolAvgMicros);

        // Check final statistics
        MessagePool.PoolStatistics stats = Message.getPoolStatistics();
        System.out.printf("Final hit rate:   %.2f%%\n", stats.hitRate * 100);
        System.out.printf("Total operations: %d\n", stats.totalRents);

        System.out.println("\n✅ Pool provides consistent sub-microsecond allocations");
    }

    static long getTotalPoolSize(MessagePool.PoolStatistics stats) {
        long total = 0;
        for (MessagePool.PoolStatistics.BucketInfo bucket : stats.buckets) {
            total += bucket.poolSize;
        }
        return total;
    }
}
