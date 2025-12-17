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
     * Attempts to send a byte array message on the socket without blocking.
     * <p>
     * This method automatically sets the {@link SendFlags#DONT_WAIT} flag to ensure
     * non-blocking behavior. If the message cannot be sent immediately (EAGAIN),
     * the method returns {@code false} instead of throwing an exception.
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe. Concurrent calls on the
     * same socket from multiple threads will result in undefined behavior. The socket
     * must not be closed while this method is executing.
     * <p>
     * <b>errno Behavior:</b> When the underlying ZMQ socket would block (EAGAIN), this
     * method returns {@code false} as a normal control flow. All other errors (e.g.,
     * ETERM, ENOTSOCK, EINTR) are thrown as {@link ZmqException}.
     *
     * @param data the message data to send (must not be null)
     * @param flags additional send flags (e.g., {@link SendFlags#SEND_MORE} for multipart messages).
     *              {@link SendFlags#DONT_WAIT} is automatically added.
     * @return {@code true} if the message was queued successfully,
     *         {@code false} if the operation would block (EAGAIN)
     * @throws NullPointerException if {@code data} or {@code flags} is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     * byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
     *
     * // Simple send
     * if (socket.trySend(data, SendFlags.NONE)) {
     *     System.out.println("Message sent");
     * } else {
     *     System.out.println("Would block - try again later");
     * }
     *
     * // Multipart send
     * socket.trySend(part1, SendFlags.SEND_MORE);
     * socket.trySend(part2, SendFlags.NONE);
     * }</pre>
     *
     * @see #send(byte[], SendFlags)
     * @see SendFlags#DONT_WAIT
     * @see SendFlags#SEND_MORE
     */
    public boolean trySend(byte[] data, SendFlags flags) {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }
        if (data == null) {
            throw new NullPointerException("data cannot be null");
        }

        // Combine with non-blocking flag (don't use fromValue as it can't handle combined flags)
        int combinedFlags = flags.getValue() | SendFlags.DONT_WAIT.getValue();

        // Expand buffer if needed
        if (data.length > sendBufferSize) {
            sendBuffer = ioArena.allocate(data.length);
            sendBufferSize = data.length;
        }
        MemorySegment.copy(data, 0, sendBuffer, ValueLayout.JAVA_BYTE, 0, data.length);

        // Direct LibZmq call with combined flags
        int result = LibZmq.send(getHandle(), sendBuffer, data.length, combinedFlags);
        if (result == -1) {
            int errno = LibZmq.errno();  // Immediately after error check
            if (errno == ZmqConstants.EAGAIN) {
                return false;  // Would block - normal control flow
            }
            ZmqException.throwIfError(-1);  // Real error - throw
        }

        // Track buffer usage for adaptive sizing
        sendBufferTotalUsed += data.length;
        sendBufferUsageCount++;

        if (sendBufferUsageCount >= BUFFER_USAGE_SAMPLE_SIZE) {
            long avgUsed = sendBufferTotalUsed / sendBufferUsageCount;
            if (shouldResetBuffer(sendBufferSize, avgUsed)) {
                // Reset to reasonable size (2x average + 1KB padding)
                int newSize = (int) (avgUsed * 2) + 1024;
                sendBuffer = ioArena.allocate(newSize);
                sendBufferSize = newSize;
            }
            // Reset counters
            sendBufferUsageCount = 0;
            sendBufferTotalUsed = 0;
        }

        return true;
    }

    /**
     * Attempts to send a UTF-8 encoded string message on the socket without blocking.
     * <p>
     * This is a convenience method that encodes the string to UTF-8 and calls
     * {@link #trySend(byte[], SendFlags)}. The same non-blocking semantics apply.
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe. Concurrent calls on the
     * same socket from multiple threads will result in undefined behavior. The socket
     * must not be closed while this method is executing.
     *
     * @param text the message text to send (must not be null)
     * @param flags additional send flags (e.g., {@link SendFlags#SEND_MORE} for multipart messages).
     *              {@link SendFlags#DONT_WAIT} is automatically added.
     * @return {@code true} if the message was queued successfully,
     *         {@code false} if the operation would block (EAGAIN)
     * @throws NullPointerException if {@code text} or {@code flags} is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     *
     * if (socket.trySend("Hello, World!", SendFlags.NONE)) {
     *     System.out.println("Message sent");
     * } else {
     *     System.out.println("Would block - try again later");
     * }
     * }</pre>
     *
     * @see #trySend(byte[], SendFlags)
     * @see #send(String, SendFlags)
     */
    public boolean trySend(String text, SendFlags flags) {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }
        if (text == null) {
            throw new NullPointerException("text cannot be null");
        }
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return trySend(data, flags);
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
     * Attempts to receive a message into a byte array buffer without blocking.
     * <p>
     * This method automatically sets the {@link RecvFlags#DONT_WAIT} flag to ensure
     * non-blocking behavior. If no message is available (EAGAIN), the method returns
     * {@code -1} instead of throwing an exception.
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe. Concurrent calls on the
     * same socket from multiple threads will result in undefined behavior. The socket
     * must not be closed while this method is executing.
     * <p>
     * <b>errno Behavior:</b> When no message is available (EAGAIN), this method returns
     * {@code -1} as a normal control flow. All other errors (e.g., ETERM, ENOTSOCK,
     * EINTR) are thrown as {@link ZmqException}.
     * <p>
     * <b>Buffer Sizing:</b> If the received message is larger than the buffer, the message
     * will be truncated. Consider using {@link #tryRecvBytes(RecvFlags)} for automatic sizing.
     *
     * @param buffer the buffer to receive the message into (must not be null)
     * @param flags additional receive flags. {@link RecvFlags#DONT_WAIT} is automatically added.
     * @return the number of bytes received (may exceed buffer size if truncated),
     *         or {@code -1} if no message is available (EAGAIN)
     * @throws NullPointerException if {@code buffer} or {@code flags} is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     * byte[] buffer = new byte[256];
     *
     * int size = socket.tryRecv(buffer, RecvFlags.NONE);
     * if (size >= 0) {
     *     System.out.println("Received " + size + " bytes");
     *     if (size > buffer.length) {
     *         System.out.println("Warning: message was truncated");
     *     }
     * } else {
     *     System.out.println("No message available");
     * }
     * }</pre>
     *
     * @see #recv(byte[], RecvFlags)
     * @see #tryRecvBytes(RecvFlags)
     * @see RecvFlags#DONT_WAIT
     */
    public int tryRecv(byte[] buffer, RecvFlags flags) {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }
        if (buffer == null) {
            throw new NullPointerException("buffer cannot be null");
        }

        // Combine with non-blocking flag (don't use fromValue as it can't handle combined flags)
        int combinedFlags = flags.getValue() | RecvFlags.DONT_WAIT.getValue();

        // Expand buffer if needed
        if (buffer.length > recvBufferSize) {
            recvBuffer = ioArena.allocate(buffer.length);
            recvBufferSize = buffer.length;
        }

        // Direct LibZmq call
        int result = LibZmq.recv(getHandle(), recvBuffer, buffer.length, combinedFlags);
        if (result == -1) {
            int errno = LibZmq.errno();  // Immediately after error check
            if (errno == ZmqConstants.EAGAIN) {
                return -1;  // Would block - normal control flow
            }
            ZmqException.throwIfError(-1);  // Real error - throw
        }
        MemorySegment.copy(recvBuffer, ValueLayout.JAVA_BYTE, 0, buffer, 0, result);

        // Track buffer usage for adaptive sizing
        recvBufferTotalUsed += result;
        recvBufferUsageCount++;

        if (recvBufferUsageCount >= BUFFER_USAGE_SAMPLE_SIZE) {
            long avgUsed = recvBufferTotalUsed / recvBufferUsageCount;
            if (shouldResetBuffer(recvBufferSize, avgUsed)) {
                // Reset to reasonable size (2x average + 1KB padding)
                int newSize = (int) (avgUsed * 2) + 1024;
                recvBuffer = ioArena.allocate(newSize);
                recvBufferSize = newSize;
            }
            // Reset counters
            recvBufferUsageCount = 0;
            recvBufferTotalUsed = 0;
        }

        return result;
    }

    /**
     * Attempts to receive a UTF-8 encoded string message without blocking.
     * <p>
     * This method reuses an internal {@link Message} object to receive the data,
     * then converts it to a UTF-8 string. If no message is available (EAGAIN), the
     * method returns {@code null} instead of throwing an exception.
     * <p>
     * <b>Performance Note:</b> This method reuses an internal {@link Message} object
     * to avoid memory allocation when {@code EAGAIN} occurs. This optimization makes
     * the method significantly faster in high-frequency polling scenarios.
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe. Concurrent calls on the
     * same socket from multiple threads will result in undefined behavior. The socket
     * must not be closed while this method is executing.
     *
     * @param flags additional receive flags. {@link RecvFlags#DONT_WAIT} is automatically added.
     * @return the received message as a UTF-8 string, or {@code null} if no message is available (EAGAIN)
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     *
     * String msg = socket.tryRecvString(RecvFlags.NONE);
     * if (msg != null) {
     *     System.out.println("Received: " + msg);
     * } else {
     *     System.out.println("No message available");
     * }
     *
     * // Event loop pattern
     * while (running) {
     *     String message = socket.tryRecvString(RecvFlags.NONE);
     *     if (message != null) {
     *         processMessage(message);
     *     } else {
     *         // Do other work or sleep briefly
     *         Thread.sleep(10);
     *     }
     * }
     * }</pre>
     *
     * @see #recvString(RecvFlags)
     * @see #tryRecv(byte[], RecvFlags)
     */
    public String tryRecvString(RecvFlags flags) {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }

        int combinedFlags = flags.getValue() | RecvFlags.DONT_WAIT.getValue();

        // Reuse message object to avoid allocation on EAGAIN
        recvStringMessage.rebuild();
        int result = LibZmq.msgRecv(recvStringMessage.msgSegment, getHandle(), combinedFlags);
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return null;  // Would block - no allocation occurred
            }
            ZmqException.throwIfError(-1);
        }
        return recvStringMessage.toString();
    }

    /**
     * Attempts to receive a message as a byte array without blocking.
     * <p>
     * This method reuses an internal {@link Message} object to receive the data,
     * then copies it to a new byte array. If no message is available (EAGAIN), the
     * method returns {@code null} instead of throwing an exception.
     * <p>
     * <b>Performance Note:</b> This method reuses an internal {@link Message} object
     * to avoid memory allocation when {@code EAGAIN} occurs. This optimization makes
     * the method significantly faster in high-frequency polling scenarios.
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe. Concurrent calls on the
     * same socket from multiple threads will result in undefined behavior. The socket
     * must not be closed while this method is executing.
     *
     * @param flags additional receive flags. {@link RecvFlags#DONT_WAIT} is automatically added.
     * @return the received message as a byte array, or {@code null} if no message is available (EAGAIN)
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     *
     * byte[] data = socket.tryRecvBytes(RecvFlags.NONE);
     * if (data != null) {
     *     System.out.println("Received " + data.length + " bytes");
     *     processData(data);
     * } else {
     *     System.out.println("No message available");
     * }
     *
     * // Polling pattern
     * while (running) {
     *     byte[] message = socket.tryRecvBytes(RecvFlags.NONE);
     *     if (message != null) {
     *         handleMessage(message);
     *     } else {
     *         // No message available, do other work
     *         performBackgroundTasks();
     *     }
     * }
     * }</pre>
     *
     * @see #recvBytes(RecvFlags)
     * @see #tryRecv(byte[], RecvFlags)
     */
    public byte[] tryRecvBytes(RecvFlags flags) {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }

        int combinedFlags = flags.getValue() | RecvFlags.DONT_WAIT.getValue();

        // Reuse message object to avoid allocation on EAGAIN
        recvMessage.rebuild();
        int result = LibZmq.msgRecv(recvMessage.msgSegment, getHandle(), combinedFlags);
        if (result == -1) {
            int errno = LibZmq.errno();
            if (errno == ZmqConstants.EAGAIN) {
                return null;  // Would block - no allocation occurred
            }
            ZmqException.throwIfError(-1);
        }
        return recvMessage.toByteArray();
    }

    // ========== Convenience Methods (cppzmq-style) ==========

    /**
     * Sends a message frame with SEND_MORE flag for multipart messaging.
     * This is a convenience method equivalent to {@code send(data, SendFlags.SEND_MORE)}.
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe.
     *
     * @param data the message data to send
     * @throws NullPointerException if data is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a ZMQ error occurs
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     * socket.sendMore("header".getBytes());
     * socket.sendMore("body1".getBytes());
     * socket.send("body2".getBytes());  // Last frame without SEND_MORE
     * }</pre>
     *
     * @see #send(byte[], SendFlags)
     * @see SendFlags#SEND_MORE
     */
    public void sendMore(byte[] data) {
        send(data, SendFlags.SEND_MORE);
    }

    /**
     * Sends a UTF-8 encoded string with SEND_MORE flag for multipart messaging.
     * This is a convenience method equivalent to {@code send(text, SendFlags.SEND_MORE)}.
     *
     * @param text the message text to send
     * @throws NullPointerException if text is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a ZMQ error occurs
     *
     * @see #sendMore(byte[])
     */
    public void sendMore(String text) {
        send(text, SendFlags.SEND_MORE);
    }

    /**
     * Sends a Message with SEND_MORE flag for multipart messaging.
     * This is a convenience method equivalent to {@code send(msg, SendFlags.SEND_MORE)}.
     *
     * @param msg the message to send
     * @throws NullPointerException if msg is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a ZMQ error occurs
     *
     * @see #sendMore(byte[])
     */
    public void sendMore(Message msg) {
        send(msg, SendFlags.SEND_MORE);
    }

    /**
     * Attempts to send a message frame with SEND_MORE flag without blocking.
     * This is a convenience method equivalent to {@code trySend(data, SendFlags.SEND_MORE)}.
     *
     * @param data the message data to send
     * @return true if sent, false if would block (EAGAIN)
     * @throws NullPointerException if data is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     * if (socket.trySendMore("header".getBytes())) {
     *     if (socket.trySend("body".getBytes(), SendFlags.NONE)) {
     *         System.out.println("Multipart message sent");
     *     }
     * }
     * }</pre>
     *
     * @see #trySend(byte[], SendFlags)
     */
    public boolean trySendMore(byte[] data) {
        return trySend(data, SendFlags.SEND_MORE);
    }

    /**
     * Attempts to send a UTF-8 encoded string with SEND_MORE flag without blocking.
     *
     * @param text the message text to send
     * @return true if sent, false if would block (EAGAIN)
     * @throws NullPointerException if text is null
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN)
     *
     * @see #trySendMore(byte[])
     */
    public boolean trySendMore(String text) {
        return trySend(text, SendFlags.SEND_MORE);
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
     * Attempts to receive a complete multipart message without blocking.
     * <p>
     * This method receives all parts of a multipart message atomically. The first frame
     * is received with {@link RecvFlags#DONT_WAIT}, so if no message is available, the
     * method returns {@code null}. Once the first frame is received, subsequent frames
     * are received with blocking semantics to ensure atomicity.
     * <p>
     * <b>Thread-Safety:</b> This method is NOT thread-safe. Concurrent calls on the
     * same socket from multiple threads will result in undefined behavior. The socket
     * must not be closed while this method is executing.
     * <p>
     * <b>Atomicity Guarantee:</b> This method ensures that either all frames of a multipart
     * message are received, or none are received (if EAGAIN occurs on the first frame).
     * Once the first frame is successfully received, all remaining frames are received with
     * blocking semantics to maintain message integrity.
     * <p>
     * <b>Error Recovery:</b> If the first frame is received successfully but an error occurs
     * on a subsequent frame, a {@link ZmqException} will be thrown with detailed context
     * including the number of frames successfully received. The partial message is discarded
     * to prevent processing incomplete data. <b>Critical:</b> The socket's internal state may
     * be corrupted after such an error and the socket should be closed and recreated for
     * reliable operation.
     *
     * @return the complete multipart message, or {@code null} if no message is available (EAGAIN)
     * @throws IllegalStateException if the socket is closed
     * @throws ZmqException if a real ZMQ error occurs (not EAGAIN), with detailed context about
     *         which frame failed if the error occurred during subsequent frame reception
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Socket socket = new Socket(ctx, SocketType.DEALER);
     *
     * MultipartMessage msg = socket.tryRecvMultipart();
     * if (msg != null) {
     *     System.out.println("Received " + msg.size() + " frames");
     *     for (byte[] frame : msg) {
     *         System.out.println("Frame: " + new String(frame, StandardCharsets.UTF_8));
     *     }
     * } else {
     *     System.out.println("No message available");
     * }
     *
     * // Event loop with multipart messages
     * while (running) {
     *     MultipartMessage message = socket.tryRecvMultipart();
     *     if (message != null) {
     *         // Process all frames together
     *         String identity = new String(message.get(0), StandardCharsets.UTF_8);
     *         String payload = new String(message.get(1), StandardCharsets.UTF_8);
     *         handleRequest(identity, payload);
     *     } else {
     *         // No message, perform other work
     *         Thread.sleep(10);
     *     }
     * }
     * }</pre>
     *
     * @see #recvMultipart()
     * @see MultipartMessage
     */
    public MultipartMessage tryRecvMultipart() {
        if (closed) {
            throw new IllegalStateException("Socket is closed");
        }
        MultipartMessage msg = new MultipartMessage();
        int framesReceived = 0;

        try (Message frame = new Message()) {
            // First frame - non-blocking
            int result = LibZmq.msgRecv(frame.msgSegment, getHandle(),
                                       RecvFlags.DONT_WAIT.getValue());
            if (result == -1) {
                int errno = LibZmq.errno();  // Immediately after error check
                if (errno == ZmqConstants.EAGAIN) {
                    return null;  // Would block - no partial state
                }
                ZmqException.throwIfError(-1);  // Real error - throw
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
        return msg;
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
