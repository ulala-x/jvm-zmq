package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 * Z85 encoding/decoding utilities.
 *
 * <p>Z85 is a string encoding format that represents binary data
 * using 85 printable ASCII characters. It's more compact than Base64,
 * encoding 4 bytes into 5 characters (80% overhead vs 33% for Base64).</p>
 *
 * <p><strong>Important constraints:</strong></p>
 * <ul>
 *   <li>Input data for encoding must have length divisible by 4</li>
 *   <li>Input string for decoding must have length divisible by 5</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Encoding
 * byte[] data = {0x12, 0x34, 0x56, 0x78}; // Must be divisible by 4
 * String encoded = Z85.encode(data);
 * System.out.println(encoded); // Prints 5-character string
 *
 * // Decoding
 * byte[] decoded = Z85.decode(encoded);
 * // decoded equals data
 * }</pre>
 *
 * @see Curve
 */
public final class Z85 {

    /**
     * Z85 alphabet (85 printable ASCII characters).
     */
    private static final String Z85_ALPHABET =
        "0123456789" +
        "abcdefghijklmnopqrstuvwxyz" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        ".-:+=^!/*?&<>()[]{}@%$#";

    private Z85() {
        // Prevent instantiation
    }

    /**
     * Encodes binary data to Z85 string.
     *
     * <p>The input data length must be divisible by 4.
     * The output string length will be (input_length * 5 / 4).</p>
     *
     * @param data The data to encode (length must be divisible by 4)
     * @return The Z85-encoded string
     * @throws NullPointerException if data is null
     * @throws IllegalArgumentException if data length is not divisible by 4
     * @throws ZmqException if encoding fails
     */
    public static String encode(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data cannot be null");
        }
        if (data.length % 4 != 0) {
            throw new IllegalArgumentException(
                "Data length must be divisible by 4, got " + data.length);
        }

        if (data.length == 0) {
            return "";
        }

        try (Arena arena = Arena.ofConfined()) {
            // Calculate output size
            int outputSize = data.length * 5 / 4 + 1; // +1 for null terminator

            // Allocate buffers
            MemorySegment dataSeg = arena.allocate(data.length);
            MemorySegment.copy(data, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, data.length);
            MemorySegment destSeg = arena.allocate(outputSize);

            // Encode
            MemorySegment result = LibZmq.z85Encode(destSeg, dataSeg, data.length);
            if (result.address() == 0) {
                throw new ZmqException(-1, "Z85 encoding failed");
            }

            // Convert to Java string (null-terminated)
            return destSeg.getUtf8String(0);
        }
    }

    /**
     * Decodes Z85 string to binary data.
     *
     * <p>The input string length must be divisible by 5.
     * The output data length will be (input_length * 4 / 5).</p>
     *
     * @param encoded The Z85-encoded string (length must be divisible by 5)
     * @return The decoded binary data
     * @throws NullPointerException if encoded is null
     * @throws IllegalArgumentException if string length is not divisible by 5
     * @throws ZmqException if decoding fails (e.g., invalid characters)
     */
    public static byte[] decode(String encoded) {
        if (encoded == null) {
            throw new NullPointerException("encoded cannot be null");
        }
        if (encoded.length() % 5 != 0) {
            throw new IllegalArgumentException(
                "Encoded string length must be divisible by 5, got " + encoded.length());
        }

        if (encoded.isEmpty()) {
            return new byte[0];
        }

        try (Arena arena = Arena.ofConfined()) {
            // Calculate output size
            int outputSize = encoded.length() * 4 / 5;

            // Allocate buffers
            MemorySegment encodedSeg = arena.allocateUtf8String(encoded);
            MemorySegment destSeg = arena.allocate(outputSize);

            // Decode
            MemorySegment result = LibZmq.z85Decode(destSeg, encodedSeg);
            if (result.address() == 0) {
                throw new ZmqException(-1, "Z85 decoding failed - invalid input");
            }

            // Copy to byte array
            byte[] output = new byte[outputSize];
            MemorySegment.copy(destSeg, ValueLayout.JAVA_BYTE, 0, output, 0, outputSize);
            return output;
        }
    }

    /**
     * Validates that a string is valid Z85 encoding.
     *
     * <p>Checks that:</p>
     * <ul>
     *   <li>Length is divisible by 5</li>
     *   <li>All characters are in the Z85 alphabet</li>
     * </ul>
     *
     * @param encoded The string to validate
     * @return true if the string is valid Z85
     */
    public static boolean isValid(String encoded) {
        if (encoded == null || encoded.length() % 5 != 0) {
            return false;
        }

        for (int i = 0; i < encoded.length(); i++) {
            if (Z85_ALPHABET.indexOf(encoded.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Pads data to be divisible by 4 (for encoding).
     *
     * <p>Adds zero bytes to the end to make length divisible by 4.
     * Returns the original data if already correctly sized.</p>
     *
     * @param data The data to pad
     * @return Padded data and the number of padding bytes added
     */
    public static PaddedData padForEncoding(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data cannot be null");
        }

        int remainder = data.length % 4;
        if (remainder == 0) {
            return new PaddedData(data, 0);
        }

        int paddingSize = 4 - remainder;
        byte[] padded = new byte[data.length + paddingSize];
        System.arraycopy(data, 0, padded, 0, data.length);
        // Rest is zero-filled by default

        return new PaddedData(padded, paddingSize);
    }

    /**
     * Removes padding from decoded data.
     *
     * @param data The decoded data
     * @param paddingSize The number of padding bytes to remove
     * @return Data with padding removed
     */
    public static byte[] removePadding(byte[] data, int paddingSize) {
        if (paddingSize == 0) {
            return data;
        }
        if (paddingSize < 0 || paddingSize >= data.length) {
            throw new IllegalArgumentException("Invalid padding size: " + paddingSize);
        }

        byte[] result = new byte[data.length - paddingSize];
        System.arraycopy(data, 0, result, 0, result.length);
        return result;
    }

    /**
     * Represents data with padding information.
     *
     * @param data The padded data
     * @param paddingSize The number of padding bytes added
     */
    public record PaddedData(byte[] data, int paddingSize) {
        public PaddedData {
            if (data == null) {
                throw new NullPointerException("data cannot be null");
            }
            if (paddingSize < 0) {
                throw new IllegalArgumentException("paddingSize cannot be negative");
            }
        }
    }
}
