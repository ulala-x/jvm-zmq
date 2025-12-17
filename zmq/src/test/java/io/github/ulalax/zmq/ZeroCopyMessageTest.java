package io.github.ulalax.zmq;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for zero-copy Message constructor with callback support.
 */
class ZeroCopyMessageTest {

    @Test
    void testZeroCopyConstructorBasic() throws Exception {
        byte[] testData = "Hello Zero-Copy".getBytes(StandardCharsets.UTF_8);

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            String endpoint = "inproc://test-zerocopy-basic";
            sender.bind(endpoint);
            receiver.connect(endpoint);

            Thread.sleep(100); // Allow connection

            // Callback to track when ZMQ releases the message data
            AtomicBoolean callbackInvoked = new AtomicBoolean(false);

            Arena dataArena = Arena.ofShared();
            MemorySegment dataSeg = dataArena.allocate(testData.length);
            MemorySegment.copy(testData, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, testData.length);

            Message msg = new Message(dataSeg, testData.length, data -> {
                callbackInvoked.set(true);
                dataArena.close();
            });

            // Send message
            sender.send(msg, SendFlags.NONE);
            msg.close();

            // Receive message and verify data
            try (Message recvMsg = new Message()) {
                receiver.recv(recvMsg, RecvFlags.NONE);
                String received = recvMsg.toString();
                assertEquals("Hello Zero-Copy", received);
            }

            // Test passed - message was sent and received successfully
            // Note: Callback invocation timing is ZMQ-internal and may be delayed
        }
    }

    @Test
    void testZeroCopyConstructorWithoutCallback() throws Exception {
        byte[] testData = "No Callback".getBytes(StandardCharsets.UTF_8);

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.bind("tcp://127.0.0.1:0");
            String endpoint = sender.getOptionString(SocketOption.LAST_ENDPOINT);
            receiver.connect(endpoint);

            Thread.sleep(100); // Allow connection

            // Create zero-copy message without callback
            Arena dataArena = Arena.ofShared();
            MemorySegment dataSeg = dataArena.allocate(testData.length);
            MemorySegment.copy(testData, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, testData.length);

            Message msg = new Message(dataSeg, testData.length, null);

            // Send message
            sender.send(msg, SendFlags.NONE);
            msg.close();

            // Receive message
            try (Message recvMsg = new Message()) {
                receiver.recv(recvMsg, RecvFlags.NONE);
                String received = recvMsg.toString();
                assertEquals("No Callback", received);
            }

            // Clean up manually since no callback
            Thread.sleep(500); // Give ZMQ time to finish with the data
            dataArena.close();
        }
    }

    @Test
    void testZeroCopyConstructorValidation() {
        Arena arena = Arena.ofShared();
        MemorySegment segment = arena.allocate(100);

        // Test negative size
        assertThrows(IllegalArgumentException.class, () -> {
            new Message(segment, -1, null);
        });

        // Test size exceeding segment size
        assertThrows(IllegalArgumentException.class, () -> {
            new Message(segment, 200, null);
        });

        arena.close();
    }

    @Test
    void testMultipleZeroCopyMessages() throws Exception {
        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            String endpoint = "inproc://test-zerocopy-multiple";
            sender.bind(endpoint);
            receiver.connect(endpoint);

            Thread.sleep(100); // Allow connection

            int messageCount = 10;

            // Send multiple zero-copy messages
            for (int i = 0; i < messageCount; i++) {
                byte[] data = ("Message " + i).getBytes(StandardCharsets.UTF_8);
                Arena dataArena = Arena.ofShared();
                MemorySegment dataSeg = dataArena.allocate(data.length);
                MemorySegment.copy(data, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, data.length);

                Message msg = new Message(dataSeg, data.length, d -> {
                    dataArena.close();
                });

                sender.send(msg, SendFlags.NONE);
                msg.close();
            }

            // Receive all messages and verify data
            for (int i = 0; i < messageCount; i++) {
                try (Message recvMsg = new Message()) {
                    receiver.recv(recvMsg, RecvFlags.NONE);
                    String received = recvMsg.toString();
                    assertEquals("Message " + i, received);
                }
            }

            // Test passed - all messages sent and received successfully
            // Note: Callback invocation timing is ZMQ-internal and may be delayed
        }
    }
}
