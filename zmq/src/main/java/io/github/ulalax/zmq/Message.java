package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * ZeroMQ message. Manages a zmq_msg_t structure.
 *
 * <p>Messages provide an efficient way to send and receive data without copying.
 * They use Arena-based memory management for automatic cleanup.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * try (Message msg = new Message()) {
 *     socket.recv(msg);
 *     String text = msg.toString();
 * }
 * }</pre>
 *
 * @see Socket
 */
public final class Message implements AutoCloseable {

    private static final Cleaner CLEANER = Cleaner.create();

    private final Arena arena;
    final MemorySegment msgSegment;  // Package-private for direct access from Socket
    private final Cleaner.Cleanable cleanable;
    private volatile boolean closed = false;
    private volatile boolean initialized = false;

    // === Zero-Copy Callback Support ===

    /**
     * Thread-safe map storing user callbacks for zero-copy messages.
     * Maps callback ID → user callback function.
     */
    private static final ConcurrentHashMap<Long, Consumer<MemorySegment>> CALLBACK_MAP
        = new ConcurrentHashMap<>();

    /**
     * Thread-safe map storing hint arenas for zero-copy messages.
     * Maps callback ID → hint arena (must be kept alive until callback is invoked).
     */
    private static final ConcurrentHashMap<Long, Arena> HINT_ARENA_MAP
        = new ConcurrentHashMap<>();

    /**
     * Atomic counter for generating unique callback IDs.
     */
    private static final AtomicLong CALLBACK_ID_GENERATOR = new AtomicLong(0);

