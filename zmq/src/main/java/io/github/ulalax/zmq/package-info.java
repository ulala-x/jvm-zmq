/**
 * High-level ZeroMQ API for Java.
 *
 * <p>This package provides a modern, idiomatic Java API for ZeroMQ messaging using
 * Java 22+ Foreign Function &amp; Memory (FFM) API. It offers a clean, type-safe interface
 * that follows Java conventions while maintaining ZeroMQ's performance characteristics.</p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Zero-copy message handling with {@link io.github.ulalax.zmq.Message}</li>
 *   <li>Automatic resource management with try-with-resources</li>
 *   <li>Type-safe socket options and message flags</li>
 *   <li>Support for all ZeroMQ socket types and patterns</li>
 *   <li>Built-in security with Curve encryption support</li>
 *   <li>Asynchronous I/O with poller support</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <p>Basic request-reply pattern:</p>
 * <pre>{@code
 * // Server
 * try (Context ctx = new Context();
 *      Socket socket = new Socket(ctx, SocketType.REP)) {
 *     socket.bind("tcp://*:5555");
 *     while (true) {
 *         String request = socket.recvString();
 *         System.out.println("Received: " + request);
 *         socket.send("World");
 *     }
 * }
 *
 * // Client
 * try (Context ctx = new Context();
 *      Socket socket = new Socket(ctx, SocketType.REQ)) {
 *     socket.connect("tcp://localhost:5555");
 *     socket.send("Hello");
 *     String reply = socket.recvString();
 *     System.out.println("Received: " + reply);
 * }
 * }</pre>
 *
 * <h2>Core Components</h2>
 * <dl>
 *   <dt>{@link io.github.ulalax.zmq.Context}</dt>
 *   <dd>The container for all ZMQ sockets. Manages I/O threads and socket lifecycle.
 *       Thread-safe and must be closed to release resources.</dd>
 *
 *   <dt>{@link io.github.ulalax.zmq.Socket}</dt>
 *   <dd>The fundamental messaging primitive. NOT thread-safe (except for special socket types).
 *       Provides methods for sending/receiving messages in various formats.</dd>
 *
 *   <dt>{@link io.github.ulalax.zmq.Message}</dt>
 *   <dd>Represents a ZeroMQ message with zero-copy semantics. Manages native memory
 *       for optimal performance.</dd>
 *
 *   <dt>{@link io.github.ulalax.zmq.Poller}</dt>
 *   <dd>Allows monitoring multiple sockets for events (readable/writable) in a single thread.
 *       Essential for building scalable multi-socket applications.</dd>
 * </dl>
 *
 * <h2>Socket Types</h2>
 * <p>ZeroMQ supports several messaging patterns through different socket types:</p>
 * <ul>
 *   <li><b>REQ/REP</b> - Synchronous request-reply (client-server)</li>
 *   <li><b>DEALER/ROUTER</b> - Asynchronous request-reply with routing</li>
 *   <li><b>PUB/SUB</b> - Publish-subscribe (one-to-many)</li>
 *   <li><b>PUSH/PULL</b> - Pipeline pattern (load distribution)</li>
 *   <li><b>PAIR</b> - Exclusive pair (bidirectional)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p><b>Important:</b> ZeroMQ sockets are NOT thread-safe. Each socket should be used
 * by only one thread at a time. The {@link io.github.ulalax.zmq.Context} is thread-safe
 * and can be shared across threads.</p>
 *
 * <h2>Resource Management</h2>
 * <p>All resources implement {@link java.lang.AutoCloseable} and should be used with
 * try-with-resources statements to ensure proper cleanup:</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket socket = new Socket(ctx, SocketType.REP);
 *      Message msg = Message.create()) {
 *     // Use resources
 * } // Automatic cleanup
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>Use {@link io.github.ulalax.zmq.Message} for zero-copy message handling</li>
 *   <li>Reuse {@link io.github.ulalax.zmq.Message} objects when possible</li>
 *   <li>Use byte arrays for small messages (&lt; 1KB)</li>
 *   <li>Configure appropriate high water marks (HWM) to prevent memory exhaustion</li>
 *   <li>Use DONTWAIT flag for non-blocking operations</li>
 * </ul>
 *
 * @see <a href="https://zeromq.org/documentation/">ZeroMQ Documentation</a>
 * @see <a href="https://github.com/ulala-x/jvm-zmq">jvm-zmq GitHub Repository</a>
 */
package io.github.ulalax.zmq;
