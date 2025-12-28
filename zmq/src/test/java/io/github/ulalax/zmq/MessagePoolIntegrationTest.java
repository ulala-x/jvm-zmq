package io.github.ulalax.zmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for MessagePool with real ZeroMQ sockets.
 */
@Tag("Integration")
@Tag("MessagePool")
class MessagePoolIntegrationTest {

    private Context context;
    private Socket sender;
    private Socket receiver;
    private MessagePool pool;

    @BeforeEach
    void setUp() {
        context = new Context();
        sender = new Socket(context, SocketType.PUSH);
        receiver = new Socket(context, SocketType.PULL);

        sender.bind("inproc://test-pool");
        receiver.connect("inproc://test-pool");

        pool = new MessagePool();
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
    @DisplayName("Should send and receive pooled message with byte array")
    void should_Send_And_Receive_Pooled_Message_With_Byte_Array() throws InterruptedException {
        // Given: Test data
        byte[] testData = "Hello from pool!".getBytes(StandardCharsets.UTF_8);

        // When: Rent message from pool and send
        Message sendMsg = pool.rent(testData);
        sender.send(sendMsg, SendFlags.NONE);
        sendMsg.close(); // Safe to close - data is managed by pool

        // Then: Should receive the message
        try (Message recvMsg = new Message()) {
            int received = receiver.recv(recvMsg, RecvFlags.NONE);

            assertThat(received)
                    .as("Received bytes")
                    .isGreaterThan(0);

            assertThat(recvMsg.toString())
                    .as("Received message content")
                    .isEqualTo("Hello from pool!");
        }
    }

    @Test
    @DisplayName("Should send and receive pooled message with size allocation")
    void should_Send_And_Receive_Pooled_Message_With_Size() throws InterruptedException {
        // Given: Rent a message and write data to it
        String testText = "Pooled message test";
        byte[] testData = testText.getBytes(StandardCharsets.UTF_8);

        Message sendMsg = pool.rent(testData.length);
        sendMsg.getPoolDataPtr().copyFrom(java.lang.foreign.MemorySegment.ofArray(testData));

        // When: Send the message
        sender.send(sendMsg, SendFlags.NONE);
        sendMsg.close();

        // Then: Should receive correctly
        try (Message recvMsg = new Message()) {
            receiver.recv(recvMsg, RecvFlags.NONE);

            assertThat(recvMsg.toString())
                    .as("Received message")
                    .isEqualTo(testText);
        }
    }

    @Test
    @DisplayName("Should handle multiple pooled messages")
    void should_Handle_Multiple_Pooled_Messages() throws InterruptedException {
        // Given: Multiple messages
        int messageCount = 10;
        String[] messages = new String[messageCount];

        for (int i = 0; i < messageCount; i++) {
            messages[i] = "Message " + i;
        }

        // When: Send all messages from pool
        for (String msg : messages) {
            Message pooledMsg = pool.rent(msg.getBytes(StandardCharsets.UTF_8));
            sender.send(pooledMsg, SendFlags.NONE);
            pooledMsg.close();
        }

        // Then: Should receive all messages in order
        for (int i = 0; i < messageCount; i++) {
            try (Message recvMsg = new Message()) {
                receiver.recv(recvMsg, RecvFlags.NONE);

                assertThat(recvMsg.toString())
                        .as("Received message " + i)
                        .isEqualTo(messages[i]);
            }
        }
    }

    @Test
    @DisplayName("Should demonstrate pool reuse benefit")
    void should_Demonstrate_Pool_Reuse_Benefit() throws InterruptedException {
        // Given: Prewarm the pool
        pool.prewarm(MessageSize.SIZE_1K, 10);

        MessagePool.PoolStatistics beforeStats = pool.getStatistics();
        assertThat(beforeStats.totalRents)
                .as("Initial rents")
                .isEqualTo(0);

        // When: Send multiple messages
        for (int i = 0; i < 5; i++) {
            String data = "Test message " + i;
            Message msg = pool.rent(data.getBytes(StandardCharsets.UTF_8));
            sender.send(msg, SendFlags.NONE);
            msg.close();
        }

        // Receive all messages
        for (int i = 0; i < 5; i++) {
            try (Message recvMsg = new Message()) {
                receiver.recv(recvMsg, RecvFlags.NONE);
            }
        }

        // Then: Statistics should show pool activity
        MessagePool.PoolStatistics afterStats = pool.getStatistics();
        assertThat(afterStats.totalRents)
                .as("Total rents after sending")
                .isEqualTo(5);

        // Hit rate depends on timing, but we can verify it's tracked
        assertThat(afterStats.poolHits + afterStats.poolMisses)
                .as("Hits + misses should equal total rents")
                .isEqualTo(afterStats.totalRents);
    }

    @Test
    @DisplayName("Should handle different message sizes efficiently")
    void should_Handle_Different_Message_Sizes() throws InterruptedException {
        // Given: Messages of different sizes
        byte[] small = new byte[50];
        byte[] medium = new byte[500];
        byte[] large = new byte[5000];

        // Fill with test data
        for (int i = 0; i < small.length; i++) small[i] = (byte) i;
        for (int i = 0; i < medium.length; i++) medium[i] = (byte) i;
        for (int i = 0; i < large.length; i++) large[i] = (byte) i;

        // When: Send messages of different sizes
        Message msg1 = pool.rent(small);
        Message msg2 = pool.rent(medium);
        Message msg3 = pool.rent(large);

        sender.send(msg1, SendFlags.NONE);
        sender.send(msg2, SendFlags.NONE);
        sender.send(msg3, SendFlags.NONE);

        msg1.close();
        msg2.close();
        msg3.close();

        // Then: Should receive all correctly
        try (Message recv1 = new Message();
             Message recv2 = new Message();
             Message recv3 = new Message()) {

            receiver.recv(recv1, RecvFlags.NONE);
            receiver.recv(recv2, RecvFlags.NONE);
            receiver.recv(recv3, RecvFlags.NONE);

            assertThat(recv1.size()).isEqualTo(small.length);
            assertThat(recv2.size()).isEqualTo(medium.length);
            assertThat(recv3.size()).isEqualTo(large.length);
        }
    }

    @Test
    @DisplayName("Should show pool statistics after usage")
    void should_Show_Pool_Statistics_After_Usage() throws InterruptedException {
        // Given: Send multiple messages
        for (int i = 0; i < 20; i++) {
            Message msg = pool.rent(("Message " + i).getBytes(StandardCharsets.UTF_8));
            sender.send(msg, SendFlags.NONE);
            msg.close();
        }

        // Receive all
        for (int i = 0; i < 20; i++) {
            try (Message recvMsg = new Message()) {
                receiver.recv(recvMsg, RecvFlags.NONE);
            }
        }

        // Allow time for callbacks
        Thread.sleep(100);

        // When: Get statistics
        MessagePool.PoolStatistics stats = pool.getStatistics();

        // Then: Statistics should be meaningful
        assertThat(stats.totalRents)
                .as("Total rents")
                .isEqualTo(20);

        assertThat(stats.outstandingBuffers)
                .as("Outstanding buffers")
                .isGreaterThanOrEqualTo(0);

        // Print statistics for inspection
        System.out.println("Pool Statistics: " + stats);
    }
}
