package io.github.ulalax.zmq;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Result of a send operation following the cppzmq design pattern.
 *
 * <p>This class represents the outcome of a ZeroMQ send operation, distinguishing between
 * three possible states:</p>
 * <ul>
 *   <li><b>Success</b> - The send operation completed successfully and contains the number of bytes sent</li>
 *   <li><b>Would Block (EAGAIN)</b> - The operation would block in non-blocking mode (empty result, not an error)</li>
 *   <li><b>Error</b> - A real ZMQ error occurred (throws {@link io.github.ulalax.zmq.core.ZmqException})</li>
 * </ul>
 *
 * <p>This design makes non-blocking I/O more explicit and type-safe by clearly separating
 * the "would block" case from actual errors.</p>
 *
 * <p><b>Thread-Safety:</b> This class is immutable and thread-safe.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Check if send succeeded
 * SendResult result = socket.send(data, SendFlags.DONT_WAIT);
 * if (result.isPresent()) {
 *     System.out.println("Sent " + result.value() + " bytes");
 * } else {
 *     System.out.println("Would block - try again later");
 * }
 *
 * // Example 2: Functional style with ifPresent
 * socket.send(data, SendFlags.DONT_WAIT)
 *     .ifPresent(bytes -> System.out.println("Sent: " + bytes));
 *
 * // Example 3: Get value with default
 * int bytesSent = result.orElse(0);
 *
 * // Example 4: Throw if would block
 * try {
 *     int bytes = result.orElseThrow();
 *     processSuccess(bytes);
 * } catch (NoSuchElementException e) {
 *     handleWouldBlock();
 * }
 *
 * // Example 5: Distinguish would-block from success
 * if (result.wouldBlock()) {
 *     // Queue message for retry
 *     retryQueue.add(data);
 * } else if (result.isPresent()) {
 *     // Success - log metrics
 *     metrics.recordSend(result.value());
 * }
 * }</pre>
 *
 * @see RecvResult
 * @see Socket#send(byte[], SendFlags)
 * @see SendFlags#DONT_WAIT
 */
public final class SendResult {

    private final Integer value;

    /**
     * Private constructor to enforce factory method usage.
     *
     * @param value the number of bytes sent, or null if would block
     */
    private SendResult(Integer value) {
        this.value = value;
    }

    /**
     * Creates a successful send result containing the number of bytes sent.
     *
     * @param bytesSent the number of bytes successfully sent (must be non-negative)
     * @return a SendResult containing the bytes sent
     * @throws IllegalArgumentException if bytesSent is negative
     */
    public static SendResult success(int bytesSent) {
        if (bytesSent < 0) {
            throw new IllegalArgumentException("bytesSent must be non-negative: " + bytesSent);
        }
        return new SendResult(bytesSent);
    }

    /**
     * Creates a "would block" result representing EAGAIN in non-blocking mode.
     *
     * <p>This is not an error condition but indicates that the operation cannot complete
     * immediately without blocking. The caller should typically retry the operation later
     * or perform other work.</p>
     *
     * @return an empty SendResult indicating would-block (EAGAIN)
     */
    public static SendResult empty() {
        return new SendResult(null);
    }

    /**
     * Checks if the send operation was successful and contains a value.
     *
     * @return true if the send succeeded, false if it would block
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Checks if the send operation would have blocked (EAGAIN).
     *
     * <p>This is the opposite of {@link #isPresent()} and is provided for clarity
     * when explicitly checking for the would-block condition.</p>
     *
     * @return true if the operation would block, false if it succeeded
     */
    public boolean wouldBlock() {
        return value == null;
    }

    /**
     * Returns the number of bytes sent.
     *
     * @return the number of bytes sent
     * @throws NoSuchElementException if the result is empty (would block)
     */
    public int value() {
        if (value == null) {
            throw new NoSuchElementException("SendResult is empty (would block)");
        }
        return value;
    }

    /**
     * Returns the number of bytes sent, or a default value if the result is empty.
     *
     * @param defaultValue the value to return if the result is empty
     * @return the number of bytes sent if present, otherwise defaultValue
     */
    public int orElse(int defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the number of bytes sent if present, otherwise throws {@link NoSuchElementException}.
     *
     * <p>This method is useful when the would-block case should be treated as an exceptional
     * condition in the current context.</p>
     *
     * @return the number of bytes sent
     * @throws NoSuchElementException if the result is empty (would block)
     */
    public int orElseThrow() {
        return value();
    }

    /**
     * Executes the given action with the number of bytes sent if the result is present.
     *
     * <p>This enables a functional programming style for handling successful sends:</p>
     * <pre>{@code
     * socket.send(data, SendFlags.DONT_WAIT)
     *     .ifPresent(bytes -> logger.info("Sent {} bytes", bytes));
     * }</pre>
     *
     * @param action the action to execute with the bytes sent value
     * @throws NullPointerException if action is null and the result is present
     */
    public void ifPresent(Consumer<Integer> action) {
        if (value != null) {
            Objects.requireNonNull(action, "action cannot be null").accept(value);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * <p>Two SendResult objects are equal if they have the same presence state and,
     * if present, the same value.</p>
     *
     * @param obj the reference object with which to compare
     * @return true if this object is equal to the obj argument, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SendResult that = (SendResult) obj;
        return Objects.equals(value, that.value);
    }

    /**
     * Returns a hash code value for this SendResult.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * Returns a string representation of this SendResult.
     *
     * <p>The format is:</p>
     * <ul>
     *   <li>"SendResult[bytes=N]" for successful sends</li>
     *   <li>"SendResult[WOULD_BLOCK]" for would-block results</li>
     * </ul>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return value != null
            ? String.format("SendResult[bytes=%d]", value)
            : "SendResult[WOULD_BLOCK]";
    }
}
