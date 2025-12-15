package io.github.ulalax.zmq.core;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.StructLayout;

import static java.lang.foreign.ValueLayout.*;

/**
 * Memory layouts for ZeroMQ native structures.
 * <p>
 * This class provides {@link StructLayout} definitions for ZeroMQ's native C structures,
 * enabling proper memory mapping when using Java's Foreign Function &amp; Memory API.
 * These layouts must exactly match the binary structure of the corresponding C structures.
 * </p>
 *
 * <h2>Defined Structures:</h2>
 * <ul>
 *   <li>{@link #ZMQ_MSG_LAYOUT} - Layout for zmq_msg_t (64 bytes, 8-byte aligned)</li>
 *   <li>{@link #ZMQ_POLLITEM_WINDOWS_LAYOUT} - Layout for zmq_pollitem_t on Windows</li>
 *   <li>{@link #ZMQ_POLLITEM_UNIX_LAYOUT} - Layout for zmq_pollitem_t on Unix/Linux/macOS</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Allocate memory for a ZMQ message
 * try (Arena arena = Arena.ofConfined()) {
 *     MemorySegment msg = arena.allocate(ZmqStructs.ZMQ_MSG_LAYOUT);
 *     LibZmq.msgInit(msg);
 *     // Use message...
 *     LibZmq.msgClose(msg);
 * }
 *
 * // Allocate array of poll items
 * try (Arena arena = Arena.ofConfined()) {
 *     StructLayout layout = ZmqStructs.getPollItemLayout();
 *     MemorySegment items = arena.allocate(layout, 10); // 10 items
 *     // Configure and use poll items...
 * }
 * }</pre>
 *
 * @see LibZmq
 * @see java.lang.foreign.StructLayout
 * @since 1.0.0
 */
public final class ZmqStructs {

    private ZmqStructs() {
        // Prevent instantiation
    }

    /**
     * Memory layout for the zmq_msg_t structure.
     * <p>
     * This layout defines the structure of a ZeroMQ message object. The structure
     * is 64 bytes in size with 8-byte alignment, consisting of eight 8-byte fields.
     * This matches the C definition of zmq_msg_t.
     * </p>
     * <p>
     * <strong>Important:</strong> This layout must exactly match the native structure size
     * and alignment. Any mismatch will cause memory corruption or crashes.
     * </p>
     *
     * @see LibZmq#msgInit(java.lang.foreign.MemorySegment)
     * @see LibZmq#msgClose(java.lang.foreign.MemorySegment)
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
     * The size of zmq_msg_t structure in bytes.
     * <p>
     * This constant is derived from {@link #ZMQ_MSG_LAYOUT} and always equals 64 bytes.
     * </p>
     */
    public static final long ZMQ_MSG_SIZE = ZMQ_MSG_LAYOUT.byteSize();

    /**
     * Memory layout for zmq_pollitem_t structure on Windows platforms.
     * <p>
     * This layout defines the polling structure for Windows, which differs from Unix
     * systems in the file descriptor field size:
     * </p>
     * <ul>
     *   <li>socket: pointer (8 bytes) - ZeroMQ socket</li>
     *   <li>fd: SOCKET/long (8 bytes) - Windows socket handle</li>
     *   <li>events: short (2 bytes) - Events to poll for</li>
     *   <li>revents: short (2 bytes) - Events that occurred</li>
     *   <li>padding: 32 bits - Alignment padding</li>
     * </ul>
     *
     * @see #ZMQ_POLLITEM_UNIX_LAYOUT
     * @see #getPollItemLayout()
     */
    public static final StructLayout ZMQ_POLLITEM_WINDOWS_LAYOUT = MemoryLayout.structLayout(
        ADDRESS.withName("socket"),
        JAVA_LONG.withName("fd"),
        JAVA_SHORT.withName("events"),
        JAVA_SHORT.withName("revents"),
        MemoryLayout.paddingLayout(32)  // Padding to align structure
    );

    /**
     * Memory layout for zmq_pollitem_t structure on Unix/Linux/macOS platforms.
     * <p>
     * This layout defines the polling structure for Unix-like systems:
     * </p>
     * <ul>
     *   <li>socket: pointer (8 bytes) - ZeroMQ socket</li>
     *   <li>fd: int (4 bytes) - Unix file descriptor</li>
     *   <li>events: short (2 bytes) - Events to poll for</li>
     *   <li>revents: short (2 bytes) - Events that occurred</li>
     * </ul>
     *
     * @see #ZMQ_POLLITEM_WINDOWS_LAYOUT
     * @see #getPollItemLayout()
     */
    public static final StructLayout ZMQ_POLLITEM_UNIX_LAYOUT = MemoryLayout.structLayout(
        ADDRESS.withName("socket"),
        JAVA_INT.withName("fd"),
        JAVA_SHORT.withName("events"),
        JAVA_SHORT.withName("revents")
    );

    /**
     * Returns the platform-appropriate memory layout for zmq_pollitem_t.
     * <p>
     * This method detects the current operating system and returns the correct
     * layout for the platform. Windows uses {@link #ZMQ_POLLITEM_WINDOWS_LAYOUT},
     * while Unix-like systems use {@link #ZMQ_POLLITEM_UNIX_LAYOUT}.
     * </p>
     *
     * @return the {@link StructLayout} for zmq_pollitem_t appropriate for the current platform
     * @see #ZMQ_POLLITEM_WINDOWS_LAYOUT
     * @see #ZMQ_POLLITEM_UNIX_LAYOUT
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
     * Provides byte offsets for fields in the zmq_pollitem_t structure.
     * <p>
     * This nested class provides constants for calculating the byte offset of each field
     * in the zmq_pollitem_t structure. These offsets are platform-aware and account for
     * differences between Windows and Unix-like systems.
     * </p>
     * <p>
     * Use these offsets when manually accessing structure fields via {@link java.lang.foreign.MemorySegment}.
     * </p>
     *
     * @see #getPollItemLayout()
     */
    public static final class PollItemOffsets {
        /** Byte offset of the socket field (always 0). */
        public static final long SOCKET = 0;

        /** Byte offset of the fd field (after the socket pointer). */
        public static final long FD = ADDRESS.byteSize();

        /** Byte offset of the events field (platform-dependent based on fd size). */
        public static final long EVENTS = isWindows() ? (ADDRESS.byteSize() + 8) : (ADDRESS.byteSize() + 4);

        /** Byte offset of the revents field (always 2 bytes after events). */
        public static final long REVENTS = EVENTS + 2;

        /**
         * Checks if the current platform is Windows.
         *
         * @return {@code true} if Windows, {@code false} otherwise
         */
        private static boolean isWindows() {
            return System.getProperty("os.name").toLowerCase().contains("win");
        }
    }
}
