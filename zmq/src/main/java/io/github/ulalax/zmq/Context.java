package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;

/**
 * ZeroMQ context. Thread-safe. Manages I/O threads and socket lifecycle.
 *
 * <p>A context is the container for all ZMQ sockets in a process.
 * It manages I/O threads that handle asynchronous message processing.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * try (Context context = new Context()) {
 *     Socket socket = new Socket(context, SocketType.REP);
 *     // use socket
 * }
 * }</pre>
 *
 * @see Socket
 * @see ContextOption
 */
public final class Context implements AutoCloseable {

    private static final Cleaner CLEANER = Cleaner.create();

    private final MemorySegment handle;
    private final Cleaner.Cleanable cleanable;
    private volatile boolean closed = false;

    /**
     * Creates a new ZMQ context with default settings (1 I/O thread).
     * @throws ZmqException if context creation fails
     */
    public Context() {
        this.handle = LibZmq.ctxNew();
        ZmqException.throwIfNull(handle);
        this.cleanable = CLEANER.register(this, new ContextCleanup(handle));
    }

    /**
     * Creates a new ZMQ context with specified I/O threads and max sockets.
     * @param ioThreads Number of I/O threads (default: 1)
     * @param maxSockets Maximum number of sockets (default: 1024)
     * @throws ZmqException if context creation or option setting fails
     */
    public Context(int ioThreads, int maxSockets) {
        this();
        setOption(ContextOption.IO_THREADS, ioThreads);
        setOption(ContextOption.MAX_SOCKETS, maxSockets);
    }

    /**
     * Gets the native context handle.
     * @return Memory segment representing zmq context
     * @throws IllegalStateException if context is closed
     */
    MemorySegment getHandle() {
        if (closed) {
            throw new IllegalStateException("Context is closed");
        }
        return handle;
    }

    /**
     * Gets a context option value.
     * @param option The option to retrieve
     * @return The option value
     * @throws ZmqException if the operation fails
     * @throws IllegalStateException if context is closed
     */
    public int getOption(ContextOption option) {
        int result = LibZmq.ctxGet(getHandle(), option.getValue());
        if (result == -1) {
            throw new ZmqException();
        }
        return result;
    }

    /**
     * Sets a context option value.
     * @param option The option to set
     * @param value The value to set
     * @throws ZmqException if the operation fails
     * @throws IllegalStateException if context is closed
     */
    public void setOption(ContextOption option, int value) {
        int result = LibZmq.ctxSet(getHandle(), option.getValue(), value);
        ZmqException.throwIfError(result);
    }

    /**
     * Shuts down the context without destroying it.
     * Existing sockets can finish operations but new operations will fail.
     * @throws ZmqException if the operation fails
     */
    public void shutdown() {
        if (!closed) {
            int result = LibZmq.ctxShutdown(handle);
            ZmqException.throwIfError(result);
        }
    }

    /**
     * Checks if a capability is available in the ZMQ library.
     * @param capability The capability name (e.g., "curve", "ipc", "pgm")
     * @return true if the capability is available
     */
    public static boolean has(String capability) {
        return LibZmq.has(capability) == 1;
    }

    /**
     * Gets the ZMQ library version.
     * @return Version object with major, minor, patch
     */
    public static Version version() {
        int[] v = LibZmq.version();
        return new Version(v[0], v[1], v[2]);
    }

    /**
     * Closes the context and releases all resources.
     * All sockets must be closed before calling this.
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            cleanable.clean();
        }
    }

    /**
     * Cleanup action for Cleaner.
     */
    private static class ContextCleanup implements Runnable {
        private final MemorySegment handle;

        ContextCleanup(MemorySegment handle) {
            this.handle = handle;
        }

        @Override
        public void run() {
            // Best effort cleanup - ignore errors
            try {
                LibZmq.ctxTerm(handle);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Version information.
     *
     * @param major Major version number
     * @param minor Minor version number
     * @param patch Patch version number
     */
    public record Version(int major, int minor, int patch) {
        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }
}
