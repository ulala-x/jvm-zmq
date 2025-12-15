package io.github.ulalax.zmq;

/**
 * Events for polling operations.
 *
 * <p>Poll events indicate what type of events to wait for:</p>
 * <ul>
 *   <li>NONE - No events</li>
 *   <li>IN - Socket has messages ready to receive</li>
 *   <li>OUT - Socket can send messages without blocking</li>
 *   <li>ERR - Socket has an error condition</li>
 *   <li>PRI - Socket has priority data (rarely used)</li>
 * </ul>
 */
public enum PollEvents {
    /** No events */
    NONE(0),
    /** Socket is readable (ZMQ_POLLIN) */
    IN(1),
    /** Socket is writable (ZMQ_POLLOUT) */
    OUT(2),
    /** Socket has error (ZMQ_POLLERR) */
    ERR(4),
    /** Socket has priority data (ZMQ_POLLPRI) */
    PRI(8);

    private final short value;

    PollEvents(int value) {
        this.value = (short) value;
    }

    /**
     * Gets the native event value.
     * @return Native value
     */
    public short getValue() {
        return value;
    }

    /**
     * Combines this event with another event.
     * @param other The other event to combine
     * @return Combined event
     */
    public PollEvents combine(PollEvents other) {
        return fromValue((short) (this.value | other.value));
    }

    /**
     * Creates a PollEvents from a native value.
     * @param value The native value
     * @return PollEvents instance
     */
    public static PollEvents fromValue(short value) {
        for (PollEvents event : values()) {
            if (event.value == value) {
                return event;
            }
        }
        // For combined events, return NONE as placeholder
        return NONE;
    }
}
