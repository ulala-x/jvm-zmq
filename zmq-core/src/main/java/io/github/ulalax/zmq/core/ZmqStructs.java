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
