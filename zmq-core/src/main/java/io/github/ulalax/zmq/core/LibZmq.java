package io.github.ulalax.zmq.core;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.*;

/**
 * Low-level bindings to libzmq using Java Foreign Function &amp; Memory (FFM) API.
 * <p>
 * This class provides direct access to all native ZeroMQ (libzmq) functions through
 * Java's FFM API introduced in Java 21. It serves as the foundation layer for higher-level
 * ZeroMQ abstractions.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create context
 * MemorySegment context = LibZmq.ctxNew();
 *
 * // Create socket
 * MemorySegment socket = LibZmq.socket(context, ZmqConstants.ZMQ_REP);
 * LibZmq.bind(socket, "tcp://*:5555");
 *
 * // Use socket...
 *
 * // Cleanup
 * LibZmq.close(socket);
 * LibZmq.ctxTerm(context);
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * These functions match libzmq's thread-safety guarantees:
 * </p>
 * <ul>
 *   <li>Context operations (ctxNew, ctxTerm, etc.) are thread-safe</li>
 *   <li>Socket operations are NOT thread-safe - a socket must not be used by multiple threads simultaneously</li>
 *   <li>Message operations are NOT thread-safe - messages are not thread-safe</li>
 * </ul>
 *
 * <h2>Error Handling:</h2>
 * <p>
 * Most functions return -1 on error and set errno. Use {@link #errno()} and {@link #strerror(int)}
 * to retrieve error information, or use {@link ZmqException#throwIfError(int)} for automatic
 * exception throwing.
 * </p>
 *
 * @see <a href="http://api.zeromq.org/">ZeroMQ API Reference</a>
 * @since 1.0.0
 */
public final class LibZmq {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    // Method handles for all libzmq functions
    private static final MethodHandle zmq_errno;
    private static final MethodHandle zmq_strerror;
    private static final MethodHandle zmq_version;

    private static final MethodHandle zmq_ctx_new;
    private static final MethodHandle zmq_ctx_term;
    private static final MethodHandle zmq_ctx_shutdown;
    private static final MethodHandle zmq_ctx_set;
    private static final MethodHandle zmq_ctx_get;

    private static final MethodHandle zmq_socket;
    private static final MethodHandle zmq_close;
    private static final MethodHandle zmq_bind;
    private static final MethodHandle zmq_connect;
    private static final MethodHandle zmq_unbind;
    private static final MethodHandle zmq_disconnect;
    private static final MethodHandle zmq_setsockopt;
    private static final MethodHandle zmq_getsockopt;
    private static final MethodHandle zmq_send;
    private static final MethodHandle zmq_recv;
    private static final MethodHandle zmq_socket_monitor;

    private static final MethodHandle zmq_msg_init;
    private static final MethodHandle zmq_msg_init_size;
    private static final MethodHandle zmq_msg_init_data;
    private static final MethodHandle zmq_msg_send;
    private static final MethodHandle zmq_msg_recv;
    private static final MethodHandle zmq_msg_close;
    private static final MethodHandle zmq_msg_move;
    private static final MethodHandle zmq_msg_copy;
    private static final MethodHandle zmq_msg_data;
    private static final MethodHandle zmq_msg_size;
    private static final MethodHandle zmq_msg_more;
    private static final MethodHandle zmq_msg_get;
    private static final MethodHandle zmq_msg_set;
    private static final MethodHandle zmq_msg_gets;

    private static final MethodHandle zmq_poll;

    private static final MethodHandle zmq_proxy;
    private static final MethodHandle zmq_proxy_steerable;
    private static final MethodHandle zmq_has;
    private static final MethodHandle zmq_z85_encode;
    private static final MethodHandle zmq_z85_decode;
    private static final MethodHandle zmq_curve_keypair;
    private static final MethodHandle zmq_curve_public;

