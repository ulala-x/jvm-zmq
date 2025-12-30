package io.github.ulalax.zmq.core;

/**
 * ZeroMQ constants for socket types, options, flags, and error codes.
 * <p>
 * This class contains all the constants from the ZeroMQ C API (zmq.h).
 * These constants are used throughout the ZeroMQ API to configure sockets,
 * control message flow, and handle errors.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create a REQ socket
 * MemorySegment socket = LibZmq.socket(context, ZmqConstants.ZMQ_REQ);
 *
 * // Set socket options
 * LibZmq.ctxSet(context, ZmqConstants.ZMQ_IO_THREADS, 4);
 *
 * // Send with non-blocking flag
 * LibZmq.send(socket, data, len, ZmqConstants.ZMQ_DONTWAIT);
 * }</pre>
 *
 * <p>
 * All constants in this class are public static final integers that correspond
 * directly to their C API counterparts.
 * </p>
 *
 * @see <a href="http://api.zeromq.org/master:zmq">ZeroMQ API Reference</a>
 * @since 1.0.0
 */
public final class ZmqConstants {

    private ZmqConstants() {
        // Prevent instantiation
    }

    // ========== Socket Types ==========

    /** Exclusive pair pattern. Used with only one peer. */
    public static final int ZMQ_PAIR = 0;

    /** Publish pattern. Distributes messages to all subscribers. */
    public static final int ZMQ_PUB = 1;

    /** Subscribe pattern. Receives messages from publishers matching subscriptions. */
    public static final int ZMQ_SUB = 2;

    /** Request pattern. Used by a client to send requests and receive replies. */
    public static final int ZMQ_REQ = 3;

    /** Reply pattern. Used by a service to receive requests and send replies. */
    public static final int ZMQ_REP = 4;

    /** Dealer pattern. Asynchronous REQ (load-balances outgoing messages). */
    public static final int ZMQ_DEALER = 5;

    /** Router pattern. Routes messages to specific peers based on identity. */
    public static final int ZMQ_ROUTER = 6;

    /** Pull pattern. Receives messages from pushers (fair-queues incoming messages). */
    public static final int ZMQ_PULL = 7;

    /** Push pattern. Sends messages to pullers (load-balances outgoing messages). */
    public static final int ZMQ_PUSH = 8;

    /** Extended publish pattern. Same as PUB except receives subscriptions as messages. */
    public static final int ZMQ_XPUB = 9;

    /** Extended subscribe pattern. Same as SUB except sends subscriptions as messages. */
    public static final int ZMQ_XSUB = 10;

    /** Stream pattern. For working with TCP connections directly. */
    public static final int ZMQ_STREAM = 11;

    // ========== Context Options ==========

    /** Number of I/O threads for the context (default: 1). */
    public static final int ZMQ_IO_THREADS = 1;

    /** Maximum number of sockets that can be created in the context. */
    public static final int ZMQ_MAX_SOCKETS = 2;

    /** Largest configurable number of sockets (read-only). */
    public static final int ZMQ_SOCKET_LIMIT = 3;

    /** Thread priority for the context's threads. */
    public static final int ZMQ_THREAD_PRIORITY = 3;

    /** Thread scheduling policy for the context's threads. */
    public static final int ZMQ_THREAD_SCHED_POLICY = 4;

    /** Maximum size of messages (default: INT_MAX). */
    public static final int ZMQ_MAX_MSGSZ = 5;

    /** Size of zmq_msg_t structure (read-only). */
    public static final int ZMQ_MSG_T_SIZE = 6;

    /** Add CPU to thread affinity list. */
    public static final int ZMQ_THREAD_AFFINITY_CPU_ADD = 7;

    /** Remove CPU from thread affinity list. */
    public static final int ZMQ_THREAD_AFFINITY_CPU_REMOVE = 8;

    /** Prefix for thread names created by the context. */
    public static final int ZMQ_THREAD_NAME_PREFIX = 9;

    // ========== Socket Options ==========

    /** I/O thread affinity for the socket. */
    public static final int ZMQ_AFFINITY = 4;

    /** Socket identity for ROUTER sockets. */
    public static final int ZMQ_ROUTING_ID = 5;

    /** Establishes a message filter on SUB socket. */
    public static final int ZMQ_SUBSCRIBE = 6;

    /** Removes a message filter from SUB socket. */
    public static final int ZMQ_UNSUBSCRIBE = 7;

