package io.github.ulalax.zmq.core;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.*;

/**
 * Low-level bindings to libzmq using Java FFM API.
 * This class provides direct access to all zmq_* functions.
 *
 * Thread Safety: These functions match libzmq's thread-safety guarantees.
 * Most socket operations are NOT thread-safe.
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
     * Gets the last error number.
     * @return Error number (errno)
     */
    public static int errno() {
        try {
            return (int) zmq_errno.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the error message for the given error number.
     * @param errnum Error number
     * @return Error message string
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
     * Gets the ZMQ library version.
     * @return Array of [major, minor, patch]
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

    public static MemorySegment ctxNew() {
        try {
            return (MemorySegment) zmq_ctx_new.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int ctxTerm(MemorySegment context) {
        try {
            return (int) zmq_ctx_term.invokeExact(context);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int ctxShutdown(MemorySegment context) {
        try {
            return (int) zmq_ctx_shutdown.invokeExact(context);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int ctxSet(MemorySegment context, int option, int optval) {
        try {
            return (int) zmq_ctx_set.invokeExact(context, option, optval);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int ctxGet(MemorySegment context, int option) {
        try {
            return (int) zmq_ctx_get.invokeExact(context, option);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Socket Management ==========

    public static MemorySegment socket(MemorySegment context, int type) {
        try {
            return (MemorySegment) zmq_socket.invokeExact(context, type);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int close(MemorySegment socket) {
        try {
            return (int) zmq_close.invokeExact(socket);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int bind(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_bind.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int connect(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_connect.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int unbind(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_unbind.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int disconnect(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateUtf8String(addr);
            return (int) zmq_disconnect.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int setSockOpt(MemorySegment socket, int option, MemorySegment optval, long optvallen) {
        try {
            return (int) zmq_setsockopt.invokeExact(socket, option, optval, optvallen);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int getSockOpt(MemorySegment socket, int option, MemorySegment optval, MemorySegment optvallen) {
        try {
            return (int) zmq_getsockopt.invokeExact(socket, option, optval, optvallen);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int send(MemorySegment socket, MemorySegment buf, long len, int flags) {
        try {
            return (int) zmq_send.invokeExact(socket, buf, len, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int recv(MemorySegment socket, MemorySegment buf, long len, int flags) {
        try {
            return (int) zmq_recv.invokeExact(socket, buf, len, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

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

    public static int msgInit(MemorySegment msg) {
        try {
            return (int) zmq_msg_init.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgInitSize(MemorySegment msg, long size) {
        try {
            return (int) zmq_msg_init_size.invokeExact(msg, size);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgInitData(MemorySegment msg, MemorySegment data, long size,
                                   MemorySegment ffn, MemorySegment hint) {
        try {
            return (int) zmq_msg_init_data.invokeExact(msg, data, size, ffn, hint);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgSend(MemorySegment msg, MemorySegment socket, int flags) {
        try {
            return (int) zmq_msg_send.invokeExact(msg, socket, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgRecv(MemorySegment msg, MemorySegment socket, int flags) {
        try {
            return (int) zmq_msg_recv.invokeExact(msg, socket, flags);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgClose(MemorySegment msg) {
        try {
            return (int) zmq_msg_close.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgMove(MemorySegment dest, MemorySegment src) {
        try {
            return (int) zmq_msg_move.invokeExact(dest, src);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgCopy(MemorySegment dest, MemorySegment src) {
        try {
            return (int) zmq_msg_copy.invokeExact(dest, src);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static MemorySegment msgData(MemorySegment msg) {
        try {
            return (MemorySegment) zmq_msg_data.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static long msgSize(MemorySegment msg) {
        try {
            return (long) zmq_msg_size.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgMore(MemorySegment msg) {
        try {
            return (int) zmq_msg_more.invokeExact(msg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgGet(MemorySegment msg, int property) {
        try {
            return (int) zmq_msg_get.invokeExact(msg, property);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int msgSet(MemorySegment msg, int property, int optval) {
        try {
            return (int) zmq_msg_set.invokeExact(msg, property, optval);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

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

    public static int poll(MemorySegment items, int nitems, long timeout) {
        try {
            return (int) zmq_poll.invokeExact(items, nitems, timeout);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // ========== Utilities ==========

    public static int proxy(MemorySegment frontend, MemorySegment backend, MemorySegment capture) {
        try {
            return (int) zmq_proxy.invokeExact(frontend, backend, capture);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int proxySteerable(MemorySegment frontend, MemorySegment backend,
                                      MemorySegment capture, MemorySegment control) {
        try {
            return (int) zmq_proxy_steerable.invokeExact(frontend, backend, capture, control);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int has(String capability) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment capSeg = arena.allocateUtf8String(capability);
            return (int) zmq_has.invokeExact(capSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static MemorySegment z85Encode(MemorySegment dest, MemorySegment data, long size) {
        try {
            return (MemorySegment) zmq_z85_encode.invokeExact(dest, data, size);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static MemorySegment z85Decode(MemorySegment dest, MemorySegment str) {
        try {
            return (MemorySegment) zmq_z85_decode.invokeExact(dest, str);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int curveKeypair(MemorySegment z85PublicKey, MemorySegment z85SecretKey) {
        try {
            return (int) zmq_curve_keypair.invokeExact(z85PublicKey, z85SecretKey);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int curvePublic(MemorySegment z85PublicKey, MemorySegment z85SecretKey) {
        try {
            return (int) zmq_curve_public.invokeExact(z85PublicKey, z85SecretKey);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
