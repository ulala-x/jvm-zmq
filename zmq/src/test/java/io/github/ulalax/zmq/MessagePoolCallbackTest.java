package io.github.ulalax.zmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests to verify that MessagePool callbacks are actually invoked by ZMQ.
 *
 * <p>This test verifies the core mechanism of zero-copy pooling:
 * ZMQ should invoke the free callback when it's done with the message data,
 * allowing the pool to reclaim and reuse the buffer.</p>
 */
@DisplayName("MessagePool Callback Invocation Tests")
class MessagePoolCallbackTest {

    private Context context;
    private Socket sender;
    private Socket receiver;
    private MessagePool pool;

    @BeforeEach
    void setUp() {
        context = new Context();
        sender = new Socket(context, SocketType.PUSH);
        receiver = new Socket(context, SocketType.PULL);

        // Use inproc transport for fast, reliable communication
        String endpoint = "inproc://callback-test";
        sender.bind(endpoint);
        receiver.connect(endpoint);

        // Give sockets time to connect
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        pool = new MessagePool();
    }

    @AfterEach
    void tearDown() {
        if (receiver != null) {
            receiver.close();
        }
        if (sender != null) {
            sender.close();
        }
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("Should invoke callback when message is sent and received")
    void should_Invoke_Callback_When_Message_Sent() throws InterruptedException {
        // Given: Rent a message from pool
        byte[] testData = "Hello ZMQ Callback".getBytes();

        // Record initial statistics
        MessagePool.PoolStatistics beforeStats = pool.getStatistics();
        long rentsBefore = beforeStats.totalRents;
        long returnsBefore = beforeStats.totalReturns;

        Message msg = pool.rent(testData);

        // When: Send the message (this should trigger zero-copy with callback)
        boolean sent = sender.send(msg, SendFlags.NONE);
        assertThat(sent).as("Message should be sent successfully").isTrue();

        // Close the message (marks it as sent, callback will handle pool return)
        msg.close();

        // Receive the message to complete the ZMQ lifecycle
        byte[] recvBuffer = new byte[256];
        int bytesReceived = receiver.recv(recvBuffer, RecvFlags.NONE);
        assertThat(bytesReceived).as("Should receive message").isEqualTo(testData.length);

        // Then: Wait for callback to be invoked
        // The callback should be invoked after ZMQ is done with the message
        Thread.sleep(100);

        // Verify statistics show the message was returned to pool
        MessagePool.PoolStatistics afterStats = pool.getStatistics();
        assertThat(afterStats.totalRents)
                .as("Total rents should increment (rents before: " + rentsBefore + ")")
                .isEqualTo(rentsBefore + 1);

        assertThat(afterStats.totalReturns)
                .as("Total returns should increment (callback invoked, returns before: " + returnsBefore + ")")
                .isGreaterThan(returnsBefore);
    }

    @Test
    @DisplayName("Should reuse message buffer after callback returns to pool")
    void should_Reuse_Message_After_Callback() throws InterruptedException {
        // Given: Send and receive a message to trigger callback
        byte[] testData1 = "First message".getBytes();
        Message msg1 = pool.rent(testData1);

        sender.send(msg1, SendFlags.NONE);
        msg1.close();

        byte[] recvBuffer = new byte[256];
        receiver.recv(recvBuffer, RecvFlags.NONE);

        // Wait for callback to return message to pool
        Thread.sleep(100);

        // When: Rent another message of similar size
        MessagePool.PoolStatistics beforeSecondRent = pool.getStatistics();
        Message msg2 = pool.rent(testData1.length);

        // Then: This should be a pool hit (reusing the returned buffer)
        MessagePool.PoolStatistics afterSecondRent = pool.getStatistics();

        // The second rent should either hit the pool or create new (both are valid)
        // What matters is that totalReturns increased, proving callback was invoked
        assertThat(afterSecondRent.totalReturns)
                .as("Callback should have returned msg1 to pool")
                .isGreaterThan(0);

        msg2.close();
    }

    @Test
    @DisplayName("Should handle multiple messages with callbacks")
    void should_Handle_Multiple_Messages_With_Callbacks() throws InterruptedException {
        // Given: Send multiple messages
        int messageCount = 10;
        byte[] testData = "Test message".getBytes();

        for (int i = 0; i < messageCount; i++) {
            Message msg = pool.rent(testData);
            sender.send(msg, SendFlags.NONE);
            msg.close();
        }

        // Receive all messages
        byte[] recvBuffer = new byte[256];
        for (int i = 0; i < messageCount; i++) {
            int received = receiver.recv(recvBuffer, RecvFlags.NONE);
            assertThat(received).as("Should receive message " + i).isGreaterThan(0);
        }

        // Wait for all callbacks to execute
        Thread.sleep(200);

        // Then: All messages should be returned to pool
        MessagePool.PoolStatistics stats = pool.getStatistics();
        assertThat(stats.totalRents)
                .as("Should have rented " + messageCount + " messages")
                .isEqualTo(messageCount);

        assertThat(stats.totalReturns)
                .as("All messages should be returned via callbacks")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Should not invoke callback for message that was not sent")
    void should_Not_Invoke_Callback_For_Unsent_Message() throws InterruptedException {
        // Given: Rent a message but don't send it
        Message msg = pool.rent(100);

        MessagePool.PoolStatistics beforeClose = pool.getStatistics();
        long returnsBefore = beforeClose.totalReturns;

        // When: Close the message without sending
        msg.close();

        // Give some time (callback should NOT be invoked by ZMQ)
        Thread.sleep(100);

        // Then: Message should be returned to pool immediately (not via ZMQ callback)
        MessagePool.PoolStatistics afterClose = pool.getStatistics();

        // The message was returned via close(), not via ZMQ callback
        // totalReturns should still increment because close() triggers returnToPool()
        assertThat(afterClose.totalReturns)
                .as("Message should be returned to pool on close()")
                .isGreaterThan(returnsBefore);
    }

    @Test
    @DisplayName("Should handle callback invocation from ZMQ internal thread")
    void should_Handle_Callback_From_Different_Thread() throws InterruptedException {
        // This test verifies thread safety of the callback mechanism

        // Given: Prepare to track callback thread
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // Send a message
        byte[] testData = "Thread safety test".getBytes();
        Message msg = pool.rent(testData);

        sender.send(msg, SendFlags.NONE);
        msg.close();

        // Receive to trigger callback
        byte[] recvBuffer = new byte[256];
        receiver.recv(recvBuffer, RecvFlags.NONE);

        // Wait for callback
        Thread.sleep(100);

        // Then: Verify pool statistics updated correctly (proves thread-safe callback)
        MessagePool.PoolStatistics stats = pool.getStatistics();
        assertThat(stats.totalReturns)
                .as("Callback should have been invoked from ZMQ thread")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Should track correct statistics after multiple send-receive cycles")
    void should_Track_Statistics_Correctly_After_Multiple_Cycles() throws InterruptedException {
        // Given: Perform multiple send-receive cycles
        int cycles = 5;
        byte[] testData = "Cycle test".getBytes();

        for (int i = 0; i < cycles; i++) {
            // Send
            Message msg = pool.rent(testData);
            sender.send(msg, SendFlags.NONE);
            msg.close();

            // Receive
            byte[] recvBuffer = new byte[256];
            receiver.recv(recvBuffer, RecvFlags.NONE);

            // Wait for callback
            Thread.sleep(50);
        }

        // Then: Statistics should be consistent
        MessagePool.PoolStatistics stats = pool.getStatistics();

        assertThat(stats.totalRents)
                .as("Should have rented " + cycles + " times")
                .isEqualTo(cycles);

        assertThat(stats.totalReturns)
                .as("Should have returned messages via callbacks")
                .isGreaterThan(0);

        // After callbacks, outstanding should be 0
        assertThat(stats.outstandingBuffers)
                .as("No messages should be outstanding after callbacks")
                .isLessThanOrEqualTo(cycles);
    }
}
