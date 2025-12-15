package io.github.ulalax.zmq;

/**
 * ZeroMQ context options.
 *
 * <p>Context options control the behavior of ZMQ contexts:</p>
 * <ul>
 *   <li>IO_THREADS - Number of I/O threads (default: 1)</li>
 *   <li>MAX_SOCKETS - Maximum number of sockets (default: 1024)</li>
 *   <li>SOCKET_LIMIT - Hard limit on sockets</li>
 *   <li>THREAD_PRIORITY - I/O thread priority</li>
 *   <li>THREAD_SCHED_POLICY - I/O thread scheduling policy</li>
 *   <li>MAX_MSG_SIZE - Maximum message size</li>
 *   <li>MSG_T_SIZE - Size of zmq_msg_t structure</li>
 *   <li>THREAD_AFFINITY_CPU_ADD - Add CPU to thread affinity</li>
 *   <li>THREAD_AFFINITY_CPU_REMOVE - Remove CPU from thread affinity</li>
 *   <li>THREAD_NAME_PREFIX - Prefix for I/O thread names</li>
 * </ul>
 */
public enum ContextOption {
    /** Number of I/O threads in the context (ZMQ_IO_THREADS) */
    IO_THREADS(1),
    /** Maximum number of sockets (ZMQ_MAX_SOCKETS) */
    MAX_SOCKETS(2),
    /** Socket limit (ZMQ_SOCKET_LIMIT) */
    SOCKET_LIMIT(3),
    /** I/O thread priority (ZMQ_THREAD_PRIORITY) */
    THREAD_PRIORITY(3),
    /** I/O thread scheduling policy (ZMQ_THREAD_SCHED_POLICY) */
    THREAD_SCHED_POLICY(4),
    /** Maximum message size (ZMQ_MAX_MSGSZ) */
    MAX_MSG_SIZE(5),
    /** Size of zmq_msg_t (ZMQ_MSG_T_SIZE) */
    MSG_T_SIZE(6),
    /** Add CPU to thread affinity (ZMQ_THREAD_AFFINITY_CPU_ADD) */
    THREAD_AFFINITY_CPU_ADD(7),
    /** Remove CPU from thread affinity (ZMQ_THREAD_AFFINITY_CPU_REMOVE) */
    THREAD_AFFINITY_CPU_REMOVE(8),
    /** I/O thread name prefix (ZMQ_THREAD_NAME_PREFIX) */
    THREAD_NAME_PREFIX(9);

    private final int value;

    ContextOption(int value) {
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
