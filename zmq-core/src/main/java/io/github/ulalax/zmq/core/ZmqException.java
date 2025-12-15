package io.github.ulalax.zmq.core;

import java.lang.foreign.MemorySegment;

/**
 * Exception thrown when a ZeroMQ operation fails.
 * <p>
 * This exception wraps error information from libzmq, including the error number (errno)
 * and a descriptive error message. It extends {@link RuntimeException} so it does not
 * need to be explicitly caught.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * try {
 *     int rc = LibZmq.bind(socket, "tcp://*:5555");
 *     ZmqException.throwIfError(rc);
 * } catch (ZmqException e) {
 *     if (e.isAgain()) {
 *         // Would block, retry later
 *     } else if (e.isTerminated()) {
 *         // Context was terminated
 *     } else {
 *         System.err.println("Error: " + e.getMessage());
 *     }
 * }
 * }</pre>
 *
 * <h2>Common Error Types:</h2>
 * <ul>
 *   <li>{@link #isAgain()} - Operation would block (EAGAIN)</li>
 *   <li>{@link #isTerminated()} - Context was terminated (ETERM)</li>
 *   <li>{@link #isInterrupted()} - Operation was interrupted (EINTR)</li>
 * </ul>
 *
 * @see LibZmq
 * @see ZmqConstants
 * @since 1.0.0
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
