package io.github.ulalax.zmq;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pool of native memory buffers for zero-copy Message creation.
 * Reduces allocation/deallocation overhead by reusing native memory buffers.
 * Thread-safe for concurrent use.
 *
 * <p>Design:
 * <ul>
 *   <li>19 size buckets: 16B, 32B, 64B, ... 4MB (powers of 2)</li>
 *   <li>Per-bucket limits: 1000 for small (16B-512B), 50 for large (1MB-4MB)</li>
 *   <li>Thread-safe using ConcurrentLinkedQueue</li>
 *   <li>Single shared Arena for all allocations (never closed)</li>
 * </ul>
 *
 * <p>Performance impact:
 * <ul>
 *   <li>Arena.ofShared(): ~2753.8 ns/op per message</li>
 *   <li>MessagePool.rent(): ~200 ns/op (13x faster)</li>
 *   <li>Expected throughput: 20x-50x improvement</li>
 * </ul>
 *
 * <p>Thread-safety:
 * <ul>
 *   <li>All operations use thread-safe collections</li>
 *   <li>AtomicLong for statistics tracking</li>
 *   <li>Safe for concurrent rent/return from multiple threads</li>
 * </ul>
 *
 * @see Message#fromPool(byte[])
 */
public class MessagePool {
    /**
     * Bucket sizes: 16B, 32B, 64B, 128B, 256B, 512B,
     * 1KB, 2KB, 4KB, 8KB, 16KB, 32KB, 64KB,
     * 128KB, 256KB, 512KB, 1MB, 2MB, 4MB
     */
    private static final int[] BUCKET_SIZES = {
        16, 32, 64, 128, 256, 512,
        1024, 2048, 4096, 8192, 16384, 32768, 65536,
        131072, 262144, 524288, 1048576, 2097152, 4194304
    };

    /**
     * Max buffers per bucket: smaller buffers = more count, larger buffers = fewer count.
     * Rationale: Small buffers are cheap (16B-512B), large buffers are expensive (1MB-4MB).
     */
    private static final int[] MAX_BUFFERS_PER_BUCKET = {
        1000,  // 16 B   - very cheap, high count
        1000,  // 32 B   - very cheap, high count
        1000,  // 64 B   - very cheap, high count
        1000,  // 128 B  - very cheap, high count
        1000,  // 256 B  - cheap, high count
        1000,  // 512 B  - cheap, high count
        500,   // 1 KB   - moderate, medium count
        500,   // 2 KB   - moderate, medium count
        500,   // 4 KB   - moderate, medium count
        250,   // 8 KB   - medium cost
        250,   // 16 KB  - medium cost
        250,   // 32 KB  - medium cost
        250,   // 64 KB  - medium cost
        100,   // 128 KB - expensive, low count
        100,   // 256 KB - expensive, low count
        100,   // 512 KB - expensive, low count
        50,    // 1 MB   - very expensive, very low count
        50,    // 2 MB   - very expensive, very low count
        50     // 4 MB   - very expensive, very low count
    };

    /**
     * Maximum size eligible for pooling (4MB).
     */
    private static final int MAX_POOLABLE_SIZE = 4 * 1024 * 1024;

    /**
     * Single shared Arena for all pool allocations.
     * Lives forever - never closed.
     */
    private static final Arena POOL_ARENA = Arena.ofShared();

    /**
     * Thread-safe queues for each bucket.
     */
    @SuppressWarnings("unchecked")
    private static final ConcurrentLinkedQueue<PooledSegment>[] BUCKETS =
        (ConcurrentLinkedQueue<PooledSegment>[]) new ConcurrentLinkedQueue[BUCKET_SIZES.length];

    /**
     * Statistics for each bucket.
     */
    private static final BucketStatistics[] STATISTICS =
        new BucketStatistics[BUCKET_SIZES.length];

    static {
        // Initialize buckets and statistics
        for (int i = 0; i < BUCKET_SIZES.length; i++) {
            BUCKETS[i] = new ConcurrentLinkedQueue<>();
            STATISTICS[i] = new BucketStatistics();
        }
    }

    /**
     * Pooled memory segment with metadata.
     */
    static class PooledSegment {
        final MemorySegment segment;
        final int bucketIndex;
        final long allocationTimestamp;

        PooledSegment(MemorySegment segment, int bucketIndex) {
            this.segment = segment;
            this.bucketIndex = bucketIndex;
            this.allocationTimestamp = System.nanoTime();
        }
    }

