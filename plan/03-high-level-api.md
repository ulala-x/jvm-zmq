# High-Level API - zmq Module

This document provides complete high-level Java API design and implementation for ZeroMQ, matching the netzmq C# API but adapted for Java idioms and FFM.

---

## Table of Contents
1. [Context.java](#contextjava)
2. [Socket.java](#socketjava)
3. [Message.java](#messagejava)
4. [Poller.java](#pollerjava)
5. [Enum Classes](#enum-classes)
6. [MultipartMessage.java](#multipartmessagejava)

---

## Context.java

High-level context class wrapping zmq_ctx_t.

**Package**: `io.github.ulalax.zmq`

### Full Implementation

```java
package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;

/**
 * ZeroMQ context. Thread-safe. Manages I/O threads and socket lifecycle.
 *
 * Usage:
 * <pre>{@code
 * try (Context context = new Context()) {
 *     Socket socket = new Socket(context, SocketType.REP);
 *     // use socket
 * }
 * }</pre>
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
     */
    public record Version(int major, int minor, int patch) {
        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }
}
```

---

## Socket.java

High-level socket class wrapping zmq socket operations.

**Package**: `io.github.ulalax.zmq`

### Full Implementation

```java
package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * ZeroMQ socket. NOT thread-safe (except for special thread-safe sockets).
 *
 * Usage:
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket socket = new Socket(ctx, SocketType.REP)) {
 *     socket.bind("tcp://*:5555");
 *     String msg = socket.recvString();
 *     socket.send("Reply");
 * }
 * }</pre>
 */
public final class Socket implements AutoCloseable {

    private static final Cleaner CLEANER = Cleaner.create();
    private static final int STACK_BUFFER_SIZE = 512;

    private final Context context;
    private final MemorySegment handle;
    private final Cleaner.Cleanable cleanable;
    private volatile boolean closed = false;

    /**
     * Creates a new ZMQ socket.
     * @param context The context to create the socket in
     * @param type The socket type
     * @throws NullPointerException if context is null
     * @throws ZmqException if socket creation fails
     */
    public Socket(Context context, SocketType type) {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }
        this.context = context;
        this.handle = LibZmq.socket(context.getHandle(), type.getValue());
        ZmqException.throwIfNull(handle);
        this.cleanable = CLEANER.register(this, new SocketCleanup(handle));
    }

    /**
     * Gets the native socket handle.
     * @return Memory segment representing zmq socket
     * @throws IllegalStateException if socket is closed
     */
    MemorySegment getHandle() {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }
        return handle;
    }

    /**
     * Gets the context this socket belongs to.
     * @return The context
     */
    public Context getContext() {
        return context;
    }

    /**
     * Checks if there are more message parts to receive.
     * @return true if more parts follow
     */
    public boolean hasMore() {
        return getOption(SocketOption.RCVMORE) != 0;
    }

    // ========== Bind/Connect/Unbind/Disconnect ==========

    /**
     * Binds the socket to an endpoint.
     * @param endpoint The endpoint to bind to (e.g., "tcp://*:5555")
     * @throws NullPointerException if endpoint is null
     * @throws ZmqException if bind fails
     */
    public void bind(String endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("endpoint cannot be null");
        }
        int result = LibZmq.bind(getHandle(), endpoint);
        ZmqException.throwIfError(result);
    }

    /**
     * Connects the socket to an endpoint.
     * @param endpoint The endpoint to connect to (e.g., "tcp://localhost:5555")
     * @throws NullPointerException if endpoint is null
     * @throws ZmqException if connect fails
     */
    public void connect(String endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("endpoint cannot be null");
        }
        int result = LibZmq.connect(getHandle(), endpoint);
        ZmqException.throwIfError(result);
    }

    /**
     * Unbinds the socket from an endpoint.
     * @param endpoint The endpoint to unbind from
     * @throws NullPointerException if endpoint is null
     * @throws ZmqException if unbind fails
     */
    public void unbind(String endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("endpoint cannot be null");
        }
        int result = LibZmq.unbind(getHandle(), endpoint);
        ZmqException.throwIfError(result);
    }

    /**
     * Disconnects the socket from an endpoint.
     * @param endpoint The endpoint to disconnect from
     * @throws NullPointerException if endpoint is null
     * @throws ZmqException if disconnect fails
     */
    public void disconnect(String endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("endpoint cannot be null");
        }
        int result = LibZmq.disconnect(getHandle(), endpoint);
        ZmqException.throwIfError(result);
    }

    // ========== Send Methods ==========

    /**
     * Sends data on the socket.
     * @param data The data to send
     * @param flags Send flags
     * @return Number of bytes sent
     * @throws NullPointerException if data is null
     * @throws ZmqException if send fails
     */
    public int send(byte[] data, SendFlags flags) {
        if (data == null) {
            throw new NullPointerException("data cannot be null");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocateFrom(ValueLayout.JAVA_BYTE, data);
            int result = LibZmq.send(getHandle(), seg, data.length, flags.getValue());
            ZmqException.throwIfError(result);
            return result;
        }
    }

    /**
     * Sends data on the socket.
     * @param data The data to send
     * @return Number of bytes sent
     */
    public int send(byte[] data) {
        return send(data, SendFlags.NONE);
    }

    /**
     * Sends a UTF-8 string on the socket.
     * @param text The text to send
     * @param flags Send flags
     * @return Number of bytes sent
     * @throws NullPointerException if text is null
     * @throws ZmqException if send fails
     */
    public int send(String text, SendFlags flags) {
        if (text == null) {
            throw new NullPointerException("text cannot be null");
        }
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return send(data, flags);
    }

    /**
     * Sends a UTF-8 string on the socket.
     * @param text The text to send
     * @return Number of bytes sent
     */
    public int send(String text) {
        return send(text, SendFlags.NONE);
    }

    /**
     * Sends a ByteBuffer on the socket.
     * @param buffer The buffer to send
     * @param flags Send flags
     * @return Number of bytes sent
     * @throws NullPointerException if buffer is null
     * @throws ZmqException if send fails
     */
    public int send(ByteBuffer buffer, SendFlags flags) {
        if (buffer == null) {
            throw new NullPointerException("buffer cannot be null");
        }
        try (Arena arena = Arena.ofConfined()) {
            int remaining = buffer.remaining();
            MemorySegment seg = arena.allocate(remaining);
            MemorySegment.copy(buffer, 0, seg, ValueLayout.JAVA_BYTE, 0, remaining);
            int result = LibZmq.send(getHandle(), seg, remaining, flags.getValue());
            ZmqException.throwIfError(result);
            buffer.position(buffer.position() + result);
            return result;
        }
    }

    /**
     * Sends a message on the socket.
     * @param message The message to send
     * @param flags Send flags
     * @return Number of bytes sent
     */
    public int send(Message message, SendFlags flags) {
        return message.send(getHandle(), flags);
    }

    /**
     * Tries to send data without blocking.
     * @param data The data to send
     * @return true if sent, false if would block
     * @throws ZmqException if send fails with error other than EAGAIN
     */
    public boolean trySend(byte[] data) {
        try {
            send(data, SendFlags.DONT_WAIT);
            return true;
        } catch (ZmqException e) {
            if (e.isAgain()) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Tries to send a string without blocking.
     * @param text The text to send
     * @return true if sent, false if would block
     */
    public boolean trySend(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return trySend(data);
    }

    // ========== Receive Methods ==========

    /**
     * Receives data into a buffer.
     * @param buffer The buffer to receive into
     * @param flags Receive flags
     * @return Number of bytes received
     * @throws NullPointerException if buffer is null
     * @throws ZmqException if receive fails
     */
    public int recv(byte[] buffer, RecvFlags flags) {
        if (buffer == null) {
            throw new NullPointerException("buffer cannot be null");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(buffer.length);
            int result = LibZmq.recv(getHandle(), seg, buffer.length, flags.getValue());
            ZmqException.throwIfError(result);
            MemorySegment.copy(seg, ValueLayout.JAVA_BYTE, 0, buffer, 0, result);
            return result;
        }
    }

    /**
     * Receives data into a buffer.
     * @param buffer The buffer to receive into
     * @return Number of bytes received
     */
    public int recv(byte[] buffer) {
        return recv(buffer, RecvFlags.NONE);
    }

    /**
     * Receives a message.
     * @param message The message to receive into
     * @param flags Receive flags
     * @return Number of bytes received
     */
    public int recv(Message message, RecvFlags flags) {
        return message.recv(getHandle(), flags);
    }

    /**
     * Receives data as a new byte array.
     * @param flags Receive flags
     * @return Received data
     * @throws ZmqException if receive fails
     */
    public byte[] recvBytes(RecvFlags flags) {
        try (Message msg = new Message()) {
            recv(msg, flags);
            return msg.toByteArray();
        }
    }

    /**
     * Receives data as a new byte array.
     * @return Received data
     */
    public byte[] recvBytes() {
        return recvBytes(RecvFlags.NONE);
    }

    /**
     * Receives a UTF-8 string.
     * @param flags Receive flags
     * @return Received string
     * @throws ZmqException if receive fails
     */
    public String recvString(RecvFlags flags) {
        byte[] data = recvBytes(flags);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Receives a UTF-8 string.
     * @return Received string
     */
    public String recvString() {
        return recvString(RecvFlags.NONE);
    }

    /**
     * Tries to receive data without blocking.
     * @param buffer The buffer to receive into
     * @return Number of bytes received, or -1 if would block
     */
    public int tryRecv(byte[] buffer) {
        try {
            return recv(buffer, RecvFlags.DONT_WAIT);
        } catch (ZmqException e) {
            if (e.isAgain()) {
                return -1;
            }
            throw e;
        }
    }

    /**
     * Tries to receive a string without blocking.
     * @return Received string, or null if would block
     */
    public String tryRecvString() {
        try {
            return recvString(RecvFlags.DONT_WAIT);
        } catch (ZmqException e) {
            if (e.isAgain()) {
                return null;
            }
            throw e;
        }
    }

    // ========== Socket Options ==========

    /**
     * Gets an integer socket option.
     * @param option The option to retrieve
     * @return The option value
     * @throws ZmqException if the operation fails
     */
    public int getOption(SocketOption option) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment size = arena.allocate(ValueLayout.JAVA_LONG);
            size.set(ValueLayout.JAVA_LONG, 0, ValueLayout.JAVA_INT.byteSize());

            int result = LibZmq.getSockOpt(getHandle(), option.getValue(), value, size);
            ZmqException.throwIfError(result);

            return value.get(ValueLayout.JAVA_INT, 0);
        }
    }

    /**
     * Gets a long socket option.
     * @param option The option to retrieve
     * @return The option value
     */
    public long getOptionLong(SocketOption option) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment value = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment size = arena.allocate(ValueLayout.JAVA_LONG);
            size.set(ValueLayout.JAVA_LONG, 0, ValueLayout.JAVA_LONG.byteSize());

            int result = LibZmq.getSockOpt(getHandle(), option.getValue(), value, size);
            ZmqException.throwIfError(result);

            return value.get(ValueLayout.JAVA_LONG, 0);
        }
    }

    /**
     * Gets a string socket option.
     * @param option The option to retrieve
     * @return The option value
     */
    public String getOptionString(SocketOption option) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(256);
            MemorySegment size = arena.allocate(ValueLayout.JAVA_LONG);
            size.set(ValueLayout.JAVA_LONG, 0, 256L);

            int result = LibZmq.getSockOpt(getHandle(), option.getValue(), buffer, size);
            ZmqException.throwIfError(result);

            long actualSize = size.get(ValueLayout.JAVA_LONG, 0);
            // Exclude null terminator
            if (actualSize > 0) {
                actualSize--;
            }

            byte[] bytes = new byte[(int)actualSize];
            MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, 0, bytes, 0, (int)actualSize);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets a byte array socket option.
     * @param option The option to retrieve
     * @param maxSize Maximum size to retrieve
     * @return The option value
     */
    public byte[] getOptionBytes(SocketOption option, int maxSize) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buffer = arena.allocate(maxSize);
            MemorySegment size = arena.allocate(ValueLayout.JAVA_LONG);
            size.set(ValueLayout.JAVA_LONG, 0, (long)maxSize);

            int result = LibZmq.getSockOpt(getHandle(), option.getValue(), buffer, size);
            ZmqException.throwIfError(result);

            long actualSize = size.get(ValueLayout.JAVA_LONG, 0);
            byte[] bytes = new byte[(int)actualSize];
            MemorySegment.copy(buffer, ValueLayout.JAVA_BYTE, 0, bytes, 0, (int)actualSize);
            return bytes;
        }
    }

    /**
     * Sets an integer socket option.
     * @param option The option to set
     * @param value The value to set
     */
    public void setOption(SocketOption option, int value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(ValueLayout.JAVA_INT);
            seg.set(ValueLayout.JAVA_INT, 0, value);
            int result = LibZmq.setSockOpt(getHandle(), option.getValue(), seg,
                ValueLayout.JAVA_INT.byteSize());
            ZmqException.throwIfError(result);
        }
    }

    /**
     * Sets a long socket option.
     * @param option The option to set
     * @param value The value to set
     */
    public void setOption(SocketOption option, long value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocate(ValueLayout.JAVA_LONG);
            seg.set(ValueLayout.JAVA_LONG, 0, value);
            int result = LibZmq.setSockOpt(getHandle(), option.getValue(), seg,
                ValueLayout.JAVA_LONG.byteSize());
            ZmqException.throwIfError(result);
        }
    }

    /**
     * Sets a string socket option.
     * @param option The option to set
     * @param value The value to set
     */
    public void setOption(SocketOption option, String value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        setOption(option, bytes);
    }

    /**
     * Sets a byte array socket option.
     * @param option The option to set
     * @param value The value to set
     */
    public void setOption(SocketOption option, byte[] value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment seg = arena.allocateFrom(ValueLayout.JAVA_BYTE, value);
            int result = LibZmq.setSockOpt(getHandle(), option.getValue(), seg, value.length);
            ZmqException.throwIfError(result);
        }
    }

    // ========== Subscribe/Unsubscribe (SUB sockets) ==========

    /**
     * Subscribes to all messages (SUB socket).
     */
    public void subscribeAll() {
        setOption(SocketOption.SUBSCRIBE, new byte[0]);
    }

    /**
     * Subscribes to messages with a specific prefix (SUB socket).
     * @param prefix The prefix to subscribe to
     */
    public void subscribe(byte[] prefix) {
        setOption(SocketOption.SUBSCRIBE, prefix);
    }

    /**
     * Subscribes to messages with a specific string prefix (SUB socket).
     * @param prefix The prefix to subscribe to
     */
    public void subscribe(String prefix) {
        setOption(SocketOption.SUBSCRIBE, prefix);
    }

    /**
     * Unsubscribes from all messages (SUB socket).
     */
    public void unsubscribeAll() {
        setOption(SocketOption.UNSUBSCRIBE, new byte[0]);
    }

    /**
     * Unsubscribes from messages with a specific prefix (SUB socket).
     * @param prefix The prefix to unsubscribe from
     */
    public void unsubscribe(byte[] prefix) {
        setOption(SocketOption.UNSUBSCRIBE, prefix);
    }

    /**
     * Unsubscribes from messages with a specific string prefix (SUB socket).
     * @param prefix The prefix to unsubscribe from
     */
    public void unsubscribe(String prefix) {
        setOption(SocketOption.UNSUBSCRIBE, prefix);
    }

    // ========== Monitor ==========

    /**
     * Starts monitoring socket events.
     * @param endpoint The inproc endpoint to publish events on (null to stop monitoring)
     * @param events The events to monitor
     */
    public void monitor(String endpoint, SocketMonitorEvent events) {
        int result = LibZmq.socketMonitor(getHandle(), endpoint, events.getValue());
        ZmqException.throwIfError(result);
    }

    /**
     * Starts monitoring all socket events.
     * @param endpoint The inproc endpoint to publish events on
     */
    public void monitor(String endpoint) {
        monitor(endpoint, SocketMonitorEvent.ALL);
    }

    /**
     * Stops monitoring socket events.
     */
    public void stopMonitor() {
        monitor(null, SocketMonitorEvent.ALL);
    }

    // ========== Close ==========

    /**
     * Closes the socket and releases resources.
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
    private static class SocketCleanup implements Runnable {
        private final MemorySegment handle;

        SocketCleanup(MemorySegment handle) {
            this.handle = handle;
        }

        @Override
        public void run() {
            try {
                LibZmq.close(handle);
            } catch (Exception ignored) {
            }
        }
    }
}
```

---

## Message.java

High-level message class with Arena-based memory management.

**Package**: `io.github.ulalax.zmq`

### Full Implementation

```java
package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.*;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * ZeroMQ message. Manages a zmq_msg_t structure.
 *
 * Usage:
 * <pre>{@code
 * try (Message msg = new Message()) {
 *     socket.recv(msg);
 *     String text = msg.toString();
 * }
 * }</pre>
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
```

---

## Poller.java

Static polling utilities.

**Package**: `io.github.ulalax.zmq`

### Full Implementation

```java
package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.*;
import java.time.Duration;

import static java.lang.foreign.ValueLayout.*;

/**
 * Polling utilities for ZeroMQ sockets.
 *
 * Usage:
 * <pre>{@code
 * PollItem[] items = {
 *     new PollItem(socket1, PollEvents.IN),
 *     new PollItem(socket2, PollEvents.IN)
 * };
 * int count = Poller.poll(items, 1000);
 * if (items[0].isReadable()) {
 *     // socket1 has data
 * }
 * }</pre>
 */
public final class Poller {

    // Thread-local cache for single-socket polling
    private static final ThreadLocal<MemorySegment> SINGLE_ITEM_CACHE =
        ThreadLocal.withInitial(() -> {
            Arena arena = Arena.ofAuto();
            return arena.allocate(ZmqStructs.getPollItemLayout());
        });

    private Poller() {
        // Prevent instantiation
    }

    /**
     * Polls multiple sockets for events.
     * @param items The items to poll
     * @param timeout Timeout in milliseconds (-1 = infinite)
     * @return Number of items with events
     */
    public static int poll(PollItem[] items, long timeout) {
        if (items == null || items.length == 0) {
            return 0;
        }

        try (Arena arena = Arena.ofConfined()) {
            // Allocate poll items array
            StructLayout layout = ZmqStructs.getPollItemLayout();
            MemorySegment pollItems = arena.allocate(layout, items.length);

            // Fill poll items
            for (int i = 0; i < items.length; i++) {
                PollItem item = items[i];
                MemorySegment itemSeg = pollItems.asSlice(i * layout.byteSize(), layout);

                // Set socket
                itemSeg.set(ADDRESS, 0, item.getSocket() != null ?
                    item.getSocket().getHandle() : MemorySegment.NULL);

                // Set fd (platform-dependent)
                if (isWindows()) {
                    itemSeg.set(JAVA_LONG, ADDRESS.byteSize(), item.getFileDescriptor());
                } else {
                    itemSeg.set(JAVA_INT, ADDRESS.byteSize(), (int)item.getFileDescriptor());
                }

                // Set events
                long eventsOffset = ZmqStructs.PollItemOffsets.EVENTS;
                itemSeg.set(JAVA_SHORT, eventsOffset, (short)item.getEvents().getValue());

                // Clear revents
                itemSeg.set(JAVA_SHORT, eventsOffset + 2, (short)0);
            }

            // Perform poll
            int result = LibZmq.poll(pollItems, items.length, timeout);
            ZmqException.throwIfError(result);

            // Copy back returned events
            for (int i = 0; i < items.length; i++) {
                MemorySegment itemSeg = pollItems.asSlice(i * layout.byteSize(), layout);
                long eventsOffset = ZmqStructs.PollItemOffsets.EVENTS;
                short revents = itemSeg.get(JAVA_SHORT, eventsOffset + 2);
                items[i].setReturnedEvents(PollEvents.fromValue(revents));
            }

            return result;
        }
    }

    /**
     * Polls multiple sockets for events.
     * @param items The items to poll
     * @param timeout Timeout duration
     * @return Number of items with events
     */
    public static int poll(PollItem[] items, Duration timeout) {
        return poll(items, timeout.toMillis());
    }

    /**
     * Polls multiple sockets for events (infinite wait).
     * @param items The items to poll
     * @return Number of items with events
     */
    public static int poll(PollItem[] items) {
        return poll(items, -1);
    }

    /**
     * Polls a single socket for events.
     * @param socket The socket to poll
     * @param events The events to wait for
     * @param timeout Timeout in milliseconds
     * @return true if events occurred
     */
    public static boolean poll(Socket socket, PollEvents events, long timeout) {
        PollItem[] items = { new PollItem(socket, events) };
        int result = poll(items, timeout);
        return result > 0 && items[0].hasEvents();
    }

    /**
     * Polls a single socket for events.
     * @param socket The socket to poll
     * @param events The events to wait for
     * @param timeout Timeout duration
     * @return true if events occurred
     */
    public static boolean poll(Socket socket, PollEvents events, Duration timeout) {
        return poll(socket, events, timeout.toMillis());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
```

### PollItem.java

```java
package io.github.ulalax.zmq;

/**
 * Represents an item to poll for events.
 */
public final class PollItem {

    private final Socket socket;
    private final long fileDescriptor;
    private final PollEvents events;
    private PollEvents returnedEvents;

    /**
     * Creates a poll item for a socket.
     * @param socket The socket to poll
     * @param events The events to wait for
     */
    public PollItem(Socket socket, PollEvents events) {
        if (socket == null) {
            throw new NullPointerException("socket cannot be null");
        }
        this.socket = socket;
        this.fileDescriptor = 0;
        this.events = events;
        this.returnedEvents = PollEvents.NONE;
    }

    /**
     * Creates a poll item for a file descriptor.
     * @param fd The file descriptor
     * @param events The events to wait for
     */
    public PollItem(long fd, PollEvents events) {
        this.socket = null;
        this.fileDescriptor = fd;
        this.events = events;
        this.returnedEvents = PollEvents.NONE;
    }

    Socket getSocket() {
        return socket;
    }

    long getFileDescriptor() {
        return fileDescriptor;
    }

    PollEvents getEvents() {
        return events;
    }

    void setReturnedEvents(PollEvents events) {
        this.returnedEvents = events;
    }

    /**
     * Gets the returned events.
     * @return Events that occurred
     */
    public PollEvents getReturnedEvents() {
        return returnedEvents;
    }

    /**
     * Checks if any events occurred.
     * @return true if events occurred
     */
    public boolean hasEvents() {
        return returnedEvents.getValue() != 0;
    }

    /**
     * Checks if socket is readable.
     * @return true if IN event occurred
     */
    public boolean isReadable() {
        return (returnedEvents.getValue() & PollEvents.IN.getValue()) != 0;
    }

    /**
     * Checks if socket is writable.
     * @return true if OUT event occurred
     */
    public boolean isWritable() {
        return (returnedEvents.getValue() & PollEvents.OUT.getValue()) != 0;
    }

    /**
     * Checks if socket has error.
     * @return true if ERR event occurred
     */
    public boolean hasError() {
        return (returnedEvents.getValue() & PollEvents.ERR.getValue()) != 0;
    }
}
```

---

## Enum Classes

### SocketType.java

```java
package io.github.ulalax.zmq;

public enum SocketType {
    PAIR(0),
    PUB(1),
    SUB(2),
    REQ(3),
    REP(4),
    DEALER(5),
    ROUTER(6),
    PULL(7),
    PUSH(8),
    XPUB(9),
    XSUB(10),
    STREAM(11);

    private final int value;

    SocketType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### SendFlags.java

```java
package io.github.ulalax.zmq;

public enum SendFlags {
    NONE(0),
    DONT_WAIT(1),
    SEND_MORE(2);

    private final int value;

    SendFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public SendFlags combine(SendFlags other) {
        return fromValue(this.value | other.value);
    }

    public static SendFlags fromValue(int value) {
        for (SendFlags flag : values()) {
            if (flag.value == value) {
                return flag;
            }
        }
        // For combined flags, return NONE as placeholder
        // Real usage should use bitwise operations
        return NONE;
    }
}
```

### RecvFlags.java

```java
package io.github.ulalax.zmq;

public enum RecvFlags {
    NONE(0),
    DONT_WAIT(1);

    private final int value;

    RecvFlags(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### PollEvents.java

```java
package io.github.ulalax.zmq;

public enum PollEvents {
    NONE(0),
    IN(1),
    OUT(2),
    ERR(4),
    PRI(8);

    private final short value;

    PollEvents(int value) {
        this.value = (short)value;
    }

    public short getValue() {
        return value;
    }

    public PollEvents combine(PollEvents other) {
        return fromValue((short)(this.value | other.value));
    }

    public static PollEvents fromValue(short value) {
        for (PollEvents event : values()) {
            if (event.value == value) {
                return event;
            }
        }
        // For combined events, create wrapper
        return new PollEvents(value) {};
    }

    // Constructor for anonymous instances
    private PollEvents(short value) {
        this.value = value;
    }
}
```

### ContextOption.java

```java
package io.github.ulalax.zmq;

public enum ContextOption {
    IO_THREADS(1),
    MAX_SOCKETS(2),
    SOCKET_LIMIT(3),
    THREAD_PRIORITY(3),
    THREAD_SCHED_POLICY(4),
    MAX_MSG_SIZE(5),
    MSG_T_SIZE(6),
    THREAD_AFFINITY_CPU_ADD(7),
    THREAD_AFFINITY_CPU_REMOVE(8),
    THREAD_NAME_PREFIX(9);

    private final int value;

    ContextOption(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### SocketOption.java

```java
package io.github.ulalax.zmq;

public enum SocketOption {
    AFFINITY(4),
    ROUTING_ID(5),
    SUBSCRIBE(6),
    UNSUBSCRIBE(7),
    RATE(8),
    RECOVERY_IVL(9),
    SNDBUF(11),
    RCVBUF(12),
    RCVMORE(13),
    FD(14),
    EVENTS(15),
    TYPE(16),
    LINGER(17),
    RECONNECT_IVL(18),
    BACKLOG(19),
    RECONNECT_IVL_MAX(21),
    MAXMSGSIZE(22),
    SNDHWM(23),
    RCVHWM(24),
    MULTICAST_HOPS(25),
    RCVTIMEO(27),
    SNDTIMEO(28),
    LAST_ENDPOINT(32),
    ROUTER_MANDATORY(33),
    TCP_KEEPALIVE(34),
    TCP_KEEPALIVE_CNT(35),
    TCP_KEEPALIVE_IDLE(36),
    TCP_KEEPALIVE_INTVL(37),
    IMMEDIATE(39),
    XPUB_VERBOSE(40),
    ROUTER_RAW(41),
    IPV6(42),
    MECHANISM(43),
    PLAIN_SERVER(44),
    PLAIN_USERNAME(45),
    PLAIN_PASSWORD(46),
    CURVE_SERVER(47),
    CURVE_PUBLICKEY(48),
    CURVE_SECRETKEY(49),
    CURVE_SERVERKEY(50),
    PROBE_ROUTER(51),
    REQ_CORRELATE(52),
    REQ_RELAXED(53),
    CONFLATE(54),
    ZAP_DOMAIN(55),
    ROUTER_HANDOVER(56),
    TOS(57),
    CONNECT_ROUTING_ID(61),
    HANDSHAKE_IVL(66),
    SOCKS_PROXY(68),
    XPUB_NODROP(69),
    BLOCKY(70),
    XPUB_MANUAL(71),
    XPUB_WELCOME_MSG(72),
    STREAM_NOTIFY(73),
    INVERT_MATCHING(74),
    HEARTBEAT_IVL(75),
    HEARTBEAT_TTL(76),
    HEARTBEAT_TIMEOUT(77),
    XPUB_VERBOSER(78),
    CONNECT_TIMEOUT(79),
    TCP_MAXRT(80),
    THREAD_SAFE(81),
    MULTICAST_MAXTPDU(84),
    BINDTODEVICE(92);

    private final int value;

    SocketOption(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### MessageProperty.java

```java
package io.github.ulalax.zmq;

public enum MessageProperty {
    MORE(1),
    SHARED(3);

    private final int value;

    MessageProperty(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### SocketMonitorEvent.java

```java
package io.github.ulalax.zmq;

public enum SocketMonitorEvent {
    CONNECTED(1),
    CONNECT_DELAYED(2),
    CONNECT_RETRIED(4),
    LISTENING(8),
    BIND_FAILED(16),
    ACCEPTED(32),
    ACCEPT_FAILED(64),
    CLOSED(128),
    CLOSE_FAILED(256),
    DISCONNECTED(512),
    MONITOR_STOPPED(1024),
    ALL(0xFFFF);

    private final int value;

    SocketMonitorEvent(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public SocketMonitorEvent combine(SocketMonitorEvent other) {
        return fromValue(this.value | other.value);
    }

    public static SocketMonitorEvent fromValue(int value) {
        for (SocketMonitorEvent event : values()) {
            if (event.value == value) {
                return event;
            }
        }
        return ALL; // Default for unknown combinations
    }
}
```

---

## MultipartMessage.java

Helper class for multipart messages (optional but useful).

```java
package io.github.ulalax.zmq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper for multipart messages.
 *
 * Usage:
 * <pre>{@code
 * // Sending
 * MultipartMessage msg = new MultipartMessage();
 * msg.add("frame1");
 * msg.add("frame2");
 * msg.send(socket);
 *
 * // Receiving
 * MultipartMessage msg = MultipartMessage.recv(socket);
 * for (byte[] frame : msg) {
 *     // process frame
 * }
 * }</pre>
 */
public final class MultipartMessage implements Iterable<byte[]> {

    private final List<byte[]> frames = new ArrayList<>();

    public MultipartMessage() {
    }

    /**
     * Adds a byte array frame.
     * @param data The frame data
     */
    public void add(byte[] data) {
        frames.add(data.clone());
    }

    /**
     * Adds a string frame (UTF-8 encoded).
     * @param text The frame text
     */
    public void addString(String text) {
        add(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Gets the number of frames.
     * @return Frame count
     */
    public int size() {
        return frames.size();
    }

    /**
     * Gets a frame by index.
     * @param index The frame index
     * @return Frame data
     */
    public byte[] get(int index) {
        return frames.get(index);
    }

    /**
     * Gets a frame as string.
     * @param index The frame index
     * @return Frame as UTF-8 string
     */
    public String getString(int index) {
        return new String(get(index), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Sends all frames on a socket.
     * @param socket The socket to send on
     */
    public void send(Socket socket) {
        for (int i = 0; i < frames.size(); i++) {
            boolean more = i < frames.size() - 1;
            SendFlags flags = more ? SendFlags.SEND_MORE : SendFlags.NONE;
            socket.send(frames.get(i), flags);
        }
    }

    /**
     * Receives all frames from a socket.
     * @param socket The socket to receive from
     * @return The multipart message
     */
    public static MultipartMessage recv(Socket socket) {
        MultipartMessage msg = new MultipartMessage();
        do {
            byte[] frame = socket.recvBytes();
            msg.add(frame);
        } while (socket.hasMore());
        return msg;
    }

    /**
     * Clears all frames.
     */
    public void clear() {
        frames.clear();
    }

    @Override
    public Iterator<byte[]> iterator() {
        return frames.iterator();
    }
}
```

---

## Next Steps

After implementing the high-level API:

1. Implement utilities (`04-utilities.md`)
2. Write unit tests for each class
3. Write integration tests for messaging patterns
4. Add Javadoc documentation
5. Create examples

## Usage Examples

### Simple REQ-REP

```java
// Server
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.REP)) {
    socket.bind("tcp://*:5555");
    while (true) {
        String request = socket.recvString();
        socket.send("Reply to: " + request);
    }
}

// Client
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.REQ)) {
    socket.connect("tcp://localhost:5555");
    socket.send("Hello");
    String reply = socket.recvString();
}
```

### PUB-SUB

```java
// Publisher
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.PUB)) {
    socket.bind("tcp://*:5556");
    while (true) {
        socket.send("weather 25C");
        Thread.sleep(1000);
    }
}

// Subscriber
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.SUB)) {
    socket.connect("tcp://localhost:5556");
    socket.subscribe("weather");
    while (true) {
        String update = socket.recvString();
        System.out.println(update);
    }
}
```

### Polling

```java
try (Context ctx = new Context();
     Socket socket1 = new Socket(ctx, SocketType.REP);
     Socket socket2 = new Socket(ctx, SocketType.REP)) {

    socket1.bind("tcp://*:5555");
    socket2.bind("tcp://*:5556");

    PollItem[] items = {
        new PollItem(socket1, PollEvents.IN),
        new PollItem(socket2, PollEvents.IN)
    };

    while (true) {
        Poller.poll(items, -1);
        if (items[0].isReadable()) {
            String msg = socket1.recvString();
            socket1.send("Reply 1");
        }
        if (items[1].isReadable()) {
            String msg = socket2.recvString();
            socket2.send("Reply 2");
        }
    }
}
```
