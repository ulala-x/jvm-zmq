package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.MemorySegment;

/**
 * ZeroMQ proxy utilities.
 *
 * <p>A proxy connects a frontend socket to a backend socket, forwarding
 * messages between them. This is useful for building message brokers,
 * queue devices, and forwarders.</p>
 *
 * <p><strong>Common proxy patterns:</strong></p>
 * <ul>
 *   <li>Queue device: ROUTER (frontend) to DEALER (backend)</li>
 *   <li>Forwarder: XSUB (frontend) to XPUB (backend)</li>
 *   <li>Streamer: PULL (frontend) to PUSH (backend)</li>
 * </ul>
 *
 * <p>Basic proxy usage:</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket frontend = new Socket(ctx, SocketType.ROUTER);
 *      Socket backend = new Socket(ctx, SocketType.DEALER)) {
 *
 *     frontend.bind("tcp://*:5555");
 *     backend.bind("tcp://*:5556");
 *
 *     // This blocks until context is terminated
 *     Proxy.start(frontend, backend);
 * }
 * }</pre>
 *
 * <p>Steerable proxy usage (with control):</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket frontend = new Socket(ctx, SocketType.ROUTER);
 *      Socket backend = new Socket(ctx, SocketType.DEALER);
 *      Socket control = new Socket(ctx, SocketType.PAIR)) {
 *
 *     frontend.bind("tcp://*:5555");
 *     backend.bind("tcp://*:5556");
 *     control.bind("inproc://control");
 *
 *     // In another thread:
 *     // control.send("PAUSE");    // Pause message flow
 *     // control.send("RESUME");   // Resume message flow
 *     // control.send("TERMINATE"); // Terminate proxy
 *
 *     Proxy.startSteerable(frontend, backend, control);
 * }
 * }</pre>
 *
 * @see Socket
 * @see SocketType
 */
public final class Proxy {

    private Proxy() {
        // Prevent instantiation
    }

    /**
     * Starts a basic proxy.
     *
     * <p>This method blocks until the context is terminated or an error occurs.
     * Messages are forwarded from frontend to backend and vice versa.</p>
     *
     * <p>If a capture socket is provided, all messages passing through
     * the proxy are also sent to the capture socket for monitoring/logging.</p>
     *
     * @param frontend The frontend socket (e.g., ROUTER, XSUB, PULL)
     * @param backend The backend socket (e.g., DEALER, XPUB, PUSH)
     * @param capture Optional capture socket for monitoring (null for none)
     * @throws NullPointerException if frontend or backend is null
     * @throws ZmqException if the proxy fails
     *
     * @see #startSteerable(Socket, Socket, Socket, Socket)
     */
    public static void start(Socket frontend, Socket backend, Socket capture) {
        if (frontend == null) {
            throw new NullPointerException("frontend cannot be null");
        }
        if (backend == null) {
            throw new NullPointerException("backend cannot be null");
        }

        MemorySegment frontendHandle = frontend.getHandle();
        MemorySegment backendHandle = backend.getHandle();
        MemorySegment captureHandle = capture != null ?
            capture.getHandle() : MemorySegment.NULL;

        int result = LibZmq.proxy(frontendHandle, backendHandle, captureHandle);
        ZmqException.throwIfError(result);
    }

    /**
     * Starts a basic proxy without capture.
     *
     * @param frontend The frontend socket
     * @param backend The backend socket
     * @throws NullPointerException if frontend or backend is null
     * @throws ZmqException if the proxy fails
     */
    public static void start(Socket frontend, Socket backend) {
        start(frontend, backend, null);
    }

    /**
     * Starts a steerable proxy.
     *
     * <p>A steerable proxy can be controlled via the control socket.
     * The control socket must be a PAIR, PUB, or SUB socket.</p>
     *
     * <p><strong>Control commands:</strong></p>
     * <ul>
     *   <li><code>PAUSE</code> - Pause message flow (messages are queued)</li>
     *   <li><code>RESUME</code> - Resume message flow</li>
     *   <li><code>TERMINATE</code> - Terminate the proxy</li>
     *   <li><code>STATISTICS</code> - Request statistics (proxy sends back 8 uint64 values)</li>
     * </ul>
     *
     * <p>Statistics format (8 uint64 values):</p>
     * <ol>
     *   <li>Messages received on frontend</li>
     *   <li>Bytes received on frontend</li>
     *   <li>Messages sent to backend</li>
     *   <li>Bytes sent to backend</li>
     *   <li>Messages received on backend</li>
     *   <li>Bytes received on backend</li>
     *   <li>Messages sent to frontend</li>
     *   <li>Bytes sent to frontend</li>
     * </ol>
     *
     * <p>Example control pattern:</p>
     * <pre>{@code
     * // In main thread:
     * Socket control = new Socket(ctx, SocketType.PAIR);
     * control.bind("inproc://proxy-control");
     * Proxy.startSteerable(frontend, backend, control);
     *
     * // In control thread:
     * Socket controller = new Socket(ctx, SocketType.PAIR);
     * controller.connect("inproc://proxy-control");
     * controller.send("PAUSE");
     * Thread.sleep(1000);
     * controller.send("RESUME");
     * controller.send("TERMINATE");
     * }</pre>
     *
     * @param frontend The frontend socket
     * @param backend The backend socket
     * @param control The control socket (PAIR, PUB, or SUB)
     * @param capture Optional capture socket for monitoring (null for none)
     * @throws NullPointerException if frontend, backend, or control is null
     * @throws ZmqException if the proxy fails
     */
    public static void startSteerable(Socket frontend, Socket backend,
                                     Socket control, Socket capture) {
        if (frontend == null) {
            throw new NullPointerException("frontend cannot be null");
        }
        if (backend == null) {
            throw new NullPointerException("backend cannot be null");
        }
        if (control == null) {
            throw new NullPointerException("control cannot be null");
        }

        MemorySegment frontendHandle = frontend.getHandle();
        MemorySegment backendHandle = backend.getHandle();
        MemorySegment captureHandle = capture != null ?
            capture.getHandle() : MemorySegment.NULL;
        MemorySegment controlHandle = control.getHandle();

        int result = LibZmq.proxySteerable(
            frontendHandle, backendHandle, captureHandle, controlHandle);
        ZmqException.throwIfError(result);
    }

