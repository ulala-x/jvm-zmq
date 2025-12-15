# FFM Bindings - zmq-core Module

This document provides complete FFM (Foreign Function & Memory) API bindings for libzmq using JDK 22.

---

## Table of Contents
1. [LibZmq.java - Function Bindings](#libzmqjava---function-bindings)
2. [ZmqConstants.java - Constants](#zmqconstantsjava---constants)
3. [ZmqStructs.java - Memory Layouts](#zmqstructsjava---memory-layouts)
4. [ZmqException.java - Exception Handling](#zmqexceptionjava---exception-handling)
5. [NativeLoader.java - Library Loading](#nativeloaderjava---library-loading)

---

## LibZmq.java - Function Bindings

Complete implementation of all libzmq functions using FFM API.

**Package**: `io.github.ulalax.zmq.core`

### Full Implementation

```java
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
            return ptr.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
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
            MemorySegment addrSeg = arena.allocateFrom(addr, StandardCharsets.UTF_8);
            return (int) zmq_bind.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int connect(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateFrom(addr, StandardCharsets.UTF_8);
            return (int) zmq_connect.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int unbind(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateFrom(addr, StandardCharsets.UTF_8);
            return (int) zmq_unbind.invokeExact(socket, addrSeg);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static int disconnect(MemorySegment socket, String addr) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment addrSeg = arena.allocateFrom(addr, StandardCharsets.UTF_8);
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
                arena.allocateFrom(addr, StandardCharsets.UTF_8) : MemorySegment.NULL;
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
            MemorySegment propSeg = arena.allocateFrom(property, StandardCharsets.UTF_8);
            MemorySegment result = (MemorySegment) zmq_msg_gets.invokeExact(msg, propSeg);
            if (result.address() == 0) {
                return null;
            }
            return result.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
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
            MemorySegment capSeg = arena.allocateFrom(capability, StandardCharsets.UTF_8);
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
```

---

## ZmqConstants.java - Constants

Complete constants class matching all ZeroMQ constants.

**Package**: `io.github.ulalax.zmq.core`

### Full Implementation

```java
package io.github.ulalax.zmq.core;

/**
 * ZeroMQ constants for socket types, options, and error codes.
 * These constants match the values defined in zmq.h.
 */
public final class ZmqConstants {

    private ZmqConstants() {
        // Prevent instantiation
    }

    // ========== Socket Types ==========
    public static final int ZMQ_PAIR = 0;
    public static final int ZMQ_PUB = 1;
    public static final int ZMQ_SUB = 2;
    public static final int ZMQ_REQ = 3;
    public static final int ZMQ_REP = 4;
    public static final int ZMQ_DEALER = 5;
    public static final int ZMQ_ROUTER = 6;
    public static final int ZMQ_PULL = 7;
    public static final int ZMQ_PUSH = 8;
    public static final int ZMQ_XPUB = 9;
    public static final int ZMQ_XSUB = 10;
    public static final int ZMQ_STREAM = 11;

    // ========== Context Options ==========
    public static final int ZMQ_IO_THREADS = 1;
    public static final int ZMQ_MAX_SOCKETS = 2;
    public static final int ZMQ_SOCKET_LIMIT = 3;
    public static final int ZMQ_THREAD_PRIORITY = 3;
    public static final int ZMQ_THREAD_SCHED_POLICY = 4;
    public static final int ZMQ_MAX_MSGSZ = 5;
    public static final int ZMQ_MSG_T_SIZE = 6;
    public static final int ZMQ_THREAD_AFFINITY_CPU_ADD = 7;
    public static final int ZMQ_THREAD_AFFINITY_CPU_REMOVE = 8;
    public static final int ZMQ_THREAD_NAME_PREFIX = 9;

    // ========== Socket Options ==========
    public static final int ZMQ_AFFINITY = 4;
    public static final int ZMQ_ROUTING_ID = 5;
    public static final int ZMQ_SUBSCRIBE = 6;
    public static final int ZMQ_UNSUBSCRIBE = 7;
    public static final int ZMQ_RATE = 8;
    public static final int ZMQ_RECOVERY_IVL = 9;
    public static final int ZMQ_SNDBUF = 11;
    public static final int ZMQ_RCVBUF = 12;
    public static final int ZMQ_RCVMORE = 13;
    public static final int ZMQ_FD = 14;
    public static final int ZMQ_EVENTS = 15;
    public static final int ZMQ_TYPE = 16;
    public static final int ZMQ_LINGER = 17;
    public static final int ZMQ_RECONNECT_IVL = 18;
    public static final int ZMQ_BACKLOG = 19;
    public static final int ZMQ_RECONNECT_IVL_MAX = 21;
    public static final int ZMQ_MAXMSGSIZE = 22;
    public static final int ZMQ_SNDHWM = 23;
    public static final int ZMQ_RCVHWM = 24;
    public static final int ZMQ_MULTICAST_HOPS = 25;
    public static final int ZMQ_RCVTIMEO = 27;
    public static final int ZMQ_SNDTIMEO = 28;
    public static final int ZMQ_LAST_ENDPOINT = 32;
    public static final int ZMQ_ROUTER_MANDATORY = 33;
    public static final int ZMQ_TCP_KEEPALIVE = 34;
    public static final int ZMQ_TCP_KEEPALIVE_CNT = 35;
    public static final int ZMQ_TCP_KEEPALIVE_IDLE = 36;
    public static final int ZMQ_TCP_KEEPALIVE_INTVL = 37;
    public static final int ZMQ_IMMEDIATE = 39;
    public static final int ZMQ_XPUB_VERBOSE = 40;
    public static final int ZMQ_ROUTER_RAW = 41;
    public static final int ZMQ_IPV6 = 42;
    public static final int ZMQ_MECHANISM = 43;
    public static final int ZMQ_PLAIN_SERVER = 44;
    public static final int ZMQ_PLAIN_USERNAME = 45;
    public static final int ZMQ_PLAIN_PASSWORD = 46;
    public static final int ZMQ_CURVE_SERVER = 47;
    public static final int ZMQ_CURVE_PUBLICKEY = 48;
    public static final int ZMQ_CURVE_SECRETKEY = 49;
    public static final int ZMQ_CURVE_SERVERKEY = 50;
    public static final int ZMQ_PROBE_ROUTER = 51;
    public static final int ZMQ_REQ_CORRELATE = 52;
    public static final int ZMQ_REQ_RELAXED = 53;
    public static final int ZMQ_CONFLATE = 54;
    public static final int ZMQ_ZAP_DOMAIN = 55;
    public static final int ZMQ_ROUTER_HANDOVER = 56;
    public static final int ZMQ_TOS = 57;
    public static final int ZMQ_CONNECT_ROUTING_ID = 61;
    public static final int ZMQ_GSSAPI_SERVER = 62;
    public static final int ZMQ_GSSAPI_PRINCIPAL = 63;
    public static final int ZMQ_GSSAPI_SERVICE_PRINCIPAL = 64;
    public static final int ZMQ_GSSAPI_PLAINTEXT = 65;
    public static final int ZMQ_HANDSHAKE_IVL = 66;
    public static final int ZMQ_SOCKS_PROXY = 68;
    public static final int ZMQ_XPUB_NODROP = 69;
    public static final int ZMQ_BLOCKY = 70;
    public static final int ZMQ_XPUB_MANUAL = 71;
    public static final int ZMQ_XPUB_WELCOME_MSG = 72;
    public static final int ZMQ_STREAM_NOTIFY = 73;
    public static final int ZMQ_INVERT_MATCHING = 74;
    public static final int ZMQ_HEARTBEAT_IVL = 75;
    public static final int ZMQ_HEARTBEAT_TTL = 76;
    public static final int ZMQ_HEARTBEAT_TIMEOUT = 77;
    public static final int ZMQ_XPUB_VERBOSER = 78;
    public static final int ZMQ_CONNECT_TIMEOUT = 79;
    public static final int ZMQ_TCP_MAXRT = 80;
    public static final int ZMQ_THREAD_SAFE = 81;
    public static final int ZMQ_MULTICAST_MAXTPDU = 84;
    public static final int ZMQ_VMCI_BUFFER_SIZE = 85;
    public static final int ZMQ_VMCI_BUFFER_MIN_SIZE = 86;
    public static final int ZMQ_VMCI_BUFFER_MAX_SIZE = 87;
    public static final int ZMQ_VMCI_CONNECT_TIMEOUT = 88;
    public static final int ZMQ_USE_FD = 89;
    public static final int ZMQ_GSSAPI_PRINCIPAL_NAMETYPE = 90;
    public static final int ZMQ_GSSAPI_SERVICE_PRINCIPAL_NAMETYPE = 91;
    public static final int ZMQ_BINDTODEVICE = 92;

    // ========== Send/Recv Flags ==========
    public static final int ZMQ_DONTWAIT = 1;
    public static final int ZMQ_SNDMORE = 2;

    // ========== Poll Events ==========
    public static final int ZMQ_POLLIN = 1;
    public static final int ZMQ_POLLOUT = 2;
    public static final int ZMQ_POLLERR = 4;
    public static final int ZMQ_POLLPRI = 8;

    // ========== Message Properties ==========
    public static final int ZMQ_MORE = 1;
    public static final int ZMQ_SHARED = 3;

    // ========== Security Mechanisms ==========
    public static final int ZMQ_NULL = 0;
    public static final int ZMQ_PLAIN = 1;
    public static final int ZMQ_CURVE = 2;
    public static final int ZMQ_GSSAPI = 3;

    // ========== Error Codes ==========
    // Platform-specific EAGAIN value
    // - Linux/Windows: 11
    // - macOS: 35
    public static final int EAGAIN = detectEAGAIN();

    public static final int ENOTSUP = 95;
    public static final int EPROTONOSUPPORT = 93;
    public static final int ENOBUFS = 105;
    public static final int ENETDOWN = 100;
    public static final int EADDRINUSE = 98;
    public static final int EADDRNOTAVAIL = 99;
    public static final int ECONNREFUSED = 111;
    public static final int EINPROGRESS = 115;
    public static final int ENOTSOCK = 88;
    public static final int EMSGSIZE = 90;
    public static final int EAFNOSUPPORT = 97;
    public static final int ENETUNREACH = 101;
    public static final int ECONNABORTED = 103;
    public static final int ECONNRESET = 104;
    public static final int ENOTCONN = 107;
    public static final int ETIMEDOUT = 110;
    public static final int EHOSTUNREACH = 113;
    public static final int ENETRESET = 102;

    // ZMQ-specific error codes (base value)
    public static final int ZMQ_HAUSNUMERO = 156384712;

    // ZMQ error codes
    public static final int ETERM = ZMQ_HAUSNUMERO + 53;
    public static final int ENOENT = 2;
    public static final int EINTR = 4;
    public static final int EACCES = 13;
    public static final int EFAULT = 14;
    public static final int EINVAL = 22;
    public static final int EMFILE = 24;

    // ========== Socket Monitor Events ==========
    public static final int ZMQ_EVENT_CONNECTED = 1;
    public static final int ZMQ_EVENT_CONNECT_DELAYED = 2;
    public static final int ZMQ_EVENT_CONNECT_RETRIED = 4;
    public static final int ZMQ_EVENT_LISTENING = 8;
    public static final int ZMQ_EVENT_BIND_FAILED = 16;
    public static final int ZMQ_EVENT_ACCEPTED = 32;
    public static final int ZMQ_EVENT_ACCEPT_FAILED = 64;
    public static final int ZMQ_EVENT_CLOSED = 128;
    public static final int ZMQ_EVENT_CLOSE_FAILED = 256;
    public static final int ZMQ_EVENT_DISCONNECTED = 512;
    public static final int ZMQ_EVENT_MONITOR_STOPPED = 1024;
    public static final int ZMQ_EVENT_ALL = 0xFFFF;

    // ========== Protocol Errors ==========
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_UNSPECIFIED = 0x10000000;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_UNEXPECTED_COMMAND = 0x10000001;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_INVALID_SEQUENCE = 0x10000002;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_KEY_EXCHANGE = 0x10000003;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_UNSPECIFIED = 0x10000011;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_MESSAGE = 0x10000012;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_HELLO = 0x10000013;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_INITIATE = 0x10000014;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_ERROR = 0x10000015;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_READY = 0x10000016;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MALFORMED_COMMAND_WELCOME = 0x10000017;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_INVALID_METADATA = 0x10000018;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_CRYPTOGRAPHIC = 0x11000001;
    public static final int ZMQ_PROTOCOL_ERROR_ZMTP_MECHANISM_MISMATCH = 0x11000002;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_UNSPECIFIED = 0x20000000;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_MALFORMED_REPLY = 0x20000001;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_BAD_REQUEST_ID = 0x20000002;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_BAD_VERSION = 0x20000003;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_INVALID_STATUS_CODE = 0x20000004;
    public static final int ZMQ_PROTOCOL_ERROR_ZAP_INVALID_METADATA = 0x20000005;
    public static final int ZMQ_PROTOCOL_ERROR_WS_UNSPECIFIED = 0x30000000;

    /**
     * Detect platform-specific EAGAIN value.
     * macOS uses 35, Linux/Windows use 11.
     */
    private static int detectEAGAIN() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return 35;
        }
        return 11;
    }
}
```

---

## ZmqStructs.java - Memory Layouts

Memory layouts for ZeroMQ structures.

**Package**: `io.github.ulalax.zmq.core`

### Full Implementation

```java
package io.github.ulalax.zmq.core;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import static java.lang.foreign.ValueLayout.*;

/**
 * Memory layouts for ZeroMQ structures.
 * These layouts define the binary structure of zmq_msg_t and zmq_pollitem_t.
 */
public final class ZmqStructs {

    private ZmqStructs() {
        // Prevent instantiation
    }

    /**
     * Layout for zmq_msg_t structure.
     * Must be exactly 64 bytes with 8-byte alignment.
     */
    public static final StructLayout ZMQ_MSG_LAYOUT = MemoryLayout.structLayout(
        JAVA_LONG.withName("p0"),
        JAVA_LONG.withName("p1"),
        JAVA_LONG.withName("p2"),
        JAVA_LONG.withName("p3"),
        JAVA_LONG.withName("p4"),
        JAVA_LONG.withName("p5"),
        JAVA_LONG.withName("p6"),
        JAVA_LONG.withName("p7")
    );

    /**
     * Size of zmq_msg_t in bytes (64 bytes).
     */
    public static final long ZMQ_MSG_SIZE = ZMQ_MSG_LAYOUT.byteSize();

    /**
     * Layout for zmq_pollitem_t on Windows.
     * - socket: pointer (8 bytes)
     * - fd: SOCKET (8 bytes on 64-bit, 4 bytes on 32-bit) - using long for simplicity
     * - events: short (2 bytes)
     * - revents: short (2 bytes)
     */
    public static final StructLayout ZMQ_POLLITEM_WINDOWS_LAYOUT = MemoryLayout.structLayout(
        ADDRESS.withName("socket"),
        JAVA_LONG.withName("fd"),
        JAVA_SHORT.withName("events"),
        JAVA_SHORT.withName("revents"),
        MemoryLayout.paddingLayout(32)  // Padding to align structure
    );

    /**
     * Layout for zmq_pollitem_t on Unix/Linux/macOS.
     * - socket: pointer (8 bytes)
     * - fd: int (4 bytes)
     * - events: short (2 bytes)
     * - revents: short (2 bytes)
     */
    public static final StructLayout ZMQ_POLLITEM_UNIX_LAYOUT = MemoryLayout.structLayout(
        ADDRESS.withName("socket"),
        JAVA_INT.withName("fd"),
        JAVA_SHORT.withName("events"),
        JAVA_SHORT.withName("revents")
    );

    /**
     * Get platform-appropriate zmq_pollitem_t layout.
     * @return Memory layout for zmq_pollitem_t
     */
    public static StructLayout getPollItemLayout() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return ZMQ_POLLITEM_WINDOWS_LAYOUT;
        } else {
            return ZMQ_POLLITEM_UNIX_LAYOUT;
        }
    }

    /**
     * Offsets for zmq_pollitem_t structure (platform-independent accessor).
     */
    public static final class PollItemOffsets {
        public static final long SOCKET = 0;
        public static final long FD = ADDRESS.byteSize();
        public static final long EVENTS = isWindows() ? (ADDRESS.byteSize() + 8) : (ADDRESS.byteSize() + 4);
        public static final long REVENTS = EVENTS + 2;

        private static boolean isWindows() {
            return System.getProperty("os.name").toLowerCase().contains("win");
        }
    }
}
```

---

## ZmqException.java - Exception Handling

Exception class for ZeroMQ errors.

**Package**: `io.github.ulalax.zmq.core`

### Full Implementation

```java
package io.github.ulalax.zmq.core;

import java.lang.foreign.MemorySegment;

/**
 * Exception thrown when a ZeroMQ operation fails.
 * Contains the error number and message from libzmq.
 */
public class ZmqException extends RuntimeException {

    private final int errorNumber;

    /**
     * Creates a ZmqException with the current zmq error.
     */
    public ZmqException() {
        this(LibZmq.errno());
    }

    /**
     * Creates a ZmqException with a specific error number.
     * @param errorNumber The zmq error number
     */
    public ZmqException(int errorNumber) {
        super(LibZmq.strerror(errorNumber));
        this.errorNumber = errorNumber;
    }

    /**
     * Creates a ZmqException with a specific error number and message.
     * @param errorNumber The zmq error number
     * @param message The error message
     */
    public ZmqException(int errorNumber, String message) {
        super(message);
        this.errorNumber = errorNumber;
    }

    /**
     * Gets the ZeroMQ error number.
     * @return Error number (errno)
     */
    public int getErrorNumber() {
        return errorNumber;
    }

    /**
     * Throws a ZmqException if the return code indicates an error.
     * @param returnCode Return code from libzmq function (usually -1 on error)
     * @throws ZmqException if returnCode is -1
     */
    public static void throwIfError(int returnCode) {
        if (returnCode == -1) {
            throw new ZmqException();
        }
    }

    /**
     * Throws a ZmqException if the pointer is null.
     * @param ptr Pointer from libzmq function
     * @throws ZmqException if ptr is NULL
     */
    public static void throwIfNull(MemorySegment ptr) {
        if (ptr == null || ptr.address() == 0) {
            throw new ZmqException();
        }
    }

    /**
     * Checks if this exception represents a "would block" error (EAGAIN).
     * @return true if this is an EAGAIN error
     */
    public boolean isAgain() {
        return errorNumber == ZmqConstants.EAGAIN;
    }

    /**
     * Checks if this exception represents a termination error (ETERM).
     * @return true if this is an ETERM error
     */
    public boolean isTerminated() {
        return errorNumber == ZmqConstants.ETERM;
    }

    /**
     * Checks if this exception represents an interrupted error (EINTR).
     * @return true if this is an EINTR error
     */
    public boolean isInterrupted() {
        return errorNumber == ZmqConstants.EINTR;
    }
}
```

---

## NativeLoader.java - Library Loading

Native library extraction and loading.

**Package**: `io.github.ulalax.zmq.core`

### Full Implementation

```java
package io.github.ulalax.zmq.core;

import java.io.*;
import java.nio.file.*;

/**
 * Loads the native libzmq library.
 * Extracts the platform-specific shared library from the JAR and loads it.
 */
public final class NativeLoader {

    private static volatile boolean loaded = false;
    private static final Object lock = new Object();

    private NativeLoader() {
        // Prevent instantiation
    }

    /**
     * Loads the native libzmq library.
     * This method is thread-safe and idempotent.
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    public static void load() {
        if (loaded) {
            return;
        }

        synchronized (lock) {
            if (loaded) {
                return;
            }

            // Check for user-specified library path
            String userPath = System.getProperty("zmq.library.path");
            if (userPath != null && !userPath.isEmpty()) {
                System.load(userPath);
                loaded = true;
                return;
            }

            // Detect platform
            String os = detectOS();
            String arch = detectArch();
            String libraryName = getLibraryName(os);

            // Resource path in JAR
            String resourcePath = "/native/" + os + "/" + arch + "/" + libraryName;

            // Extract and load
            try {
                File tempFile = extractLibrary(resourcePath, libraryName);
                System.load(tempFile.getAbsolutePath());
                loaded = true;
            } catch (IOException e) {
                throw new UnsatisfiedLinkError("Failed to load native library: " + e.getMessage());
            }
        }
    }

    /**
     * Detects the operating system.
     * @return "windows", "linux", or "macos"
     */
    private static String detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        } else if (os.contains("nux") || os.contains("nix")) {
            return "linux";
        } else if (os.contains("mac")) {
            return "macos";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }
    }

    /**
     * Detects the CPU architecture.
     * @return "x86_64" or "aarch64"
     */
    private static String detectArch() {
        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            return "x86_64";
        } else if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "aarch64";
        } else {
            throw new UnsupportedOperationException("Unsupported architecture: " + arch);
        }
    }

    /**
     * Gets the library file name for the platform.
     * @param os Operating system
     * @return Library file name
     */
    private static String getLibraryName(String os) {
        switch (os) {
            case "windows":
                return "libzmq.dll";
            case "linux":
                return "libzmq.so";
            case "macos":
                return "libzmq.dylib";
            default:
                throw new IllegalArgumentException("Unknown OS: " + os);
        }
    }

    /**
     * Extracts the native library from JAR to a temporary file.
     * @param resourcePath Path to resource in JAR
     * @param libraryName Name of the library
     * @return Temporary file containing the library
     * @throws IOException if extraction fails
     */
    private static File extractLibrary(String resourcePath, String libraryName) throws IOException {
        // Get resource as stream
        InputStream in = NativeLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new FileNotFoundException("Native library not found in JAR: " + resourcePath);
        }

        // Create temp file
        // Use a unique name to avoid conflicts
        String tempFileName = "jvm-zmq-" + System.currentTimeMillis() + "-" + libraryName;
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "jvm-zmq");
        Files.createDirectories(tempDir);

        File tempFile = tempDir.resolve(tempFileName).toFile();
        tempFile.deleteOnExit();

        // Copy to temp file
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            in.close();
        }

        // Make executable (Unix/macOS)
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            tempFile.setExecutable(true, false);
            tempFile.setReadable(true, false);
        }

        return tempFile;
    }

    /**
     * Checks if the native library has been loaded.
     * @return true if loaded
     */
    public static boolean isLoaded() {
        return loaded;
    }
}
```

---

## Usage Example

Example of using the FFM bindings directly:

```java
import io.github.ulalax.zmq.core.*;
import java.lang.foreign.*;

public class FFMExample {
    public static void main(String[] args) {
        // Version check
        int[] version = LibZmq.version();
        System.out.printf("ZMQ Version: %d.%d.%d%n",
            version[0], version[1], version[2]);

        // Create context
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctx = LibZmq.ctxNew();
            ZmqException.throwIfNull(ctx);

            // Create socket
            MemorySegment socket = LibZmq.socket(ctx, ZmqConstants.ZMQ_REP);
            ZmqException.throwIfNull(socket);

            // Bind
            int rc = LibZmq.bind(socket, "tcp://*:5555");
            ZmqException.throwIfError(rc);

            // Allocate message
            MemorySegment msg = arena.allocate(ZmqStructs.ZMQ_MSG_LAYOUT);
            rc = LibZmq.msgInit(msg);
            ZmqException.throwIfError(rc);

            // Receive
            rc = LibZmq.msgRecv(msg, socket, 0);
            ZmqException.throwIfError(rc);

            // Get data
            MemorySegment data = LibZmq.msgData(msg);
            long size = LibZmq.msgSize(msg);

            // Print received data
            byte[] bytes = data.reinterpret(size).toArray(ValueLayout.JAVA_BYTE);
            System.out.println("Received: " + new String(bytes));

            // Close
            LibZmq.msgClose(msg);
            LibZmq.close(socket);
            LibZmq.ctxTerm(ctx);
        }
    }
}
```

---

## Key FFM Patterns

### 1. Arena Management
Always use try-with-resources for Arena:
```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment seg = arena.allocate(...);
    // Use segment
} // Automatic cleanup
```

### 2. String Marshalling
Convert Java String to C string:
```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment str = arena.allocateFrom(javaString, StandardCharsets.UTF_8);
    // Pass str to native function
}
```

Convert C string to Java String:
```java
MemorySegment cStr = ...; // from native
String javaStr = cStr.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
```

### 3. Error Handling
Always check return codes:
```java
int rc = LibZmq.someFunction(...);
if (rc == -1) {
    int errno = LibZmq.errno();
    String error = LibZmq.strerror(errno);
    throw new ZmqException(errno, error);
}
```

### 4. Memory Segments
Never use MemorySegment after Arena is closed:
```java
MemorySegment segment;
try (Arena arena = Arena.ofConfined()) {
    segment = arena.allocate(100);
} // segment is now INVALID - do not use!
```

---

## Next Steps

After implementing the FFM bindings, proceed to:
- Implement high-level API (`03-high-level-api.md`)
- Implement utilities (`04-utilities.md`)
- Write tests
- Add documentation