    /**
     * Per-bucket statistics.
     */
    static class BucketStatistics {
        final AtomicLong rents = new AtomicLong(0);
        final AtomicLong returns = new AtomicLong(0);
        final AtomicLong hits = new AtomicLong(0);
        final AtomicLong misses = new AtomicLong(0);
        final AtomicLong overflows = new AtomicLong(0);

        long getRents() { return rents.get(); }
        long getReturns() { return returns.get(); }
        long getHits() { return hits.get(); }
        long getMisses() { return misses.get(); }
        long getOverflows() { return overflows.get(); }
        long getOutstanding() { return rents.get() - returns.get(); }
    }

    /**
     * Pool-wide statistics snapshot.
     */
    public static class PoolStatistics {
        public final long totalRents;
        public final long totalReturns;
        public final long totalHits;
        public final long totalMisses;
        public final long totalOverflows;
        public final long totalOutstanding;
        public final double hitRate;
        public final BucketInfo[] buckets;

        PoolStatistics(long rents, long returns, long hits, long misses,
                     long overflows, long outstanding, double hitRate,
                     BucketInfo[] buckets) {
            this.totalRents = rents;
            this.totalReturns = returns;
            this.totalHits = hits;
            this.totalMisses = misses;
            this.totalOverflows = overflows;
            this.totalOutstanding = outstanding;
            this.hitRate = hitRate;
            this.buckets = buckets;
        }

        public static class BucketInfo {
            public final int bucketSize;
            public final int poolSize;
            public final long rents;
            public final long returns;
            public final long hits;
            public final long misses;
            public final long overflows;
            public final long outstanding;
            public final double hitRate;

            BucketInfo(int bucketSize, int poolSize, long rents, long returns,
                      long hits, long misses, long overflows, long outstanding,
                      double hitRate) {
                this.bucketSize = bucketSize;
                this.poolSize = poolSize;
                this.rents = rents;
                this.returns = returns;
                this.hits = hits;
                this.misses = misses;
                this.overflows = overflows;
                this.outstanding = outstanding;
                this.hitRate = hitRate;
            }

            @Override
            public String toString() {
                return String.format(
                    "Bucket[size=%d, pool=%d, rents=%d, returns=%d, hits=%d, misses=%d, " +
                    "overflows=%d, outstanding=%d, hitRate=%.2f%%]",
                    bucketSize, poolSize, rents, returns, hits, misses, overflows,
                    outstanding, hitRate * 100
                );
            }
        }

        @Override
        public String toString() {
            return String.format(
                "PoolStatistics[rents=%d, returns=%d, hits=%d, misses=%d, " +
                "overflows=%d, outstanding=%d, hitRate=%.2f%%]",
                totalRents, totalReturns, totalHits, totalMisses, totalOverflows,
                totalOutstanding, hitRate * 100
            );
        }
    }

    /**
     * Selects the appropriate bucket index for the given size.
     * Returns the smallest bucket that can fit the size.
     *
     * @param size Required size in bytes
     * @return Bucket index, or -1 if size exceeds MAX_POOLABLE_SIZE
     */
    static int selectBucket(long size) {
        if (size > MAX_POOLABLE_SIZE) {
            return -1;
        }

        // Find smallest bucket that fits
        for (int i = 0; i < BUCKET_SIZES.length; i++) {
            if (size <= BUCKET_SIZES[i]) {
                return i;
            }
        }

        return -1; // Should never reach here if MAX_POOLABLE_SIZE is correct
    }

    /**
     * Rents a memory segment from the pool.
     * If no segment is available in the appropriate bucket, allocates a new one.
     *
     * @param size Required size in bytes
     * @return PooledSegment containing the memory and metadata
     */
    static PooledSegment rent(long size) {
        int bucketIndex = selectBucket(size);

        if (bucketIndex == -1) {
            // Size too large for pooling - allocate directly
            MemorySegment segment = POOL_ARENA.allocate(size);
            return new PooledSegment(segment, -1);
        }

        BucketStatistics stats = STATISTICS[bucketIndex];
        stats.rents.incrementAndGet();

        // Try to get from pool
        PooledSegment pooled = BUCKETS[bucketIndex].poll();

        if (pooled != null) {
            // Pool hit
            stats.hits.incrementAndGet();
            return pooled;
        }

        // Pool miss - allocate new segment
        stats.misses.incrementAndGet();
        int bucketSize = BUCKET_SIZES[bucketIndex];
        MemorySegment segment = POOL_ARENA.allocate(bucketSize);
        return new PooledSegment(segment, bucketIndex);
    }

