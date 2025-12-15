package io.github.ulalax.zmq;

import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * Throughput Benchmark Tests for ZMQ messaging patterns.
 * <p>
 * These tests measure message processing throughput (messages/sec and MB/sec)
 * using PUSH-PULL pattern with various message sizes.
 * <p>
 * Tests are tagged with @Tag("benchmark") and @Disabled by default to exclude from regular CI runs.
 * To run: ./gradlew test --tests "*ThroughputBenchmark*" -Dtest.profile=benchmark
 */
@Tag("benchmark")
@DisplayName("Throughput Benchmark Tests")
@Disabled("Performance benchmarks - run manually when needed")
class ThroughputBenchmarkTest {

    private static final String BENCHMARK_SEPARATOR = "========================================";
    private static final int WARMUP_MESSAGE_COUNT = 1000;
    private static final int BENCHMARK_MESSAGE_COUNT = 10000;
    private static final int WARMUP_ITERATIONS = 3;

    @Nested
    @DisplayName("PUSH-PULL Throughput Benchmarks")
    class PushPullThroughputBenchmarks {

        @Test
        @DisplayName("Should measure throughput with 1-byte messages")
        void should_Measure_Throughput_With_1_Byte_Messages() throws Exception {
            runThroughputBenchmark(1, "1B");
        }

        @Test
        @DisplayName("Should measure throughput with 128-byte messages")
        void should_Measure_Throughput_With_128_Byte_Messages() throws Exception {
            runThroughputBenchmark(128, "128B");
        }

        @Test
        @DisplayName("Should measure throughput with 1KB messages")
        void should_Measure_Throughput_With_1KB_Messages() throws Exception {
            runThroughputBenchmark(1024, "1KB");
        }

        @Test
        @DisplayName("Should measure throughput with 64KB messages")
        void should_Measure_Throughput_With_64KB_Messages() throws Exception {
            runThroughputBenchmark(64 * 1024, "64KB");
        }

        /**
         * Runs a throughput benchmark with specified message size.
         *
         * @param messageSize size of each message in bytes
         * @param sizeLabel human-readable size label
         */
        private void runThroughputBenchmark(int messageSize, String sizeLabel) throws Exception {
            System.out.println(BENCHMARK_SEPARATOR);
            System.out.println("THROUGHPUT BENCHMARK - Message Size: " + sizeLabel);
            System.out.println(BENCHMARK_SEPARATOR);

            // Warm-up phase
            System.out.println("Warming up...");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                executeThroughputTest(messageSize, WARMUP_MESSAGE_COUNT, false);
                Thread.sleep(100);
            }

            System.out.println("Running benchmark...");
            Thread.sleep(500); // Brief pause before actual benchmark

            // Actual benchmark
            BenchmarkResult result = executeThroughputTest(messageSize, BENCHMARK_MESSAGE_COUNT, true);

            // Display results
            displayThroughputResults(result, sizeLabel);

            // Verify basic sanity
            assertThat(result.messagesPerSecond)
                    .as("Messages per second should be positive")
                    .isGreaterThan(0);
        }

