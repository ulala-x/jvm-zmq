package io.github.ulalax.zmq;

/**
 * ZeroMQ socket types.
 *
 * <p>Socket types define the messaging pattern and behavior:</p>
 * <ul>
 *   <li>PAIR - Exclusive pair, connects two sockets</li>
 *   <li>PUB - Publisher, one-to-many distribution</li>
 *   <li>SUB - Subscriber, receives from publishers</li>
 *   <li>REQ - Request, synchronous request-reply client</li>
 *   <li>REP - Reply, synchronous request-reply server</li>
 *   <li>DEALER - Asynchronous request-reply client</li>
 *   <li>ROUTER - Asynchronous request-reply server</li>
 *   <li>PULL - Pipeline receiver</li>
 *   <li>PUSH - Pipeline sender</li>
 *   <li>XPUB - Extended publisher for manual subscriptions</li>
 *   <li>XSUB - Extended subscriber for manual subscriptions</li>
 *   <li>STREAM - Raw connection to a single peer</li>
 * </ul>
 */
public enum SocketType {
    /** Exclusive pair socket */
    PAIR(0),
    /** Publisher socket */
    PUB(1),
    /** Subscriber socket */
    SUB(2),
    /** Request socket */
    REQ(3),
    /** Reply socket */
    REP(4),
    /** Dealer socket (asynchronous REQ) */
    DEALER(5),
    /** Router socket (asynchronous REP) */
    ROUTER(6),
    /** Pull socket (pipeline receiver) */
    PULL(7),
    /** Push socket (pipeline sender) */
    PUSH(8),
    /** Extended publisher socket */
    XPUB(9),
    /** Extended subscriber socket */
    XSUB(10),
    /** Stream socket */
    STREAM(11);

    private final int value;

    SocketType(int value) {
        this.value = value;
    }

    /**
     * Gets the native ZeroMQ socket type value.
     * @return Native value
     */
    public int getValue() {
        return value;
    }
}