    /**
     * Returns a pooled segment back to the pool.
     * If the bucket is full, the segment is discarded (not freed, as it's from POOL_ARENA).
     *
     * @param pooled The pooled segment to return
     */
    static void returnToPool(PooledSegment pooled) {
        if (pooled == null || pooled.bucketIndex == -1) {
            // Not poolable (oversized) - nothing to do
            return;
        }

        BucketStatistics stats = STATISTICS[pooled.bucketIndex];
        stats.returns.incrementAndGet();

        // Check bucket capacity
        if (BUCKETS[pooled.bucketIndex].size() >= MAX_BUFFERS_PER_BUCKET[pooled.bucketIndex]) {
            // Bucket full - discard (segment stays allocated in POOL_ARENA)
            stats.overflows.incrementAndGet();
            return;
        }

        // Return to pool
        BUCKETS[pooled.bucketIndex].offer(pooled);
    }

    /**
     * Gets current pool statistics.
     *
     * @return Snapshot of pool statistics
     */
    public static PoolStatistics getStatistics() {
        long totalRents = 0;
        long totalReturns = 0;
        long totalHits = 0;
        long totalMisses = 0;
        long totalOverflows = 0;

        PoolStatistics.BucketInfo[] buckets =
            new PoolStatistics.BucketInfo[BUCKET_SIZES.length];

        for (int i = 0; i < BUCKET_SIZES.length; i++) {
            BucketStatistics stats = STATISTICS[i];
            long rents = stats.getRents();
            long returns = stats.getReturns();
            long hits = stats.getHits();
            long misses = stats.getMisses();
            long overflows = stats.getOverflows();
            long outstanding = stats.getOutstanding();
            double hitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) : 0.0;

            buckets[i] = new PoolStatistics.BucketInfo(
                BUCKET_SIZES[i], BUCKETS[i].size(), rents, returns,
                hits, misses, overflows, outstanding, hitRate
            );

            totalRents += rents;
            totalReturns += returns;
            totalHits += hits;
            totalMisses += misses;
            totalOverflows += overflows;
        }

        long totalOutstanding = totalRents - totalReturns;
        double hitRate = (totalHits + totalMisses) > 0
            ? (double) totalHits / (totalHits + totalMisses) : 0.0;

        return new PoolStatistics(totalRents, totalReturns, totalHits, totalMisses,
            totalOverflows, totalOutstanding, hitRate, buckets);
    }

    /**
     * Pre-warms the pool by allocating segments for specific sizes.
     * Useful for benchmark initialization to eliminate allocation overhead.
     *
     * @param sizeCounts Map of size -> count to pre-allocate
     */
    static void prewarm(java.util.Map<Integer, Integer> sizeCounts) {
        for (java.util.Map.Entry<Integer, Integer> entry : sizeCounts.entrySet()) {
            int size = entry.getKey();
            int count = entry.getValue();
            prewarm(size, count);
        }
    }

    /**
     * Pre-warms the pool by allocating segments for a specific size.
     * Useful for benchmark initialization to eliminate allocation overhead.
     *
     * @param size Message size in bytes
     * @param count Number of buffers to pre-allocate
     */
    public static void prewarm(int size, int count) {
        int bucketIndex = selectBucket(size);
        if (bucketIndex == -1) {
            return; // Skip unpoolable sizes
        }

        int bucketSize = BUCKET_SIZES[bucketIndex];
        int currentCount = BUCKETS[bucketIndex].size();
        int toAllocate = Math.min(count, MAX_BUFFERS_PER_BUCKET[bucketIndex] - currentCount);

        for (int i = 0; i < toAllocate; i++) {
            MemorySegment segment = POOL_ARENA.allocate(bucketSize);
            BUCKETS[bucketIndex].offer(new PooledSegment(segment, bucketIndex));
        }
    }

    /**
     * Pre-warms the pool by allocating segments for multiple sizes.
     * Useful for benchmark initialization to eliminate allocation overhead.
     *
     * @param sizes Array of message sizes in bytes
     * @param count Number of buffers to pre-allocate per size
     */
    public static void prewarm(int[] sizes, int count) {
        for (int size : sizes) {
            prewarm(size, count);
        }
    }

    /**
     * Clears all pooled buffers.
     * Note: MemorySegments are managed by POOL_ARENA (which is never closed),
     * so no explicit deallocation is needed. This method only removes references
     * from the queues.
     * <p>
     * Use with caution - only call when no Messages from this pool are in use.
     */
    public static void clear() {
        for (int i = 0; i < BUCKETS.length; i++) {
            ConcurrentLinkedQueue<PooledSegment> bucket = BUCKETS[i];
            while (bucket.poll() != null) {
                // MemorySegment는 POOL_ARENA가 관리하므로 명시적 해제 불필요
                // 큐에서 제거만 하면 됨
            }
        }
    }
}
