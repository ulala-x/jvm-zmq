package io.github.ulalax.zmq;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for adaptive buffer sizing in Socket.
 * This test verifies that buffers dynamically adjust their size based on usage patterns.
 */
class AdaptiveBufferSizingTest {

    /**
     * Tests that send buffer resizes after 100 operations with significantly smaller messages.
     * Strategy:
     * 1. Send one large message (32KB) to expand buffer
     * 2. Send 100 small messages (1KB)
     * 3. Verify buffer shrinks to ~2x average (2KB + 1KB padding = 3KB)
     */
    @Test
    void testSendBufferAdaptiveSizing() throws Exception {
        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PAIR);
             Socket receiver = new Socket(ctx, SocketType.PAIR)) {

            String endpoint = "inproc://adaptive-send-test";
            receiver.bind(endpoint);
            sender.connect(endpoint);

            // Step 1: Send one large message (32KB) to expand buffer
            byte[] largeMessage = new byte[32 * 1024];
            for (int i = 0; i < largeMessage.length; i++) {
                largeMessage[i] = (byte) (i % 256);
            }
            assertTrue(sender.send(largeMessage, SendFlags.DONT_WAIT), "Large message should be sent");
            receiver.recv(new byte[32 * 1024], RecvFlags.DONT_WAIT); // Consume message

            // Get initial buffer size (should be >= 32KB)
            int initialBufferSize = getSendBufferSize(sender);
            assertTrue(initialBufferSize >= 32 * 1024,
                    "Buffer should expand to at least 32KB, got: " + initialBufferSize);

            // Step 2: Send 100 small messages (1KB each) to trigger adaptive resizing
            byte[] smallMessage = new byte[1024];
            for (int i = 0; i < 100; i++) {
                assertTrue(sender.send(smallMessage, SendFlags.DONT_WAIT),
                        "Small message " + i + " should be sent");
                receiver.recv(new byte[1024], RecvFlags.DONT_WAIT); // Consume message
            }

            // Step 3: Verify buffer has shrunk to reasonable size
            int finalBufferSize = getSendBufferSize(sender);
            int expectedMaxSize = (1024 * 2) + 1024; // 2x average + 1KB padding = 3KB
            // Allow some tolerance for Arena allocation overhead (typically aligns to page boundaries)
            int maxSizeWithTolerance = expectedMaxSize + 1024; // Allow +1KB tolerance

            assertTrue(finalBufferSize < initialBufferSize,
                    "Buffer should shrink from " + initialBufferSize + " to something smaller, got: " + finalBufferSize);
            assertTrue(finalBufferSize <= maxSizeWithTolerance,
                    "Buffer should be at most " + maxSizeWithTolerance + " bytes (with tolerance), got: " + finalBufferSize);

            // Verify it's much smaller than the initial oversized buffer (at least 4x reduction)
            assertTrue(finalBufferSize < (initialBufferSize / 4),
                    "Buffer should shrink to less than 25% of original size");

            System.out.println("Send buffer adaptive sizing test passed:");
            System.out.println("  Initial size: " + initialBufferSize + " bytes");
            System.out.println("  Final size:   " + finalBufferSize + " bytes");
            System.out.println("  Target size:  " + expectedMaxSize + " bytes");
            System.out.println("  Shrink ratio: " + String.format("%.1f", (double) initialBufferSize / finalBufferSize) + "x");
        }
    }

    /**
     * Tests that receive buffer resizes after 100 operations with significantly smaller messages.
     */
    @Test
    void testRecvBufferAdaptiveSizing() throws Exception {
        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PAIR);
             Socket receiver = new Socket(ctx, SocketType.PAIR)) {

            String endpoint = "inproc://adaptive-recv-test";
            receiver.bind(endpoint);
            sender.connect(endpoint);

            // Step 1: Receive one large message (32KB) to expand buffer
            byte[] largeMessage = new byte[32 * 1024];
            sender.send(largeMessage, SendFlags.DONT_WAIT);
            int received = receiver.recv(new byte[32 * 1024], RecvFlags.DONT_WAIT);
            assertEquals(32 * 1024, received, "Should receive full large message");

            // Get initial buffer size
            int initialBufferSize = getRecvBufferSize(receiver);
            assertTrue(initialBufferSize >= 32 * 1024,
                    "Buffer should expand to at least 32KB, got: " + initialBufferSize);

            // Step 2: Receive 100 small messages (1KB each)
            byte[] smallMessage = new byte[1024];
            for (int i = 0; i < 100; i++) {
                sender.send(smallMessage, SendFlags.DONT_WAIT);
                received = receiver.recv(new byte[1024], RecvFlags.DONT_WAIT);
                assertEquals(1024, received, "Should receive 1KB message");
            }

            // Step 3: Verify buffer has shrunk
            int finalBufferSize = getRecvBufferSize(receiver);
            int expectedMaxSize = (1024 * 2) + 1024; // 2x average + 1KB padding = 3KB
            // Allow some tolerance for Arena allocation overhead
            int maxSizeWithTolerance = expectedMaxSize + 1024; // Allow +1KB tolerance

            assertTrue(finalBufferSize < initialBufferSize,
                    "Buffer should shrink from " + initialBufferSize + " to something smaller, got: " + finalBufferSize);
            assertTrue(finalBufferSize <= maxSizeWithTolerance,
                    "Buffer should be at most " + maxSizeWithTolerance + " bytes (with tolerance), got: " + finalBufferSize);

            // Verify it's much smaller than the initial oversized buffer
            assertTrue(finalBufferSize < (initialBufferSize / 4),
                    "Buffer should shrink to less than 25% of original size");

            System.out.println("Recv buffer adaptive sizing test passed:");
            System.out.println("  Initial size: " + initialBufferSize + " bytes");
            System.out.println("  Final size:   " + finalBufferSize + " bytes");
            System.out.println("  Target size:  " + expectedMaxSize + " bytes");
            System.out.println("  Shrink ratio: " + String.format("%.1f", (double) initialBufferSize / finalBufferSize) + "x");
        }
    }

    /**
     * Tests that buffer does NOT reset when messages are consistently large.
     */
    @Test
    void testNoResetForConsistentlyLargeMessages() throws Exception {
        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PAIR);
             Socket receiver = new Socket(ctx, SocketType.PAIR)) {

            String endpoint = "inproc://no-reset-test";
            receiver.bind(endpoint);
            sender.connect(endpoint);

            // Send 100 large messages (10KB each)
            byte[] message = new byte[10 * 1024];
            for (int i = 0; i < 100; i++) {
                assertTrue(sender.send(message, SendFlags.DONT_WAIT));
                receiver.recv(new byte[10 * 1024], RecvFlags.DONT_WAIT);
            }

            // Buffer should still be large (at least 10KB, probably exactly 10KB or slightly larger)
            int sendBufferSize = getSendBufferSize(sender);
            assertTrue(sendBufferSize >= 10 * 1024,
                    "Buffer should remain at least 10KB for consistent large messages, got: " + sendBufferSize);

            System.out.println("No-reset test passed:");
            System.out.println("  Send buffer size: " + sendBufferSize + " bytes (>= 10KB as expected)");
        }
    }

    /**
     * Tests that very small messages (< 1KB) don't trigger reset (avoid thrashing).
     */
    @Test
    void testNoResetForTinyMessages() throws Exception {
        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PAIR);
             Socket receiver = new Socket(ctx, SocketType.PAIR)) {

            String endpoint = "inproc://tiny-message-test";
            receiver.bind(endpoint);
            sender.connect(endpoint);

            // First expand buffer with one large message
            byte[] largeMessage = new byte[16 * 1024];
            sender.send(largeMessage, SendFlags.DONT_WAIT);
            receiver.recv(new byte[16 * 1024], RecvFlags.DONT_WAIT);

            int initialBufferSize = getSendBufferSize(sender);

            // Send 100 tiny messages (100 bytes each)
            // Average usage will be 100 bytes, but threshold is 1KB minimum
            // So buffer should NOT reset (even though it's 4x larger)
            byte[] tinyMessage = new byte[100];
            for (int i = 0; i < 100; i++) {
                sender.send(tinyMessage, SendFlags.DONT_WAIT);
                receiver.recv(new byte[100], RecvFlags.DONT_WAIT);
            }

            int finalBufferSize = getSendBufferSize(sender);

            // Buffer should NOT have shrunk (or only slightly due to other logic)
            // because average usage (100 bytes) is below 1KB threshold
            assertEquals(initialBufferSize, finalBufferSize,
                    "Buffer should not reset for tiny messages (average < 1KB), " +
                    "initial: " + initialBufferSize + ", final: " + finalBufferSize);

            System.out.println("Tiny message test passed:");
            System.out.println("  Buffer size remained: " + finalBufferSize + " bytes (no reset as expected)");
        }
    }

    // Helper methods using reflection to access private fields
    private int getSendBufferSize(Socket socket) throws Exception {
        Field field = Socket.class.getDeclaredField("sendBufferSize");
        field.setAccessible(true);
        return (int) field.get(socket);
    }

    private int getRecvBufferSize(Socket socket) throws Exception {
        Field field = Socket.class.getDeclaredField("recvBufferSize");
        field.setAccessible(true);
        return (int) field.get(socket);
    }
}
