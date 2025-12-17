/**
 * Low-level ZeroMQ bindings using Java Foreign Function &amp; Memory (FFM) API.
 *
 * <p>This package provides direct access to the native libzmq library through Java 22+
 * FFM API. It offers zero-overhead interoperability with native ZeroMQ functions while
 * maintaining type safety through Java's foreign function interface.</p>
 *
 * <h2>Overview</h2>
 * <p>The core package contains the foundational layer for jvm-zmq, exposing the raw
 * libzmq C API to Java code. Most users should use the high-level API in
 * {@link io.github.ulalax.zmq} package instead, but this package is available for
 * advanced use cases requiring direct native access.</p>
 *
 * <h2>Key Components</h2>
 * <dl>
 *   <dt>{@link io.github.ulalax.zmq.core.LibZmq}</dt>
 *   <dd>Direct bindings to libzmq C functions. Provides one-to-one mapping of ZeroMQ
 *       API functions using MethodHandles and native linking.</dd>
 *
 *   <dt>{@link io.github.ulalax.zmq.core.ZmqConstants}</dt>
 *   <dd>Constant definitions from zmq.h header file. Includes socket types, options,
 *       flags, and event codes.</dd>
 *
 *   <dt>{@link io.github.ulalax.zmq.core.ZmqException}</dt>
 *   <dd>Exception type for ZeroMQ errors. Maps native error codes to Java exceptions
 *       with descriptive messages.</dd>
 *
 *   <dt>{@link io.github.ulalax.zmq.core.ZmqStructs}</dt>
 *   <dd>Memory layout definitions for ZeroMQ C structures (zmq_msg_t, zmq_pollitem_t).
 *       Uses Java FFM's MemoryLayout for safe structure access.</dd>
 *
 *   <dt>{@link io.github.ulalax.zmq.core.NativeLoader}</dt>
 *   <dd>Handles loading of native libzmq library across different platforms
 *       (Linux, macOS, Windows).</dd>
 * </dl>
 *
 * <h2>FFM API Usage</h2>
 * <p>This package leverages Java 22's Foreign Function &amp; Memory API for native
 * interoperability:</p>
 * <ul>
 *   <li>{@link java.lang.foreign.MemorySegment} - Represents native memory regions</li>
 *   <li>{@link java.lang.foreign.Arena} - Manages memory lifecycle</li>
 *   <li>{@link java.lang.foreign.SymbolLookup} - Resolves native function symbols</li>
 *   <li>{@link java.lang.foreign.FunctionDescriptor} - Describes native function signatures</li>
 * </ul>
 *
 * <h2>Native Library Loading</h2>
 * <p>The native libzmq library is loaded automatically on first use. The loader searches
 * for the library in the following order:</p>
 * <ol>
 *   <li>Bundled native libraries in JAR resources (platform-specific)</li>
 *   <li>System library paths (LD_LIBRARY_PATH, DYLD_LIBRARY_PATH, PATH)</li>
 * </ol>
 *
 * <h2>Supported Platforms</h2>
 * <ul>
 *   <li>Linux (x86_64, aarch64) - libzmq.so</li>
 *   <li>macOS (x86_64, aarch64) - libzmq.dylib</li>
 *   <li>Windows (x86_64, aarch64) - libzmq.dll</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>Native ZeroMQ errors are converted to {@link io.github.ulalax.zmq.core.ZmqException}
 * with errno codes and descriptive messages. Common error codes include:</p>
 * <ul>
 *   <li><b>EINVAL</b> - Invalid argument</li>
 *   <li><b>EAGAIN</b> - Operation would block (non-blocking mode)</li>
 *   <li><b>ETERM</b> - Context was terminated</li>
 *   <li><b>ENOTSOCK</b> - Invalid socket</li>
 *   <li><b>EINTR</b> - Operation interrupted by signal</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This package directly exposes the native libzmq API, which has the following
 * thread safety characteristics:</p>
 * <ul>
 *   <li>Context operations are thread-safe</li>
 *   <li>Socket operations are NOT thread-safe (except for specific socket types)</li>
 *   <li>Message operations are NOT thread-safe</li>
 * </ul>
 *
 * <h2>Memory Management</h2>
 * <p>Native memory is managed through Java FFM's Arena mechanism. Memory is automatically
 * freed when the Arena scope ends. Users must ensure proper cleanup to prevent memory leaks:</p>
 * <pre>{@code
 * try (Arena arena = Arena.ofConfined()) {
 *     MemorySegment msg = arena.allocate(ZmqStructs.ZMQ_MSG_T_LAYOUT);
 *     LibZmq.msgInit(msg);
 *     // Use message
 *     LibZmq.msgClose(msg);
 * } // Memory automatically freed
 * }</pre>
 *
 * <h2>Performance Notes</h2>
 * <ul>
 *   <li>FFM API provides near-native performance with zero-copy semantics</li>
 *   <li>Method handles are pre-resolved and cached for optimal performance</li>
 *   <li>Memory segments avoid heap allocations for large messages</li>
 *   <li>Native access must be enabled with --enable-native-access JVM flag</li>
 * </ul>
 *
 * <h2>Advanced Usage Example</h2>
 * <pre>{@code
 * // Direct low-level API usage
 * try (Arena arena = Arena.ofConfined()) {
 *     MemorySegment ctx = LibZmq.ctxNew();
 *     MemorySegment socket = LibZmq.socket(ctx, ZmqConstants.ZMQ_REP);
 *
 *     // Bind socket
 *     MemorySegment endpoint = arena.allocateUtf8String("tcp://*:5555");
 *     LibZmq.bind(socket, endpoint);
 *
 *     // Receive message
 *     MemorySegment msg = arena.allocate(ZmqStructs.ZMQ_MSG_T_LAYOUT);
 *     LibZmq.msgInit(msg);
 *     LibZmq.msgRecv(msg, socket, 0);
 *
 *     // Clean up
 *     LibZmq.msgClose(msg);
 *     LibZmq.close(socket);
 *     LibZmq.ctxTerm(ctx);
 * }
 * }</pre>
 *
 * @see io.github.ulalax.zmq
 * @see <a href="https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html">Java FFM API</a>
 * @see <a href="https://zeromq.org/documentation/">ZeroMQ C API Documentation</a>
 */
package io.github.ulalax.zmq.core;
