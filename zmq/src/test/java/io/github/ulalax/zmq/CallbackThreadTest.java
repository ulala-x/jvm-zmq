package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;

/**
 * Thread verification test for ZMQ zmq_free_fn callback.
 *
 * <p>This test verifies whether the ZMQ free callback is invoked on the same thread
 * that sends the message, or on a different thread. This is critical for choosing
 * the correct Arena allocation strategy:</p>
 *
 * <ul>
 *   <li><b>Same thread</b>: Can use Arena.ofConfined() (953x faster)</li>
 *   <li><b>Different thread</b>: Must use Arena.ofShared()</li>
 * </ul>
 *
 * <p>Current implementation (Message.java line 203) uses Arena.ofShared() assuming
 * different threads. If this test proves same-thread execution, we can optimize
 * to Arena.ofConfined() for dramatic performance improvement.</p>
 */
@DisplayName("ZMQ Free Callback Thread Verification")
class CallbackThreadTest {

    @Test
    @DisplayName("Should verify zmq_free_fn callback thread execution")
    void should_Verify_Callback_Thread_Execution() throws InterruptedException {
        // Given: Thread ID trackers
        long mainThreadId = Thread.currentThread().getId();
        String mainThreadName = Thread.currentThread().getName();
        AtomicLong callbackThreadId = new AtomicLong(-1);
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        CountDownLatch callbackLatch = new CountDownLatch(1);

        System.out.println("\n========================================");
        System.out.println("ZMQ Free Callback Thread Verification");
        System.out.println("========================================");
        System.out.println("Main thread ID: " + mainThreadId);
        System.out.println("Main thread name: " + mainThreadName);

        Arena dataArena = Arena.ofShared();

        {
            // When: Send zero-copy message with callback
            Context ctx = new Context();
            Socket server = new Socket(ctx, SocketType.PUSH);
            Socket client = new Socket(ctx, SocketType.PULL);

            // Use TCP transport (inproc may not call free callback)
            server.bind("tcp://127.0.0.1:0");
            String endpoint = server.getOptionString(SocketOption.LAST_ENDPOINT);
            client.connect(endpoint);

            // Give sockets time to connect
            Thread.sleep(100);

            // Allocate data with shared arena (required for cross-thread access)
            MemorySegment data = dataArena.allocate(64);

            // Fill with test data
            for (int i = 0; i < 64; i++) {
                data.set(ValueLayout.JAVA_BYTE, i, (byte) i);
            }

            // Create zero-copy message with thread-tracking callback
            Consumer<MemorySegment> callback = seg -> {
                long callbackThread = Thread.currentThread().getId();
                String callbackThreadNameVal = Thread.currentThread().getName();

                callbackThreadId.set(callbackThread);
                callbackInvoked.set(true);

                System.out.println("\n[CALLBACK INVOKED]");
                System.out.println("Callback thread ID: " + callbackThread);
                System.out.println("Callback thread name: " + callbackThreadNameVal);

                // Close data arena after ZMQ is done
                try {
                    dataArena.close();
                } catch (Exception e) {
                    System.err.println("Error closing data arena: " + e.getMessage());
                }

                callbackLatch.countDown();
            };

            Message msg = new Message(data, 64, callback);

            // Send message
            System.out.println("\nSending message from thread: " + mainThreadId);
            server.send(msg, SendFlags.NONE);
            msg.close();

            // Receive message (this triggers ZMQ to eventually free the message)
            System.out.println("Receiving message...");
            Message received = new Message();
            client.recv(received, RecvFlags.NONE);

            // Verify data integrity
            byte[] receivedData = received.toByteArray();
            assertThat(receivedData.length).isEqualTo(64);
            for (int i = 0; i < 64; i++) {
                assertThat(receivedData[i]).isEqualTo((byte) i);
            }

            // CRITICAL: Close received message to release ZMQ's reference
            received.close();

            // Close sockets
            System.out.println("Closing sockets...");
            server.close();
            client.close();

            // CRITICAL: Close context to force all pending messages to be freed
            System.out.println("Closing context to trigger cleanup...");
            ctx.close();
        }

        // Wait for callback to be invoked (with timeout)
        System.out.println("Waiting for callback invocation...");
        boolean callbackCompleted = callbackLatch.await(10, TimeUnit.SECONDS);

        // Then: Verify callback was invoked
        assertThat(callbackCompleted)
                .as("Callback should complete within timeout")
                .isTrue();

        assertThat(callbackInvoked.get())
                .as("Callback should be invoked")
                .isTrue();

        // Analyze thread execution
        long actualCallbackThreadId = callbackThreadId.get();
        boolean sameThread = (mainThreadId == actualCallbackThreadId);

        System.out.println("\n========================================");
        System.out.println("RESULT");
        System.out.println("========================================");
        System.out.println("Main thread ID:     " + mainThreadId);
        System.out.println("Callback thread ID: " + actualCallbackThreadId);
        System.out.println("Same thread:        " + sameThread);
        System.out.println("========================================");

        if (sameThread) {
            System.out.println("\n✅ OPTIMIZATION AVAILABLE!");
            System.out.println("Callback runs on SAME thread as sender.");
            System.out.println("Can use Arena.ofConfined() instead of Arena.ofShared().");
            System.out.println("Expected performance improvement: ~953x faster allocation");
            System.out.println("\nRecommendation:");
            System.out.println("  Change Message.java line 203:");
            System.out.println("  - hintArena = Arena.ofShared();");
            System.out.println("  + hintArena = Arena.ofConfined();");
        } else {
            System.out.println("\n❌ NO OPTIMIZATION POSSIBLE");
            System.out.println("Callback runs on DIFFERENT thread.");
            System.out.println("Must keep Arena.ofShared() for thread safety.");
            System.out.println("\nCurrent implementation is correct.");
        }
        System.out.println("========================================\n");

        // Additional assertions for test framework
        assertThat(actualCallbackThreadId)
                .as("Callback thread ID should be set")
                .isNotEqualTo(-1);
    }

}