    /**
     * Starts a steerable proxy without capture.
     *
     * @param frontend The frontend socket
     * @param backend The backend socket
     * @param control The control socket
     * @throws NullPointerException if any parameter is null
     * @throws ZmqException if the proxy fails
     */
    public static void startSteerable(Socket frontend, Socket backend, Socket control) {
        startSteerable(frontend, backend, control, null);
    }

    /**
     * Proxy control commands for steerable proxies.
     */
    public static final class Commands {
        /** Pause message flow (messages are queued) */
        public static final String PAUSE = "PAUSE";

        /** Resume message flow */
        public static final String RESUME = "RESUME";

        /** Terminate the proxy */
        public static final String TERMINATE = "TERMINATE";

        /** Request statistics */
        public static final String STATISTICS = "STATISTICS";

        private Commands() {
            // Prevent instantiation
        }
    }

    /**
     * Proxy statistics returned by STATISTICS command.
     */
    public static class Statistics {
        private final long frontendMessagesReceived;
        private final long frontendBytesReceived;
        private final long backendMessagesSent;
        private final long backendBytesSent;
        private final long backendMessagesReceived;
        private final long backendBytesReceived;
        private final long frontendMessagesSent;
        private final long frontendBytesSent;

        /**
         * Parses statistics from the 64-byte response.
         * @param data The 64-byte statistics data (8 uint64 values)
         * @return Parsed statistics
         * @throws IllegalArgumentException if data length is not 64
         */
        public static Statistics parse(byte[] data) {
            if (data.length != 64) {
                throw new IllegalArgumentException("Statistics data must be 64 bytes");
            }

            long[] values = new long[8];
            for (int i = 0; i < 8; i++) {
                long value = 0;
                for (int j = 0; j < 8; j++) {
                    value |= ((long)(data[i * 8 + j] & 0xFF)) << (j * 8);
                }
                values[i] = value;
            }

            return new Statistics(
                values[0], values[1], values[2], values[3],
                values[4], values[5], values[6], values[7]
            );
        }

        private Statistics(long frontendMessagesReceived, long frontendBytesReceived,
                         long backendMessagesSent, long backendBytesSent,
                         long backendMessagesReceived, long backendBytesReceived,
                         long frontendMessagesSent, long frontendBytesSent) {
            this.frontendMessagesReceived = frontendMessagesReceived;
            this.frontendBytesReceived = frontendBytesReceived;
            this.backendMessagesSent = backendMessagesSent;
            this.backendBytesSent = backendBytesSent;
            this.backendMessagesReceived = backendMessagesReceived;
            this.backendBytesReceived = backendBytesReceived;
            this.frontendMessagesSent = frontendMessagesSent;
            this.frontendBytesSent = frontendBytesSent;
        }

        /**
         * Gets the number of messages received on the frontend socket.
         * @return Message count
         */
        public long getFrontendMessagesReceived() { return frontendMessagesReceived; }

        /**
         * Gets the number of bytes received on the frontend socket.
         * @return Byte count
         */
        public long getFrontendBytesReceived() { return frontendBytesReceived; }

        /**
         * Gets the number of messages sent to the backend socket.
         * @return Message count
         */
        public long getBackendMessagesSent() { return backendMessagesSent; }

        /**
         * Gets the number of bytes sent to the backend socket.
         * @return Byte count
         */
        public long getBackendBytesSent() { return backendBytesSent; }

        /**
         * Gets the number of messages received on the backend socket.
         * @return Message count
         */
        public long getBackendMessagesReceived() { return backendMessagesReceived; }

        /**
         * Gets the number of bytes received on the backend socket.
         * @return Byte count
         */
        public long getBackendBytesReceived() { return backendBytesReceived; }

        /**
         * Gets the number of messages sent to the frontend socket.
         * @return Message count
         */
        public long getFrontendMessagesSent() { return frontendMessagesSent; }

        /**
         * Gets the number of bytes sent to the frontend socket.
         * @return Byte count
         */
        public long getFrontendBytesSent() { return frontendBytesSent; }

        @Override
        public String toString() {
            return String.format(
                "Statistics{" +
                "frontend: %d msgs (%d bytes) in, %d msgs (%d bytes) out, " +
                "backend: %d msgs (%d bytes) in, %d msgs (%d bytes) out}",
                frontendMessagesReceived, frontendBytesReceived,
                frontendMessagesSent, frontendBytesSent,
                backendMessagesReceived, backendBytesReceived,
                backendMessagesSent, backendBytesSent
            );
        }
    }
}
