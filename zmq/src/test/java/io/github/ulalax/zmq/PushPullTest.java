package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PUSH-PULL pattern (pipeline pattern).
 */
@Tag("Socket")
@Tag("PushPull")
class PushPullTest {

    @Nested
    @DisplayName("Basic Push-Pull")
    class BasicPushPull {

        @Test
        @DisplayName("Should send and receive messages via PUSH-PULL")
        void should_Send_And_Receive_Messages_Via_Push_Pull() throws Exception {
            // Given: A PUSH sender and PULL receiver
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-push-pull");
                pusher.connect("inproc://test-push-pull");
                Thread.sleep(50);

                // When: Push a message
                String message = "Work item";
                pusher.send(message);

                // Then: Pull should receive the message
                puller.setOption(SocketOption.RCVTIMEO, 1000);
                byte[] received = puller.recvBytes().value();

                assertThat(received).isNotNull();
                assertThat(new String(received, StandardCharsets.UTF_8))
                        .as("Received message")
                        .isEqualTo(message);
            }
        }

        @Test
        @DisplayName("Should handle multiple messages in sequence")
        void should_Handle_Multiple_Messages_In_Sequence() throws Exception {
            // Given: A PUSH-PULL connection
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-sequence");
                pusher.connect("inproc://test-sequence");
                Thread.sleep(50);

                puller.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Push multiple messages
                // Then: Should receive them in order
                for (int i = 0; i < 5; i++) {
                    String message = "Message-" + i;
                    pusher.send(message);

                    byte[] received = puller.recvBytes().value();
                    assertThat(new String(received, StandardCharsets.UTF_8))
                            .as("Message #" + i)
                            .isEqualTo(message);
                }
            }
        }
    }

    @Nested
    @DisplayName("Load Distribution")
    class LoadDistribution {

        @Test
        @DisplayName("Should distribute work to multiple pullers")
        void should_Distribute_Work_To_Multiple_Pullers() throws Exception {
            // Given: One pusher and two pullers
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller1 = new Socket(ctx, SocketType.PULL);
                 Socket puller2 = new Socket(ctx, SocketType.PULL)) {

                pusher.bind("inproc://test-distribution");
                puller1.connect("inproc://test-distribution");
                puller2.connect("inproc://test-distribution");
                Thread.sleep(100);

                puller1.setOption(SocketOption.RCVTIMEO, 1000);
                puller2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Push multiple messages
                int messageCount = 6;
                for (int i = 0; i < messageCount; i++) {
                    pusher.send("Work-" + i);
                }

                Thread.sleep(50);

                // Then: Both pullers should receive some messages (round-robin distribution)
                List<String> received1 = new ArrayList<>();
                List<String> received2 = new ArrayList<>();

                for (int i = 0; i < messageCount / 2; i++) {
                    byte[] msg1 = puller1.recvBytes().value();
                    if (msg1 != null) {
                        received1.add(new String(msg1, StandardCharsets.UTF_8));
                    }

                    byte[] msg2 = puller2.recvBytes().value();
                    if (msg2 != null) {
                        received2.add(new String(msg2, StandardCharsets.UTF_8));
                    }
                }

                assertThat(received1)
                        .as("Puller 1 received messages")
                        .isNotEmpty();
                assertThat(received2)
                        .as("Puller 2 received messages")
                        .isNotEmpty();

                // Verify total message count
                int totalReceived = received1.size() + received2.size();
                assertThat(totalReceived)
                        .as("Total messages received")
                        .isEqualTo(messageCount);
            }
        }
    }

    @Nested
    @DisplayName("Fan-out Pattern")
    class FanOutPattern {

        @Test
        @DisplayName("Should fan out from multiple pushers to single puller")
        void should_Fan_Out_From_Multiple_Pushers_To_Single_Puller() throws Exception {
            // Given: Two pushers and one puller
            try (Context ctx = new Context();
                 Socket pusher1 = new Socket(ctx, SocketType.PUSH);
                 Socket pusher2 = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-fanout");
                pusher1.connect("inproc://test-fanout");
                pusher2.connect("inproc://test-fanout");
                Thread.sleep(100);

                puller.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Both pushers send messages
                pusher1.send("From-Pusher-1");
                pusher2.send("From-Pusher-2");

                // Then: Puller should receive both messages
                List<String> received = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    byte[] msg = puller.recvBytes().value();
                    assertThat(msg).isNotNull();
                    received.add(new String(msg, StandardCharsets.UTF_8));
                }

                assertThat(received)
                        .as("Received messages from both pushers")
                        .hasSize(2)
                        .containsExactlyInAnyOrder("From-Pusher-1", "From-Pusher-2");
            }
        }
    }

    @Nested
    @DisplayName("Pipeline Pattern")
    class PipelinePattern {

        @Test
        @DisplayName("Should implement multi-stage pipeline")
        void should_Implement_Multi_Stage_Pipeline() throws Exception {
            // Given: A three-stage pipeline (producer -> worker -> sink)
            try (Context ctx = new Context();
                 Socket producer = new Socket(ctx, SocketType.PUSH);
                 Socket workerIn = new Socket(ctx, SocketType.PULL);
                 Socket workerOut = new Socket(ctx, SocketType.PUSH);
                 Socket sink = new Socket(ctx, SocketType.PULL)) {

                // Setup pipeline connections
                producer.bind("inproc://stage1");
                workerIn.connect("inproc://stage1");

                workerOut.bind("inproc://stage2");
                sink.connect("inproc://stage2");

                Thread.sleep(100);

                workerIn.setOption(SocketOption.RCVTIMEO, 1000);
                sink.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Producer sends data
                String input = "raw-data";
                producer.send(input);

                // Worker receives, processes, and forwards
                byte[] received = workerIn.recvBytes().value();
                assertThat(received).isNotNull();
                String processed = new String(received, StandardCharsets.UTF_8) + "-processed";
                workerOut.send(processed);

                // Then: Sink receives processed data
                byte[] output = sink.recvBytes().value();
                assertThat(output).isNotNull();
                assertThat(new String(output, StandardCharsets.UTF_8))
                        .as("Processed output")
                        .isEqualTo("raw-data-processed");
            }
        }
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentOperations {

        @Test
        @DisplayName("Should handle concurrent push operations")
        void should_Handle_Concurrent_Push_Operations() throws Exception {
            // Given: Multiple threads pushing to a single puller
            try (Context ctx = new Context();
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-concurrent");
                Thread.sleep(50);

                int threadCount = 3;
                int messagesPerThread = 5;
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(threadCount);

                // When: Multiple threads push concurrently
                for (int t = 0; t < threadCount; t++) {
                    final int threadId = t;
                    new Thread(() -> {
                        try (Socket pusher = new Socket(ctx, SocketType.PUSH)) {
                            pusher.connect("inproc://test-concurrent");
                            Thread.sleep(50);

                            startLatch.await();
                            for (int i = 0; i < messagesPerThread; i++) {
                                pusher.send("Thread-" + threadId + "-Msg-" + i);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            doneLatch.countDown();
                        }
                    }).start();
                }

                Thread.sleep(100);
                startLatch.countDown();
                doneLatch.await(5, TimeUnit.SECONDS);

                // Then: Puller should receive all messages
                puller.setOption(SocketOption.RCVTIMEO, 2000);
                List<String> receivedMessages = new ArrayList<>();

                int totalExpected = threadCount * messagesPerThread;
                for (int i = 0; i < totalExpected; i++) {
                    byte[] msg = puller.recvBytes().value();
                    if (msg != null) {
                        receivedMessages.add(new String(msg, StandardCharsets.UTF_8));
                    }
                }

                assertThat(receivedMessages)
                        .as("All concurrent messages received")
                        .hasSize(totalExpected);
            }
        }
    }

    @Nested
    @DisplayName("Binary Data Transfer")
    class BinaryDataTransfer {

        @Test
        @DisplayName("Should transfer binary data correctly")
        void should_Transfer_Binary_Data_Correctly() throws Exception {
            // Given: A PUSH-PULL connection
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-binary-push-pull");
                pusher.connect("inproc://test-binary-push-pull");
                Thread.sleep(50);

                puller.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Push binary data
                byte[] binaryData = new byte[]{0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF};
                pusher.send(binaryData);

                // Then: Should receive exact binary data
                byte[] received = puller.recvBytes().value();
                assertThat(received)
                        .as("Binary data integrity")
                        .isEqualTo(binaryData);
            }
        }

        @Test
        @DisplayName("Should transfer large binary payloads")
        void should_Transfer_Large_Binary_Payloads() throws Exception {
            // Given: A PUSH-PULL connection
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-large-binary");
                pusher.connect("inproc://test-large-binary");
                Thread.sleep(50);

                puller.setOption(SocketOption.RCVTIMEO, 2000);

                // When: Push large binary data (1MB)
                byte[] largeData = new byte[1024 * 1024];
                for (int i = 0; i < largeData.length; i++) {
                    largeData[i] = (byte) (i % 256);
                }
                pusher.send(largeData);

                // Then: Should receive complete large data
                byte[] received = puller.recvBytes().value();
                assertThat(received)
                        .as("Large binary data integrity")
                        .hasSize(largeData.length)
                        .isEqualTo(largeData);
            }
        }
    }

    @Nested
    @DisplayName("Flow Control")
    class FlowControl {

        @Test
        @DisplayName("Should handle high-water mark limits")
        void should_Handle_High_Water_Mark_Limits() throws Exception {
            // Given: A PUSH-PULL connection with low HWM
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                // Set low high-water mark
                pusher.setOption(SocketOption.SNDHWM, 10);

                puller.bind("inproc://test-hwm");
                pusher.connect("inproc://test-hwm");
                Thread.sleep(100);

                // When: Send messages up to and beyond HWM
                // Then: Should handle according to HWM policy
                assertThatCode(() -> {
                    for (int i = 0; i < 20; i++) {
                        pusher.send("Message-" + i, SendFlags.DONT_WAIT);
                    }
                })
                        .as("Sending with HWM limit")
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should respect send timeout")
        void should_Respect_Send_Timeout() {
            // Given: A PUSH socket with send timeout but no connected puller
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH)) {

                pusher.setOption(SocketOption.SNDTIMEO, 100);
                pusher.setOption(SocketOption.SNDHWM, 1);

                // When: Try to send without connected puller using DONT_WAIT
                // Then: Should return immediately (either succeed or fail with EAGAIN)
                long startTime = System.currentTimeMillis();

                // Using send with DONT_WAIT which handles EAGAIN gracefully
                SendResult sent = pusher.send("data", SendFlags.DONT_WAIT);

                long elapsedTime = System.currentTimeMillis() - startTime;

                // Send should not block with DONT_WAIT
                assertThat(elapsedTime)
                        .as("Send operation should be non-blocking")
                        .isLessThan(200L);

                // Without a connected puller, send will likely fail (would block) or queue locally
                assertThat(sent.isPresent() || sent.wouldBlock())
                        .as("Send without puller may succeed (queued) or would block")
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty messages")
        void should_Handle_Empty_Messages() throws Exception {
            // Given: A PUSH-PULL connection
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-empty");
                pusher.connect("inproc://test-empty");
                Thread.sleep(50);

                puller.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Push empty message
                pusher.send("");

                // Then: Should receive empty message
                byte[] received = puller.recvBytes().value();
                assertThat(received)
                        .as("Empty message")
                        .isNotNull()
                        .isEmpty();
            }
        }

        @Test
        @DisplayName("Should handle disconnection gracefully")
        void should_Handle_Disconnection_Gracefully() throws Exception {
            // Given: A PUSH-PULL connection
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                puller.bind("inproc://test-disconnect");
                pusher.connect("inproc://test-disconnect");
                Thread.sleep(50);

                // When: Send a message, then disconnect
                pusher.send("Before disconnect");
                pusher.disconnect("inproc://test-disconnect");

                // Then: Should handle gracefully
                puller.setOption(SocketOption.RCVTIMEO, 1000);
                byte[] received = puller.recvBytes().value();
                assertThat(received).isNotNull();
            }
        }
    }
}