    /** Maximum send rate for multicast transports (kilobits per second). */
    public static final int ZMQ_RATE = 8;

    /** Multicast recovery interval (milliseconds). */
    public static final int ZMQ_RECOVERY_IVL = 9;

    /** Kernel transmit buffer size (bytes). */
    public static final int ZMQ_SNDBUF = 11;

    /** Kernel receive buffer size (bytes). */
    public static final int ZMQ_RCVBUF = 12;

    /** More message parts to follow (read-only). */
    public static final int ZMQ_RCVMORE = 13;

    /** File descriptor associated with the socket (read-only). */
    public static final int ZMQ_FD = 14;

    /** Socket event state (read-only, returns ZMQ_POLLIN/ZMQ_POLLOUT). */
    public static final int ZMQ_EVENTS = 15;

    /** Socket type (read-only). */
    public static final int ZMQ_TYPE = 16;

    /** Linger period for socket shutdown (milliseconds, -1 = infinite, 0 = immediate). */
    public static final int ZMQ_LINGER = 17;

    /** Initial reconnection interval (milliseconds). */
    public static final int ZMQ_RECONNECT_IVL = 18;

    /** Maximum pending connections for listening sockets. */
    public static final int ZMQ_BACKLOG = 19;

    /** Maximum reconnection interval (milliseconds). */
    public static final int ZMQ_RECONNECT_IVL_MAX = 21;

    /** Maximum acceptable message size (bytes, 0 = unlimited). */
    public static final int ZMQ_MAXMSGSIZE = 22;

    /** High water mark for outbound messages (number of messages). */
    public static final int ZMQ_SNDHWM = 23;

    /** High water mark for inbound messages (number of messages). */
    public static final int ZMQ_RCVHWM = 24;

    /** Maximum hops for multicast packets. */
    public static final int ZMQ_MULTICAST_HOPS = 25;

    /** Timeout for receive operations (milliseconds, -1 = infinite). */
    public static final int ZMQ_RCVTIMEO = 27;

    /** Timeout for send operations (milliseconds, -1 = infinite). */
    public static final int ZMQ_SNDTIMEO = 28;

    /** Last endpoint bound for TCP and IPC transports (read-only). */
    public static final int ZMQ_LAST_ENDPOINT = 32;

    /** Accept only routable messages on ROUTER sockets. */
    public static final int ZMQ_ROUTER_MANDATORY = 33;

    /** Enable/disable TCP keepalive. */
    public static final int ZMQ_TCP_KEEPALIVE = 34;

    /** TCP keepalive count. */
    public static final int ZMQ_TCP_KEEPALIVE_CNT = 35;

    /** TCP keepalive idle time (seconds). */
    public static final int ZMQ_TCP_KEEPALIVE_IDLE = 36;

    /** TCP keepalive interval (seconds). */
    public static final int ZMQ_TCP_KEEPALIVE_INTVL = 37;

    /** Queue messages only to completed connections. */
    public static final int ZMQ_IMMEDIATE = 39;

    /** Provide all subscription messages on XPUB sockets. */
    public static final int ZMQ_XPUB_VERBOSE = 40;

    /** Switch ROUTER socket to raw mode. */
    public static final int ZMQ_ROUTER_RAW = 41;

    /** Enable IPv6 on socket. */
    public static final int ZMQ_IPV6 = 42;

    /** Security mechanism (read-only). */
    public static final int ZMQ_MECHANISM = 43;

    /** PLAIN server role. */
    public static final int ZMQ_PLAIN_SERVER = 44;

    /** PLAIN security username. */
    public static final int ZMQ_PLAIN_USERNAME = 45;

    /** PLAIN security password. */
    public static final int ZMQ_PLAIN_PASSWORD = 46;

    /** CURVE server role. */
    public static final int ZMQ_CURVE_SERVER = 47;

    /** CURVE public key. */
    public static final int ZMQ_CURVE_PUBLICKEY = 48;

    /** CURVE secret key. */
    public static final int ZMQ_CURVE_SECRETKEY = 49;

