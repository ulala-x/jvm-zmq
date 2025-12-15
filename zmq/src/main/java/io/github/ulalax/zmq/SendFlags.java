package io.github.ulalax.zmq;

/**
 * Flags for send operations.
 *
 * <p>Send flags modify the behavior of send operations:</p>
 * <ul>
 *   <li>NONE - Blocking send (default)</li>
 *   <li>DONT_WAIT - Non-blocking send (returns immediately if would block)</li>
 *   <li>SEND_MORE - Indicates more message parts follow</li>
 * </ul>
 */
public enum SendFlags {
    /** No flags (blocking send) */
    NONE(0),
    /** Non-blocking send (ZMQ_DONTWAIT) */
    DONT_WAIT(1),
    /** More message parts follow (ZMQ_SNDMORE) */
    SEND_MORE(2);

    private final int value;

    SendFlags(int value) {
        this.value = value;
    }

    /**
     * Gets the native flag value.
     * @return Native value
     */
    public int getValue() {
        return value;
    }

    /**
     * Combines this flag with another flag.
     * @param other The other flag to combine
     * @return Combined flag value
     */
    public SendFlags combine(SendFlags other) {
        return fromValue(this.value | other.value);
    }

    /**
     * Creates a SendFlags from a native value.
     * @param value The native value
     * @return SendFlags instance
     */
    public static SendFlags fromValue(int value) {
        for (SendFlags flag : values()) {
            if (flag.value == value) {
                return flag;
            }
        }
        // For combined flags, return NONE as placeholder
        return NONE;
    }
}
