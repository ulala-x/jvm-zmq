package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.*;
import java.time.Duration;

import static java.lang.foreign.ValueLayout.*;

/**
 * Instance-based polling for ZeroMQ sockets with optimized performance.
 *
 * <p>The Poller allows you to wait for events on multiple sockets simultaneously.
 * This implementation uses instance-based design to optimize high-frequency polling
 * by maintaining native memory structures across multiple poll operations.</p>
 *
 * <h2>Performance Optimization</h2>
 * <p>Unlike static polling approaches that allocate native memory on every poll,
 * this instance-based design maintains Arena and MemorySegment as instance fields.
 * Socket and event information is written to native memory only once during registration,
 * and subsequent polls only read the returned events (revents), significantly reducing
 * memory allocation overhead in high-frequency scenarios.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * try (Poller poller = new Poller()) {
 *     int idx1 = poller.register(socket1, PollEvents.IN);
 *     int idx2 = poller.register(socket2, PollEvents.IN);
 *
 *     while (running) {
 *         int count = poller.poll(Duration.ofMillis(100));
 *         if (count > 0) {
 *             if (poller.isReadable(idx1)) {
 *                 // Handle socket1
 *             }
 *             if (poller.isReadable(idx2)) {
 *                 // Handle socket2
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is NOT thread-safe. Each Poller instance should be used by a single thread.
 * If you need to poll from multiple threads, create separate Poller instances.</p>
 *
 * @see PollEvents
 * @see Socket
 * @since 1.0.0
 */
public class Poller implements AutoCloseable {

    /** Platform-specific poll item layout */
    private final StructLayout pollItemLayout;

    /** Size in bytes of a single poll item */
    private final long itemSize;

    /** Arena for native memory management */
    private final Arena arena;

    /** Native memory segment holding poll items array */
    private MemorySegment pollItems;

    /** Current capacity (number of items that can be stored) */
    private int capacity;

    /** Current size (number of registered items) */
    private int size;

    /** Flag indicating whether this poller is closed */
    private boolean closed;

    /** Flag indicating whether the platform is Windows */
    private final boolean isWindows;

    /** Default initial capacity */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * Creates a new Poller with default initial capacity.
     *
     * @throws IllegalStateException if native memory allocation fails
     */
    public Poller() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a new Poller with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity for number of sockets
     * @throws IllegalArgumentException if initialCapacity is less than 1
     * @throws IllegalStateException if native memory allocation fails
     */
    public Poller(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Initial capacity must be at least 1");
        }

        this.pollItemLayout = ZmqStructs.getPollItemLayout();
        this.itemSize = pollItemLayout.byteSize();
        this.arena = Arena.ofConfined();
        this.capacity = initialCapacity;
        this.size = 0;
        this.closed = false;
        this.isWindows = isWindowsPlatform();