    /** CURVE server key. */
    public static final int ZMQ_CURVE_SERVERKEY = 50;
    public static final int ZMQ_PROBE_ROUTER = 51;
    public static final int ZMQ_REQ_CORRELATE = 52;
    public static final int ZMQ_REQ_RELAXED = 53;
    public static final int ZMQ_CONFLATE = 54;
    public static final int ZMQ_ZAP_DOMAIN = 55;
    public static final int ZMQ_ROUTER_HANDOVER = 56;
    public static final int ZMQ_TOS = 57;
    public static final int ZMQ_CONNECT_ROUTING_ID = 61;
    public static final int ZMQ_GSSAPI_SERVER = 62;
    public static final int ZMQ_GSSAPI_PRINCIPAL = 63;
    public static final int ZMQ_GSSAPI_SERVICE_PRINCIPAL = 64;
    public static final int ZMQ_GSSAPI_PLAINTEXT = 65;
    public static final int ZMQ_HANDSHAKE_IVL = 66;
    public static final int ZMQ_SOCKS_PROXY = 68;
    public static final int ZMQ_XPUB_NODROP = 69;
    public static final int ZMQ_BLOCKY = 70;
    public static final int ZMQ_XPUB_MANUAL = 71;
    public static final int ZMQ_XPUB_WELCOME_MSG = 72;
    public static final int ZMQ_STREAM_NOTIFY = 73;
    public static final int ZMQ_INVERT_MATCHING = 74;
    public static final int ZMQ_HEARTBEAT_IVL = 75;
    public static final int ZMQ_HEARTBEAT_TTL = 76;
    public static final int ZMQ_HEARTBEAT_TIMEOUT = 77;
    public static final int ZMQ_XPUB_VERBOSER = 78;
    public static final int ZMQ_CONNECT_TIMEOUT = 79;
    public static final int ZMQ_TCP_MAXRT = 80;
    public static final int ZMQ_THREAD_SAFE = 81;
    public static final int ZMQ_MULTICAST_MAXTPDU = 84;
    public static final int ZMQ_VMCI_BUFFER_SIZE = 85;
    public static final int ZMQ_VMCI_BUFFER_MIN_SIZE = 86;
    public static final int ZMQ_VMCI_BUFFER_MAX_SIZE = 87;
    public static final int ZMQ_VMCI_CONNECT_TIMEOUT = 88;
    public static final int ZMQ_USE_FD = 89;
    public static final int ZMQ_GSSAPI_PRINCIPAL_NAMETYPE = 90;
    public static final int ZMQ_GSSAPI_SERVICE_PRINCIPAL_NAMETYPE = 91;
    public static final int ZMQ_BINDTODEVICE = 92;

    // ========== Send/Recv Flags ==========

    /** Non-blocking mode flag. Send/receive operations return immediately with EAGAIN if they would block. */
    public static final int ZMQ_DONTWAIT = 1;

    /** Multi-part message flag. Indicates that more message parts will follow. */
    public static final int ZMQ_SNDMORE = 2;

    // ========== Poll Events ==========

    /** At least one message can be received from the socket without blocking. */
    public static final int ZMQ_POLLIN = 1;

    /** At least one message can be sent to the socket without blocking. */
    public static final int ZMQ_POLLOUT = 2;

    /** The socket has encountered an error condition. */
    public static final int ZMQ_POLLERR = 4;

    /** Priority data is available to read. */
    public static final int ZMQ_POLLPRI = 8;

    // ========== Message Properties ==========

    /** Indicates whether there are more message parts to follow. */
    public static final int ZMQ_MORE = 1;

    /** Indicates the message is shared and uses reference counting. */
    public static final int ZMQ_SHARED = 3;

    // ========== Security Mechanisms ==========

    /** No security mechanism (default). */
    public static final int ZMQ_NULL = 0;

    /** PLAIN security mechanism (username/password). */
    public static final int ZMQ_PLAIN = 1;

    /** CURVE security mechanism (elliptic curve cryptography). */
    public static final int ZMQ_CURVE = 2;

    /** GSSAPI security mechanism (Kerberos). */
    public static final int ZMQ_GSSAPI = 3;

    // ========== Error Codes ==========

    /**
     * Resource temporarily unavailable (would block).
     * Platform-specific value: Linux/Windows = 11, macOS = 35.
     */
    public static final int EAGAIN = detectEAGAIN();

    /** Operation not supported. */
    public static final int ENOTSUP = 95;

    /** Protocol not supported. */
    public static final int EPROTONOSUPPORT = 93;

    /** No buffer space available. */
    public static final int ENOBUFS = 105;

    /** Network is down. */
    public static final int ENETDOWN = 100;

    /** Address already in use. */
    public static final int EADDRINUSE = 98;

    /** Address not available. */
    public static final int EADDRNOTAVAIL = 99;

    /** Connection refused. */
    public static final int ECONNREFUSED = 111;

