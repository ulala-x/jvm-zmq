package io.github.ulalax.zmq;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic result of a receive operation following the cppzmq design pattern.
 *
 * <p>This class represents the outcome of a ZeroMQ receive operation, distinguishing between
 * three possible states:</p>
 * <ul>
 *   <li><b>Success</b> - The receive operation completed successfully and contains the received value</li>
 *   <li><b>Would Block (EAGAIN)</b> - The operation would block in non-blocking mode (empty result, not an error)</li>
 *   <li><b>Error</b> - A real ZMQ error occurred (throws {@link io.github.ulalax.zmq.core.ZmqException})</li>
 * </ul>
 *
 * <p>This design makes non-blocking I/O more explicit and type-safe by clearly separating
 * the "would block" case from actual errors. The generic type parameter allows type-safe
 * handling of different receive results (byte arrays, strings, multipart messages, etc.).</p>
 *
 * <p><b>Thread-Safety:</b> This class is immutable and thread-safe.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Example 1: Receive bytes
 * RecvResult<byte[]> result = socket.recvBytes(RecvFlags.DONT_WAIT);
 * if (result.isPresent()) {
 *     byte[] data = result.value();
 *     System.out.println("Received " + data.length + " bytes");
 * }
 *
 * // Example 2: Receive string with functional style
 * socket.recvString(RecvFlags.DONT_WAIT)
 *     .ifPresent(msg -> System.out.println("Message: " + msg));
 *
 * // Example 3: Transform result with map
 * RecvResult<Integer> length = socket.recvBytes(RecvFlags.DONT_WAIT)
 *     .map(bytes -> bytes.length);
 * length.ifPresent(len -> System.out.println("Length: " + len));
 *
 * // Example 4: Get value with default
 * String msg = socket.recvString(RecvFlags.DONT_WAIT)
 *     .orElse("No message");
 *
 * // Example 5: Receive integer (bytes received)
 * RecvResult<Integer> bytesRecv = socket.recv(buffer, RecvFlags.DONT_WAIT);
 * if (bytesRecv.wouldBlock()) {
 *     // No data available - retry later
 *     scheduler.scheduleRetry();
 * } else {
 *     int bytes = bytesRecv.value();
 *     processBuffer(buffer, bytes);
 * }
 *
 * // Example 6: Chain transformations
 * socket.recvString(RecvFlags.DONT_WAIT)
 *     .map(String::toUpperCase)
 *     .map(String::trim)
 *     .ifPresent(this::processMessage);
 * }</pre>
 *
 * @param <T> the type of the received value
 * @see SendResult
 * @see Socket#recv(byte[], RecvFlags)
 * @see Socket#recvString(RecvFlags)
 * @see Socket#recvBytes(RecvFlags)
 * @see RecvFlags#DONT_WAIT
 */
public final class RecvResult<T> {

    private final T value;

    /**
     * Private constructor to enforce factory method usage.
     *
     * @param value the received value, or null if would block
     */
    private RecvResult(T value) {
        this.value = value;
    }

    /**
     * Creates a successful receive result containing the received value.
     *
     * @param <T> the type of the received value
     * @param value the received value (must not be null)
     * @return a RecvResult containing the received value
     * @throws NullPointerException if value is null
     */
    public static <T> RecvResult<T> success(T value) {
        Objects.requireNonNull(value, "value cannot be null for success result");
        return new RecvResult<>(value);
    }

    /**
     * Creates a "would block" result representing EAGAIN in non-blocking mode.
     *
     * <p>This is not an error condition but indicates that the operation cannot complete
     * immediately without blocking. The caller should typically retry the operation later
     * or perform other work.</p>
     *
     * @param <T> the expected type of the receive value
     * @return an empty RecvResult indicating would-block (EAGAIN)
     */
    public static <T> RecvResult<T> empty() {
        return new RecvResult<>(null);
    }

    /**
     * Checks if the receive operation was successful and contains a value.
     *
     * @return true if the receive succeeded, false if it would block
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * Checks if the receive operation would have blocked (EAGAIN).
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
     * Returns the received value.
     *
     * @return the received value
     * @throws NoSuchElementException if the result is empty (would block)
     */
    public T value() {
        if (value == null) {
            throw new NoSuchElementException("RecvResult is empty (would block)");
        }
        return value;
    }

    /**
     * Returns the received value, or a default value if the result is empty.
     *
     * @param defaultValue the value to return if the result is empty
     * @return the received value if present, otherwise defaultValue
     */
    public T orElse(T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * Returns the received value if present, otherwise throws {@link NoSuchElementException}.
     *
     * <p>This method is useful when the would-block case should be treated as an exceptional
     * condition in the current context.</p>
     *
     * @return the received value
     * @throws NoSuchElementException if the result is empty (would block)
     */
    public T orElseThrow() {
        return value();
    }

    /**
     * Executes the given action with the received value if the result is present.
     *
     * <p>This enables a functional programming style for handling successful receives:</p>
     * <pre>{@code
     * socket.recvString(RecvFlags.DONT_WAIT)
     *     .ifPresent(msg -> logger.info("Received: {}", msg));
     * }</pre>
     *
     * @param action the action to execute with the received value
     * @throws NullPointerException if action is null and the result is present
     */
    public void ifPresent(Consumer<? super T> action) {
        if (value != null) {
            Objects.requireNonNull(action, "action cannot be null").accept(value);
        }
    }

    /**
     * Transforms the received value using the given mapping function if present.
     *
     * <p>This method allows chaining transformations on the received value in a type-safe manner.
     * If the result is empty (would block), the mapping function is not applied and an empty
     * result of the target type is returned.</p>
     *
     * <p><b>Examples:</b></p>
     * <pre>{@code
     * // Transform byte array to length
     * RecvResult<Integer> length = socket.recvBytes(RecvFlags.DONT_WAIT)
     *     .map(bytes -> bytes.length);
     *
     * // Transform string to uppercase
     * RecvResult<String> upper = socket.recvString(RecvFlags.DONT_WAIT)
     *     .map(String::toUpperCase);
     *
     * // Chain multiple transformations
     * socket.recvString(RecvFlags.DONT_WAIT)
     *     .map(String::trim)
     *     .map(Integer::parseInt)
     *     .ifPresent(num -> System.out.println("Number: " + num));
     * }</pre>
     *
     * @param <U> the type of the result of the mapping function
     * @param mapper the mapping function to apply to the value
     * @return a RecvResult containing the transformed value if present, otherwise an empty RecvResult
     * @throws NullPointerException if mapper is null and the result is present
     * @throws NullPointerException if mapper returns null
     */
    public <U> RecvResult<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        if (value == null) {
            return empty();
        }
        U mapped = mapper.apply(value);
        Objects.requireNonNull(mapped, "mapper must not return null");
        return success(mapped);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * <p>Two RecvResult objects are equal if they have the same presence state and,
     * if present, the same value according to {@link Objects#equals(Object, Object)}.</p>
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
        RecvResult<?> that = (RecvResult<?>) obj;
        return Objects.equals(value, that.value);
    }

    /**
     * Returns a hash code value for this RecvResult.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    /**
     * Returns a string representation of this RecvResult.
     *
     * <p>The format is:</p>
     * <ul>
     *   <li>"RecvResult[value]" for successful receives (where value is the string representation of T)</li>
     *   <li>"RecvResult[WOULD_BLOCK]" for would-block results</li>
     * </ul>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return value != null
            ? String.format("RecvResult[%s]", value)
            : "RecvResult[WOULD_BLOCK]";
    }
}