        /**
         * Executes a single throughput test run.
         *
         * @param messageSize size of each message
         * @param messageCount number of messages to send
         * @param measureTime whether to measure and return timing
         * @return benchmark result
         */
        private BenchmarkResult executeThroughputTest(int messageSize, int messageCount, boolean measureTime) throws Exception {
            try (Context ctx = new Context();
                 Socket pusher = new Socket(ctx, SocketType.PUSH);
                 Socket puller = new Socket(ctx, SocketType.PULL)) {

                // Configure sockets for maximum throughput
                pusher.setOption(SocketOption.SNDHWM, 0); // Unlimited send queue
                puller.setOption(SocketOption.RCVHWM, 0); // Unlimited receive queue
                pusher.setOption(SocketOption.LINGER, 0); // Don't wait on close

                // Bind and connect
                puller.bind("tcp://127.0.0.1:0");
                String endpoint = puller.getOptionString(SocketOption.LAST_ENDPOINT);
                pusher.connect(endpoint);

                Thread.sleep(100); // Allow connection to establish

                // Prepare message data
                byte[] messageData = new byte[messageSize];
                Arrays.fill(messageData, (byte) 'A');

                CountDownLatch receiverReady = new CountDownLatch(1);
                CountDownLatch receiverDone = new CountDownLatch(1);
                AtomicLong receiveEndTime = new AtomicLong(0);

                // Receiver thread
                Thread receiverThread = new Thread(() -> {
                    try {
                        receiverReady.countDown();
                        byte[] buffer = new byte[messageSize];

                        for (int i = 0; i < messageCount; i++) {
                            int received = puller.recv(buffer, RecvFlags.NONE);
                            if (received != messageSize) {
                                throw new RuntimeException("Received unexpected size: " + received);
                            }
                        }

                        receiveEndTime.set(System.nanoTime());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        receiverDone.countDown();
                    }
                }, "ThroughputReceiver");

                receiverThread.start();
                receiverReady.await(5, TimeUnit.SECONDS);

                // Give receiver time to be ready
                Thread.sleep(50);

                // Start sending
                long startTime = System.nanoTime();

                for (int i = 0; i < messageCount; i++) {
                    pusher.send(messageData, SendFlags.NONE);
                }

                // Wait for all messages to be received
                boolean completed = receiverDone.await(30, TimeUnit.SECONDS);
                long endTime = receiveEndTime.get();

                assertThat(completed)
                        .as("Receiver should complete within timeout")
                        .isTrue();

                receiverThread.join(1000);

                if (!measureTime) {
                    return new BenchmarkResult(0, 0, messageSize, messageCount);
                }

                // Calculate metrics
                long elapsedNanos = endTime - startTime;
                double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
                double messagesPerSecond = messageCount / elapsedSeconds;
                double megabytesPerSecond = (messageCount * messageSize) / (1024.0 * 1024.0 * elapsedSeconds);

                return new BenchmarkResult(messagesPerSecond, megabytesPerSecond, messageSize, messageCount);
            }
        }

