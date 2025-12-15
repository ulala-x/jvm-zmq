package io.github.ulalax.zmq;

/**
 * ZeroMQ socket monitor events.
 *
 * <p>Monitor events are triggered when socket state changes occur.
 * These events can be monitored using {@link Socket#monitor(String, SocketMonitorEvent)}.</p>
 *
 * <p>Events include:</p>
 * <ul>
 *   <li>CONNECTED - Socket connected to peer</li>
 *   <li>CONNECT_DELAYED - Synchronous connect failed, retrying</li>
 *   <li>CONNECT_RETRIED - Asynchronous connect attempt</li>
 *   <li>LISTENING - Socket bound to address</li>
 *   <li>BIND_FAILED - Bind attempt failed</li>
 *   <li>ACCEPTED - Connection accepted</li>
 *   <li>ACCEPT_FAILED - Accept attempt failed</li>
 *   <li>CLOSED - Socket closed</li>
 *   <li>CLOSE_FAILED - Close attempt failed</li>
 *   <li>DISCONNECTED - Socket disconnected from peer</li>
 *   <li>MONITOR_STOPPED - Monitoring stopped</li>
 *   <li>ALL - All events</li>
 * </ul>
 */
public enum SocketMonitorEvent {
    /** Socket connected (ZMQ_EVENT_CONNECTED) */
    CONNECTED(1),
    /** Connect delayed (ZMQ_EVENT_CONNECT_DELAYED) */
    CONNECT_DELAYED(2),
    /** Connect retried (ZMQ_EVENT_CONNECT_RETRIED) */
    CONNECT_RETRIED(4),
    /** Socket listening (ZMQ_EVENT_LISTENING) */
    LISTENING(8),
    /** Bind failed (ZMQ_EVENT_BIND_FAILED) */
    BIND_FAILED(16),
    /** Connection accepted (ZMQ_EVENT_ACCEPTED) */
    ACCEPTED(32),
    /** Accept failed (ZMQ_EVENT_ACCEPT_FAILED) */
    ACCEPT_FAILED(64),
    /** Socket closed (ZMQ_EVENT_CLOSED) */
    CLOSED(128),
    /** Close failed (ZMQ_EVENT_CLOSE_FAILED) */
    CLOSE_FAILED(256),
    /** Socket disconnected (ZMQ_EVENT_DISCONNECTED) */
    DISCONNECTED(512),
    /** Monitor stopped (ZMQ_EVENT_MONITOR_STOPPED) */
    MONITOR_STOPPED(1024),
    /** All events (ZMQ_EVENT_ALL) */
    ALL(0xFFFF);

    private final int value;

    SocketMonitorEvent(int value) {
        this.value = value;
    }

    /**
     * Gets the native event value.
     * @return Native value
     */
    public int getValue() {
        return value;
    }

    /**
     * Combines this event with another event.
     * @param other The other event to combine
     * @return Combined event
     */
    public SocketMonitorEvent combine(SocketMonitorEvent other) {
        return fromValue(this.value | other.value);
    }

    /**
     * Creates a SocketMonitorEvent from a native value.
     * @param value The native value
     * @return SocketMonitorEvent instance
     */
    public static SocketMonitorEvent fromValue(int value) {
        for (SocketMonitorEvent event : values()) {
            if (event.value == value) {
                return event;
            }
        }
        return ALL; // Default for unknown combinations
    }
}
