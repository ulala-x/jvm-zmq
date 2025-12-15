package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.*;
import java.time.Duration;

import static java.lang.foreign.ValueLayout.*;

/**
 * Polling utilities for ZeroMQ sockets.
 *
 * <p>The Poller allows you to wait for events on multiple sockets simultaneously.
 * This is useful for building event-driven applications.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * PollItem[] items = {
 *     new PollItem(socket1, PollEvents.IN),
 *     new PollItem(socket2, PollEvents.IN)
 * };
 * int count = Poller.poll(items, 1000);
 * if (items[0].isReadable()) {
 *     // socket1 has data
 * }
 * }</pre>
 *
 * @see PollItem
 * @see PollEvents
 */
public final class Poller {

    // Thread-local cache for single-socket polling
    private static final ThreadLocal<MemorySegment> SINGLE_ITEM_CACHE =
        ThreadLocal.withInitial(() -> {
            Arena arena = Arena.ofAuto();
            return arena.allocate(ZmqStructs.getPollItemLayout());
        });

    private Poller() {
        // Prevent instantiation
    }

    /**
     * Polls multiple sockets for events.
     * @param items The items to poll
     * @param timeout Timeout in milliseconds (-1 = infinite)
     * @return Number of items with events
     */
    public static int poll(PollItem[] items, long timeout) {
        if (items == null || items.length == 0) {
            return 0;
        }

        try (Arena arena = Arena.ofConfined()) {
            // Allocate poll items array
            StructLayout layout = ZmqStructs.getPollItemLayout();
            MemorySegment pollItems = arena.allocate(layout.byteSize() * items.length);

            // Fill poll items
            for (int i = 0; i < items.length; i++) {
                PollItem item = items[i];
                MemorySegment itemSeg = pollItems.asSlice(i * layout.byteSize(), layout);

                // Set socket
                itemSeg.set(ADDRESS, 0, item.getSocket() != null ?
                    item.getSocket().getHandle() : MemorySegment.NULL);

                // Set fd (platform-dependent)
                if (isWindows()) {
                    itemSeg.set(JAVA_LONG, ADDRESS.byteSize(), item.getFileDescriptor());
                } else {
                    itemSeg.set(JAVA_INT, ADDRESS.byteSize(), (int)item.getFileDescriptor());
                }

                // Set events
                long eventsOffset = ZmqStructs.PollItemOffsets.EVENTS;
                itemSeg.set(JAVA_SHORT, eventsOffset, (short)item.getEvents().getValue());

                // Clear revents
                itemSeg.set(JAVA_SHORT, eventsOffset + 2, (short)0);
            }

            // Perform poll
            int result = LibZmq.poll(pollItems, items.length, timeout);
            ZmqException.throwIfError(result);

            // Copy back returned events
            for (int i = 0; i < items.length; i++) {
                MemorySegment itemSeg = pollItems.asSlice(i * layout.byteSize(), layout);
                long eventsOffset = ZmqStructs.PollItemOffsets.EVENTS;
                short revents = itemSeg.get(JAVA_SHORT, eventsOffset + 2);
                items[i].setReturnedEvents(PollEvents.fromValue(revents));
            }

            return result;
        }
    }

    /**
     * Polls multiple sockets for events.
     * @param items The items to poll
     * @param timeout Timeout duration
     * @return Number of items with events
     */
    public static int poll(PollItem[] items, Duration timeout) {
        return poll(items, timeout.toMillis());
    }

    /**
     * Polls multiple sockets for events (infinite wait).
     * @param items The items to poll
     * @return Number of items with events
     */
    public static int poll(PollItem[] items) {
        return poll(items, -1);
    }

    /**
     * Polls a single socket for events.
     * @param socket The socket to poll
     * @param events The events to wait for
     * @param timeout Timeout in milliseconds
     * @return true if events occurred
     */
    public static boolean poll(Socket socket, PollEvents events, long timeout) {
        PollItem[] items = { new PollItem(socket, events) };
        int result = poll(items, timeout);
        return result > 0 && items[0].hasEvents();
    }

    /**
     * Polls a single socket for events.
     * @param socket The socket to poll
     * @param events The events to wait for
     * @param timeout Timeout duration
     * @return true if events occurred
     */
    public static boolean poll(Socket socket, PollEvents events, Duration timeout) {
        return poll(socket, events, timeout.toMillis());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
