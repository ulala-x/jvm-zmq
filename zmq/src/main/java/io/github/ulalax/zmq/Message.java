package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.*;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;

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
    private final MemorySegment msgSegment;
    private final Cleaner.Cleanable cleanable;
    private volatile boolean closed = false;
    private volatile boolean initialized = false;

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
