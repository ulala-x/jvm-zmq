package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.LibZmq;
import io.github.ulalax.zmq.core.ZmqStructs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A high-performance message pooling system for ZeroMQ messages.
 *
 * <p>This class implements a sophisticated memory pool for {@link Message} objects using
 * a two-tier caching strategy to minimize allocation overhead and garbage collection pressure.</p>
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li><strong>19 Size Buckets:</strong> Messages are allocated in power-of-2 size buckets
 *       from 16 bytes to 4 megabytes</li>
 *   <li><strong>Thread-Local Cache (Tier 1):</strong> Each thread maintains a fast cache
 *       with up to 8 messages per bucket for lock-free access</li>
 *   <li><strong>Shared Pool (Tier 2):</strong> A global {@link ConcurrentLinkedQueue} per bucket
 *       provides thread-safe sharing when thread-local caches are exhausted</li>
 *   <li><strong>Zero-Copy Integration:</strong> Uses {@code zmq_msg_init_data} to enable
 *       zero-copy message passing with automatic pool return via callbacks</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Get a message from the pool
 * Message msg = MessagePool.SHARED.rent(1024);
 * msg.getPoolDataPtr().copyFrom(MemorySegment.ofArray(myData));
 *
 * // Send the message (automatically returns to pool)
 * socket.send(msg);
 * msg.close(); // Safe to close - pool callback handles return
 *
 * // Rent with data
 * Message msg2 = MessagePool.SHARED.rent(new byte[]{1, 2, 3});
 * socket.send(msg2);
 * msg2.close();
 * }</pre>
 *
 * <h2>Performance Optimization</h2>
 * <pre>{@code
 * // Pre-warm the pool for expected workload
 * MessagePool.SHARED.prewarm(MessageSize.SIZE_1K, 100);
 * MessagePool.SHARED.prewarm(MessageSize.SIZE_4K, 50);
 *
 * // Configure max buffers per bucket
 * MessagePool.SHARED.setMaxBuffers(MessageSize.SIZE_1M, 10);
 *
 * // Monitor pool health
 * PoolStatistics stats = MessagePool.SHARED.getStatistics();
 * System.out.println("Hit rate: " + stats.getHitRate());
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is fully thread-safe. All operations are lock-free or use concurrent
 * data structures. Thread-local caches eliminate contention for most operations.</p>
 *
 * @see Message
 * @see MessageSize
 */
public final class MessagePool {

    /**
     * Shared singleton instance for application-wide message pooling.
     */
    public static final MessagePool SHARED = new MessagePool();

    // ========== Constants ==========

    /**
     * Bucket sizes in bytes (19 buckets: 16B to 4MB).
     */
    private static final int[] BUCKET_SIZES = {
        16, 32, 64, 128, 256, 512,
        1024, 2048, 4096, 8192, 16384, 32768,
        65536, 131072, 262144, 524288,
        1048576, 2097152, 4194304
    };

    /**
     * Number of buckets.
     */
    private static final int BUCKET_COUNT = 19;

    /**
     * Maximum messages per bucket in thread-local cache.
     */
    private static final int THREAD_LOCAL_CACHE_SIZE = 8;

    /**
     * Default maximum buffers per bucket in shared pool.
     */
    private final int[] maxBuffersPerBucket = {
        1000, 1000, 1000, 1000, 1000, 1000,    // 16B ~ 512B
        1000, 500, 500, 500, 250, 250,         // 1K ~ 32K
        100, 100, 100, 100,                    // 64K ~ 512K
        50, 50, 50                             // 1M ~ 4M
    };

    // ========== Pool Data Structures ==========

    /**
     * Thread-local cache (Tier 1): Fast, lock-free access per thread.
     */
    private final ThreadLocal<ThreadLocalCache> threadLocalCache =
        ThreadLocal.withInitial(ThreadLocalCache::new);

    /**
     * Shared pool (Tier 2): Global concurrent queue per bucket.
     */
    @SuppressWarnings("unchecked")
    private final ConcurrentLinkedQueue<Message>[] sharedPool = new ConcurrentLinkedQueue[BUCKET_COUNT];

    /**
     * Counter for number of messages in each shared pool bucket.
     */
    private final AtomicInteger[] pooledMessageCounts = new AtomicInteger[BUCKET_COUNT];

    // ========== Statistics ==========

    private final AtomicLong totalRents = new AtomicLong();
    private final AtomicLong totalReturns = new AtomicLong();
    private final AtomicLong poolHits = new AtomicLong();
    private final AtomicLong poolMisses = new AtomicLong();
    private final AtomicLong poolRejects = new AtomicLong();

    // ========== Zero-Copy Callback Support ==========

    /**
     * Singleton callback stub for zmq_msg_init_data.
     * Lazily initialized and lives in global arena.
     */
    private static MemorySegment poolCallbackStub = null;

    /**
     * Function descriptor for ZMQ free callback.
     */
    private static final FunctionDescriptor FREE_CALLBACK_DESC =
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS);

    /**
     * Maps callback ID (stored in hint) to Message instance for pool return.
     */
    private static final ConcurrentHashMap<Long, Message> CALLBACK_MESSAGE_MAP =
        new ConcurrentHashMap<>();

    /**
     * Atomic counter for generating unique callback IDs.
     */
    private static final AtomicLong CALLBACK_ID_GENERATOR = new AtomicLong(0);

    /**
     * Pool for hint pointers (8-byte MemorySegments storing callback IDs).
     */
    private static final ConcurrentLinkedQueue<MemorySegment> HINT_PTR_POOL =
        new ConcurrentLinkedQueue<>();

    /**
     * Shared arena for allocating hint pointers.
     */
    private static final Arena HINT_ARENA = Arena.ofShared();

    /**
     * Shared arena for allocating all pooled message data buffers.
     * This arena lives forever and is never closed - all pooled messages share it.
     * Individual buffers are not freed; instead, they are recycled through the pool.
     */
    private static final Arena POOL_DATA_ARENA = Arena.ofShared();

    static {
        // Pre-allocate 1000 hint pointers
        for (int i = 0; i < 1000; i++) {
            HINT_PTR_POOL.offer(HINT_ARENA.allocate(ValueLayout.JAVA_LONG));
        }
    }

    // ========== Constructor ==========

    /**
     * Creates a new MessagePool instance.
     * Initialize all shared pool queues and counters.
     */
    public MessagePool() {
        for (int i = 0; i < BUCKET_COUNT; i++) {
            sharedPool[i] = new ConcurrentLinkedQueue<>();
            pooledMessageCounts[i] = new AtomicInteger(0);
        }
    }

    // ========== Public API ==========

    /**
     * Rents a message of the specified size from the pool.
     *
     * <p>The pool automatically selects the smallest bucket that can accommodate the
     * requested size. The returned message may have a larger buffer than requested.</p>
     *
     * <p><strong>Lifecycle:</strong></p>
     * <ol>
     *   <li>Check thread-local cache (fast path)</li>
     *   <li>Check shared pool (slower path)</li>
     *   <li>Allocate new message if pool is exhausted (slowest path)</li>
     * </ol>
     *
     * <p>The message is automatically returned to the pool when ZeroMQ releases it
     * after sending, or when {@link Message#close()} is called for unsent pooled messages.</p>
     *
     * @param size The requested size in bytes
     * @return A {@link Message} with buffer size &ge; size
     * @throws IllegalArgumentException if size is negative or exceeds maximum bucket size
     */
    public Message rent(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative: " + size);
        }

        // Select appropriate bucket
        int bucketIndex = selectBucket(size);
        int bucketSize = BUCKET_SIZES[bucketIndex];

        totalRents.incrementAndGet();

        // Tier 1: Try thread-local cache first
        ThreadLocalCache cache = threadLocalCache.get();
        Message msg = cache.tryGet(bucketIndex);
        if (msg != null) {
            poolHits.incrementAndGet();
            msg.prepareForReuse();
            msg.actualDataSize = size; // Set directly without calling zmq_msg_init_data
            return msg;
        }

        // Tier 2: Try shared pool
        msg = sharedPool[bucketIndex].poll();
        if (msg != null) {
            pooledMessageCounts[bucketIndex].decrementAndGet();
            poolHits.incrementAndGet();
            msg.prepareForReuse();
            msg.actualDataSize = size; // Set directly without calling zmq_msg_init_data
            return msg;
        }

        // Tier 3: Allocate new message
        poolMisses.incrementAndGet();
        return createPooledMessage(bucketSize, bucketIndex, size);
    }

    /**
     * Rents a message from the pool and copies the provided data into it.
     *
     * <p>This is a convenience method that combines {@link #rent(int)} with data copying.</p>
     *
     * @param data The data to copy into the message
     * @return A {@link Message} containing the data
     * @throws NullPointerException if data is null
     */
    public Message rent(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data cannot be null");
        }

        Message msg = rent(data.length);
        if (data.length > 0) {
            msg.getPoolDataPtr().copyFrom(MemorySegment.ofArray(data));
        }
        // Initialize zmq_msg_t with actual data after copying
        msg.setActualDataSize(data.length);
        return msg;
    }

    /**
     * Pre-allocates messages in the pool for the specified size bucket.
     *
     * <p>Pre-warming improves performance by avoiding allocations during
     * critical paths. Call this method during application startup.</p>
     *
     * @param size The message size bucket to pre-warm
     * @param count The number of messages to pre-allocate
     * @throws IllegalArgumentException if count is negative
     */
    public void prewarm(MessageSize size, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative: " + count);
        }

        int bucketIndex = size.getBucketIndex();
        int bucketSize = size.getBytes();

        for (int i = 0; i < count; i++) {
            Message msg = createPooledMessage(bucketSize, bucketIndex, bucketSize);
            sharedPool[bucketIndex].offer(msg);
            pooledMessageCounts[bucketIndex].incrementAndGet();
        }
    }

    /**
     * Pre-allocates messages for multiple size buckets.
     *
     * @param sizes The message size buckets to pre-warm
     * @param count The number of messages to pre-allocate per bucket
     */
    public void prewarm(MessageSize[] sizes, int count) {
        for (MessageSize size : sizes) {
            prewarm(size, count);
        }
    }

    /**
     * Pre-allocates messages with custom configuration per bucket.
     *
     * @param configuration Map of {@link MessageSize} to count
     */
    public void prewarm(Map<MessageSize, Integer> configuration) {
        for (Map.Entry<MessageSize, Integer> entry : configuration.entrySet()) {
            prewarm(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets the maximum number of messages to keep in the shared pool for a specific size bucket.
     *
     * <p>When the pool exceeds this limit, returned messages are disposed instead of cached.</p>
     *
     * @param size The message size bucket
     * @param maxBuffers The maximum number of messages to pool (must be &ge; 0)
     * @throws IllegalArgumentException if maxBuffers is negative
     */
    public void setMaxBuffers(MessageSize size, int maxBuffers) {
        if (maxBuffers < 0) {
            throw new IllegalArgumentException("maxBuffers cannot be negative: " + maxBuffers);
        }

        int bucketIndex = size.getBucketIndex();
        maxBuffersPerBucket[bucketIndex] = maxBuffers;
    }

    /**
     * Gets the maximum number of messages allowed in the shared pool for a specific size bucket.
     *
     * @param size The message size bucket
     * @return The maximum buffer count
     */
    public int getMaxBuffers(MessageSize size) {
        return maxBuffersPerBucket[size.getBucketIndex()];
    }

    /**
     * Gets comprehensive statistics about pool performance.
     *
     * @return A {@link PoolStatistics} snapshot
     */
    public PoolStatistics getStatistics() {
        long rents = totalRents.get();
        long returns = totalReturns.get();
        long hits = poolHits.get();
        long misses = poolMisses.get();
        long rejects = poolRejects.get();
        long outstanding = rents - returns;

        return new PoolStatistics(rents, returns, hits, misses, rejects, outstanding);
    }

    /**
     * Gets the current count of pooled messages per bucket.
     *
     * @return Map of bucket index to message count
     */
    public Map<Integer, Integer> getPoolCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < BUCKET_COUNT; i++) {
            counts.put(i, pooledMessageCounts[i].get());
        }
        return counts;
    }

    /**
     * Clears all messages from the pool and resets statistics.
     *
     * <p><strong>Warning:</strong> This does not affect messages currently in use.
     * Outstanding messages will still attempt to return to the pool when released.</p>
     */
    public void clear() {
        // Clear all shared pool queues
        for (int i = 0; i < BUCKET_COUNT; i++) {
            Message msg;
            while ((msg = sharedPool[i].poll()) != null) {
                msg.disposePooledMessage();
            }
            pooledMessageCounts[i].set(0);
        }

        // Reset statistics
        totalRents.set(0);
        totalReturns.set(0);
        poolHits.set(0);
        poolMisses.set(0);
        poolRejects.set(0);

        // Note: Cannot clear thread-local caches from here
        // They will be cleared naturally when threads access them
    }

    // ========== Internal Methods ==========


    /**
     * Selects the appropriate bucket index for the given size.
     *
     * @param size The requested size in bytes
     * @return The bucket index (0-18)
     */
    private static int selectBucket(int size) {
        if (size <= 16) {
            return 0;
        }

        // Find the smallest bucket that fits the size
        // Using Integer.numberOfLeadingZeros for fast power-of-2 computation
        int bits = 32 - Integer.numberOfLeadingZeros(size - 1);
        int bucketIndex = bits - 4; // 2^4 = 16 (first bucket)

        if (bucketIndex >= BUCKET_COUNT) {
            return BUCKET_COUNT - 1; // Max bucket
        }

        return bucketIndex;
    }

    /**
     * Creates a new pooled message with zero-copy callback.
     *
     * @param bucketSize The bucket size (allocated buffer size)
     * @param bucketIndex The bucket index
     * @param actualSize The actual data size (may be smaller than bucketSize)
     * @return A new {@link Message} configured for pooling
     */
    private Message createPooledMessage(int bucketSize, int bucketIndex, int actualSize) {
        // Allocate native memory for message data from shared pool arena
        MemorySegment dataPtr = POOL_DATA_ARENA.allocate(bucketSize);

        // Create Message with skipInit constructor
        Message msg = new Message(true);
        msg.isFromPool = true;
        msg.poolDataPtr = dataPtr;
        msg.poolBucketIndex = bucketIndex;
        msg.bufferSize = bucketSize;
        msg.actualDataSize = actualSize;

        // Generate unique callback ID
        long callbackId = CALLBACK_ID_GENERATOR.incrementAndGet();

        // Store message in callback map for pool return
        CALLBACK_MESSAGE_MAP.put(callbackId, msg);

        // Get or allocate hint pointer
        MemorySegment hint = HINT_PTR_POOL.poll();
        if (hint == null) {
            hint = HINT_ARENA.allocate(ValueLayout.JAVA_LONG);
        }
        hint.set(ValueLayout.JAVA_LONG, 0, callbackId);

        // Make hint final for lambda capture
        final MemorySegment hintPtr = hint;

        // Store hint and callback ID in message for reinitialization
        msg.poolHintPtr = hintPtr;
        msg.poolCallbackId = callbackId;

        // Set reusable callback for pool return
        msg.reusableCallback = () -> returnMessageToPool(msg, callbackId, hintPtr);

        // Initialize zmq_msg_t with zmq_msg_init_data
        MemorySegment callbackStub = getPoolCallbackStub();
        int result = LibZmq.msgInitData(msg.msgSegment, dataPtr, actualSize, callbackStub, hintPtr);

        if (result != 0) {
            // Cleanup on failure
            CALLBACK_MESSAGE_MAP.remove(callbackId);
            HINT_PTR_POOL.offer(hintPtr);
            // Note: We do NOT close POOL_DATA_ARENA - it's shared and lives forever
            // The allocated memory will remain but won't be used
            throw new RuntimeException("Failed to initialize pooled message: zmq_msg_init_data returned " + result);
        }

        // Mark as initialized (important for Message methods to work)
        msg.initialized = true;
        msg.needsReinitialization = false; // Already initialized with zmq_msg_init_data

        return msg;
    }

    /**
     * Returns a message to the pool (called by the reusable callback).
     *
     * <p>This method is invoked by ZMQ's free callback when it's done with the message data.
     * The callback is responsible for returning the message to the pool for reuse.</p>
     *
     * <p><b>Important:</b> We do NOT call zmq_msg_init_data again here because:
     * <ul>
     *   <li>Zero-copy messages should only be initialized once with zmq_msg_init_data</li>
     *   <li>After ZMQ sends the message, it calls our callback to free the data</li>
     *   <li>We don't free the data - we return the message to the pool instead</li>
     *   <li>When the message is rented again, it will be re-initialized with new data</li>
     * </ul>
     *
     * @param msg The message to return
     * @param callbackId The callback ID for cleanup
     * @param hintPtr The hint pointer to return to pool
     */
    private void returnMessageToPool(Message msg, long callbackId, MemorySegment hintPtr) {
        // Increment return counter
        totalReturns.incrementAndGet();

        int bucketIndex = msg.poolBucketIndex;

        // Tier 1: Try to return to thread-local cache first
        ThreadLocalCache cache = threadLocalCache.get();
        if (cache.tryReturn(msg, bucketIndex)) {
            return;
        }

        // Tier 2: Try to return to shared pool
        int currentCount = pooledMessageCounts[bucketIndex].get();
        if (currentCount < maxBuffersPerBucket[bucketIndex]) {
            sharedPool[bucketIndex].offer(msg);
            pooledMessageCounts[bucketIndex].incrementAndGet();
        } else {
            // Tier 3: Pool is full - dispose the message
            poolRejects.incrementAndGet();
            // Clean up callback map entry
            CALLBACK_MESSAGE_MAP.remove(callbackId);
            HINT_PTR_POOL.offer(hintPtr);
            // Note: We do NOT close POOL_DATA_ARENA or free the memory
            // The memory remains allocated but unused (acceptable for a pool at capacity)
            msg.poolDataPtr = MemorySegment.NULL;
        }
    }

    /**
     * Static callback invoked by ZMQ when message is released.
     * Delegates to the message's reusable callback.
     *
     * @param data Pointer to message data (unused)
     * @param hint Pointer containing callback ID
     */
    private static void staticPoolCallback(MemorySegment data, MemorySegment hint) {
        if (hint.equals(MemorySegment.NULL)) {
            return;
        }

        try {
            // Read callback ID from hint
            MemorySegment hintSegment = hint.reinterpret(ValueLayout.JAVA_LONG.byteSize());
            long callbackId = hintSegment.get(ValueLayout.JAVA_LONG, 0);

            // Retrieve message from callback map
            Message msg = CALLBACK_MESSAGE_MAP.get(callbackId);

            if (msg != null && msg.reusableCallback != null) {
                // Invoke the reusable callback to return message to pool
                msg.reusableCallback.run();
            }
        } catch (Exception e) {
            // Swallow exceptions - must not corrupt ZMQ state
            System.err.println("Error in MessagePool callback: " + e.getMessage());
        }
    }

    /**
     * Lazily creates and returns the static pool callback stub.
     * Thread-safe singleton pattern.
     * Package-private for Message.prepareForReuse() access.
     *
     * @return MemorySegment pointing to native callback function
     */
    static synchronized MemorySegment getPoolCallbackStub() {
        if (poolCallbackStub == null) {
            try {
                Linker linker = Linker.nativeLinker();
                MethodHandle handle = MethodHandles.lookup()
                    .findStatic(MessagePool.class, "staticPoolCallback",
                        MethodType.methodType(void.class,
                            MemorySegment.class, MemorySegment.class));

                // Use global arena - callback stub lives forever
                poolCallbackStub = linker.upcallStub(handle,
                    FREE_CALLBACK_DESC, Arena.global());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create MessagePool callback stub", e);
            }
        }
        return poolCallbackStub;
    }

    // ========== Inner Classes ==========

    /**
     * Thread-local cache for fast, lock-free message access.
     *
     * <p>Each thread maintains a small cache (8 messages per bucket) to avoid
     * contention on the shared pool.</p>
     */
    private static class ThreadLocalCache {
        /**
         * Cache storage: [bucketIndex][slotIndex] -> Message
         */
        private final Message[][] buckets = new Message[BUCKET_COUNT][THREAD_LOCAL_CACHE_SIZE];

        /**
         * Count of messages in each bucket.
         */
        private final int[] counts = new int[BUCKET_COUNT];

        /**
         * Tries to get a message from the cache.
         *
         * @param bucketIndex The bucket index
         * @return A message, or null if cache is empty
         */
        Message tryGet(int bucketIndex) {
            int count = counts[bucketIndex];
            if (count == 0) {
                return null;
            }

            Message msg = buckets[bucketIndex][count - 1];
            buckets[bucketIndex][count - 1] = null;
            counts[bucketIndex] = count - 1;
            return msg;
        }

        /**
         * Tries to return a message to the cache.
         *
         * @param msg The message to return
         * @param bucketIndex The bucket index
         * @return true if cached, false if cache is full
         */
        boolean tryReturn(Message msg, int bucketIndex) {
            int count = counts[bucketIndex];
            if (count >= THREAD_LOCAL_CACHE_SIZE) {
                return false; // Cache full
            }

            buckets[bucketIndex][count] = msg;
            counts[bucketIndex] = count + 1;
            return true;
        }
    }

    /**
     * Immutable statistics snapshot for the message pool.
     *
     * <p>Provides comprehensive metrics about pool performance and health.</p>
     */
    public static class PoolStatistics {
        /**
         * Total number of messages rented from the pool.
         */
        public final long totalRents;

        /**
         * Total number of messages returned to the pool.
         */
        public final long totalReturns;

        /**
         * Number of rent operations that found a message in the pool (cache hit).
         */
        public final long poolHits;

        /**
         * Number of rent operations that required new allocation (cache miss).
         */
        public final long poolMisses;

        /**
         * Number of return operations rejected due to pool being full.
         */
        public final long poolRejects;

        /**
         * Number of messages currently outstanding (rented but not returned).
         */
        public final long outstandingBuffers;

        /**
         * Creates a new statistics snapshot.
         *
         * @param totalRents Total rents
         * @param totalReturns Total returns
         * @param poolHits Cache hits
         * @param poolMisses Cache misses
         * @param poolRejects Rejected returns
         * @param outstandingBuffers Outstanding messages
         */
        public PoolStatistics(long totalRents, long totalReturns, long poolHits,
                              long poolMisses, long poolRejects, long outstandingBuffers) {
            this.totalRents = totalRents;
            this.totalReturns = totalReturns;
            this.poolHits = poolHits;
            this.poolMisses = poolMisses;
            this.poolRejects = poolRejects;
            this.outstandingBuffers = outstandingBuffers;
        }

        /**
         * Calculates the pool hit rate (0.0 to 1.0).
         *
         * <p>A higher hit rate indicates better pool efficiency.
         * Target: &gt; 0.95 for optimal performance.</p>
         *
         * @return Hit rate as a decimal (e.g., 0.95 = 95%)
         */
        public double getHitRate() {
            return totalRents == 0 ? 0.0 : (double) poolHits / totalRents;
        }

        @Override
        public String toString() {
            return String.format(
                "PoolStatistics{rents=%d, returns=%d, hits=%d, misses=%d, " +
                "rejects=%d, outstanding=%d, hitRate=%.2f%%}",
                totalRents, totalReturns, poolHits, poolMisses,
                poolRejects, outstandingBuffers, getHitRate() * 100
            );
        }
    }
}