    /**
     * Function descriptor for ZMQ free callback.
     * Signature: void (*zmq_free_fn)(void *data, void *hint)
     */
    private static final FunctionDescriptor FREE_CALLBACK_DESC
        = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS);

    /**
     * Singleton static callback stub. Lazily initialized.
     * Lives in global arena (never freed).
     */
    private static MemorySegment staticCallbackStub = null;

    /**
     * User-provided callback for this message (if using zero-copy constructor).
     */
    private Consumer<MemorySegment> userCallback;

    /**
     * Callback ID for cleanup (if using zero-copy constructor).
     */
    private Long callbackId;

    /**
     * Creates an empty message.
     */
    public Message() {
        this.arena = Arena.ofConfined();
        this.msgSegment = arena.allocate(ZmqStructs.ZMQ_MSG_LAYOUT);
        int result = LibZmq.msgInit(msgSegment);
        ZmqException.throwIfError(result);
        this.initialized = true;
        this.cleanable = CLEANER.register(this, new MessageCleanup(msgSegment));
    }

    /**
     * Creates a message with a specific size.
     * @param size The size in bytes
     * @throws IllegalArgumentException if size is negative
     */
    public Message(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative");
        }
        this.arena = Arena.ofConfined();
        this.msgSegment = arena.allocate(ZmqStructs.ZMQ_MSG_LAYOUT);
        int result = LibZmq.msgInitSize(msgSegment, size);
        ZmqException.throwIfError(result);
        this.initialized = true;
        this.cleanable = CLEANER.register(this, new MessageCleanup(msgSegment));
    }

    /**
     * Creates a message with data.
     * @param data The data to initialize with
     */
    public Message(byte[] data) {
        this(data.length);
        if (data.length > 0) {
            MemorySegment.copy(data, 0, data(), ValueLayout.JAVA_BYTE, 0, data.length);
        }
    }

    /**
     * Creates a message with UTF-8 string data.
     * @param text The text to initialize with
     */
    public Message(String text) {
        this(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates a message with external native memory and custom free callback.
     * This enables true zero-copy by allowing ZMQ to directly use pre-allocated
     * native memory without internal copying.
     *
     * <p>The callback will be invoked when ZMQ is done with the message data,
     * allowing custom cleanup logic such as:
     * <ul>
     *   <li>Closing the Arena that allocated the memory</li>
     *   <li>Returning memory to a custom pool</li>
     *   <li>Tracking message lifecycle for debugging</li>
     * </ul>
     *
     * <p><b>Important Threading Considerations</b>:
     * <ul>
     *   <li>The callback is invoked from ZMQ's internal thread, not your thread</li>
     *   <li>The callback must be thread-safe</li>
     *   <li>The callback must not throw exceptions (they will be swallowed)</li>
     *   <li>Use Arena.ofShared() for data if callback needs to close it</li>
     * </ul>
     *
     * @param data Native memory segment containing message data
     * @param size Size of the message in bytes (must be ≤ data.byteSize())
     * @param freeCallback Callback invoked when ZMQ releases the message (can be null)
     * @throws IllegalArgumentException if size is negative or exceeds data size
     * @throws RuntimeException if ZMQ message initialization fails
     *
     * <p>Example - Zero-copy with Arena cleanup:</p>
     * <pre>{@code
     * Arena dataArena = Arena.ofShared(); // Must be shared for cross-thread close
     * MemorySegment dataSeg = dataArena.allocate(1024);
     *
     * // Fill dataSeg with your data...
     * dataSeg.copyFrom(MemorySegment.ofArray(myByteArray));
     *
     * // Create zero-copy message
     * Message msg = new Message(dataSeg, 1024, data -> {
     *     dataArena.close(); // Called when ZMQ is done
     * });
     *
     * socket.send(msg);
     * msg.close(); // Safe to close - data is owned by ZMQ until callback
     * }</pre>
     */
    public Message(MemorySegment data, long size, Consumer<MemorySegment> freeCallback) {
        if (size < 0) {
            throw new IllegalArgumentException("Size cannot be negative: " + size);
        }
        if (size > data.byteSize()) {
            throw new IllegalArgumentException(
                "Size " + size + " exceeds data segment size " + data.byteSize());
        }

        // Allocate zmq_msg_t structure (message metadata, not data itself)
        this.arena = Arena.ofConfined();
        this.msgSegment = arena.allocate(ZmqStructs.ZMQ_MSG_LAYOUT);

        MemorySegment ffnPtr;
        MemorySegment hintPtr;
        Arena hintArena = null;

        if (freeCallback != null) {
            // Generate unique callback ID
            this.callbackId = CALLBACK_ID_GENERATOR.incrementAndGet();

            // Store callback in thread-safe map
            CALLBACK_MAP.put(callbackId, freeCallback);
            this.userCallback = freeCallback; // Keep instance reference

            // Allocate hint segment to store callback ID
            // MUST use shared arena - ZMQ callback runs on different thread
            hintArena = Arena.ofShared();
            hintPtr = hintArena.allocate(ValueLayout.JAVA_LONG);
            hintPtr.set(ValueLayout.JAVA_LONG, 0, callbackId);

            // Store hint arena - it must stay alive until callback is invoked
            HINT_ARENA_MAP.put(callbackId, hintArena);

            // Get static callback function pointer
            ffnPtr = getCallbackStub();
        } else {
            // No callback - ZMQ won't free the data
            this.callbackId = null;
            this.userCallback = null;
            ffnPtr = MemorySegment.NULL;
            hintPtr = MemorySegment.NULL;
        }

        // Initialize ZMQ message with external data
        int result = LibZmq.msgInitData(msgSegment, data, size, ffnPtr, hintPtr);

        if (result != 0) {
            // Cleanup on initialization failure
            if (callbackId != null) {
                CALLBACK_MAP.remove(callbackId);
                HINT_ARENA_MAP.remove(callbackId);
            }
            if (hintArena != null) {
                try {
                    hintArena.close();
                } catch (Exception ignored) {}
            }
            arena.close();
            ZmqException.throwIfError(result);
        }

        this.initialized = true;
        // Register cleaner for automatic cleanup
        this.cleanable = CLEANER.register(this, new MessageCleanup(msgSegment));
    }

    /**
     * Static free callback implementation called by ZMQ.
     * This is invoked from ZMQ's internal thread when message is released.
     *
     * @param data Pointer to message data
     * @param hint Pointer containing callback ID (as long)
     */
    private static void staticFreeCallback(MemorySegment data, MemorySegment hint) {
        if (hint.equals(MemorySegment.NULL)) {
            return;
        }

        long callbackId = -1;
        try {
            // Reinterpret hint pointer to read the callback ID
            // ZMQ passes us a raw pointer, we need to give it a size to read from it
            MemorySegment hintSegment = hint.reinterpret(ValueLayout.JAVA_LONG.byteSize());

            // Extract callback ID from hint
            callbackId = hintSegment.get(ValueLayout.JAVA_LONG, 0);

            // Retrieve and remove callback from map
            Consumer<MemorySegment> callback = CALLBACK_MAP.remove(callbackId);

            if (callback != null) {
                // Invoke user callback with data pointer
                callback.accept(data);
            }
        } catch (Exception e) {
            // Swallow exceptions - must not corrupt ZMQ state
            // In production, log this error
            System.err.println("Error in ZMQ free callback: " + e.getMessage());
        } finally {
            // Clean up hint arena
            if (callbackId != -1) {
                Arena hintArena = HINT_ARENA_MAP.remove(callbackId);
                if (hintArena != null) {
                    try {
                        hintArena.close();
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * Lazily creates and returns the static callback stub.
     * Thread-safe singleton pattern.
     *
     * @return MemorySegment pointing to native callback function
     */
    private static synchronized MemorySegment getCallbackStub() {
        if (staticCallbackStub == null) {
            try {
                Linker linker = Linker.nativeLinker();
                MethodHandle handle = MethodHandles.lookup()
                    .findStatic(Message.class, "staticFreeCallback",
                        MethodType.methodType(void.class,
                            MemorySegment.class, MemorySegment.class));

                // Use global arena - callback stub lives forever
                staticCallbackStub = linker.upcallStub(handle,
                    FREE_CALLBACK_DESC, Arena.global());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create ZMQ callback stub", e);
            }
        }
        return staticCallbackStub;
    }

    /**
     * Gets the message data as a MemorySegment.
     * @return Memory segment pointing to message data
     * @throws IllegalStateException if message is not initialized or closed
     */
    public MemorySegment data() {
        ensureInitialized();
        MemorySegment dataPtr = LibZmq.msgData(msgSegment);
        long size = LibZmq.msgSize(msgSegment);
        return dataPtr.reinterpret(size, arena, null);
    }

    /**
     * Gets the size of the message.
     * @return Size in bytes
     */
    public int size() {
        ensureInitialized();
        return (int) LibZmq.msgSize(msgSegment);
    }

    /**
     * Checks if more message parts follow.
     * @return true if more parts follow
     */
    public boolean more() {
        ensureInitialized();
        return LibZmq.msgMore(msgSegment) != 0;
    }

    /**
     * Gets a message property.
     * @param property The property to get
     * @return The property value
     */
    public int getProperty(MessageProperty property) {
        ensureInitialized();
        int result = LibZmq.msgGet(msgSegment, property.getValue());
        if (result == -1) {
            throw new ZmqException();
        }
        return result;
    }

    /**
     * Sets a message property.
     * @param property The property to set
     * @param value The value to set
     */
    public void setProperty(MessageProperty property, int value) {
        ensureInitialized();
        int result = LibZmq.msgSet(msgSegment, property.getValue(), value);
        ZmqException.throwIfError(result);
    }

    /**
     * Gets message metadata.
     * @param property The metadata property name
     * @return The metadata value, or null if not found
     */
    public String getMetadata(String property) {
        ensureInitialized();
        return LibZmq.msgGets(msgSegment, property);
    }

    /**
     * Converts message data to byte array.
     * @return Copy of message data
     */
    public byte[] toByteArray() {
        MemorySegment data = data();
        int size = size();
        byte[] result = new byte[size];
        MemorySegment.copy(data, ValueLayout.JAVA_BYTE, 0, result, 0, size);
        return result;
    }

    /**
     * Converts message data to UTF-8 string.
     * @return String representation of message
     */
    @Override
    public String toString() {
        return new String(toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Rebuilds the message as empty.
     */
    public void rebuild() {
        if (initialized) {
            LibZmq.msgClose(msgSegment);
        }
        int result = LibZmq.msgInit(msgSegment);
        ZmqException.throwIfError(result);
        initialized = true;
    }

    /**
     * Rebuilds the message with a specific size.
     * @param size The new size
     */
    public void rebuild(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size cannot be negative");
        }
        if (initialized) {
            LibZmq.msgClose(msgSegment);
        }
        int result = LibZmq.msgInitSize(msgSegment, size);
        ZmqException.throwIfError(result);
        initialized = true;
    }

    /**
     * Moves content from source message to this message.
     * @param source The source message
     */
    public void move(Message source) {
        ensureInitialized();
        source.ensureInitialized();
        int result = LibZmq.msgMove(msgSegment, source.msgSegment);
        ZmqException.throwIfError(result);
    }

    /**
     * Copies content from source message to this message.
     * @param source The source message
     */
    public void copy(Message source) {
        ensureInitialized();
        source.ensureInitialized();
        int result = LibZmq.msgCopy(msgSegment, source.msgSegment);
        ZmqException.throwIfError(result);
    }

    /**
     * Sends this message on a socket.
     * @param socket The socket handle
     * @param flags Send flags
     * @return Number of bytes sent
     */
    int send(MemorySegment socket, SendFlags flags) {
        ensureInitialized();
        int result = LibZmq.msgSend(msgSegment, socket, flags.getValue());
        ZmqException.throwIfError(result);
        return result;
    }

    /**
     * Receives a message from a socket.
     * @param socket The socket handle
     * @param flags Receive flags
     * @return Number of bytes received
     */
    int recv(MemorySegment socket, RecvFlags flags) {
        ensureInitialized();
        int result = LibZmq.msgRecv(msgSegment, socket, flags.getValue());
        if (result == -1) {
            throw new ZmqException();
        }
        return result;
    }

    private void ensureInitialized() {
        if (!initialized || closed) {
            throw new IllegalStateException("Message not initialized or closed");
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;

            // Remove callback from map if present (prevents memory leak)
            // Note: We don't close the hint arena here because ZMQ may not have
            // called the callback yet. The hint arena will be closed in staticFreeCallback.
            if (callbackId != null) {
                CALLBACK_MAP.remove(callbackId);
                // Don't remove from HINT_ARENA_MAP - let the callback clean it up
                callbackId = null;
                userCallback = null;
            }

            cleanable.clean();
            arena.close();
        }
    }

    /**
     * Cleanup action for Cleaner.
     */
    private static class MessageCleanup implements Runnable {
        private final MemorySegment msgSegment;

        MessageCleanup(MemorySegment msgSegment) {
            this.msgSegment = msgSegment;
        }

        @Override
        public void run() {
            try {
                LibZmq.msgClose(msgSegment);
            } catch (Exception ignored) {
            }
        }
    }
}
