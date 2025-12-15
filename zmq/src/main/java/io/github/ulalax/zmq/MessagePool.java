package io.github.ulalax.zmq;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Object pool for Message instances to reduce allocations.
 *
 * <p>The pool maintains a thread-safe queue of Message objects that can be reused
 * to avoid the overhead of creating new Message instances and their native resources.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * Message msg = MessagePool.rent();
 * try {
 *     socket.recv(msg);
 *     // process message
 * } finally {
 *     MessagePool.returnMessage(msg);
 * }
 * }</pre>
 *
 * @see Message
 */
public final class MessagePool {

    private static final int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 4;
    private static final ConcurrentLinkedQueue<Message> pool = new ConcurrentLinkedQueue<>();
    private static volatile int poolSize = 0;

    // Private constructor to prevent instantiation
    private MessagePool() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Rents a message from the pool.
     *
     * <p>If the pool is empty, a new Message instance is created.
     * The returned message is guaranteed to be in a clean state (empty).</p>
     *
     * @return A message instance from the pool
     */
    public static Message rent() {
        Message msg = pool.poll();
        if (msg != null) {
            poolSize--;
            return msg;
        }
        return new Message();
    }

    /**
     * Returns a message to the pool.
     *
     * <p>The message is rebuilt to reset its state before being returned to the pool.
     * If the rebuild fails or the pool is already full, the message is disposed.</p>
     *
     * @param message The message to return to the pool
     */
    public static void returnMessage(Message message) {
        if (message == null) {
            return;
        }

        try {
            // Don't exceed a reasonable pool size
            if (poolSize < DEFAULT_POOL_SIZE) {
                // Rebuild the message to reset its state
                message.rebuild();
                pool.offer(message);
                poolSize++;
            } else {
                // Pool is full, dispose the message
                message.close();
            }
        } catch (Exception e) {
            // If rebuild fails, dispose the message
            try {
                message.close();
            } catch (Exception ignored) {
                // Ignore close errors
            }
        }
    }

    /**
     * Clears the pool and disposes all pooled messages.
     *
     * <p>This method should be called when the pool is no longer needed
     * to ensure proper cleanup of native resources.</p>
     */
    public static void clear() {
        Message msg;
        while ((msg = pool.poll()) != null) {
            try {
                msg.close();
            } catch (Exception ignored) {
                // Ignore close errors
            }
        }
        poolSize = 0;
    }

    /**
     * Gets the current number of messages in the pool.
     *
     * @return The number of pooled messages
     */
    public static int getPoolSize() {
        return poolSize;
    }
}
