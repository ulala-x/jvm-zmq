package io.github.ulalax.zmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for cppzmq-style convenience methods (sendMore, trySendMore).
 */
class ConvenienceMethodsTest {

    private Context context;
    private Socket sender;
    private Socket receiver;

    @BeforeEach
    void setUp() {
        context = new Context();
        sender = new Socket(context, SocketType.DEALER);
        receiver = new Socket(context, SocketType.DEALER);

        sender.bind("inproc://test-convenience");
        receiver.connect("inproc://test-convenience");

        // Allow connection to establish
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    void tearDown() {
        if (sender != null) {
            sender.close();
        }
        if (receiver != null) {
            receiver.close();
        }
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testSendMoreByteArray() {
        // Send multipart message using sendMore convenience method
        sender.sendMore("header".getBytes(StandardCharsets.UTF_8));
        sender.sendMore("body1".getBytes(StandardCharsets.UTF_8));
        sender.send("body2".getBytes(StandardCharsets.UTF_8));

        // Receive and verify
        byte[] part1 = receiver.recvBytes();
        assertTrue(receiver.hasMore());
        assertEquals("header", new String(part1, StandardCharsets.UTF_8));

        byte[] part2 = receiver.recvBytes();
        assertTrue(receiver.hasMore());
        assertEquals("body1", new String(part2, StandardCharsets.UTF_8));

        byte[] part3 = receiver.recvBytes();
        assertFalse(receiver.hasMore());
        assertEquals("body2", new String(part3, StandardCharsets.UTF_8));
    }

    @Test
    void testSendMoreString() {
        // Send multipart message using sendMore string variant
        sender.sendMore("header");
        sender.sendMore("body1");
        sender.send("body2");

        // Receive and verify
        String part1 = receiver.recvString();
        assertTrue(receiver.hasMore());
        assertEquals("header", part1);

        String part2 = receiver.recvString();
        assertTrue(receiver.hasMore());
        assertEquals("body1", part2);

        String part3 = receiver.recvString();
        assertFalse(receiver.hasMore());
        assertEquals("body2", part3);
    }

    @Test
    void testSendMoreMessage() {
        // Send multipart message using sendMore Message variant
        try (Message msg1 = new Message("header".getBytes(StandardCharsets.UTF_8));
             Message msg2 = new Message("body".getBytes(StandardCharsets.UTF_8))) {

            sender.sendMore(msg1);
            sender.send(msg2, SendFlags.NONE);

            // Receive and verify
            String part1 = receiver.recvString();
            assertTrue(receiver.hasMore());
            assertEquals("header", part1);

            String part2 = receiver.recvString();
            assertFalse(receiver.hasMore());
            assertEquals("body", part2);
        }
    }

    @Test
    void testTrySendMoreByteArray() {
        // Send multipart message using trySendMore
        assertTrue(sender.trySendMore("header".getBytes(StandardCharsets.UTF_8)));
        assertTrue(sender.trySend("body".getBytes(StandardCharsets.UTF_8), SendFlags.NONE));

        // Receive and verify
        byte[] part1 = receiver.recvBytes();
        assertTrue(receiver.hasMore());
        assertEquals("header", new String(part1, StandardCharsets.UTF_8));

        byte[] part2 = receiver.recvBytes();
        assertFalse(receiver.hasMore());
        assertEquals("body", new String(part2, StandardCharsets.UTF_8));
    }

    @Test
    void testTrySendMoreString() {
        // Send multipart message using trySendMore string variant
        assertTrue(sender.trySendMore("header"));
        assertTrue(sender.trySend("body", SendFlags.NONE));

        // Receive and verify
        String part1 = receiver.recvString();
        assertTrue(receiver.hasMore());
        assertEquals("header", part1);

        String part2 = receiver.recvString();
        assertFalse(receiver.hasMore());
        assertEquals("body", part2);
    }

    @Test
    void testSendMoreNullData() {
        assertThrows(NullPointerException.class, () -> sender.sendMore((byte[]) null));
        assertThrows(NullPointerException.class, () -> sender.sendMore((String) null));
        assertThrows(NullPointerException.class, () -> sender.sendMore((Message) null));
    }

    @Test
    void testTrySendMoreNullData() {
        assertThrows(NullPointerException.class, () -> sender.trySendMore((byte[]) null));
        assertThrows(NullPointerException.class, () -> sender.trySendMore((String) null));
    }

    @Test
    void testClosedSocketSendMore() {
        sender.close();
        assertThrows(IllegalStateException.class,
            () -> sender.sendMore("test".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalStateException.class,
            () -> sender.sendMore("test"));
    }

    @Test
    void testClosedSocketTrySendMore() {
        sender.close();
        assertThrows(IllegalStateException.class,
            () -> sender.trySendMore("test".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalStateException.class,
            () -> sender.trySendMore("test"));
    }
}
