package io.github.ulalax.zmq;

/**
 * ZeroMQ message properties.
 *
 * <p>Message properties provide metadata about messages:</p>
 * <ul>
 *   <li>MORE - Indicates more message parts follow</li>
 *   <li>SHARED - Indicates message data is shared</li>
 * </ul>
 */
public enum MessageProperty {
    /** More message parts follow (ZMQ_MORE) */
    MORE(1),
    /** Message is shared (ZMQ_SHARED) */
    SHARED(3);

    private final int value;

    MessageProperty(int value) {
        this.value = value;
    }

    /**
     * Gets the native property value.
     * @return Native value
     */
    public int getValue() {
        return value;
    }
}
