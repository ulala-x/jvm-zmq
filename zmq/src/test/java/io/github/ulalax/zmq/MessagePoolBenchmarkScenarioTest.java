package io.github.ulalax.zmq;

import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests that verify MessagePool benchmark scenarios work correctly.
 * These tests mirror the benchmark code to ensure it won't hang.
 */
@Tag("Integration")
@Tag("Benchmark")
class MessagePoolBenchmarkScenarioTest {

    private Context ctx;
    private Socket router1;
    private Socket router2;
    private byte[] router2Id;
    private byte[] identityBuffer;

    @BeforeEach
    void setUp() throws Exception {
        ctx = new Context();
        router1 = new Socket(ctx, SocketType.ROUTER);
        router2 = new Socket(ctx, SocketType.ROUTER);

        byte[] router1Id = "r1".getBytes(StandardCharsets.UTF_8);
        router2Id = "r2".getBytes(StandardCharsets.UTF_8);
        router1.setOption(SocketOption.ROUTING_ID, router1Id);
        router2.setOption(SocketOption.ROUTING_ID, router2Id);

        router1.setOption(SocketOption.SNDHWM, 0);
        router1.setOption(SocketOption.RCVHWM, 0);
        router1.setOption(SocketOption.LINGER, 0);
        router2.setOption(SocketOption.SNDHWM, 0);
        router2.setOption(SocketOption.RCVHWM, 0);
        router2.setOption(SocketOption.LINGER, 0);

        router1.bind("tcp://127.0.0.1:0");
        String endpoint = router1.getOptionString(SocketOption.LAST_ENDPOINT);
        router2.connect(endpoint);

        Thread.sleep(100);

        // Handshake
        try (Message id = new Message(router1Id);
             Message greeting = new Message("hi".getBytes(StandardCharsets.UTF_8))) {
            router2.send(id, SendFlags.SEND_MORE);
            router2.send(greeting, SendFlags.NONE);
        }

        try (Message handshakeId = new Message();
             Message handshakeMsg = new Message()) {
            router1.recv(handshakeId, RecvFlags.NONE);
            router1.recv(handshakeMsg, RecvFlags.NONE);
        }

        identityBuffer = new byte[64];

        // Prewarm the pool
        MessagePool.SHARED.prewarm(MessageSize.SIZE_1K, 100);
    }

    @AfterEach
    void tearDown() {
        if (router1 != null) {
            router1.setOption(SocketOption.LINGER, 0);
            router1.close();
        }
        if (router2 != null) {
            router2.setOption(SocketOption.LINGER, 0);
            router2.close();
        }
        if (ctx != null) ctx.close();
        MessagePool.SHARED.clear();
    }

    @Test
    @DisplayName("MessagePooled_SendRecv scenario should work")
    void messagePooled_SendRecv_ShouldWork() throws Exception {
        int messageSize = 1024;
        int messageCount = 10;

        CountDownLatch latch = new CountDownLatch(messageCount);

        Thread receiver = new Thread(() -> {
            try {
                for (int n = 0; n < messageCount; n++) {
                    // Receive identity
                    router2.recv(identityBuffer, RecvFlags.NONE);
                    // Receive payload as regular Message (non-pooled)
                    try (Message msg = new Message()) {
                        router2.recv(msg, RecvFlags.NONE);
                        // Verify data
                        assertThat(msg.size()).isEqualTo(messageSize);
                    }
                    latch.countDown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiver.start();

        for (int i = 0; i < messageCount; i++) {
            // Send identity as simple byte[] (small data, no pooling needed)
            router1.send(router2Id, SendFlags.SEND_MORE);

            // Rent message from pool for sending (no data copying)
            Message msg = MessagePool.SHARED.rent(messageSize);
            router1.send(msg, SendFlags.DONT_WAIT);
            msg.close(); // Automatically returns to pool
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        receiver.join(1000);

        assertThat(completed)
            .as("All messages should be received")
            .isTrue();

        System.out.println("MessagePooled_SendRecv: OK");
    }

    @Test
    @DisplayName("MessagePooled_SendRecv_WithReceivePool scenario should work")
    void messagePooled_SendRecv_WithReceivePool_ShouldWork() throws Exception {
        int messageSize = 1024;
        int messageCount = 10;

        CountDownLatch latch = new CountDownLatch(messageCount);

        Thread receiver = new Thread(() -> {
            try {
                for (int n = 0; n < messageCount; n++) {
                    // Receive identity
                    router2.recv(identityBuffer, RecvFlags.NONE);

                    // Receive payload using pooled Message
                    Message msg = MessagePool.SHARED.rent(messageSize);
                    router2.recv(msg, messageSize);
                    // Verify data
                    assertThat(msg.size()).isEqualTo(messageSize);
                    msg.close(); // Automatically returns to pool

                    latch.countDown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        receiver.start();

        for (int i = 0; i < messageCount; i++) {
            // Send identity as simple byte[] (small data, no pooling needed)
            router1.send(router2Id, SendFlags.SEND_MORE);

            // Rent message from pool for sending (no data copying)
            Message msg = MessagePool.SHARED.rent(messageSize);
            router1.send(msg, SendFlags.DONT_WAIT);
            msg.close(); // Automatically returns to pool
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        receiver.join(1000);

        assertThat(completed)
            .as("All messages should be received with receive pool")
            .isTrue();

        System.out.println("MessagePooled_SendRecv_WithReceivePool: OK");
    }

    @Test
    @DisplayName("Multiple message sizes should work with pool")
    void multipleSizes_ShouldWork() throws Exception {
        int[] sizes = {64, 512, 1024};

        for (int messageSize : sizes) {
            // Send identity as simple byte[] (small data, no pooling needed)
            router1.send(router2Id, SendFlags.SEND_MORE);

            // Rent message from pool for sending (no data copying)
            Message sendMsg = MessagePool.SHARED.rent(messageSize);
            router1.send(sendMsg, SendFlags.DONT_WAIT);
            sendMsg.close();

            // Receive
            router2.recv(identityBuffer, RecvFlags.NONE);
            try (Message recvMsg = new Message()) {
                router2.recv(recvMsg, RecvFlags.NONE);
                assertThat(recvMsg.size())
                    .as("Message size " + messageSize)
                    .isEqualTo(messageSize);
            }
        }

        System.out.println("Multiple sizes test: OK");
    }
}
