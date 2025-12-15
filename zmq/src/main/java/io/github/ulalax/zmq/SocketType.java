package io.github.ulalax.zmq;

/**
 * ZeroMQ socket types.
 *
 * <p>Socket types define the messaging pattern and behavior. Each socket type
 * implements a specific messaging pattern and can only connect to compatible socket types.</p>
 *
 * <p><strong>Basic Request-Reply Pattern:</strong></p>
 * <ul>
 *   <li><strong>REQ</strong> - Request socket (synchronous client): Sends requests and waits for replies.
 *       Must alternate between send and receive. Compatible with REP or ROUTER.</li>
 *   <li><strong>REP</strong> - Reply socket (synchronous server): Receives requests and sends replies.
 *       Must alternate between receive and send. Compatible with REQ or DEALER.</li>
 *   <li><strong>DEALER</strong> - Dealer socket (asynchronous client): Like REQ but asynchronous.
 *       Can send multiple requests without waiting for replies. Compatible with REP, DEALER, or ROUTER.</li>
 *   <li><strong>ROUTER</strong> - Router socket (asynchronous server): Like REP but asynchronous.
 *       Routes messages to specific clients based on identity. Compatible with REQ, DEALER, or ROUTER.</li>
 * </ul>
 *
 * <p><strong>Publish-Subscribe Pattern:</strong></p>
 * <ul>
 *   <li><strong>PUB</strong> - Publisher socket: Distributes messages to all subscribers.
 *       Messages are sent to all connected SUB sockets. Compatible with SUB or XSUB.</li>
 *   <li><strong>SUB</strong> - Subscriber socket: Receives published messages.
 *       Must set subscription filters to receive messages. Compatible with PUB or XPUB.</li>
 *   <li><strong>XPUB</strong> - Extended publisher: Like PUB but receives subscription messages.
 *       Useful for building proxies. Compatible with SUB or XSUB.</li>
 *   <li><strong>XSUB</strong> - Extended subscriber: Like SUB but sends subscription messages.
 *       Useful for building proxies. Compatible with PUB or XPUB.</li>
 * </ul>
 *
 * <p><strong>Pipeline Pattern:</strong></p>
 * <ul>
 *   <li><strong>PUSH</strong> - Push socket: Distributes messages to workers in round-robin.
 *       Load balances messages across connected PULL sockets. Compatible with PULL.</li>
 *   <li><strong>PULL</strong> - Pull socket: Receives messages from workers.
 *       Fair-queues messages from connected PUSH sockets. Compatible with PUSH.</li>
 * </ul>
 *
 * <p><strong>Exclusive Pair Pattern:</strong></p>
 * <ul>
 *   <li><strong>PAIR</strong> - Pair socket: Exclusive one-to-one connection.
 *       Not for general use, mainly for inproc connections. Compatible only with another PAIR.</li>
 * </ul>
 *
 * <p><strong>Raw TCP Pattern:</strong></p>
 * <ul>
 *   <li><strong>STREAM</strong> - Stream socket: Raw TCP connection.
 *       Sends and receives raw TCP data with routing ID frames. Use with caution.</li>
 * </ul>
 *
 * @see Socket
 */
public enum SocketType {
    /**
     * Exclusive pair socket. Connects two sockets exclusively.
     * <p>Compatible with: PAIR</p>
     * <p>Pattern: One-to-one exclusive connection (mainly for inproc)</p>
     */
    PAIR(0),

    /**
     * Publisher socket. Distributes messages to all subscribers.
     * <p>Compatible with: SUB, XSUB</p>
     * <p>Pattern: Publish-Subscribe (publisher side)</p>
     */
    PUB(1),

    /**
     * Subscriber socket. Receives published messages.
     * <p>Compatible with: PUB, XPUB</p>
     * <p>Pattern: Publish-Subscribe (subscriber side)</p>
     * <p>Note: Must set subscription filters to receive messages</p>
     */
    SUB(2),

    /**
     * Request socket. Sends requests and waits for replies (synchronous).
     * <p>Compatible with: REP, ROUTER</p>
     * <p>Pattern: Request-Reply (client side, synchronous)</p>
     * <p>Note: Must alternate between send and receive</p>
     */
    REQ(3),

    /**
     * Reply socket. Receives requests and sends replies (synchronous).
     * <p>Compatible with: REQ, DEALER</p>
     * <p>Pattern: Request-Reply (server side, synchronous)</p>
     * <p>Note: Must alternate between receive and send</p>
     */
    REP(4),

    /**
     * Dealer socket. Asynchronous request-reply client.
     * <p>Compatible with: REP, DEALER, ROUTER</p>
     * <p>Pattern: Request-Reply (client side, asynchronous)</p>
     * <p>Note: Like REQ but can send multiple requests without waiting</p>
     */
    DEALER(5),

    /**
     * Router socket. Asynchronous request-reply server with routing.
     * <p>Compatible with: REQ, DEALER, ROUTER</p>
     * <p>Pattern: Request-Reply (server side, asynchronous)</p>
     * <p>Note: Routes messages to specific clients based on identity</p>
     */
    ROUTER(6),

    /**
     * Pull socket. Receives messages from workers (pipeline pattern).
     * <p>Compatible with: PUSH</p>
     * <p>Pattern: Pipeline (receiver side)</p>
     * <p>Note: Fair-queues messages from all connected PUSH sockets</p>
     */
    PULL(7),

    /**
     * Push socket. Distributes messages to workers (pipeline pattern).
     * <p>Compatible with: PULL</p>
     * <p>Pattern: Pipeline (sender side)</p>
     * <p>Note: Load balances messages across connected PULL sockets</p>
     */
    PUSH(8),

    /**
     * Extended publisher socket. Like PUB but receives subscription messages.
     * <p>Compatible with: SUB, XSUB</p>
     * <p>Pattern: Publish-Subscribe (publisher side with subscription awareness)</p>
     * <p>Note: Useful for building proxies and monitoring subscriptions</p>
     */
    XPUB(9),

    /**
     * Extended subscriber socket. Like SUB but sends subscription messages.
     * <p>Compatible with: PUB, XPUB</p>
     * <p>Pattern: Publish-Subscribe (subscriber side with manual control)</p>
     * <p>Note: Useful for building proxies and forwarding subscriptions</p>
     */
    XSUB(10),

    /**
     * Stream socket. Raw TCP connection to a single peer.
     * <p>Compatible with: Any TCP socket</p>
     * <p>Pattern: Raw TCP (not a messaging pattern)</p>
     * <p>Note: Sends/receives raw TCP data with routing ID frames. Use with caution.</p>
     */
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
