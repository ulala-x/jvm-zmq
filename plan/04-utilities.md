# Utilities - Additional Features

This document provides complete implementation of utility classes for ZeroMQ including CURVE encryption, Z85 encoding, and proxy functionality.

---

## Table of Contents
1. [Curve.java - CURVE Key Management](#curvejava---curve-key-management)
2. [Z85.java - Z85 Encoding/Decoding](#z85java---z85-encodingdecoding)
3. [Proxy.java - Proxy Functionality](#proxyjava---proxy-functionality)

---

## Curve.java - CURVE Key Management

CURVE encryption key generation and management utilities.

**Package**: `io.github.ulalax.zmq`

### Full Implementation

```java
package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
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
 * KeyPair serverKeys = Curve.generateKeypair();
 * KeyPair clientKeys = Curve.generateKeypair();
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
            String publicKeyStr = publicKey.getString(0, StandardCharsets.US_ASCII);
            String secretKeyStr = secretKey.getString(0, StandardCharsets.US_ASCII);

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
            MemorySegment secretKeySeg = arena.allocateFrom(secretKey, StandardCharsets.US_ASCII);

            // Derive public key
            int result = LibZmq.curvePublic(publicKey, secretKeySeg);
            ZmqException.throwIfError(result);

            // Convert to Java string
            return publicKey.getString(0, StandardCharsets.US_ASCII);
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
```

---

## Z85.java - Z85 Encoding/Decoding

Z85 encoding/decoding utilities for binary data.

**Package**: `io.github.ulalax.zmq`

### Full Implementation

```java
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
            MemorySegment dataSeg = arena.allocateFrom(ValueLayout.JAVA_BYTE, data);
            MemorySegment destSeg = arena.allocate(outputSize);

            // Encode
            MemorySegment result = LibZmq.z85Encode(destSeg, dataSeg, data.length);
            if (result.address() == 0) {
                throw new ZmqException(-1, "Z85 encoding failed");
            }

            // Convert to Java string (null-terminated)
            return destSeg.getString(0, StandardCharsets.US_ASCII);
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
            MemorySegment encodedSeg = arena.allocateFrom(encoded, StandardCharsets.US_ASCII);
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
```

---

## Proxy.java - Proxy Functionality

ZeroMQ proxy utilities for building message brokers and forwarders.

**Package**: `io.github.ulalax.zmq`

### Full Implementation

```java
package io.github.ulalax.zmq;

import io.github.ulalax.zmq.core.*;
import java.lang.foreign.MemorySegment;

/**
 * ZeroMQ proxy utilities.
 *
 * <p>A proxy connects a frontend socket to a backend socket, forwarding
 * messages between them. This is useful for building message brokers,
 * queue devices, and forwarders.</p>
 *
 * <p><strong>Common proxy patterns:</strong></p>
 * <ul>
 *   <li>Queue device: ROUTER (frontend) to DEALER (backend)</li>
 *   <li>Forwarder: XSUB (frontend) to XPUB (backend)</li>
 *   <li>Streamer: PULL (frontend) to PUSH (backend)</li>
 * </ul>
 *
 * <p>Basic proxy usage:</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket frontend = new Socket(ctx, SocketType.ROUTER);
 *      Socket backend = new Socket(ctx, SocketType.DEALER)) {
 *
 *     frontend.bind("tcp://*:5555");
 *     backend.bind("tcp://*:5556");
 *
 *     // This blocks until context is terminated
 *     Proxy.start(frontend, backend);
 * }
 * }</pre>
 *
 * <p>Steerable proxy usage (with control):</p>
 * <pre>{@code
 * try (Context ctx = new Context();
 *      Socket frontend = new Socket(ctx, SocketType.ROUTER);
 *      Socket backend = new Socket(ctx, SocketType.DEALER);
 *      Socket control = new Socket(ctx, SocketType.PAIR)) {
 *
 *     frontend.bind("tcp://*:5555");
 *     backend.bind("tcp://*:5556");
 *     control.bind("inproc://control");
 *
 *     // In another thread:
 *     // control.send("PAUSE");    // Pause message flow
 *     // control.send("RESUME");   // Resume message flow
 *     // control.send("TERMINATE"); // Terminate proxy
 *
 *     Proxy.startSteerable(frontend, backend, control);
 * }
 * }</pre>
 *
 * @see Socket
 * @see SocketType
 */
public final class Proxy {

    private Proxy() {
        // Prevent instantiation
    }

    /**
     * Starts a basic proxy.
     *
     * <p>This method blocks until the context is terminated or an error occurs.
     * Messages are forwarded from frontend to backend and vice versa.</p>
     *
     * <p>If a capture socket is provided, all messages passing through
     * the proxy are also sent to the capture socket for monitoring/logging.</p>
     *
     * @param frontend The frontend socket (e.g., ROUTER, XSUB, PULL)
     * @param backend The backend socket (e.g., DEALER, XPUB, PUSH)
     * @param capture Optional capture socket for monitoring (null for none)
     * @throws NullPointerException if frontend or backend is null
     * @throws ZmqException if the proxy fails
     *
     * @see #startSteerable(Socket, Socket, Socket, Socket)
     */
    public static void start(Socket frontend, Socket backend, Socket capture) {
        if (frontend == null) {
            throw new NullPointerException("frontend cannot be null");
        }
        if (backend == null) {
            throw new NullPointerException("backend cannot be null");
        }

        MemorySegment frontendHandle = frontend.getHandle();
        MemorySegment backendHandle = backend.getHandle();
        MemorySegment captureHandle = capture != null ?
            capture.getHandle() : MemorySegment.NULL;

        int result = LibZmq.proxy(frontendHandle, backendHandle, captureHandle);
        ZmqException.throwIfError(result);
    }

    /**
     * Starts a basic proxy without capture.
     *
     * @param frontend The frontend socket
     * @param backend The backend socket
     * @throws NullPointerException if frontend or backend is null
     * @throws ZmqException if the proxy fails
     */
    public static void start(Socket frontend, Socket backend) {
        start(frontend, backend, null);
    }

    /**
     * Starts a steerable proxy.
     *
     * <p>A steerable proxy can be controlled via the control socket.
     * The control socket must be a PAIR, PUB, or SUB socket.</p>
     *
     * <p><strong>Control commands:</strong></p>
     * <ul>
     *   <li><code>PAUSE</code> - Pause message flow (messages are queued)</li>
     *   <li><code>RESUME</code> - Resume message flow</li>
     *   <li><code>TERMINATE</code> - Terminate the proxy</li>
     *   <li><code>STATISTICS</code> - Request statistics (proxy sends back 8 uint64 values)</li>
     * </ul>
     *
     * <p>Statistics format (8 uint64 values):</p>
     * <ol>
     *   <li>Messages received on frontend</li>
     *   <li>Bytes received on frontend</li>
     *   <li>Messages sent to backend</li>
     *   <li>Bytes sent to backend</li>
     *   <li>Messages received on backend</li>
     *   <li>Bytes received on backend</li>
     *   <li>Messages sent to frontend</li>
     *   <li>Bytes sent to frontend</li>
     * </ol>
     *
     * <p>Example control pattern:</p>
     * <pre>{@code
     * // In main thread:
     * Socket control = new Socket(ctx, SocketType.PAIR);
     * control.bind("inproc://proxy-control");
     * Proxy.startSteerable(frontend, backend, control);
     *
     * // In control thread:
     * Socket controller = new Socket(ctx, SocketType.PAIR);
     * controller.connect("inproc://proxy-control");
     * controller.send("PAUSE");
     * Thread.sleep(1000);
     * controller.send("RESUME");
     * controller.send("TERMINATE");
     * }</pre>
     *
     * @param frontend The frontend socket
     * @param backend The backend socket
     * @param control The control socket (PAIR, PUB, or SUB)
     * @param capture Optional capture socket for monitoring (null for none)
     * @throws NullPointerException if frontend, backend, or control is null
     * @throws ZmqException if the proxy fails
     */
    public static void startSteerable(Socket frontend, Socket backend,
                                     Socket control, Socket capture) {
        if (frontend == null) {
            throw new NullPointerException("frontend cannot be null");
        }
        if (backend == null) {
            throw new NullPointerException("backend cannot be null");
        }
        if (control == null) {
            throw new NullPointerException("control cannot be null");
        }

        MemorySegment frontendHandle = frontend.getHandle();
        MemorySegment backendHandle = backend.getHandle();
        MemorySegment captureHandle = capture != null ?
            capture.getHandle() : MemorySegment.NULL;
        MemorySegment controlHandle = control.getHandle();

        int result = LibZmq.proxySteerable(
            frontendHandle, backendHandle, captureHandle, controlHandle);
        ZmqException.throwIfError(result);
    }

    /**
     * Starts a steerable proxy without capture.
     *
     * @param frontend The frontend socket
     * @param backend The backend socket
     * @param control The control socket
     * @throws NullPointerException if any parameter is null
     * @throws ZmqException if the proxy fails
     */
    public static void startSteerable(Socket frontend, Socket backend, Socket control) {
        startSteerable(frontend, backend, control, null);
    }

    /**
     * Proxy control commands for steerable proxies.
     */
    public static final class Commands {
        /** Pause message flow (messages are queued) */
        public static final String PAUSE = "PAUSE";

        /** Resume message flow */
        public static final String RESUME = "RESUME";

        /** Terminate the proxy */
        public static final String TERMINATE = "TERMINATE";

        /** Request statistics */
        public static final String STATISTICS = "STATISTICS";

        private Commands() {
            // Prevent instantiation
        }
    }

    /**
     * Proxy statistics returned by STATISTICS command.
     */
    public static class Statistics {
        private final long frontendMessagesReceived;
        private final long frontendBytesReceived;
        private final long backendMessagesSent;
        private final long backendBytesSent;
        private final long backendMessagesReceived;
        private final long backendBytesReceived;
        private final long frontendMessagesSent;
        private final long frontendBytesSent;

        /**
         * Parses statistics from the 64-byte response.
         * @param data The 64-byte statistics data (8 uint64 values)
         * @return Parsed statistics
         * @throws IllegalArgumentException if data length is not 64
         */
        public static Statistics parse(byte[] data) {
            if (data.length != 64) {
                throw new IllegalArgumentException("Statistics data must be 64 bytes");
            }

            long[] values = new long[8];
            for (int i = 0; i < 8; i++) {
                long value = 0;
                for (int j = 0; j < 8; j++) {
                    value |= ((long)(data[i * 8 + j] & 0xFF)) << (j * 8);
                }
                values[i] = value;
            }

            return new Statistics(
                values[0], values[1], values[2], values[3],
                values[4], values[5], values[6], values[7]
            );
        }

        private Statistics(long frontendMessagesReceived, long frontendBytesReceived,
                         long backendMessagesSent, long backendBytesSent,
                         long backendMessagesReceived, long backendBytesReceived,
                         long frontendMessagesSent, long frontendBytesSent) {
            this.frontendMessagesReceived = frontendMessagesReceived;
            this.frontendBytesReceived = frontendBytesReceived;
            this.backendMessagesSent = backendMessagesSent;
            this.backendBytesSent = backendBytesSent;
            this.backendMessagesReceived = backendMessagesReceived;
            this.backendBytesReceived = backendBytesReceived;
            this.frontendMessagesSent = frontendMessagesSent;
            this.frontendBytesSent = frontendBytesSent;
        }

        public long getFrontendMessagesReceived() { return frontendMessagesReceived; }
        public long getFrontendBytesReceived() { return frontendBytesReceived; }
        public long getBackendMessagesSent() { return backendMessagesSent; }
        public long getBackendBytesSent() { return backendBytesSent; }
        public long getBackendMessagesReceived() { return backendMessagesReceived; }
        public long getBackendBytesReceived() { return backendBytesReceived; }
        public long getFrontendMessagesSent() { return frontendMessagesSent; }
        public long getFrontendBytesSent() { return frontendBytesSent; }

        @Override
        public String toString() {
            return String.format(
                "Statistics{" +
                "frontend: %d msgs (%d bytes) in, %d msgs (%d bytes) out, " +
                "backend: %d msgs (%d bytes) in, %d msgs (%d bytes) out}",
                frontendMessagesReceived, frontendBytesReceived,
                frontendMessagesSent, frontendBytesSent,
                backendMessagesReceived, backendBytesReceived,
                backendMessagesSent, backendBytesSent
            );
        }
    }
}
```

---

## Usage Examples

### CURVE Encryption

```java
// Server
try (Context ctx = new Context();
     Socket server = new Socket(ctx, SocketType.REP)) {

    // Check if CURVE is available
    if (!Context.has("curve")) {
        System.err.println("CURVE not supported");
        return;
    }

    // Generate server keypair
    Curve.KeyPair serverKeys = Curve.generateKeypair();

    // Configure CURVE server
    server.setOption(SocketOption.CURVE_SERVER, 1);
    server.setOption(SocketOption.CURVE_SECRETKEY, serverKeys.secretKey());

    server.bind("tcp://*:5555");

    // Print public key for clients
    System.out.println("Server public key: " + serverKeys.publicKey());

    while (true) {
        String request = server.recvString();
        server.send("Reply: " + request);
    }
}

// Client
try (Context ctx = new Context();
     Socket client = new Socket(ctx, SocketType.REQ)) {

    // Generate client keypair
    Curve.KeyPair clientKeys = Curve.generateKeypair();

    // Configure CURVE client
    client.setOption(SocketOption.CURVE_SERVERKEY, serverPublicKey);
    client.setOption(SocketOption.CURVE_PUBLICKEY, clientKeys.publicKey());
    client.setOption(SocketOption.CURVE_SECRETKEY, clientKeys.secretKey());

    client.connect("tcp://localhost:5555");

    client.send("Hello");
    String reply = client.recvString();
}
```

### Z85 Encoding

```java
// Encoding binary data
byte[] data = {0x12, 0x34, 0x56, 0x78}; // Must be divisible by 4
String encoded = Z85.encode(data);
System.out.println("Encoded: " + encoded); // e.g., "HelloWorld" (5 chars)

// Decoding
byte[] decoded = Z85.decode(encoded);
System.out.println("Decoded matches: " + Arrays.equals(data, decoded));

// Handling arbitrary-length data
byte[] arbitraryData = "Hello, World!".getBytes();
Z85.PaddedData padded = Z85.padForEncoding(arbitraryData);
String encoded2 = Z85.encode(padded.data());

// When decoding, remove padding
byte[] decoded2 = Z85.decode(encoded2);
byte[] original = Z85.removePadding(decoded2, padded.paddingSize());
```

### Queue Proxy

```java
// Run in separate thread
new Thread(() -> {
    try (Context ctx = new Context();
         Socket frontend = new Socket(ctx, SocketType.ROUTER);
         Socket backend = new Socket(ctx, SocketType.DEALER)) {

        // Clients connect to frontend
        frontend.bind("tcp://*:5555");

        // Workers connect to backend
        backend.bind("tcp://*:5556");

        // Start proxy (blocks until terminated)
        Proxy.start(frontend, backend);
    }
}).start();

// Client
try (Context ctx = new Context();
     Socket client = new Socket(ctx, SocketType.REQ)) {
    client.connect("tcp://localhost:5555");
    client.send("Request");
    String reply = client.recvString();
}

// Worker
try (Context ctx = new Context();
     Socket worker = new Socket(ctx, SocketType.REP)) {
    worker.connect("tcp://localhost:5556");
    while (true) {
        String request = worker.recvString();
        worker.send("Reply: " + request);
    }
}
```

### Steerable Proxy with Statistics

```java
// Control thread
new Thread(() -> {
    try (Context ctx = new Context();
         Socket controller = new Socket(ctx, SocketType.PAIR)) {

        controller.connect("inproc://proxy-control");

        // Wait a bit
        Thread.sleep(5000);

        // Pause proxy
        controller.send(Proxy.Commands.PAUSE);
        System.out.println("Proxy paused");

        Thread.sleep(2000);

        // Resume proxy
        controller.send(Proxy.Commands.RESUME);
        System.out.println("Proxy resumed");

        Thread.sleep(5000);

        // Get statistics
        controller.send(Proxy.Commands.STATISTICS);
        byte[] statsData = controller.recvBytes();
        Proxy.Statistics stats = Proxy.Statistics.parse(statsData);
        System.out.println(stats);

        // Terminate
        controller.send(Proxy.Commands.TERMINATE);
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();

// Proxy thread
try (Context ctx = new Context();
     Socket frontend = new Socket(ctx, SocketType.ROUTER);
     Socket backend = new Socket(ctx, SocketType.DEALER);
     Socket control = new Socket(ctx, SocketType.PAIR)) {

    frontend.bind("tcp://*:5555");
    backend.bind("tcp://*:5556");
    control.bind("inproc://proxy-control");

    Proxy.startSteerable(frontend, backend, control);
}
```

---

## Testing Utilities

### Test CURVE Availability

```java
@Test
public void testCurveAvailability() {
    boolean hasCurve = Context.has("curve");
    System.out.println("CURVE available: " + hasCurve);

    if (hasCurve) {
        Curve.KeyPair keys = Curve.generateKeypair();
        assertNotNull(keys.publicKey());
        assertNotNull(keys.secretKey());
        assertEquals(40, keys.publicKey().length());
        assertEquals(40, keys.secretKey().length());

        // Test key derivation
        String derivedPublic = Curve.derivePublicKey(keys.secretKey());
        assertEquals(keys.publicKey(), derivedPublic);
    }
}
```

### Test Z85 Round-Trip

```java
@Test
public void testZ85RoundTrip() {
    byte[] original = {
        0x00, 0x11, 0x22, 0x33,
        0x44, 0x55, 0x66, 0x77,
        (byte)0x88, (byte)0x99, (byte)0xAA, (byte)0xBB
    };

    String encoded = Z85.encode(original);
    assertNotNull(encoded);
    assertEquals(15, encoded.length()); // 12 bytes -> 15 chars

    byte[] decoded = Z85.decode(encoded);
    assertArrayEquals(original, decoded);
}
```

### Test Proxy

```java
@Test
public void testBasicProxy() throws Exception {
    Context ctx = new Context();

    // Start proxy in thread
    Thread proxyThread = new Thread(() -> {
        try (Socket frontend = new Socket(ctx, SocketType.ROUTER);
             Socket backend = new Socket(ctx, SocketType.DEALER)) {
            frontend.bind("inproc://frontend");
            backend.bind("inproc://backend");
            Proxy.start(frontend, backend);
        }
    });
    proxyThread.start();

    Thread.sleep(100); // Let proxy start

    // Client
    Socket client = new Socket(ctx, SocketType.REQ);
    client.connect("inproc://frontend");

    // Worker
    Socket worker = new Socket(ctx, SocketType.REP);
    worker.connect("inproc://backend");

    // Send request
    client.send("Hello");

    // Worker receives
    String request = worker.recvString();
    assertEquals("Hello", request);

    // Worker replies
    worker.send("World");

    // Client receives
    String reply = client.recvString();
    assertEquals("World", reply);

    // Cleanup
    client.close();
    worker.close();
    ctx.close();
    proxyThread.interrupt();
}
```

---

## Next Steps

After implementing utilities:

1. Write comprehensive tests for each utility
2. Add integration tests combining utilities (e.g., CURVE + proxy)
3. Create example programs demonstrating common patterns
4. Document security best practices for CURVE
5. Add performance benchmarks

## Security Considerations

### CURVE Best Practices

1. **Key Storage**: Never hardcode secret keys in source code
2. **Key Generation**: Always generate new keypairs; never reuse
3. **Key Distribution**: Use secure channels to distribute public keys
4. **Key Rotation**: Implement key rotation for long-running services
5. **Validation**: Always validate that CURVE is available before use

### Z85 Considerations

1. **Not Encryption**: Z85 is encoding, not encryption
2. **Padding**: Be careful with padding when encoding sensitive data
3. **Validation**: Always validate Z85 strings before decoding

---

This completes the utilities documentation. All utility classes provide production-ready implementations with comprehensive error handling, validation, and documentation.
