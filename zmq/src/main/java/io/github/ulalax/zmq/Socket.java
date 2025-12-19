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
 * <p>This API uses {@link SendResult} and {@link RecvResult} to distinguish between
 * successful operations, would-block (EAGAIN) conditions, and real errors:</p>
 * <ul>
 *   <li><b>Success</b> - Operation completed, result contains value</li>
 *   <li><b>Would Block</b> - Operation cannot complete without blocking (empty result)</li>
 *   <li><b>Error</b> - Real ZMQ error occurred (throws {@link ZmqException})</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket socket = new Socket(ctx, SocketType.REP)) {
 *     socket.bind("tcp://*:5555");
 *
 *     // Blocking receive
 *     RecvResult<String> result = socket.recvString();
 *     result.ifPresent(msg -> System.out.println("Received: " + msg));
 *
 *     // Non-blocking send
 *     SendResult sendResult = socket.send("Reply", SendFlags.DONT_WAIT);
 *     if (sendResult.wouldBlock()) {
 *         System.out.println("Would block - retry later");
 *     }
 * }
 * }</pre>
 *
 * @see SocketType
 * @see SocketOption
 * @see SendResult
 * @see RecvResult
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
    /**
     * Dynamic send buffer that grows as needed and periodically resets
     * to prevent excessive memory usage from occasional large messages.
     */
    private MemorySegment sendBuffer;
    private int sendBufferSize = 8192; // Default 8KB
    /**
     * Dynamic receive buffer that grows as needed and periodically resets
     * to prevent excessive memory usage from occasional large messages.
     */
    private MemorySegment recvBuffer;
    private int recvBufferSize = 8192; // Default 8KB

    // Buffer usage tracking for adaptive sizing
    private static final int BUFFER_RESET_THRESHOLD_MULTIPLIER = 4;  // Reset if 4x larger than needed
    private static final int BUFFER_USAGE_SAMPLE_SIZE = 100;  // Track last 100 operations
    private int sendBufferUsageCount = 0;
    private long sendBufferTotalUsed = 0;
    private int recvBufferUsageCount = 0;
    private long recvBufferTotalUsed = 0;

    // Reusable Message objects to avoid allocations on EAGAIN
    private Message recvMessage;       // Reusable for tryRecvBytes()
    private Message recvStringMessage;  // Reusable for tryRecvString()

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
        // Use ofShared() to allow access from multiple threads
        this.ioArena = Arena.ofShared();
        this.sendBuffer = ioArena.allocate(sendBufferSize);
        this.recvBuffer = ioArena.allocate(recvBufferSize);

        // Initialize reusable Message objects
        this.recvMessage = new Message();
        this.recvStringMessage = new Message();
    }

    /**
     * Gets the native socket handle.
     * @return Memory segment representing zmq socket
     * @throws IllegalStateException if socket is closed
     */
    public MemorySegment getHandle() {
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
     *
     * <p>This method sends data and returns a {@link SendResult} that indicates whether
     * the operation succeeded, would block (EAGAIN), or failed with an error.</p>
     *
     * @param data The data to send
     * @param flags Send flags (e.g., {@link SendFlags#DONT_WAIT} for non-blocking)
     * @return SendResult containing bytes sent on success, or empty if would block
     * @throws NullPointerException if data is null
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * SendResult result = socket.send(data, SendFlags.DONT_WAIT);
     * if (result.isPresent()) {
     *     System.out.println("Sent " + result.value() + " bytes");
     * } else if (result.wouldBlock()) {
     *     System.out.println("Would block - retry later");
     * }
     * }</pre>
     *
     * @see SendResult
     * @see SendFlags
     */
    public SendResult send(byte[] data, SendFlags flags) {
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
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return SendResult.empty();
            }
            throw new ZmqException(errno);
        }

        // Track buffer usage for adaptive sizing
        sendBufferTotalUsed += data.length;
        sendBufferUsageCount++;

        if (sendBufferUsageCount >= BUFFER_USAGE_SAMPLE_SIZE) {
            long avgUsed = sendBufferTotalUsed / sendBufferUsageCount;
            if (shouldResetBuffer(sendBufferSize, avgUsed)) {
                int newSize = (int) (avgUsed * 2) + 1024;
                sendBuffer = ioArena.allocate(newSize);
                sendBufferSize = newSize;
            }
            sendBufferUsageCount = 0;
            sendBufferTotalUsed = 0;
        }

        return SendResult.success(result);
    }

    /**
     * Sends data on the socket with default flags.
     * @param data The data to send
     * @return SendResult containing bytes sent on success, or empty if would block
     * @see #send(byte[], SendFlags)
     */
    public SendResult send(byte[] data) {
        return send(data, SendFlags.NONE);
    }

    /**
     * Sends a UTF-8 string on the socket.
     * @param text The text to send
     * @param flags Send flags
     * @return SendResult containing bytes sent on success, or empty if would block
     * @throws NullPointerException if text is null
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     * @see #send(byte[], SendFlags)
     */
    public SendResult send(String text, SendFlags flags) {
        if (text == null) {
            throw new NullPointerException("text cannot be null");
        }
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return send(data, flags);
    }

    /**
     * Sends a UTF-8 string on the socket with default flags.
     * @param text The text to send
     * @return SendResult containing bytes sent on success, or empty if would block
     * @see #send(String, SendFlags)
     */
    public SendResult send(String text) {
        return send(text, SendFlags.NONE);
    }

    /**
     * Sends a ByteBuffer on the socket.
     * @param buffer The buffer to send
     * @param flags Send flags
     * @return SendResult containing bytes sent on success, or empty if would block
     * @throws NullPointerException if buffer is null
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     */
    public SendResult send(ByteBuffer buffer, SendFlags flags) {
        if (buffer == null) {
            throw new NullPointerException("buffer cannot be null");
        }
        int remaining = buffer.remaining();
        int result;

        if (buffer.isDirect()) {
            // DirectByteBuffer: use MemorySegment.ofBuffer() for zero-copy
            MemorySegment seg = MemorySegment.ofBuffer(buffer);
            result = LibZmq.send(getHandle(), seg, remaining, flags.getValue());
            if (result == -1) {
                int errno = LibZmq.errno();
                if (errno == ZmqConstants.EAGAIN) {
                    return SendResult.empty();
                }
                throw new ZmqException(errno);
            }
            buffer.position(buffer.position() + result);
        } else {
            // Heap ByteBuffer: copy to sendBuffer
            if (remaining > sendBufferSize) {
                sendBuffer = ioArena.allocate(remaining);
                sendBufferSize = remaining;
            }
            MemorySegment.copy(buffer, 0, sendBuffer, ValueLayout.JAVA_BYTE, 0, remaining);
            result = LibZmq.send(getHandle(), sendBuffer, remaining, flags.getValue());
            if (result == -1) {
                int errno = LibZmq.errno();
                if (errno == ZmqConstants.EAGAIN) {
                    return SendResult.empty();
                }
                throw new ZmqException(errno);
            }
            buffer.position(buffer.position() + result);
        }
        return SendResult.success(result);
    }

    /**
     * Sends a message on the socket.
     * @param message The message to send
     * @param flags Send flags
     * @return SendResult containing bytes sent on success, or empty if would block
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     */
    public SendResult send(Message message, SendFlags flags) {
        int result = message.send(getHandle(), flags);
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return SendResult.empty();
            }
            throw new ZmqException(errno);
        }
        return SendResult.success(result);
    }


    // ========== Receive Methods ==========

    /**
     * Receives data into a buffer.
     *
     * <p>This method receives data and returns a {@link RecvResult} that indicates whether
     * the operation succeeded, would block (EAGAIN), or failed with an error.</p>
     *
     * @param buffer The buffer to receive into
     * @param flags Receive flags (e.g., {@link RecvFlags#DONT_WAIT} for non-blocking)
     * @return RecvResult containing bytes received on success, or empty if would block
     * @throws NullPointerException if buffer is null
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * byte[] buffer = new byte[256];
     * RecvResult<Integer> result = socket.recv(buffer, RecvFlags.DONT_WAIT);
     * if (result.isPresent()) {
     *     int bytesRead = result.value();
     *     System.out.println("Received " + bytesRead + " bytes");
     * } else if (result.wouldBlock()) {
     *     System.out.println("No data available");
     * }
     * }</pre>
     *
     * @see RecvResult
     * @see RecvFlags
     */
    public RecvResult<Integer> recv(byte[] buffer, RecvFlags flags) {
        if (buffer == null) {
            throw new NullPointerException("buffer cannot be null");
        }
        // Expand buffer if needed
        if (buffer.length > recvBufferSize) {
            recvBuffer = ioArena.allocate(buffer.length);
            recvBufferSize = buffer.length;
        }
        int result = LibZmq.recv(getHandle(), recvBuffer, buffer.length, flags.getValue());
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return RecvResult.empty();
            }
            throw new ZmqException(errno);
        }
        MemorySegment.copy(recvBuffer, ValueLayout.JAVA_BYTE, 0, buffer, 0, result);

        // Track buffer usage for adaptive sizing
        recvBufferTotalUsed += result;
        recvBufferUsageCount++;

        if (recvBufferUsageCount >= BUFFER_USAGE_SAMPLE_SIZE) {
            long avgUsed = recvBufferTotalUsed / recvBufferUsageCount;
            if (shouldResetBuffer(recvBufferSize, avgUsed)) {
                int newSize = (int) (avgUsed * 2) + 1024;
                recvBuffer = ioArena.allocate(newSize);
                recvBufferSize = newSize;
            }
            recvBufferUsageCount = 0;
            recvBufferTotalUsed = 0;
        }

        return RecvResult.success(result);
    }

    /**
     * Receives data into a buffer with default flags.
     * @param buffer The buffer to receive into
     * @return RecvResult containing bytes received on success, or empty if would block
     * @see #recv(byte[], RecvFlags)
     */
    public RecvResult<Integer> recv(byte[] buffer) {
        return recv(buffer, RecvFlags.NONE);
    }

    /**
     * Receives a message.
     * @param message The message to receive into
     * @param flags Receive flags
     * @return RecvResult containing bytes received on success, or empty if would block
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     */
    public RecvResult<Integer> recv(Message message, RecvFlags flags) {
        int result = message.recv(getHandle(), flags);
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return RecvResult.empty();
            }
            throw new ZmqException(errno);
        }
        return RecvResult.success(result);
    }

    /**
     * Receives data as a new byte array.
     *
     * <p>This method uses reusable internal {@link Message} buffer for optimal performance.</p>
     *
     * @param flags Receive flags
     * @return RecvResult containing received byte array on success, or empty if would block
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * RecvResult<byte[]> result = socket.recvBytes(RecvFlags.DONT_WAIT);
     * result.ifPresent(data -> {
     *     System.out.println("Received " + data.length + " bytes");
     *     processData(data);
     * });
     * }</pre>
     */
    public RecvResult<byte[]> recvBytes(RecvFlags flags) {
        recvMessage.rebuild();
        int result = LibZmq.msgRecv(recvMessage.msgSegment, getHandle(), flags.getValue());
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return RecvResult.empty();
            }
            throw new ZmqException(errno);
        }
        return RecvResult.success(recvMessage.toByteArray());
    }

    /**
     * Receives data as a new byte array with default flags.
     * @return RecvResult containing received byte array on success, or empty if would block
     * @see #recvBytes(RecvFlags)
     */
    public RecvResult<byte[]> recvBytes() {
        return recvBytes(RecvFlags.NONE);
    }

    /**
     * Receives a UTF-8 string.
     *
     * <p>This method uses reusable internal {@link Message} buffer for optimal performance.</p>
     *
     * @param flags Receive flags
     * @return RecvResult containing received string on success, or empty if would block
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * RecvResult<String> result = socket.recvString(RecvFlags.DONT_WAIT);
     * result.ifPresent(msg -> System.out.println("Message: " + msg));
     *
     * // Or with map
     * result.map(String::toUpperCase)
     *       .ifPresent(this::processMessage);
     * }</pre>
     */
    public RecvResult<String> recvString(RecvFlags flags) {
        recvStringMessage.rebuild();
        int result = LibZmq.msgRecv(recvStringMessage.msgSegment, getHandle(), flags.getValue());
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return RecvResult.empty();
            }
            throw new ZmqException(errno);
        }
        return RecvResult.success(recvStringMessage.toString());
    }

    /**
     * Receives a UTF-8 string with default flags.
     * @return RecvResult containing received string on success, or empty if would block
     * @see #recvString(RecvFlags)
     */
    public RecvResult<String> recvString() {
        return recvString(RecvFlags.NONE);
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
     * Receives a complete multipart message.
     *
     * <p>This method receives all parts of a multipart message atomically. The first frame
     * is received with the specified flags. If EAGAIN occurs on the first frame, an empty
     * result is returned. Once the first frame is received, subsequent frames are received
     * with blocking semantics to ensure atomicity.</p>
     *
     * <p><b>Atomicity Guarantee:</b> This method ensures that either all frames of a multipart
     * message are received, or none are received (if EAGAIN occurs on the first frame).
     * Once the first frame is successfully received, all remaining frames are received with
     * blocking semantics to maintain message integrity.</p>
     *
     * <p><b>Error Recovery:</b> If the first frame is received successfully but an error occurs
     * on a subsequent frame, a {@link ZmqException} will be thrown with detailed context
     * including the number of frames successfully received. The partial message is discarded
     * to prevent processing incomplete data. <b>Critical:</b> The socket's internal state may
     * be corrupted after such an error and the socket should be closed and recreated for
     * reliable operation.</p>
     *
     * @return RecvResult containing the complete multipart message, or empty if would block
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN), with detailed context about
     *         which frame failed if the error occurred during subsequent frame reception
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * RecvResult<MultipartMessage> result = socket.recvMultipart();
     * result.ifPresent(msg -> {
     *     System.out.println("Received " + msg.size() + " frames");
     *     for (byte[] frame : msg) {
     *         System.out.println("Frame: " + new String(frame, StandardCharsets.UTF_8));
     *     }
     * });
     *
     * // Event loop with multipart messages
     * while (running) {
     *     socket.recvMultipart().ifPresent(message -> {
     *         String identity = new String(message.get(0), StandardCharsets.UTF_8);
     *         String payload = new String(message.get(1), StandardCharsets.UTF_8);
     *         handleRequest(identity, payload);
     *     });
     *     Thread.sleep(10);
     * }
     * }</pre>
     *
     * @see MultipartMessage
     */
    public RecvResult<MultipartMessage> recvMultipart() {
        MultipartMessage msg = new MultipartMessage();
        int framesReceived = 0;

        try (Message frame = new Message()) {
            // First frame - use DONT_WAIT to allow non-blocking
            int result = LibZmq.msgRecv(frame.msgSegment, getHandle(),
                                       RecvFlags.DONT_WAIT.getValue());
            if (result == -1) {
                int errno = LibZmq.errno();
                if (errno == ZmqConstants.EAGAIN) {
                    return RecvResult.empty();
                }
                throw new ZmqException(errno);
            }
            msg.add(frame.toByteArray());
            framesReceived = 1;

            // Subsequent frames - blocking to maintain atomicity
            // Note: If an error occurs here, the socket's internal state may be corrupted
            // with a partial message. The socket should be considered unreliable after this error.
            while (hasMore()) {
                frame.rebuild();
                result = LibZmq.msgRecv(frame.msgSegment, getHandle(),
                                       RecvFlags.NONE.getValue());
                if (result == -1) {
                    // Error on subsequent frame - provide detailed context
                    int errno = LibZmq.errno();
                    throw new ZmqException(errno,
                            "Failed to receive complete multipart message: " +
                            "received " + framesReceived + " frame(s) before error. " +
                            "Socket state may be corrupted and should be closed.");
                }
                msg.add(frame.toByteArray());
                framesReceived++;
            }
        }
        return RecvResult.success(msg);
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

    // ========== Buffer Management ==========

    /**
     * Checks if a buffer should be reset to save memory.
     * Resets buffer if it's significantly larger than average usage.
     *
     * @param bufferSize current buffer size
     * @param avgUsed average bytes actually used
     * @return true if buffer should be reset
     */
    private boolean shouldResetBuffer(long bufferSize, long avgUsed) {
        // Only reset if buffer is at least 4x larger than average usage
        // and average usage is at least 1KB (avoid thrashing on tiny messages)
        return bufferSize > avgUsed * BUFFER_RESET_THRESHOLD_MULTIPLIER
            && avgUsed >= 1024;
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
            // Clean up reusable Message objects
            if (recvMessage != null) {
                recvMessage.close();
                recvMessage = null;
            }
            if (recvStringMessage != null) {
                recvStringMessage.close();
                recvStringMessage = null;
            }
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
