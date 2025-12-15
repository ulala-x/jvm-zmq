package io.github.ulalax.zmq;

/**
 * Flags for receive operations.
 *
 * <p>Receive flags modify the behavior of receive operations:</p>
 * <ul>
 *   <li>NONE - Blocking receive (default)</li>
 *   <li>DONT_WAIT - Non-blocking receive (returns immediately if no message)</li>
 * </ul>
 */
public enum RecvFlags {
    /** No flags (blocking receive) */
    NONE(0),
    /** Non-blocking receive (ZMQ_DONTWAIT) */
    DONT_WAIT(1);

    private final int value;

    RecvFlags(int value) {
        this.value = value;
    }

    /**
     * Gets the native flag value.
     * @return Native value
     */
    public int getValue() {
        return value;
    }
}
