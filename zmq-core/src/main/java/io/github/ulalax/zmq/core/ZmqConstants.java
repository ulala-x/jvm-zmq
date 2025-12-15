package io.github.ulalax.zmq.core;

/**
 * ZeroMQ constants for socket types, options, and error codes.
 * These constants match the values defined in zmq.h.
 */
public final class ZmqConstants {

    private ZmqConstants() {
        // Prevent instantiation
    }

    // ========== Socket Types ==========
    public static final int ZMQ_PAIR = 0;
    public static final int ZMQ_PUB = 1;
    public static final int ZMQ_SUB = 2;
    public static final int ZMQ_REQ = 3;
    public static final int ZMQ_REP = 4;
    public static final int ZMQ_DEALER = 5;
    public static final int ZMQ_ROUTER = 6;
    public static final int ZMQ_PULL = 7;
    public static final int ZMQ_PUSH = 8;
    public static final int ZMQ_XPUB = 9;
    public static final int ZMQ_XSUB = 10;
    public static final int ZMQ_STREAM = 11;

    // ========== Context Options ==========
    public static final int ZMQ_IO_THREADS = 1;
    public static final int ZMQ_MAX_SOCKETS = 2;
    public static final int ZMQ_SOCKET_LIMIT = 3;
    public static final int ZMQ_THREAD_PRIORITY = 3;
    public static final int ZMQ_THREAD_SCHED_POLICY = 4;
    public static final int ZMQ_MAX_MSGSZ = 5;
    public static final int ZMQ_MSG_T_SIZE = 6;
    public static final int ZMQ_THREAD_AFFINITY_CPU_ADD = 7;
    public static final int ZMQ_THREAD_AFFINITY_CPU_REMOVE = 8;
    public static final int ZMQ_THREAD_NAME_PREFIX = 9;

    // ========== Socket Options ==========
    public static final int ZMQ_AFFINITY = 4;
    public static final int ZMQ_ROUTING_ID = 5;
    public static final int ZMQ_SUBSCRIBE = 6;
    public static final int ZMQ_UNSUBSCRIBE = 7;
    public static final int ZMQ_RATE = 8;
    public static final int ZMQ_RECOVERY_IVL = 9;
    public static final int ZMQ_SNDBUF = 11;
    public static final int ZMQ_RCVBUF = 12;
    public static final int ZMQ_RCVMORE = 13;
    public static final int ZMQ_FD = 14;
    public static final int ZMQ_EVENTS = 15;
    public static final int ZMQ_TYPE = 16;
    public static final int ZMQ_LINGER = 17;
    public static final int ZMQ_RECONNECT_IVL = 18;
    public static final int ZMQ_BACKLOG = 19;
    public static final int ZMQ_RECONNECT_IVL_MAX = 21;
    public static final int ZMQ_MAXMSGSIZE = 22;
    public static final int ZMQ_SNDHWM = 23;
    public static final int ZMQ_RCVHWM = 24;
    public static final int ZMQ_MULTICAST_HOPS = 25;
    public static final int ZMQ_RCVTIMEO = 27;
    public static final int ZMQ_SNDTIMEO = 28;
    public static final int ZMQ_LAST_ENDPOINT = 32;
    public static final int ZMQ_ROUTER_MANDATORY = 33;
    public static final int ZMQ_TCP_KEEPALIVE = 34;
    public static final int ZMQ_TCP_KEEPALIVE_CNT = 35;
    public static final int ZMQ_TCP_KEEPALIVE_IDLE = 36;
    public static final int ZMQ_TCP_KEEPALIVE_INTVL = 37;
    public static final int ZMQ_IMMEDIATE = 39;
    public static final int ZMQ_XPUB_VERBOSE = 40;
    public static final int ZMQ_ROUTER_RAW = 41;
    public static final int ZMQ_IPV6 = 42;
    public static final int ZMQ_MECHANISM = 43;
    public static final int ZMQ_PLAIN_SERVER = 44;
    public static final int ZMQ_PLAIN_USERNAME = 45;
    public static final int ZMQ_PLAIN_PASSWORD = 46;
    public static final int ZMQ_CURVE_SERVER = 47;
    public static final int ZMQ_CURVE_PUBLICKEY = 48;
    public static final int ZMQ_CURVE_SECRETKEY = 49;
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
    public static final int ZMQ_DONTWAIT = 1;
    public static final int ZMQ_SNDMORE = 2;

    // ========== Poll Events ==========
    public static final int ZMQ_POLLIN = 1;
    public static final int ZMQ_POLLOUT = 2;
    public static final int ZMQ_POLLERR = 4;
    public static final int ZMQ_POLLPRI = 8;

    // ========== Message Properties ==========
    public static final int ZMQ_MORE = 1;
    public static final int ZMQ_SHARED = 3;

    // ========== Security Mechanisms ==========
    public static final int ZMQ_NULL = 0;
    public static final int ZMQ_PLAIN = 1;
    public static final int ZMQ_CURVE = 2;
    public static final int ZMQ_GSSAPI = 3;

    // ========== Error Codes ==========
    // Platform-specific EAGAIN value
    // - Linux/Windows: 11
    // - macOS: 35
    public static final int EAGAIN = detectEAGAIN();

    public static final int ENOTSUP = 95;
    public static final int EPROTONOSUPPORT = 93;
    public static final int ENOBUFS = 105;
    public static final int ENETDOWN = 100;
    public static final int EADDRINUSE = 98;
    public static final int EADDRNOTAVAIL = 99;
    public static final int ECONNREFUSED = 111;
    public static final int EINPROGRESS = 115;
    public static final int ENOTSOCK = 88;
    public static final int EMSGSIZE = 90;
    public static final int EAFNOSUPPORT = 97;
    public static final int ENETUNREACH = 101;
    public static final int ECONNABORTED = 103;
    public static final int ECONNRESET = 104;
    public static final int ENOTCONN = 107;
    public static final int ETIMEDOUT = 110;
    public static final int EHOSTUNREACH = 113;
    public static final int ENETRESET = 102;

    // ZMQ-specific error codes (base value)
    public static final int ZMQ_HAUSNUMERO = 156384712;

    // ZMQ error codes
    public static final int ETERM = ZMQ_HAUSNUMERO + 53;
    public static final int ENOENT = 2;
    public static final int EINTR = 4;
    public static final int EACCES = 13;
    public static final int EFAULT = 14;
    public static final int EINVAL = 22;
    public static final int EMFILE = 24;

    // ========== Socket Monitor Events ==========
    public static final int ZMQ_EVENT_CONNECTED = 1;
    public static final int ZMQ_EVENT_CONNECT_DELAYED = 2;
    public static final int ZMQ_EVENT_CONNECT_RETRIED = 4;
    public static final int ZMQ_EVENT_LISTENING = 8;
    public static final int ZMQ_EVENT_BIND_FAILED = 16;
    public static final int ZMQ_EVENT_ACCEPTED = 32;
    public static final int ZMQ_EVENT_ACCEPT_FAILED = 64;
    public static final int ZMQ_EVENT_CLOSED = 128;
    public static final int ZMQ_EVENT_CLOSE_FAILED = 256;
    public static final int ZMQ_EVENT_DISCONNECTED = 512;
    public static final int ZMQ_EVENT_MONITOR_STOPPED = 1024;
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
     * Detect platform-specific EAGAIN value.
     * macOS uses 35, Linux/Windows use 11.
     */
    private static int detectEAGAIN() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return 35;
        }
        return 11;
    }
}
