package io.github.ulalax.zmq;

/**
 * Represents an item to poll for events.
 *
 * <p>Poll items are used with {@link Poller} to wait for events on sockets.
 * Each item specifies a socket and the events to wait for.</p>
 *
 * @see Poller
 * @see PollEvents
 */
public final class PollItem {

    private final Socket socket;
    private final long fileDescriptor;
    private final PollEvents events;
    private PollEvents returnedEvents;

    /**
     * Creates a poll item for a socket.
     * @param socket The socket to poll
     * @param events The events to wait for
     */
    public PollItem(Socket socket, PollEvents events) {
        if (socket == null) {
            throw new NullPointerException("socket cannot be null");
        }
        this.socket = socket;
        this.fileDescriptor = 0;
        this.events = events;
        this.returnedEvents = PollEvents.NONE;
    }

    /**
     * Creates a poll item for a file descriptor.
     * @param fd The file descriptor
     * @param events The events to wait for
     */
    public PollItem(long fd, PollEvents events) {
        this.socket = null;
        this.fileDescriptor = fd;
        this.events = events;
        this.returnedEvents = PollEvents.NONE;
    }

    Socket getSocket() {
        return socket;
    }

    long getFileDescriptor() {
        return fileDescriptor;
    }

    PollEvents getEvents() {
        return events;
    }

    void setReturnedEvents(PollEvents events) {
        this.returnedEvents = events;
    }

    /**
     * Gets the returned events.
     * @return Events that occurred
     */
    public PollEvents getReturnedEvents() {
        return returnedEvents;
    }

    /**
     * Checks if any events occurred.
     * @return true if events occurred
     */
    public boolean hasEvents() {
        return returnedEvents.getValue() != 0;
    }

    /**
     * Checks if socket is readable.
     * @return true if IN event occurred
     */
    public boolean isReadable() {
        return (returnedEvents.getValue() & PollEvents.IN.getValue()) != 0;
    }

    /**
     * Checks if socket is writable.
     * @return true if OUT event occurred
     */
    public boolean isWritable() {
        return (returnedEvents.getValue() & PollEvents.OUT.getValue()) != 0;
    }

    /**
     * Checks if socket has error.
     * @return true if ERR event occurred
     */
    public boolean hasError() {
        return (returnedEvents.getValue() & PollEvents.ERR.getValue()) != 0;
    }
}