        /**
         * Displays throughput benchmark results.
         */
        private void displayThroughputResults(BenchmarkResult result, String sizeLabel) {
            System.out.println();
            System.out.println("Results:");
            System.out.println("  Message Size:        " + sizeLabel + " (" + result.messageSize + " bytes)");
            System.out.println("  Messages Sent:       " + formatNumber(result.messageCount));
            System.out.println("  Throughput:          " + formatNumber((long) result.messagesPerSecond) + " msg/sec");
            System.out.println("  Bandwidth:           " + String.format("%.2f", result.megabytesPerSecond) + " MB/sec");
            System.out.println("  Per-message Latency: " + String.format("%.2f", 1_000_000.0 / result.messagesPerSecond) + " μs");
            System.out.println(BENCHMARK_SEPARATOR);
            System.out.println();
        }
    }

    @Nested
    @DisplayName("PUB-SUB Throughput Benchmarks")
    class PubSubThroughputBenchmarks {

        @Test
        @DisplayName("Should measure PUB-SUB throughput with 1KB messages")
        void should_Measure_PubSub_Throughput_With_1KB_Messages() throws Exception {
            runPubSubThroughputBenchmark(1024, "1KB");
        }

        private void runPubSubThroughputBenchmark(int messageSize, String sizeLabel) throws Exception {
            System.out.println(BENCHMARK_SEPARATOR);
            System.out.println("PUB-SUB THROUGHPUT BENCHMARK - Message Size: " + sizeLabel);
            System.out.println(BENCHMARK_SEPARATOR);

            // Warm-up
            System.out.println("Warming up...");
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                executePubSubTest(messageSize, WARMUP_MESSAGE_COUNT, false);
                Thread.sleep(100);
            }

            System.out.println("Running benchmark...");
            Thread.sleep(500);

            // Actual benchmark
            BenchmarkResult result = executePubSubTest(messageSize, BENCHMARK_MESSAGE_COUNT, true);

            // Display results
            displayThroughputResults(result, sizeLabel);

            assertThat(result.messagesPerSecond)
                    .as("Messages per second should be positive")
                    .isGreaterThan(0);
        }

        private BenchmarkResult executePubSubTest(int messageSize, int messageCount, boolean measureTime) throws Exception {
            try (Context ctx = new Context();
                 Socket publisher = new Socket(ctx, SocketType.PUB);
                 Socket subscriber = new Socket(ctx, SocketType.SUB)) {

                // Configure sockets
                publisher.setOption(SocketOption.SNDHWM, 0);
                subscriber.setOption(SocketOption.RCVHWM, 0);
                publisher.setOption(SocketOption.LINGER, 0);

                // Bind and connect
                publisher.bind("tcp://127.0.0.1:0");
                String endpoint = publisher.getOptionString(SocketOption.LAST_ENDPOINT);
                subscriber.connect(endpoint);
                subscriber.setOption(SocketOption.SUBSCRIBE, "".getBytes(StandardCharsets.UTF_8));

                Thread.sleep(200); // PUB-SUB needs more time to sync

                byte[] messageData = new byte[messageSize];
                Arrays.fill(messageData, (byte) 'B');

                CountDownLatch receiverDone = new CountDownLatch(1);
                AtomicLong receiveEndTime = new AtomicLong(0);

                // Subscriber thread
                Thread subscriberThread = new Thread(() -> {
                    try {
                        byte[] buffer = new byte[messageSize];
                        for (int i = 0; i < messageCount; i++) {
                            int received = subscriber.recv(buffer, RecvFlags.NONE);
                            if (received != messageSize) {
                                throw new RuntimeException("Received unexpected size: " + received);
                            }
                        }
                        receiveEndTime.set(System.nanoTime());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        receiverDone.countDown();
                    }
                }, "PubSubSubscriber");

                subscriberThread.start();
                Thread.sleep(100);

                // Start publishing
                long startTime = System.nanoTime();

                for (int i = 0; i < messageCount; i++) {
                    publisher.send(messageData, SendFlags.NONE);
                }

                // Wait for completion
                boolean completed = receiverDone.await(30, TimeUnit.SECONDS);
                long endTime = receiveEndTime.get();

                assertThat(completed).isTrue();
                subscriberThread.join(1000);

                if (!measureTime) {
                    return new BenchmarkResult(0, 0, messageSize, messageCount);
                }

                long elapsedNanos = endTime - startTime;
                double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
                double messagesPerSecond = messageCount / elapsedSeconds;
                double megabytesPerSecond = (messageCount * messageSize) / (1024.0 * 1024.0 * elapsedSeconds);

                return new BenchmarkResult(messagesPerSecond, megabytesPerSecond, messageSize, messageCount);
            }
        }

        private void displayThroughputResults(BenchmarkResult result, String sizeLabel) {
            System.out.println();
            System.out.println("Results:");
            System.out.println("  Message Size:        " + sizeLabel + " (" + result.messageSize + " bytes)");
            System.out.println("  Messages Published:  " + formatNumber(result.messageCount));
            System.out.println("  Throughput:          " + formatNumber((long) result.messagesPerSecond) + " msg/sec");
            System.out.println("  Bandwidth:           " + String.format("%.2f", result.megabytesPerSecond) + " MB/sec");
            System.out.println(BENCHMARK_SEPARATOR);
            System.out.println();
        }
    }

    /**
     * Helper method to format large numbers with comma separators.
     */
    private static String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * Container for benchmark results.
     */
    private static class BenchmarkResult {
        final double messagesPerSecond;
        final double megabytesPerSecond;
        final int messageSize;
        final int messageCount;

        BenchmarkResult(double messagesPerSecond, double megabytesPerSecond, int messageSize, int messageCount) {
            this.messagesPerSecond = messagesPerSecond;
            this.megabytesPerSecond = megabytesPerSecond;
            this.messageSize = messageSize;
            this.messageCount = messageCount;
        }
    }
}
