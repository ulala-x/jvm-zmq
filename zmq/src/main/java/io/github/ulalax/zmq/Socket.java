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
 * <p>Sockets are the building blocks of ZMQ messaging. Each socket has a type
 * that determines its behavior and which other socket types it can communicate with.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket socket = new Socket(ctx, SocketType.REP)) {
 *     socket.bind("tcp://*:5555");
 *     String msg = socket.recvString();
 *     socket.send("Reply");
 * }
 * }</pre>
 *
 * @see SocketType
 * @see SocketOption
 */
public final class Socket implements AutoCloseable {

    private static final Cleaner CLEANER = Cleaner.create();
    private static final int STACK_BUFFER_SIZE = 512;

    private final Context context;
    private final MemorySegment handle;
    private final Cleaner.Cleanable cleanable;
    private volatile boolean closed = false;

    // I/O Arena for send/recv operations to avoid repeated Arena allocations
    private final Arena ioArena;
    private MemorySegment sendBuffer;
    private MemorySegment recvBuffer;
    private int sendBufferSize = 8192; // Default 8KB
    private int recvBufferSize = 8192; // Default 8KB

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

        // Initialize I/O Arena and buffers for send/recv operations
        this.ioArena = Arena.ofConfined();
        this.sendBuffer = ioArena.allocate(sendBufferSize);
        this.recvBuffer = ioArena.allocate(recvBufferSize);
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
        // Expand buffer if needed
        if (data.length > sendBufferSize) {
            sendBuffer = ioArena.allocate(data.length);
            sendBufferSize = data.length;
        }
        MemorySegment.copy(data, 0, sendBuffer, ValueLayout.JAVA_BYTE, 0, data.length);
        int result = LibZmq.send(getHandle(), sendBuffer, data.length, flags.getValue());
        ZmqException.throwIfError(result);
        return result;
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
        int remaining = buffer.remaining();
        int result;

        if (buffer.isDirect()) {
            // DirectByteBuffer: use MemorySegment.ofBuffer() for zero-copy
            MemorySegment seg = MemorySegment.ofBuffer(buffer);
            result = LibZmq.send(getHandle(), seg, remaining, flags.getValue());
            ZmqException.throwIfError(result);
            buffer.position(buffer.position() + result);
        } else {
            // Heap ByteBuffer: copy to sendBuffer
            if (remaining > sendBufferSize) {
                sendBuffer = ioArena.allocate(remaining);
                sendBufferSize = remaining;
            }
            MemorySegment.copy(buffer, 0, sendBuffer, ValueLayout.JAVA_BYTE, 0, remaining);
            result = LibZmq.send(getHandle(), sendBuffer, remaining, flags.getValue());
            ZmqException.throwIfError(result);
            buffer.position(buffer.position() + result);
        }
        return result;
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
        // Expand buffer if needed
        if (buffer.length > recvBufferSize) {
            recvBuffer = ioArena.allocate(buffer.length);
            recvBufferSize = buffer.length;
        }
        int result = LibZmq.recv(getHandle(), recvBuffer, buffer.length, flags.getValue());
        ZmqException.throwIfError(result);
        MemorySegment.copy(recvBuffer, ValueLayout.JAVA_BYTE, 0, buffer, 0, result);
        return result;
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

    /**
     * Tries to receive data as a byte array without blocking.
     * @return Received data, or null if would block
     */
    public byte[] tryRecvBytes() {
        try {
            return recvBytes(RecvFlags.DONT_WAIT);
        } catch (ZmqException e) {
            if (e.isAgain()) {
                return null;
            }
            throw e;
        }
    }

    // ========== Multipart Methods ==========

    /**
     * Sends a multipart message.
     * @param message The multipart message to send
     * @throws NullPointerException if message is null
     * @throws ZmqException if send fails
     */
    public void sendMultipart(MultipartMessage message) {
        if (message == null) {
            throw new NullPointerException("message cannot be null");
        }
        message.send(this);
    }

    /**
     * Receives a multipart message.
     * @return The received multipart message
     * @throws ZmqException if receive fails
     */
    public MultipartMessage recvMultipart() {
        return MultipartMessage.recv(this);
    }

    /**
     * Tries to receive a multipart message without blocking.
     * @return The received multipart message, or null if would block
     */
    public MultipartMessage tryRecvMultipart() {
        try {
            MultipartMessage msg = new MultipartMessage();
            do {
                byte[] frame = recvBytes(RecvFlags.DONT_WAIT);
                msg.add(frame);
            } while (hasMore());
            return msg;
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
            MemorySegment seg = arena.allocate(value.length);
            MemorySegment.copy(value, 0, seg, ValueLayout.JAVA_BYTE, 0, value.length);
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
            // Release I/O Arena before closing socket
            if (ioArena != null) {
                ioArena.close();
            }
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
