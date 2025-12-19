package io.github.ulalax.zmq;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic test to identify the byte[] recv issue in benchmarks.
 */
public class ByteArrayRecvTest {

    @Test
    public void testRouterToRouterByteArrayRecv() throws Exception {
        System.out.println("=== Starting byte[] recv diagnostic test ===");

        try (Context ctx = new Context();
             Socket router1 = new Socket(ctx, SocketType.ROUTER);
             Socket router2 = new Socket(ctx, SocketType.ROUTER)) {

            // Set routing IDs
            byte[] router1Id = "r1".getBytes(StandardCharsets.UTF_8);
            byte[] router2Id = "r2".getBytes(StandardCharsets.UTF_8);
            router1.setOption(SocketOption.ROUTING_ID, router1Id);
            router2.setOption(SocketOption.ROUTING_ID, router2Id);

            // Configure sockets
            router1.setOption(SocketOption.SNDHWM, 0);
            router1.setOption(SocketOption.RCVHWM, 0);
            router1.setOption(SocketOption.LINGER, 0);
            router2.setOption(SocketOption.SNDHWM, 0);
            router2.setOption(SocketOption.RCVHWM, 0);
            router2.setOption(SocketOption.LINGER, 0);

            // Bind and connect
            router1.bind("tcp://127.0.0.1:0");
            String endpoint = router1.getOptionString(SocketOption.LAST_ENDPOINT);
            router2.connect(endpoint);
            System.out.println("Router1 bound to: " + endpoint);

            Thread.sleep(100);

            // Handshake: Router2 -> Router1
            System.out.println("Starting handshake...");
            try (Message id = new Message(router1Id);
                 Message greeting = new Message("hi".getBytes(StandardCharsets.UTF_8))) {
                router2.send(id, SendFlags.SEND_MORE);
                router2.send(greeting, SendFlags.NONE);
                System.out.println("Router2 sent handshake");
            }

            try (Message handshakeId = new Message();
                 Message handshakeMsg = new Message()) {
                router1.recv(handshakeId, RecvFlags.NONE);
                router1.recv(handshakeMsg, RecvFlags.NONE);
                System.out.println("Router1 received handshake");
            }

            // Test 1: Send and receive using Message objects (should work)
            System.out.println("\n=== Test 1: Message-based send/recv ===");
            testMessageSendRecv(router1, router2, router2Id);

            // Test 2: Send and receive using byte[] (benchmark pattern)
            System.out.println("\n=== Test 2: byte[] send/recv (benchmark pattern) ===");
            testByteArraySendRecv(router1, router2, router2Id);
        }

        System.out.println("\n=== Test completed successfully ===");
    }

    private void testMessageSendRecv(Socket router1, Socket router2, byte[] router2Id) throws Exception {
        CountDownLatch latch = new CountDownLatch(10);
        byte[] testData = new byte[64];
        Arrays.fill(testData, (byte) 'M');

        Thread receiver = new Thread(() -> {
            try {
                System.out.println("[Message Receiver] Started");
                Message identityMsg = new Message();
                Message payloadMsg = new Message();

                for (int i = 0; i < 10; i++) {
                    router2.recv(identityMsg, RecvFlags.NONE);
                    router2.recv(payloadMsg, RecvFlags.NONE);
                    System.out.println("[Message Receiver] Received message " + (i + 1));

                    identityMsg.close();
                    payloadMsg.close();
                    identityMsg = new Message();
                    payloadMsg = new Message();

                    latch.countDown();
                }

                identityMsg.close();
                payloadMsg.close();
                System.out.println("[Message Receiver] Finished");
            } catch (Exception e) {
                System.err.println("[Message Receiver] Error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        receiver.start();
        Thread.sleep(50); // Give receiver time to start

        System.out.println("[Message Sender] Starting to send...");
        for (int i = 0; i < 10; i++) {
            try (Message idMsg = new Message(router2Id);
                 Message payloadMsg = new Message(testData)) {
                router1.send(idMsg, SendFlags.SEND_MORE);
                router1.send(payloadMsg, SendFlags.DONT_WAIT);
                System.out.println("[Message Sender] Sent message " + (i + 1));
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS),
            "Message-based recv timeout - received " + (10 - latch.getCount()) + "/10 messages");
        receiver.join();
        System.out.println("[Message Test] SUCCESS - all messages received");
    }

    private void testByteArraySendRecv(Socket router1, Socket router2, byte[] router2Id) throws Exception {
        CountDownLatch latch = new CountDownLatch(10);
        byte[] testData = new byte[64];
        Arrays.fill(testData, (byte) 'B');

        byte[] identityBuffer = new byte[256];
        byte[] recvBuffer = new byte[65536];

        Thread receiver = new Thread(() -> {
            try {
                System.out.println("[ByteArray Receiver] Started");

                for (int i = 0; i < 10; i++) {
                    System.out.println("[ByteArray Receiver] Waiting for message " + (i + 1) + "...");

                    // Receive identity frame
                    int idSize = router2.recv(identityBuffer, RecvFlags.NONE).value();
                    System.out.println("[ByteArray Receiver] Received identity frame, size=" + idSize);

                    // Receive payload frame
                    int payloadSize = router2.recv(recvBuffer, RecvFlags.NONE).value();
                    System.out.println("[ByteArray Receiver] Received payload frame, size=" + payloadSize);

                    latch.countDown();
                }

                System.out.println("[ByteArray Receiver] Finished");
            } catch (Exception e) {
                System.err.println("[ByteArray Receiver] Error: " + e.getMessage());
                e.printStackTrace();
            }
        });

        receiver.start();
        Thread.sleep(50); // Give receiver time to start

        System.out.println("[ByteArray Sender] Starting to send...");
        for (int i = 0; i < 10; i++) {
            router1.send(router2Id, SendFlags.SEND_MORE);

            byte[] sendBuffer = new byte[testData.length];
            System.arraycopy(testData, 0, sendBuffer, 0, testData.length);
            router1.send(sendBuffer, SendFlags.DONT_WAIT);

            System.out.println("[ByteArray Sender] Sent message " + (i + 1));
        }

        boolean success = latch.await(5, TimeUnit.SECONDS);
        long received = 10 - latch.getCount();

        System.out.println("[ByteArray Test] Received " + received + "/10 messages");

        if (!success) {
            System.err.println("[ByteArray Test] FAILED - timeout");
            System.err.println("Receiver thread state: " + receiver.getState());
            receiver.interrupt();
        } else {
            System.out.println("[ByteArray Test] SUCCESS");
        }

        receiver.join(1000);

        assertTrue(success,
            "byte[] recv timeout - received " + received + "/10 messages");
    }
}