    static {
        // Load native library
        NativeLoader.load();

        // Get library symbol lookup
        LOOKUP = SymbolLookup.loaderLookup();

        // Initialize all method handles
        try {
            // Error handling
            zmq_errno = downcall("zmq_errno",
                FunctionDescriptor.of(JAVA_INT));
            zmq_strerror = downcall("zmq_strerror",
                FunctionDescriptor.of(ADDRESS, JAVA_INT));
            zmq_version = downcall("zmq_version",
                FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

            // Context management
            zmq_ctx_new = downcall("zmq_ctx_new",
                FunctionDescriptor.of(ADDRESS));
            zmq_ctx_term = downcall("zmq_ctx_term",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
            zmq_ctx_shutdown = downcall("zmq_ctx_shutdown",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
            zmq_ctx_set = downcall("zmq_ctx_set",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
            zmq_ctx_get = downcall("zmq_ctx_get",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));

            // Socket management
            zmq_socket = downcall("zmq_socket",
                FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
            zmq_close = downcall("zmq_close",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
            zmq_bind = downcall("zmq_bind",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            zmq_connect = downcall("zmq_connect",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            zmq_unbind = downcall("zmq_unbind",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            zmq_disconnect = downcall("zmq_disconnect",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            zmq_setsockopt = downcall("zmq_setsockopt",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_LONG));
            zmq_getsockopt = downcall("zmq_getsockopt",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
            zmq_send = downcall("zmq_send",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT));
            zmq_recv = downcall("zmq_recv",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT));
            zmq_socket_monitor = downcall("zmq_socket_monitor",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));

            // Message management
            zmq_msg_init = downcall("zmq_msg_init",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
            zmq_msg_init_size = downcall("zmq_msg_init_size",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG));
            zmq_msg_init_data = downcall("zmq_msg_init_data",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS));
            zmq_msg_send = downcall("zmq_msg_send",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
            zmq_msg_recv = downcall("zmq_msg_recv",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
            zmq_msg_close = downcall("zmq_msg_close",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
            zmq_msg_move = downcall("zmq_msg_move",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            zmq_msg_copy = downcall("zmq_msg_copy",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            zmq_msg_data = downcall("zmq_msg_data",
                FunctionDescriptor.of(ADDRESS, ADDRESS));
            zmq_msg_size = downcall("zmq_msg_size",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS));
            zmq_msg_more = downcall("zmq_msg_more",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
            zmq_msg_get = downcall("zmq_msg_get",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
            zmq_msg_set = downcall("zmq_msg_set",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));
            zmq_msg_gets = downcall("zmq_msg_gets",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

            // Polling
            zmq_poll = downcall("zmq_poll",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG));

            // Utilities
            zmq_proxy = downcall("zmq_proxy",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
            zmq_proxy_steerable = downcall("zmq_proxy_steerable",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
            zmq_has = downcall("zmq_has",
                FunctionDescriptor.of(JAVA_INT, ADDRESS));
            zmq_z85_encode = downcall("zmq_z85_encode",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
            zmq_z85_decode = downcall("zmq_z85_decode",
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
            zmq_curve_keypair = downcall("zmq_curve_keypair",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            zmq_curve_public = downcall("zmq_curve_public",
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private LibZmq() {
        // Prevent instantiation
    }

    /**
     * Helper to create downcall handle.
     */
    private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
        return LINKER.downcallHandle(
            LOOKUP.find(name).orElseThrow(
                () -> new UnsatisfiedLinkError("Failed to find function: " + name)
            ),
            descriptor
        );
    }

    // ========== Error Handling ==========

    /**
     * Retrieves the last error number set by a ZeroMQ function.
     * <p>
     * This function corresponds to {@code zmq_errno()} in the C API.
     * The error number can be used to determine what went wrong with the last operation.
     * </p>
     *
     * @return the error number (errno) from the last ZeroMQ operation
     * @see #strerror(int)
     */
    public static int errno() {
        try {
            return (int) zmq_errno.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts an error number to a human-readable error message string.
     * <p>
     * This function corresponds to {@code zmq_strerror()} in the C API.
     * </p>
     *
     * @param errnum the error number to convert
     * @return a string describing the error
     * @see #errno()
     */
    public static String strerror(int errnum) {
        try {
            MemorySegment ptr = (MemorySegment) zmq_strerror.invokeExact(errnum);
            return ptr.reinterpret(Long.MAX_VALUE).getUtf8String(0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reports the version of the ZeroMQ library.
     * <p>
     * This function corresponds to {@code zmq_version()} in the C API.
     * The version is returned as a three-element array containing the major,
     * minor, and patch version numbers.
     * </p>
     *
     * @return an array of three integers [major, minor, patch] representing the library version
     */
    public static int[] version() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment major = arena.allocate(JAVA_INT);
            MemorySegment minor = arena.allocate(JAVA_INT);
            MemorySegment patch = arena.allocate(JAVA_INT);

            zmq_version.invokeExact(major, minor, patch);

            return new int[] {
                major.get(JAVA_INT, 0),
                minor.get(JAVA_INT, 0),
                patch.get(JAVA_INT, 0)
            };
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Context Management ==========

    /**
     * Creates a new ZeroMQ context.
     * <p>
     * A context is the container for all sockets in a single process. It manages I/O threads
     * and other resources. You must create exactly one context per process.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_ctx_new()} in the C API.
     * </p>
     *
     * @return a pointer to the newly created context, or NULL on error
     * @see #ctxTerm(MemorySegment)
     * @see #ctxShutdown(MemorySegment)
     */
    public static MemorySegment ctxNew() {
        try {
            return (MemorySegment) zmq_ctx_new.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Terminates a ZeroMQ context.
     * <p>
     * This function destroys the context and closes all sockets that might still be
     * associated with it. It will block until all pending operations complete and all
     * sockets are closed.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_ctx_term()} in the C API.
     * </p>
     *
     * @param context the context to terminate
     * @return 0 on success, -1 on error
     * @see #ctxNew()
     * @see #ctxShutdown(MemorySegment)
     */
    public static int ctxTerm(MemorySegment context) {
        try {
            return (int) zmq_ctx_term.invokeExact(context);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Shuts down a ZeroMQ context without destroying it.
     * <p>
     * This function causes all blocking operations currently in progress on sockets
     * associated with the context to return immediately with an error code of ETERM.
     * Unlike {@link #ctxTerm(MemorySegment)}, this does not destroy the context.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_ctx_shutdown()} in the C API.
     * </p>
     *
     * @param context the context to shutdown
     * @return 0 on success, -1 on error
     * @see #ctxTerm(MemorySegment)
     */
    public static int ctxShutdown(MemorySegment context) {
        try {
            return (int) zmq_ctx_shutdown.invokeExact(context);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a context option.
     * <p>
     * This function sets the specified option for the given context.
     * Available options include ZMQ_IO_THREADS, ZMQ_MAX_SOCKETS, etc.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_ctx_set()} in the C API.
     * </p>
     *
     * @param context the context to modify
     * @param option the option to set (e.g., {@link ZmqConstants#ZMQ_IO_THREADS})
     * @param optval the value to set for the option
     * @return 0 on success, -1 on error
     * @see #ctxGet(MemorySegment, int)
     * @see ZmqConstants
     */
    public static int ctxSet(MemorySegment context, int option, int optval) {
        try {
            return (int) zmq_ctx_set.invokeExact(context, option, optval);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a context option value.
     * <p>
     * This function retrieves the value of the specified option for the given context.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_ctx_get()} in the C API.
     * </p>
     *
     * @param context the context to query
     * @param option the option to retrieve (e.g., {@link ZmqConstants#ZMQ_IO_THREADS})
     * @return the option value on success, -1 on error
     * @see #ctxSet(MemorySegment, int, int)
     * @see ZmqConstants
     */
    public static int ctxGet(MemorySegment context, int option) {
        try {
            return (int) zmq_ctx_get.invokeExact(context, option);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Socket Management ==========

    /**
     * Creates a ZeroMQ socket within the specified context.
     * <p>
     * This function creates a socket of the specified type and associates it with the context.
     * The socket type determines the semantics of communication.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_socket()} in the C API.
     * </p>
     *
     * @param context the context in which to create the socket
     * @param type the socket type (e.g., {@link ZmqConstants#ZMQ_REQ}, {@link ZmqConstants#ZMQ_REP})
     * @return a pointer to the newly created socket, or NULL on error
     * @see #close(MemorySegment)
     * @see ZmqConstants
     */
    public static MemorySegment socket(MemorySegment context, int type) {
        try {
            return (MemorySegment) zmq_socket.invokeExact(context, type);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes a ZeroMQ socket.
     * <p>
     * This function destroys the socket and releases all resources associated with it.
     * Any outstanding messages will be discarded unless the linger period is set.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_close()} in the C API.
     * </p>
     *
     * @param socket the socket to close
     * @return 0 on success, -1 on error
     * @see #socket(MemorySegment, int)
     */
    public static int close(MemorySegment socket) {
        try {
            return (int) zmq_close.invokeExact(socket);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Binds a socket to a local endpoint and accepts incoming connections.
     * <p>
     * This function binds the socket to a local endpoint and then accepts incoming
     * connections on that endpoint. The endpoint is a string consisting of a transport
     * protocol followed by an address.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_bind()} in the C API.
     * </p>
     *
     * <h3>Example endpoints:</h3>
     * <ul>
     *   <li>{@code tcp://*:5555} - TCP on all interfaces, port 5555</li>
     *   <li>{@code ipc:///tmp/feeds/0} - Unix domain socket</li>
     *   <li>{@code inproc://my-endpoint} - In-process transport</li>
     * </ul>
     *
     * @param socket the socket to bind
     * @param addr the endpoint address string
     * @return 0 on success, -1 on error
     * @see #connect(MemorySegment, String)
     * @see #unbind(MemorySegment, String)
     */
    public static int bind(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_bind.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Connects a socket to an endpoint and accepts outgoing connections.
     * <p>
     * This function connects the socket to an endpoint and then accepts outgoing
     * connections to that endpoint.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_connect()} in the C API.
     * </p>
     *
     * @param socket the socket to connect
     * @param addr the endpoint address string
     * @return 0 on success, -1 on error
     * @see #bind(MemorySegment, String)
     * @see #disconnect(MemorySegment, String)
     */
    public static int connect(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_connect.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unbinds a socket from an endpoint.
     * <p>
     * This function unbinds the socket from the specified endpoint. The endpoint must
     * have been previously bound using {@link #bind(MemorySegment, String)}.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_unbind()} in the C API.
     * </p>
     *
     * @param socket the socket to unbind
     * @param addr the endpoint address string to unbind from
     * @return 0 on success, -1 on error
     * @see #bind(MemorySegment, String)
     */
    public static int unbind(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_unbind.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Disconnects a socket from an endpoint.
     * <p>
     * This function disconnects the socket from the specified endpoint. The endpoint must
     * have been previously connected using {@link #connect(MemorySegment, String)}.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_disconnect()} in the C API.
     * </p>
     *
     * @param socket the socket to disconnect
     * @param addr the endpoint address string to disconnect from
     * @return 0 on success, -1 on error
     * @see #connect(MemorySegment, String)
     */
    public static int disconnect(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_disconnect.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a socket option.
     * <p>
     * This function sets the specified option on the given socket.
     * Options control various aspects of socket behavior such as I/O threads,
     * message patterns, and transport protocols.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_setsockopt()} in the C API.
     * </p>
     *
     * @param socket the socket to modify
     * @param option the option to set (e.g., {@link ZmqConstants#ZMQ_SUBSCRIBE})
     * @param optval a pointer to the option value
     * @param optvallen the size of the option value in bytes
     * @return 0 on success, -1 on error
     * @see #getSockOpt(MemorySegment, int, MemorySegment, MemorySegment)
     * @see ZmqConstants
     */
    public static int setSockOpt(MemorySegment socket, int option, MemorySegment optval, long optvallen) {
        try {
            return (int) zmq_setsockopt.invokeExact(socket, option, optval, optvallen);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a socket option value.
     * <p>
     * This function retrieves the value of the specified option for the given socket.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_getsockopt()} in the C API.
     * </p>
     *
     * @param socket the socket to query
     * @param option the option to retrieve
     * @param optval a pointer to store the option value
     * @param optvallen a pointer to the size of the option value buffer (input/output)
     * @return 0 on success, -1 on error
     * @see #setSockOpt(MemorySegment, int, MemorySegment, long)
     * @see ZmqConstants
     */
    public static int getSockOpt(MemorySegment socket, int option, MemorySegment optval, MemorySegment optvallen) {
        try {
            return (int) zmq_getsockopt.invokeExact(socket, option, optval, optvallen);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a message on a socket.
     * <p>
     * This function queues a message to be sent on the socket. The message is not
     * necessarily sent immediately. Use flags to control blocking behavior and multi-part messages.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_send()} in the C API.
     * </p>
     *
     * @param socket the socket to send on
     * @param buf a pointer to the message data
     * @param len the length of the message in bytes
     * @param flags flags to control send behavior (e.g., {@link ZmqConstants#ZMQ_DONTWAIT}, {@link ZmqConstants#ZMQ_SNDMORE})
     * @return the number of bytes sent on success, -1 on error
     * @see #recv(MemorySegment, MemorySegment, long, int)
     * @see #msgSend(MemorySegment, MemorySegment, int)
     */
    public static int send(MemorySegment socket, MemorySegment buf, long len, int flags) {
        try {
            return (int) zmq_send.invokeExact(socket, buf, len, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Receives a message from a socket.
     * <p>
     * This function receives a message from the socket and stores it in the buffer.
     * Use flags to control blocking behavior.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_recv()} in the C API.
     * </p>
     *
     * @param socket the socket to receive from
     * @param buf a pointer to store the received message data
     * @param len the size of the buffer in bytes
     * @param flags flags to control receive behavior (e.g., {@link ZmqConstants#ZMQ_DONTWAIT})
     * @return the number of bytes received on success, -1 on error
     * @see #send(MemorySegment, MemorySegment, long, int)
     * @see #msgRecv(MemorySegment, MemorySegment, int)
     */
    public static int recv(MemorySegment socket, MemorySegment buf, long len, int flags) {
        try {
            return (int) zmq_recv.invokeExact(socket, buf, len, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Spawns a socket monitor to track socket events.
     * <p>
     * This function spawns a PAIR socket that publishes socket state changes (events)
     * over the inproc:// transport to the specified endpoint.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_socket_monitor()} in the C API.
     * </p>
     *
     * @param socket the socket to monitor
     * @param addr the endpoint address to publish events on (inproc:// protocol), or null to disable monitoring
     * @param events a bitmask of events to monitor (e.g., {@link ZmqConstants#ZMQ_EVENT_ALL})
     * @return 0 on success, -1 on error
     * @see ZmqConstants
     */
    public static int socketMonitor(MemorySegment socket, String addr, int events) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = addr != null ?
                arena.allocateUtf8String(addr) : MemorySegment.NULL;
            return (int) zmq_socket_monitor.invokeExact(socket, addrSeg, events);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Message Management ==========

    /**
     * Initializes an empty ZeroMQ message.
     * <p>
     * This function initializes a message object with zero length.
     * Use this before calling {@link #msgRecv(MemorySegment, MemorySegment, int)}.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_init()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message structure to initialize
     * @return 0 on success, -1 on error
     * @see #msgClose(MemorySegment)
     * @see #msgInitSize(MemorySegment, long)
     */
    public static int msgInit(MemorySegment msg) {
        try {
            return (int) zmq_msg_init.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes a ZeroMQ message of a specified size.
     * <p>
     * This function initializes a message object to hold a message of the specified size.
     * The message content is uninitialized.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_init_size()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message structure to initialize
     * @param size the size of the message in bytes
     * @return 0 on success, -1 on error
     * @see #msgClose(MemorySegment)
     * @see #msgInit(MemorySegment)
     */
    public static int msgInitSize(MemorySegment msg, long size) {
        try {
            return (int) zmq_msg_init_size.invokeExact(msg, size);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes a ZeroMQ message from a pre-allocated buffer.
     * <p>
     * This function initializes a message object to reference existing data.
     * A free function can be specified to deallocate the data when the message is released.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_init_data()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message structure to initialize
     * @param data a pointer to the message data
     * @param size the size of the message data in bytes
     * @param ffn a pointer to the free function (or NULL)
     * @param hint a hint passed to the free function (or NULL)
     * @return 0 on success, -1 on error
     * @see #msgClose(MemorySegment)
     */
    public static int msgInitData(MemorySegment msg, MemorySegment data, long size,
                                   MemorySegment ffn, MemorySegment hint) {
        try {
            return (int) zmq_msg_init_data.invokeExact(msg, data, size, ffn, hint);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a message on a socket (zero-copy).
     * <p>
     * This function queues the message to be sent on the socket. After a successful call,
     * the message is transferred to ZeroMQ and the message structure is reinitialized to
     * an empty message. This is a zero-copy operation.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_send()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message to send
     * @param socket the socket to send on
     * @param flags flags to control send behavior (e.g., {@link ZmqConstants#ZMQ_DONTWAIT}, {@link ZmqConstants#ZMQ_SNDMORE})
     * @return the number of bytes sent on success, -1 on error
     * @see #msgRecv(MemorySegment, MemorySegment, int)
     * @see #send(MemorySegment, MemorySegment, long, int)
     */
    public static int msgSend(MemorySegment msg, MemorySegment socket, int flags) {
        try {
            return (int) zmq_msg_send.invokeExact(msg, socket, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Receives a message from a socket (zero-copy).
     * <p>
     * This function receives a message from the socket and stores it in the message structure.
     * Any content previously stored in the message is released. This is a zero-copy operation.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_recv()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message structure to receive into
     * @param socket the socket to receive from
     * @param flags flags to control receive behavior (e.g., {@link ZmqConstants#ZMQ_DONTWAIT})
     * @return the number of bytes received on success, -1 on error
     * @see #msgSend(MemorySegment, MemorySegment, int)
     * @see #recv(MemorySegment, MemorySegment, long, int)
     */
    public static int msgRecv(MemorySegment msg, MemorySegment socket, int flags) {
        try {
            return (int) zmq_msg_recv.invokeExact(msg, socket, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Releases a ZeroMQ message.
     * <p>
     * This function releases the message and frees all resources associated with it.
     * After calling this function, the message structure should be reinitialized
     * before being used again.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_close()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message to release
     * @return 0 on success, -1 on error
     * @see #msgInit(MemorySegment)
     */
    public static int msgClose(MemorySegment msg) {
        try {
            return (int) zmq_msg_close.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Moves the content of one message to another (destructive).
     * <p>
     * This function moves the content of the source message to the destination message.
     * After the operation, the source message is reinitialized to an empty message.
     * No actual data copying occurs - this is a pointer operation.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_move()} in the C API.
     * </p>
     *
     * @param dest a pointer to the destination message
     * @param src a pointer to the source message
     * @return 0 on success, -1 on error
     * @see #msgCopy(MemorySegment, MemorySegment)
     */
    public static int msgMove(MemorySegment dest, MemorySegment src) {
        try {
            return (int) zmq_msg_move.invokeExact(dest, src);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies the content of one message to another.
     * <p>
     * This function copies the content of the source message to the destination message.
     * The copy is shallow - both messages reference the same underlying data with
     * reference counting.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_copy()} in the C API.
     * </p>
     *
     * @param dest a pointer to the destination message
     * @param src a pointer to the source message
     * @return 0 on success, -1 on error
     * @see #msgMove(MemorySegment, MemorySegment)
     */
    public static int msgCopy(MemorySegment dest, MemorySegment src) {
        try {
            return (int) zmq_msg_copy.invokeExact(dest, src);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a pointer to the message data.
     * <p>
     * This function returns a pointer to the message content. The returned pointer
     * is valid only as long as the message is valid and has not been closed or modified.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_data()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message
     * @return a pointer to the message data
     * @see #msgSize(MemorySegment)
     */
    public static MemorySegment msgData(MemorySegment msg) {
        try {
            return (MemorySegment) zmq_msg_data.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the size of the message data in bytes.
     * <p>
     * This function returns the size of the message content in bytes.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_size()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message
     * @return the size of the message in bytes
     * @see #msgData(MemorySegment)
     */
    public static long msgSize(MemorySegment msg) {
        try {
            return (long) zmq_msg_size.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if more message parts are to follow.
     * <p>
     * This function indicates whether there are more message parts to follow after
     * receiving a message part. It is used for receiving multi-part messages.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_more()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message
     * @return 1 if more message parts follow, 0 if this is the last part
     * @see #msgRecv(MemorySegment, MemorySegment, int)
     */
    public static int msgMore(MemorySegment msg) {
        try {
            return (int) zmq_msg_more.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a message property value.
     * <p>
     * This function retrieves the value of the specified message metadata property.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_get()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message
     * @param property the property to retrieve (e.g., {@link ZmqConstants#ZMQ_MORE})
     * @return the property value on success, -1 on error
     * @see #msgSet(MemorySegment, int, int)
     */
    public static int msgGet(MemorySegment msg, int property) {
        try {
            return (int) zmq_msg_get.invokeExact(msg, property);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a message property value.
     * <p>
     * This function sets the specified message metadata property to the given value.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_set()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message
     * @param property the property to set
     * @param optval the value to set for the property
     * @return 0 on success, -1 on error
     * @see #msgGet(MemorySegment, int)
     */
    public static int msgSet(MemorySegment msg, int property, int optval) {
        try {
            return (int) zmq_msg_set.invokeExact(msg, property, optval);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a message metadata property as a string.
     * <p>
     * This function retrieves the value of the specified message metadata property
     * as a string.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_msg_gets()} in the C API.
     * </p>
     *
     * @param msg a pointer to the message
     * @param property the name of the property to retrieve
     * @return the property value as a string, or null if the property does not exist
     * @see #msgGet(MemorySegment, int)
     */
    public static String msgGets(MemorySegment msg, String property) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment propSeg = arena.allocateUtf8String(property);
            MemorySegment result = (MemorySegment) zmq_msg_gets.invokeExact(msg, propSeg);
            if (result.address() == 0) {
                return null;
            }
            return result.reinterpret(Long.MAX_VALUE).getUtf8String(0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Polling ==========

    /**
     * Polls for events on multiple ZeroMQ sockets.
     * <p>
     * This function allows an application to multiplex input/output events over
     * a set of sockets. It blocks until one or more of the requested events occurs,
     * or until the timeout expires.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_poll()} in the C API.
     * </p>
     *
     * @param items a pointer to an array of {@code zmq_pollitem_t} structures
     * @param nitems the number of items in the poll items array
     * @param timeout the timeout in milliseconds (-1 for infinite, 0 for non-blocking)
     * @return the number of items with events on success, -1 on error
     * @see ZmqStructs#getPollItemLayout()
     */
    public static int poll(MemorySegment items, int nitems, long timeout) {
        try {
            return (int) zmq_poll.invokeExact(items, nitems, timeout);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Utilities ==========

    /**
     * Starts a built-in ZeroMQ proxy device.
     * <p>
     * This function connects a frontend socket to a backend socket, forwarding
     * messages between them. Optionally, all messages can be captured by a third socket.
     * This function blocks indefinitely.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_proxy()} in the C API.
     * </p>
     *
     * @param frontend the frontend socket
     * @param backend the backend socket
     * @param capture the capture socket (or NULL to disable capturing)
     * @return 0 on success, -1 on error (typically only returns on error)
     * @see #proxySteerable(MemorySegment, MemorySegment, MemorySegment, MemorySegment)
     */
    public static int proxy(MemorySegment frontend, MemorySegment backend, MemorySegment capture) {
        try {
            return (int) zmq_proxy.invokeExact(frontend, backend, capture);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts a built-in ZeroMQ proxy device with control flow.
     * <p>
     * This function is similar to {@link #proxy(MemorySegment, MemorySegment, MemorySegment)},
     * but adds a control socket that allows the proxy to be paused, resumed, or terminated.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_proxy_steerable()} in the C API.
     * </p>
     *
     * @param frontend the frontend socket
     * @param backend the backend socket
     * @param capture the capture socket (or NULL to disable capturing)
     * @param control the control socket to receive commands (PAUSE, RESUME, TERMINATE)
     * @return 0 on success, -1 on error
     * @see #proxy(MemorySegment, MemorySegment, MemorySegment)
     */
    public static int proxySteerable(MemorySegment frontend, MemorySegment backend,
                                      MemorySegment capture, MemorySegment control) {
        try {
            return (int) zmq_proxy_steerable.invokeExact(frontend, backend, capture, control);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if a specific capability is available in the ZeroMQ library.
     * <p>
     * This function checks whether the library was compiled with support for
     * a specific capability such as "ipc", "pgm", "tipc", "norm", "curve", "gssapi", or "draft".
     * </p>
     * <p>
     * This function corresponds to {@code zmq_has()} in the C API.
     * </p>
     *
     * @param capability the name of the capability to check (e.g., "ipc", "curve")
     * @return 1 if the capability is available, 0 if not available
     */
    public static int has(String capability) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment capSeg = arena.allocateUtf8String(capability);
            return (int) zmq_has.invokeExact(capSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes binary data into Z85 printable text.
     * <p>
     * This function encodes a binary key as Z85 printable text. The size of the binary
     * data must be divisible by 4. The output buffer must be at least (size * 5/4) + 1 bytes.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_z85_encode()} in the C API.
     * </p>
     *
     * @param dest a pointer to the destination buffer for the encoded string
     * @param data a pointer to the binary data to encode
     * @param size the size of the binary data in bytes (must be divisible by 4)
     * @return a pointer to the destination buffer on success, NULL on error
     * @see #z85Decode(MemorySegment, MemorySegment)
     */
    public static MemorySegment z85Encode(MemorySegment dest, MemorySegment data, long size) {
        try {
            return (MemorySegment) zmq_z85_encode.invokeExact(dest, data, size);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decodes Z85 printable text into binary data.
     * <p>
     * This function decodes a Z85 encoded string into binary data. The output buffer
     * must be at least (strlen(str) * 4/5) bytes.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_z85_decode()} in the C API.
     * </p>
     *
     * @param dest a pointer to the destination buffer for the decoded binary data
     * @param str a pointer to the Z85 encoded string
     * @return a pointer to the destination buffer on success, NULL on error
     * @see #z85Encode(MemorySegment, MemorySegment, long)
     */
    public static MemorySegment z85Decode(MemorySegment dest, MemorySegment str) {
        try {
            return (MemorySegment) zmq_z85_decode.invokeExact(dest, str);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a new CURVE key pair.
     * <p>
     * This function generates a new random CURVE key pair, consisting of a public
     * key and a secret key. Both keys are Z85-encoded strings of 40 characters plus
     * null terminator. The buffers must be at least 41 bytes each.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_curve_keypair()} in the C API.
     * </p>
     *
     * @param z85PublicKey a pointer to a buffer to store the Z85-encoded public key (41 bytes)
     * @param z85SecretKey a pointer to a buffer to store the Z85-encoded secret key (41 bytes)
     * @return 0 on success, -1 on error
     * @see #curvePublic(MemorySegment, MemorySegment)
     */
    public static int curveKeypair(MemorySegment z85PublicKey, MemorySegment z85SecretKey) {
        try {
            return (int) zmq_curve_keypair.invokeExact(z85PublicKey, z85SecretKey);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Derives the public key from a CURVE secret key.
     * <p>
     * This function derives the public key from a given CURVE secret key.
     * Both keys are Z85-encoded strings of 40 characters plus null terminator.
     * The public key buffer must be at least 41 bytes.
     * </p>
     * <p>
     * This function corresponds to {@code zmq_curve_public()} in the C API.
     * </p>
     *
     * @param z85PublicKey a pointer to a buffer to store the Z85-encoded public key (41 bytes)
     * @param z85SecretKey a pointer to the Z85-encoded secret key
     * @return 0 on success, -1 on error
     * @see #curveKeypair(MemorySegment, MemorySegment)
     */
    public static int curvePublic(MemorySegment z85PublicKey, MemorySegment z85SecretKey) {
        try {
            return (int) zmq_curve_public.invokeExact(z85PublicKey, z85SecretKey);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