    /** Operation in progress. */
    public static final int EINPROGRESS = 115;

    /** Socket operation on non-socket. */
    public static final int ENOTSOCK = 88;

    /** Message too long. */
    public static final int EMSGSIZE = 90;

    /** Address family not supported. */
    public static final int EAFNOSUPPORT = 97;

    /** Network unreachable. */
    public static final int ENETUNREACH = 101;

    /** Connection aborted. */
    public static final int ECONNABORTED = 103;

    /** Connection reset by peer. */
    public static final int ECONNRESET = 104;

    /** Socket is not connected. */
    public static final int ENOTCONN = 107;

    /** Connection timed out. */
    public static final int ETIMEDOUT = 110;

    /** No route to host. */
    public static final int EHOSTUNREACH = 113;

    /** Network dropped connection on reset. */
    public static final int ENETRESET = 102;

    /**
     * Base error number for ZMQ-specific errors.
     * All ZMQ-specific errors are offset from this value.
     */
    public static final int ZMQ_HAUSNUMERO = 156384712;

    /** ZMQ context was terminated. */
    public static final int ETERM = ZMQ_HAUSNUMERO + 53;

    /** No such file or directory. */
    public static final int ENOENT = 2;

    /** Interrupted system call. */
    public static final int EINTR = 4;

    /** Permission denied. */
    public static final int EACCES = 13;

    /** Bad address. */
    public static final int EFAULT = 14;

    /** Invalid argument. */
    public static final int EINVAL = 22;

    /** Too many open files. */
    public static final int EMFILE = 24;

    // ========== Socket Monitor Events ==========

    /** Connection established event. */
    public static final int ZMQ_EVENT_CONNECTED = 1;

    /** Synchronous connect failed, will retry. */
    public static final int ZMQ_EVENT_CONNECT_DELAYED = 2;

    /** Asynchronous connect/reconnection attempt. */
    public static final int ZMQ_EVENT_CONNECT_RETRIED = 4;

    /** Socket bound to an address, ready to accept connections. */
    public static final int ZMQ_EVENT_LISTENING = 8;

    /** Socket could not bind to an address. */
    public static final int ZMQ_EVENT_BIND_FAILED = 16;

    /** Connection accepted. */
    public static final int ZMQ_EVENT_ACCEPTED = 32;

    /** Connection acceptance failed. */
    public static final int ZMQ_EVENT_ACCEPT_FAILED = 64;

    /** Connection closed. */
    public static final int ZMQ_EVENT_CLOSED = 128;

    /** Connection close failed. */
    public static final int ZMQ_EVENT_CLOSE_FAILED = 256;

    /** Connection disconnected. */
    public static final int ZMQ_EVENT_DISCONNECTED = 512;

    /** Event monitoring stopped. */
    public static final int ZMQ_EVENT_MONITOR_STOPPED = 1024;

    /** Bitmask to monitor all events. */
    public static final int ZMQ_EVENT_ALL = 0xFFFF;

    // ========== Protocol Errors ==========
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_UNSPECIFIED = 0x10000000;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_UNEXPECTED_COMMAND = 0x10000001;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_INVALID_SEQUENCE = 0x10000002;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_KEY_EXCHANGE = 0x10000003;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_UNSPECIFIED = 0x10000011;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_MESSAGE = 0x10000012;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_HELLO = 0x10000013;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_INITIATE = 0x10000014;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_ERROR = 0x10000015;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_READY = 0x10000016;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_WELCOME = 0x10000017;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_INVALID_METADATA = 0x10000018;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_CRYPTOGRAPHIC = 0x11000001;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MECHANISM_MISMATCH = 0x11000002;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_UNSPECIFIED = 0x20000000;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_MALFORMED_REPLY = 0x20000001;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_BAD_REQUEST_ID = 0x20000002;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_BAD_VERSION = 0x20000003;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_INVALID_STATUS_CODE = 0x20000004;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_INVALID_METADATA = 0x20000005;
    public static final int ZMQ_PROTOCOL_ERROR_WS_UNSPECIFIED = 0x30000000;

    /**
     * Detects the platform-specific EAGAIN error code value.
     * <p>
     * The EAGAIN error code has different numeric values on different platforms:
     * </p>
     * <ul>
     *   <li>macOS: 35</li>
     *   <li>Linux/Windows: 11</li>
     * </ul>
     *
     * @return the platform-specific EAGAIN error code
     */
    private static int detectEAGAIN() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return 35;
        }
        return 11;
    }
}