        // Allocate initial native memory
        this.pollItems = arena.allocate(itemSize * capacity);
    }

    /**
     * Registers a socket for polling with the specified events.
     *
     * <p>The socket and event information is immediately written to native memory.
     * Subsequent poll operations will only update the returned events.</p>
     *
     * @param socket the socket to register
     * @param events the events to monitor (e.g., {@link PollEvents#IN}, {@link PollEvents#OUT})
     * @return the index of the registered socket, used for querying poll results
     * @throws IllegalArgumentException if socket is null or events is null
     * @throws IllegalStateException if this poller is closed
     */
    public int register(Socket socket, PollEvents events) {
        if (closed) {
            throw new IllegalStateException("Poller is closed");
        }
        if (socket == null) {
            throw new IllegalArgumentException("Socket cannot be null");
        }
        if (events == null) {
            throw new IllegalArgumentException("Events cannot be null");
        }

        // Grow if necessary
        if (size >= capacity) {
            grow();
        }

        int index = size++;
        writePollItem(index, socket, 0, events);

        return index;
    }

    /**
     * Registers a raw file descriptor for polling with the specified events.
     *
     * <p>This method allows polling of non-ZeroMQ file descriptors. Set socket to null
     * when using this method.</p>
     *
     * @param fd the file descriptor to register
     * @param events the events to monitor
     * @return the index of the registered file descriptor
     * @throws IllegalArgumentException if events is null
     * @throws IllegalStateException if this poller is closed
     */
    public int register(long fd, PollEvents events) {
        if (closed) {
            throw new IllegalStateException("Poller is closed");
        }
        if (events == null) {
            throw new IllegalArgumentException("Events cannot be null");
        }

        // Grow if necessary
        if (size >= capacity) {
            grow();
        }

        int index = size++;
        writePollItem(index, null, fd, events);

        return index;
    }

    /**
     * Unregisters a socket or file descriptor at the specified index.
     *
     * <p>This removes the item from the polling set. Indices of items after the
     * removed item will shift down by one. If you need stable indices, consider
     * using {@link #modify(int, PollEvents)} with {@link PollEvents#NONE} instead.</p>
     *
     * @param index the index of the item to unregister
     * @throws IllegalArgumentException if index is out of bounds
     * @throws IllegalStateException if this poller is closed
     */
    public void unregister(int index) {
        if (closed) {
            throw new IllegalStateException("Poller is closed");
        }
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }

        // Shift all subsequent items down
        if (index < size - 1) {
            long srcOffset = (index + 1) * itemSize;
            long dstOffset = index * itemSize;
            long bytesToCopy = (size - index - 1) * itemSize;

            MemorySegment.copy(pollItems, srcOffset, pollItems, dstOffset, bytesToCopy);
        }

        size--;
    }

    /**
     * Modifies the events to monitor for a registered socket.
     *
     * <p>This allows changing what events to wait for without unregistering
     * and re-registering the socket.</p>
     *
     * @param index the index of the socket to modify
     * @param events the new events to monitor
     * @throws IllegalArgumentException if index is out of bounds or events is null
     * @throws IllegalStateException if this poller is closed
     */
    public void modify(int index, PollEvents events) {
        if (closed) {
            throw new IllegalStateException("Poller is closed");
        }
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        if (events == null) {
            throw new IllegalArgumentException("Events cannot be null");
        }

        long offset = index * itemSize + ZmqStructs.PollItemOffsets.EVENTS;
        pollItems.set(JAVA_SHORT, offset, events.getValue());
    }

    /**
     * Polls all registered sockets for events with the specified timeout.
     *
     * <p>This method blocks until at least one event occurs on any registered socket,
     * or until the timeout expires. After a successful poll, use the query methods
     * ({@link #isReadable(int)}, {@link #isWritable(int)}, etc.) to check which
     * events occurred.</p>
     *
     * @param timeout the timeout in milliseconds (-1 for infinite, 0 for non-blocking)
     * @return the number of sockets with events, 0 on timeout, -1 on error
     * @throws IllegalStateException if this poller is closed
     */
    public int poll(long timeout) {
        if (closed) {
            throw new IllegalStateException("Poller is closed");
        }
        if (size == 0) {
            return 0;
        }

        // Call native poll - this only updates revents fields
        int result = LibZmq.poll(pollItems, size, timeout);

        // Return result (error handling is done by caller if needed)
        return result;
    }

    /**
     * Polls all registered sockets for events with the specified timeout.
     *
     * @param timeout the timeout duration (null for infinite)
     * @return the number of sockets with events, 0 on timeout, -1 on error
     * @throws IllegalStateException if this poller is closed
     */
    public int poll(Duration timeout) {
        if (timeout == null) {
            return poll(-1);
        }
        return poll(timeout.toMillis());
    }

    /**
     * Polls all registered sockets for events (infinite wait).
     *
     * @return the number of sockets with events, -1 on error
     * @throws IllegalStateException if this poller is closed
     */
    public int poll() {
        return poll(-1);
    }

    /**
     * Checks if the socket at the specified index is readable.
     *
     * <p>This method should be called after {@link #poll(long)} returns a positive value.</p>
     *
     * @param index the index of the socket to check
     * @return true if the socket has data ready to receive
     * @throws IllegalArgumentException if index is out of bounds
     * @throws IllegalStateException if this poller is closed
     */
    public boolean isReadable(int index) {
        return hasEvent(index, PollEvents.IN);
    }

    /**
     * Checks if the socket at the specified index is writable.
     *
     * <p>This method should be called after {@link #poll(long)} returns a positive value.</p>
     *
     * @param index the index of the socket to check
     * @return true if the socket can send messages without blocking
     * @throws IllegalArgumentException if index is out of bounds
     * @throws IllegalStateException if this poller is closed
     */
    public boolean isWritable(int index) {
        return hasEvent(index, PollEvents.OUT);
    }

    /**
     * Checks if the socket at the specified index has an error condition.
     *
     * <p>This method should be called after {@link #poll(long)} returns a positive value.</p>
     *
     * @param index the index of the socket to check
     * @return true if the socket has an error condition
     * @throws IllegalArgumentException if index is out of bounds
     * @throws IllegalStateException if this poller is closed
     */
    public boolean hasError(int index) {
        return hasEvent(index, PollEvents.ERR);
    }

    /**
     * Gets all returned events for the socket at the specified index.
     *
     * <p>This method should be called after {@link #poll(long)} returns a positive value.
     * It returns the actual events that occurred, which may be a combination of multiple
     * event types.</p>
     *
     * @param index the index of the socket to check
     * @return the returned events as a PollEvents value
     * @throws IllegalArgumentException if index is out of bounds
     * @throws IllegalStateException if this poller is closed
     */
    public PollEvents getReturnedEvents(int index) {
        if (closed) {
            throw new IllegalStateException("Poller is closed");
        }
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }

        long offset = index * itemSize + ZmqStructs.PollItemOffsets.REVENTS;
        short revents = pollItems.get(JAVA_SHORT, offset);
        return PollEvents.fromValue(revents);
    }

    /**
     * Returns the number of currently registered sockets.
     *
     * @return the number of registered sockets
     */
    public int size() {
        return size;
    }

    /**
     * Checks if this poller is closed.
     *
     * @return true if this poller has been closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Closes this poller and releases all associated native memory.
     *
     * <p>After calling this method, the poller cannot be used anymore.
     * This method is idempotent - calling it multiple times has no effect.</p>
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            arena.close();
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Writes a poll item to native memory at the specified index.
     */
    private void writePollItem(int index, Socket socket, long fd, PollEvents events) {
        long baseOffset = index * itemSize;

        // Set socket pointer
        MemorySegment socketHandle = (socket != null) ? socket.getHandle() : MemorySegment.NULL;
        pollItems.set(ADDRESS, baseOffset + ZmqStructs.PollItemOffsets.SOCKET, socketHandle);

        // Set file descriptor (platform-dependent)
        long fdOffset = baseOffset + ZmqStructs.PollItemOffsets.FD;
        if (isWindows) {
            pollItems.set(JAVA_LONG, fdOffset, fd);
        } else {
            pollItems.set(JAVA_INT, fdOffset, (int) fd);
        }

        // Set events
        long eventsOffset = baseOffset + ZmqStructs.PollItemOffsets.EVENTS;
        pollItems.set(JAVA_SHORT, eventsOffset, events.getValue());

        // Clear revents
        pollItems.set(JAVA_SHORT, eventsOffset + 2, (short) 0);
    }

    /**
     * Checks if a specific event occurred at the given index.
     */
    private boolean hasEvent(int index, PollEvents event) {
        if (closed) {
            throw new IllegalStateException("Poller is closed");
        }
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }

        long offset = index * itemSize + ZmqStructs.PollItemOffsets.REVENTS;
        short revents = pollItems.get(JAVA_SHORT, offset);
        return (revents & event.getValue()) != 0;
    }

    /**
     * Grows the internal capacity by doubling it.
     */
    private void grow() {
        int newCapacity = capacity * 2;
        MemorySegment newPollItems = arena.allocate(itemSize * newCapacity);

        // Copy existing data
        if (size > 0) {
            MemorySegment.copy(pollItems, 0, newPollItems, 0, size * itemSize);
        }

        this.pollItems = newPollItems;
        this.capacity = newCapacity;
    }

    /**
     * Checks if the current platform is Windows.
     */
    private static boolean isWindowsPlatform() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}
