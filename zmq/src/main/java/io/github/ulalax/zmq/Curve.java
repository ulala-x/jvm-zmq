package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

/**
 * CURVE encryption key utilities.
 * Provides methods for generating and managing CURVE keypairs.
 *
 * <p>CURVE is ZeroMQ's built-in security mechanism based on CurveZMQ.
 * It provides authentication and encryption for socket connections.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * // Check if CURVE is available
 * if (!Context.has("curve")) {
 *     System.err.println("CURVE not available");
 *     return;
 * }
 *
 * // Generate keypair
 * Curve.KeyPair serverKeys = Curve.generateKeypair();
 * Curve.KeyPair clientKeys = Curve.generateKeypair();
 *
 * // Server setup
 * Socket server = new Socket(context, SocketType.REP);
 * server.setOption(SocketOption.CURVE_SERVER, 1);
 * server.setOption(SocketOption.CURVE_SECRETKEY, serverKeys.secretKey());
 * server.bind("tcp://*:5555");
 *
 * // Client setup
 * Socket client = new Socket(context, SocketType.REQ);
 * client.setOption(SocketOption.CURVE_SERVERKEY, serverKeys.publicKey());
 * client.setOption(SocketOption.CURVE_PUBLICKEY, clientKeys.publicKey());
 * client.setOption(SocketOption.CURVE_SECRETKEY, clientKeys.secretKey());
 * client.connect("tcp://localhost:5555");
 * }</pre>
 *
 * @see Context#has(String)
 * @see SocketOption#CURVE_SERVER
 * @see SocketOption#CURVE_PUBLICKEY
 * @see SocketOption#CURVE_SECRETKEY
 * @see SocketOption#CURVE_SERVERKEY
 */
public final class Curve {

    /**
     * Size of CURVE key in binary format (32 bytes).
     */
    public static final int KEY_SIZE = 32;

    /**
     * Size of CURVE key in Z85 format (40 characters + null terminator).
     */
    public static final int Z85_KEY_SIZE = 41;

    private Curve() {
        // Prevent instantiation
    }

    /**
     * Generates a new CURVE keypair.
     *
     * <p>The keys are returned in Z85 format (printable ASCII strings).
     * Each key is 40 characters long.</p>
     *
     * @return A KeyPair containing the public and secret keys
     * @throws UnsupportedOperationException if CURVE is not available
     * @throws ZmqException if keypair generation fails
     */
    public static KeyPair generateKeypair() {
        if (!Context.has("curve")) {
            throw new UnsupportedOperationException("CURVE security is not available");
        }

        try (Arena arena = Arena.ofConfined()) {
            // Allocate buffers for Z85-encoded keys (41 bytes each including null terminator)
            MemorySegment publicKey = arena.allocate(Z85_KEY_SIZE);
            MemorySegment secretKey = arena.allocate(Z85_KEY_SIZE);

            // Generate keypair
            int result = LibZmq.curveKeypair(publicKey, secretKey);
            ZmqException.throwIfError(result);

            // Convert to Java strings (null-terminated C strings)
            String publicKeyStr = publicKey.getUtf8String(0);
            String secretKeyStr = secretKey.getUtf8String(0);

            return new KeyPair(publicKeyStr, secretKeyStr);
        }
    }

    /**
     * Derives the public key from a secret key.
     *
     * <p>This is useful when you have stored only the secret key
     * and need to compute the corresponding public key.</p>
     *
     * @param secretKey The secret key in Z85 format (40 characters)
     * @return The corresponding public key in Z85 format
     * @throws NullPointerException if secretKey is null
     * @throws IllegalArgumentException if secretKey format is invalid
     * @throws UnsupportedOperationException if CURVE is not available
     * @throws ZmqException if public key derivation fails
     */
    public static String derivePublicKey(String secretKey) {
        if (secretKey == null) {
            throw new NullPointerException("secretKey cannot be null");
        }
        if (secretKey.length() != Z85_KEY_SIZE - 1) {
            throw new IllegalArgumentException(
                "secretKey must be " + (Z85_KEY_SIZE - 1) + " characters in Z85 format");
        }
        if (!Context.has("curve")) {
            throw new UnsupportedOperationException("CURVE security is not available");
        }

        try (Arena arena = Arena.ofConfined()) {
            // Allocate buffers
            MemorySegment publicKey = arena.allocate(Z85_KEY_SIZE);
            MemorySegment secretKeySeg = arena.allocateUtf8String(secretKey);

            // Derive public key
            int result = LibZmq.curvePublic(publicKey, secretKeySeg);
            ZmqException.throwIfError(result);

            // Convert to Java string
            return publicKey.getUtf8String(0);
        }
    }

    /**
     * Validates that a key is in correct Z85 format.
     *
     * @param key The key to validate
     * @return true if the key is valid Z85 format
     */
    public static boolean isValidKey(String key) {
        if (key == null || key.length() != Z85_KEY_SIZE - 1) {
            return false;
        }

        // Z85 alphabet
        String z85Chars = "0123456789" +
                         "abcdefghijklmnopqrstuvwxyz" +
                         "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                         ".-:+=^!/*?&<>()[]{}@%$#";

        for (int i = 0; i < key.length(); i++) {
            if (z85Chars.indexOf(key.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }

    /**
     * Represents a CURVE keypair.
     *
     * @param publicKey The public key in Z85 format (40 characters)
     * @param secretKey The secret key in Z85 format (40 characters)
     */
    public record KeyPair(String publicKey, String secretKey) {
        public KeyPair {
            if (publicKey == null) {
                throw new NullPointerException("publicKey cannot be null");
            }
            if (secretKey == null) {
                throw new NullPointerException("secretKey cannot be null");
            }
            if (publicKey.length() != Z85_KEY_SIZE - 1) {
                throw new IllegalArgumentException("publicKey must be 40 characters");
            }
            if (secretKey.length() != Z85_KEY_SIZE - 1) {
                throw new IllegalArgumentException("secretKey must be 40 characters");
            }
        }

        @Override
        public String toString() {
            // Don't expose secret key in toString
            return "KeyPair{publicKey='" + publicKey + "', secretKey='***'}";
        }
    }
}
