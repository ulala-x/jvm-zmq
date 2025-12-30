package io.github.ulalax.zmq;

/**
 * Predefined message size buckets for memory pooling and allocation optimization.
 *
 * <p>This enum provides 16 size buckets ranging from 128 bytes to 4 megabytes,
 * each being a power of 2. These buckets are designed for efficient message
 * buffer management and memory pool allocation strategies.</p>
 *
 * <p><strong>Note:</strong> Messages <= 64 bytes are not pooled and use regular Message allocation.</p>
 *
 * <p><strong>Size Categories:</strong></p>
 * <ul>
 *   <li><strong>Small (128B - 512B):</strong> Small control messages, headers</li>
 *   <li><strong>Medium (1KB - 4KB):</strong> Standard messages, most common use case</li>
 *   <li><strong>Large (8KB - 64KB):</strong> Larger payloads, serialized objects</li>
 *   <li><strong>Very Large (128KB - 1MB):</strong> File chunks, batch data</li>
 *   <li><strong>Extra Large (2MB - 4MB):</strong> Large files, bulk transfers</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Find appropriate bucket for a 1000-byte message
 * MessageSize size = MessageSize.of(1000);  // Returns SIZE_1K
 * int bufferSize = size.getBytes();         // Returns 1024
 * int index = size.getBucketIndex();        // Returns 3
 * }</pre>
 *
 * @see Message
 */
public enum MessageSize {
    /** 128 bytes - Minimum bucket size for pooled messages */
    SIZE_128(128, 0),

    /** 256 bytes */
    SIZE_256(256, 1),

    /** 512 bytes */
    SIZE_512(512, 2),

    /** 1 kilobyte (1024 bytes) */
    SIZE_1K(1024, 3),

    /** 2 kilobytes (2048 bytes) */
    SIZE_2K(2048, 4),

    /** 4 kilobytes (4096 bytes) */
    SIZE_4K(4096, 5),

    /** 8 kilobytes (8192 bytes) */
    SIZE_8K(8192, 6),

    /** 16 kilobytes (16384 bytes) */
    SIZE_16K(16384, 7),

    /** 32 kilobytes (32768 bytes) */
    SIZE_32K(32768, 8),

    /** 64 kilobytes (65536 bytes) */
    SIZE_64K(65536, 9),

    /** 128 kilobytes (131072 bytes) */
    SIZE_128K(131072, 10),

    /** 256 kilobytes (262144 bytes) */
    SIZE_256K(262144, 11),

    /** 512 kilobytes (524288 bytes) */
    SIZE_512K(524288, 12),

    /** 1 megabyte (1048576 bytes) */
    SIZE_1M(1048576, 13),

    /** 2 megabytes (2097152 bytes) */
    SIZE_2M(2097152, 14),

    /** 4 megabytes (4194304 bytes) - Maximum bucket size */
    SIZE_4M(4194304, 15);

    private final int bytes;
    private final int bucketIndex;

    MessageSize(int bytes, int bucketIndex) {
        this.bytes = bytes;
        this.bucketIndex = bucketIndex;
    }

    /**
     * Gets the size in bytes for this bucket.
     *
     * @return The number of bytes this size represents
     */
    public int getBytes() {
        return bytes;
    }

    /**
     * Gets the bucket index (0-18).
     *
     * <p>The bucket index can be used for array-based pooling strategies
     * where each index corresponds to a specific size bucket.</p>
     *
     * @return The zero-based bucket index (0 for SIZE_16, 18 for SIZE_4M)
     */
    public int getBucketIndex() {
        return bucketIndex;
    }

    /**
     * Finds the smallest MessageSize bucket that can hold the given size.
     *
     * <p>This method performs a linear search to find the appropriate bucket.
     * If the requested size is larger than the maximum bucket (4MB), it returns
     * SIZE_4M. If the size is 0 or negative, it returns SIZE_128.</p>
     *
     * <p><strong>Note:</strong> Messages <= 64 bytes should not use this method as they are not pooled.</p>
     *
     * <p><strong>Examples:</strong></p>
     * <ul>
     *   <li>{@code of(0)} returns SIZE_128</li>
     *   <li>{@code of(100)} returns SIZE_128</li>
     *   <li>{@code of(1024)} returns SIZE_1K</li>
     *   <li>{@code of(1025)} returns SIZE_2K</li>
     *   <li>{@code of(10000000)} returns SIZE_4M</li>
     * </ul>
     *
     * @param size The requested size in bytes
     * @return The smallest MessageSize bucket that can accommodate the size
     */
    public static MessageSize of(int size) {
        // Handle edge cases
        if (size <= 0) {
            return SIZE_128;
        }

        // Find the smallest bucket that fits the requested size
        for (MessageSize messageSize : values()) {
            if (size <= messageSize.bytes) {
                return messageSize;
            }
        }

        // If size exceeds maximum bucket, return the largest
        return SIZE_4M;
    }

    /**
     * Gets a MessageSize by its bucket index.
     *
     * @param index The bucket index (0-18)
     * @return The MessageSize at the specified index
     * @throws IllegalArgumentException if index is out of range
     */
    public static MessageSize fromBucketIndex(int index) {
        if (index < 0 || index >= values().length) {
            throw new IllegalArgumentException(
                "Invalid bucket index: " + index + ". Must be between 0 and " + (values().length - 1)
            );
        }

        for (MessageSize size : values()) {
            if (size.bucketIndex == index) {
                return size;
            }
        }

        // This should never happen if enum is properly defined
        throw new IllegalStateException("Bucket index mapping error for index: " + index);
    }

    /**
     * Returns a string representation of this message size.
     *
     * @return A string in the format "MessageSize{bytes=1024, index=6}"
     */
    @Override
    public String toString() {
        return "MessageSize{bytes=" + bytes + ", index=" + bucketIndex + "}";
    }
}
