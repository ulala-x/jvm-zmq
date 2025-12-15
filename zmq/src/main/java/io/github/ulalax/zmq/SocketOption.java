package io.github.ulalax.zmq;

/**
 * ZeroMQ socket options.
 *
 * <p>Socket options control the behavior of individual sockets.
 * This enum contains all standard ZMQ socket options.</p>
 *
 * <p>Common options include:</p>
 * <ul>
 *   <li>SUBSCRIBE/UNSUBSCRIBE - Subscription filters for SUB sockets</li>
 *   <li>SNDHWM/RCVHWM - High water marks for send/receive queues</li>
 *   <li>SNDTIMEO/RCVTIMEO - Send/receive timeouts</li>
 *   <li>LINGER - Socket linger period</li>
 *   <li>CURVE_* - CURVE encryption options</li>
 *   <li>PLAIN_* - PLAIN authentication options</li>
 * </ul>
 */
public enum SocketOption {
    /** Socket affinity (ZMQ_AFFINITY) */
    AFFINITY(4),
    /** Socket routing ID (ZMQ_ROUTING_ID, formerly ZMQ_IDENTITY) */
    ROUTING_ID(5),
    /** Subscribe to topic (ZMQ_SUBSCRIBE) */
    SUBSCRIBE(6),
    /** Unsubscribe from topic (ZMQ_UNSUBSCRIBE) */
    UNSUBSCRIBE(7),
    /** Multicast data rate (ZMQ_RATE) */
    RATE(8),
    /** Multicast recovery interval (ZMQ_RECOVERY_IVL) */
    RECOVERY_IVL(9),
    /** Kernel send buffer size (ZMQ_SNDBUF) */
    SNDBUF(11),
    /** Kernel receive buffer size (ZMQ_RCVBUF) */
    RCVBUF(12),
    /** More message parts follow (ZMQ_RCVMORE) */
    RCVMORE(13),
    /** Socket file descriptor (ZMQ_FD) */
    FD(14),
    /** Socket events (ZMQ_EVENTS) */
    EVENTS(15),
    /** Socket type (ZMQ_TYPE) */
    TYPE(16),
    /** Linger period for socket shutdown (ZMQ_LINGER) */
    LINGER(17),
    /** Reconnection interval (ZMQ_RECONNECT_IVL) */
    RECONNECT_IVL(18),
    /** Connection backlog (ZMQ_BACKLOG) */
    BACKLOG(19),
    /** Maximum reconnection interval (ZMQ_RECONNECT_IVL_MAX) */
    RECONNECT_IVL_MAX(21),
    /** Maximum message size (ZMQ_MAXMSGSIZE) */
    MAXMSGSIZE(22),
    /** Send high water mark (ZMQ_SNDHWM) */
    SNDHWM(23),
    /** Receive high water mark (ZMQ_RCVHWM) */
    RCVHWM(24),
    /** Multicast hops (ZMQ_MULTICAST_HOPS) */
    MULTICAST_HOPS(25),
    /** Receive timeout (ZMQ_RCVTIMEO) */
    RCVTIMEO(27),
    /** Send timeout (ZMQ_SNDTIMEO) */
    SNDTIMEO(28),
    /** Last endpoint bound to (ZMQ_LAST_ENDPOINT) */
    LAST_ENDPOINT(32),
    /** Router mandatory routing (ZMQ_ROUTER_MANDATORY) */
    ROUTER_MANDATORY(33),
    /** TCP keepalive (ZMQ_TCP_KEEPALIVE) */
    TCP_KEEPALIVE(34),
    /** TCP keepalive count (ZMQ_TCP_KEEPALIVE_CNT) */
    TCP_KEEPALIVE_CNT(35),
    /** TCP keepalive idle (ZMQ_TCP_KEEPALIVE_IDLE) */
    TCP_KEEPALIVE_IDLE(36),
    /** TCP keepalive interval (ZMQ_TCP_KEEPALIVE_INTVL) */
    TCP_KEEPALIVE_INTVL(37),
    /** Immediate connection (ZMQ_IMMEDIATE) */
    IMMEDIATE(39),
    /** XPUB verbose mode (ZMQ_XPUB_VERBOSE) */
    XPUB_VERBOSE(40),
    /** Router raw mode (ZMQ_ROUTER_RAW) */
    ROUTER_RAW(41),
    /** IPv6 support (ZMQ_IPV6) */
    IPV6(42),
    /** Security mechanism (ZMQ_MECHANISM) */
    MECHANISM(43),
    /** PLAIN server mode (ZMQ_PLAIN_SERVER) */
    PLAIN_SERVER(44),
    /** PLAIN username (ZMQ_PLAIN_USERNAME) */
    PLAIN_USERNAME(45),
    /** PLAIN password (ZMQ_PLAIN_PASSWORD) */
    PLAIN_PASSWORD(46),
    /** CURVE server mode (ZMQ_CURVE_SERVER) */
    CURVE_SERVER(47),
    /** CURVE public key (ZMQ_CURVE_PUBLICKEY) */
    CURVE_PUBLICKEY(48),
    /** CURVE secret key (ZMQ_CURVE_SECRETKEY) */
    CURVE_SECRETKEY(49),
    /** CURVE server key (ZMQ_CURVE_SERVERKEY) */
    CURVE_SERVERKEY(50),
    /** Probe router (ZMQ_PROBE_ROUTER) */
    PROBE_ROUTER(51),
    /** REQ correlate (ZMQ_REQ_CORRELATE) */
    REQ_CORRELATE(52),
    /** REQ relaxed (ZMQ_REQ_RELAXED) */
    REQ_RELAXED(53),
    /** Conflate messages (ZMQ_CONFLATE) */
    CONFLATE(54),
    /** ZAP domain (ZMQ_ZAP_DOMAIN) */
    ZAP_DOMAIN(55),
    /** Router handover (ZMQ_ROUTER_HANDOVER) */
    ROUTER_HANDOVER(56),
    /** Type of service (ZMQ_TOS) */
    TOS(57),
    /** Connect routing ID (ZMQ_CONNECT_ROUTING_ID) */
    CONNECT_ROUTING_ID(61),
    /** Handshake interval (ZMQ_HANDSHAKE_IVL) */
    HANDSHAKE_IVL(66),
    /** SOCKS proxy (ZMQ_SOCKS_PROXY) */
    SOCKS_PROXY(68),
    /** XPUB no drop (ZMQ_XPUB_NODROP) */
    XPUB_NODROP(69),
    /** Blocky mode (ZMQ_BLOCKY) */
    BLOCKY(70),
    /** XPUB manual subscriptions (ZMQ_XPUB_MANUAL) */
    XPUB_MANUAL(71),
    /** XPUB welcome message (ZMQ_XPUB_WELCOME_MSG) */
    XPUB_WELCOME_MSG(72),
    /** Stream notifications (ZMQ_STREAM_NOTIFY) */
    STREAM_NOTIFY(73),
    /** Invert matching (ZMQ_INVERT_MATCHING) */
    INVERT_MATCHING(74),
    /** Heartbeat interval (ZMQ_HEARTBEAT_IVL) */
    HEARTBEAT_IVL(75),
    /** Heartbeat TTL (ZMQ_HEARTBEAT_TTL) */
    HEARTBEAT_TTL(76),
    /** Heartbeat timeout (ZMQ_HEARTBEAT_TIMEOUT) */
    HEARTBEAT_TIMEOUT(77),
    /** XPUB verboser mode (ZMQ_XPUB_VERBOSER) */
    XPUB_VERBOSER(78),
    /** Connect timeout (ZMQ_CONNECT_TIMEOUT) */
    CONNECT_TIMEOUT(79),
    /** TCP maximum retransmit timeout (ZMQ_TCP_MAXRT) */
    TCP_MAXRT(80),
    /** Thread safe socket (ZMQ_THREAD_SAFE) */
    THREAD_SAFE(81),
    /** Multicast max TPDU (ZMQ_MULTICAST_MAXTPDU) */
    MULTICAST_MAXTPDU(84),
    /** Bind to device (ZMQ_BINDTODEVICE) */
    BINDTODEVICE(92);

    private final int value;

    SocketOption(int value) {
        this.value = value;
    }

    /**
     * Gets the native option value.
     * @return Native value
     */
    public int getValue() {
        return value;
    }
}
