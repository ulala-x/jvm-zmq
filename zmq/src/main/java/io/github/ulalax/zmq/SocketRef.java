package io.github.ulalax.zmq;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * Non-owning reference to a ZMQ socket. Equivalent to cppzmq's socket_ref.
 *
 * <p>This class provides a lightweight reference to a socket without managing its lifecycle.
 * It is useful for cases where you need to pass a socket reference without transferring ownership.</p>
 *
 * @see Socket
 */
public final class SocketRef {

    private final MemorySegment handle;

    /**
     * Creates a socket reference from a native handle.
     * @param handle The native socket handle
     */
    SocketRef(MemorySegment handle) {
        this.handle = handle;
    }

    /**
     * Creates a socket reference from a Socket.
     * @param socket The socket to reference
     * @return A new socket reference
     * @throws NullPointerException if socket is null
     */
    public static SocketRef from(Socket socket) {
        Objects.requireNonNull(socket, "socket cannot be null");
        return new SocketRef(socket.getHandle());
    }

    /**
     * Gets the native socket handle.
     * @return The native handle
     */
    public MemorySegment getHandle() {
        return handle;
    }

    /**
     * Checks if this reference is valid (non-null handle).
     * @return true if valid
     */
    public boolean isValid() {
        return handle != null && !handle.equals(MemorySegment.NULL);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SocketRef other)) return false;
        return Objects.equals(handle, other.handle);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(handle);
    }

    @Override
    public String toString() {
        return "SocketRef[handle=" + handle + "]";
    }
}
