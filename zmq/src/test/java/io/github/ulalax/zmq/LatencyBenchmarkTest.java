package io.github.ulalax.zmq;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Latency Benchmark Tests for ZMQ messaging patterns.
 * <p>
 * These tests measure round-trip latency using REQ-REP pattern with various message sizes.
 * Statistics include: average, minimum, maximum, median, and p99 latency.
 * <p>
 * Tests are tagged with @Tag("benchmark") and @Disabled by default to exclude from regular CI runs.
 * To run: ./gradlew test --tests "*LatencyBenchmark*" -Dtest.profile=benchmark
 */
@Tag("benchmark")
@DisplayName("Latency Benchmark Tests")
@Disabled("Performance benchmarks - run manually when needed")
class LatencyBenchmarkTest {

    private static final String BENCHMARK_SEPARATOR = "========================================";
    private static final int WARMUP_ITERATIONS = 100;
    private static final int BENCHMARK_ITERATIONS = 1000;
    private static final int WARMUP_ROUNDS = 3;

    @Nested
    @DisplayName("REQ-REP Round-trip Latency Benchmarks")
    class ReqRepLatencyBenchmarks {

        @Test
        @DisplayName("Should measure latency with 1-byte messages")
        void should_Measure_Latency_With_1_Byte_Messages() throws Exception {
            runLatencyBenchmark(1, "1B");
        }

        @Test
        @DisplayName("Should measure latency with 128-byte messages")
        void should_Measure_Latency_With_128_Byte_Messages() throws Exception {
            runLatencyBenchmark(128, "128B");
        }

        @Test
        @DisplayName("Should measure latency with 1KB messages")
        void should_Measure_Latency_With_1KB_Messages() throws Exception {
            runLatencyBenchmark(1024, "1KB");
        }

        @Test
        @DisplayName("Should measure latency with 64KB messages")
        void should_Measure_Latency_With_64KB_Messages() throws Exception {
            runLatencyBenchmark(64 * 1024, "64KB");
        }

        /**
         * Runs a latency benchmark with specified message size.
         *
         * @param messageSize size of each message in bytes
         * @param sizeLabel human-readable size label
         */
        private void runLatencyBenchmark(int messageSize, String sizeLabel) throws Exception {
            System.out.println(BENCHMARK_SEPARATOR);
            System.out.println("LATENCY BENCHMARK - Message Size: " + sizeLabel);
            System.out.println(BENCHMARK_SEPARATOR);

            // Warm-up phase
            System.out.println("Warming up...");
            for (int i = 0; i < WARMUP_ROUNDS; i++) {
                executeLatencyTest(messageSize, WARMUP_ITERATIONS, false);
                Thread.sleep(50);
            }

            System.out.println("Running benchmark...");
            Thread.sleep(500); // Brief pause before actual benchmark

            // Actual benchmark
            LatencyResult result = executeLatencyTest(messageSize, BENCHMARK_ITERATIONS, true);

            // Display results
            displayLatencyResults(result, sizeLabel);

            // Verify basic sanity
            assertThat(result.averageLatency)
                    .as("Average latency should be positive")
                    .isGreaterThan(0.0);
            assertThat(result.minLatency)
                    .as("Min latency should be less than or equal to average")
                    .isLessThanOrEqualTo(result.averageLatency);
            assertThat(result.maxLatency)
                    .as("Max latency should be greater than or equal to average")
                    .isGreaterThanOrEqualTo(result.averageLatency);
        }

        /**
         * Executes a single latency test run.
         *
         * @param messageSize size of each message
         * @param iterations number of round-trips to perform
         * @param collectStats whether to collect and return statistics
         * @return latency result with statistics
         */
        private LatencyResult executeLatencyTest(int messageSize, int iterations, boolean collectStats) throws Exception {
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                // Configure sockets
                server.setOption(SocketOption.LINGER, 0);
                client.setOption(SocketOption.LINGER, 0);
                client.setOption(SocketOption.RCVTIMEO, 5000); // 5 second timeout
                server.setOption(SocketOption.RCVTIMEO, 5000);

                // Bind and connect
                server.bind("tcp://127.0.0.1:0");
                String endpoint = server.getOptionString(SocketOption.LAST_ENDPOINT);
                client.connect(endpoint);

                Thread.sleep(100); // Allow connection to establish

                // Prepare message data
                byte[] requestData = new byte[messageSize];
                byte[] responseData = new byte[messageSize];
                byte[] receiveBuffer = new byte[messageSize];
                Arrays.fill(requestData, (byte) 'Q');
                Arrays.fill(responseData, (byte) 'R');

                List<Long> latencies = collectStats ? new ArrayList<>(iterations) : null;

                // Create echo server thread
                Thread serverThread = new Thread(() -> {
                    try {
                        byte[] serverBuffer = new byte[messageSize];
                        for (int i = 0; i < iterations; i++) {
                            // Receive request
                            int received = server.recv(serverBuffer, RecvFlags.NONE);
                            if (received != messageSize) {
                                throw new RuntimeException("Server received unexpected size: " + received);
                            }

                            // Send response
                            server.send(responseData, SendFlags.NONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, "LatencyServer");

                serverThread.start();
                Thread.sleep(50); // Give server time to start

                // Execute request-response cycles and measure latency
                long totalLatency = 0;

                for (int i = 0; i < iterations; i++) {
                    long startNanos = System.nanoTime();

                    // Send request
                    client.send(requestData, SendFlags.NONE);

                    // Receive response
                    int received = client.recv(receiveBuffer, RecvFlags.NONE);
                    if (received != messageSize) {
                        throw new RuntimeException("Client received unexpected size: " + received);
                    }

                    long endNanos = System.nanoTime();
                    long latencyNanos = endNanos - startNanos;

                    if (collectStats) {
                        latencies.add(latencyNanos);
                    }
                    totalLatency += latencyNanos;
                }

                serverThread.join(5000);

                if (!collectStats) {
                    return new LatencyResult(0, 0, 0, 0, 0, messageSize, iterations);
                }

                // Calculate statistics
                return calculateLatencyStatistics(latencies, messageSize, iterations);
            }
        }

        /**
         * Calculates latency statistics from collected measurements.
         */
        private LatencyResult calculateLatencyStatistics(List<Long> latencies, int messageSize, int iterations) {
            Collections.sort(latencies);

            long minNanos = latencies.get(0);
            long maxNanos = latencies.get(latencies.size() - 1);

            long totalNanos = 0;
            for (long latency : latencies) {
                totalNanos += latency;
            }
            double averageNanos = (double) totalNanos / latencies.size();

            // Calculate percentiles
            int p50Index = (int) (latencies.size() * 0.50);
            int p99Index = (int) (latencies.size() * 0.99);

            long medianNanos = latencies.get(Math.min(p50Index, latencies.size() - 1));
            long p99Nanos = latencies.get(Math.min(p99Index, latencies.size() - 1));

            // Convert to microseconds
            double averageMicros = averageNanos / 1000.0;
            double minMicros = minNanos / 1000.0;
            double maxMicros = maxNanos / 1000.0;
            double medianMicros = medianNanos / 1000.0;
            double p99Micros = p99Nanos / 1000.0;

            return new LatencyResult(averageMicros, minMicros, maxMicros, medianMicros, p99Micros, messageSize, iterations);
        }

        /**
         * Displays latency benchmark results.
         */
        private void displayLatencyResults(LatencyResult result, String sizeLabel) {
            System.out.println();
            System.out.println("Results:");
            System.out.println("  Message Size:        " + sizeLabel + " (" + result.messageSize + " bytes)");
            System.out.println("  Iterations:          " + formatNumber(result.iterations));
            System.out.println("  Average Latency:     " + String.format("%.2f", result.averageLatency) + " μs");
            System.out.println("  Median Latency:      " + String.format("%.2f", result.medianLatency) + " μs");
            System.out.println("  Min Latency:         " + String.format("%.2f", result.minLatency) + " μs");
            System.out.println("  Max Latency:         " + String.format("%.2f", result.maxLatency) + " μs");
            System.out.println("  P99 Latency:         " + String.format("%.2f", result.p99Latency) + " μs");
            System.out.println();
            System.out.println("  Round-trips/sec:     " + formatNumber((long) (1_000_000.0 / result.averageLatency)));
            System.out.println(BENCHMARK_SEPARATOR);
            System.out.println();
        }
    }

    @Nested
    @DisplayName("DEALER-ROUTER Latency Benchmarks")
    class DealerRouterLatencyBenchmarks {

        @Test
        @DisplayName("Should measure DEALER-ROUTER latency with 1KB messages")
        void should_Measure_DealerRouter_Latency_With_1KB_Messages() throws Exception {
            runDealerRouterLatencyBenchmark(1024, "1KB");
        }

        private void runDealerRouterLatencyBenchmark(int messageSize, String sizeLabel) throws Exception {
            System.out.println(BENCHMARK_SEPARATOR);
            System.out.println("DEALER-ROUTER LATENCY BENCHMARK - Message Size: " + sizeLabel);
            System.out.println(BENCHMARK_SEPARATOR);

            // Warm-up
            System.out.println("Warming up...");
            for (int i = 0; i < WARMUP_ROUNDS; i++) {
                executeDealerRouterTest(messageSize, WARMUP_ITERATIONS, false);
                Thread.sleep(50);
            }

            System.out.println("Running benchmark...");
            Thread.sleep(500);

            // Actual benchmark
            LatencyResult result = executeDealerRouterTest(messageSize, BENCHMARK_ITERATIONS, true);

            // Display results
            displayDealerRouterResults(result, sizeLabel);

            assertThat(result.averageLatency)
                    .as("Average latency should be positive")
                    .isGreaterThan(0.0);
        }

        private LatencyResult executeDealerRouterTest(int messageSize, int iterations, boolean collectStats) throws Exception {
            try (Context ctx = new Context();
                 Socket router = new Socket(ctx, SocketType.ROUTER);
                 Socket dealer = new Socket(ctx, SocketType.DEALER)) {

                // Configure sockets
                router.setOption(SocketOption.LINGER, 0);
                dealer.setOption(SocketOption.LINGER, 0);
                dealer.setOption(SocketOption.RCVTIMEO, 5000);
                router.setOption(SocketOption.RCVTIMEO, 5000);

                // Bind and connect
                router.bind("tcp://127.0.0.1:0");
                String endpoint = router.getOptionString(SocketOption.LAST_ENDPOINT);
                dealer.connect(endpoint);

                Thread.sleep(100);

                byte[] messageData = new byte[messageSize];
                Arrays.fill(messageData, (byte) 'D');

                List<Long> latencies = collectStats ? new ArrayList<>(iterations) : null;

                // Router echo server thread
                Thread routerThread = new Thread(() -> {
                    try {
                        byte[] identity = new byte[256];
                        byte[] buffer = new byte[messageSize];

                        for (int i = 0; i < iterations; i++) {
                            // Receive identity frame
                            int idLen = router.recv(identity, RecvFlags.NONE);

                            // Receive message frame
                            int msgLen = router.recv(buffer, RecvFlags.NONE);

                            // Echo back: identity + message
                            byte[] identityFrame = Arrays.copyOf(identity, idLen);
                            byte[] messageFrame = Arrays.copyOf(buffer, msgLen);
                            router.send(identityFrame, SendFlags.SEND_MORE);
                            router.send(messageFrame, SendFlags.NONE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, "RouterEchoServer");

                routerThread.start();
                Thread.sleep(50);

                byte[] receiveBuffer = new byte[messageSize];
                long totalLatency = 0;

                // Execute cycles
                for (int i = 0; i < iterations; i++) {
                    long startNanos = System.nanoTime();

                    // Send message
                    dealer.send(messageData, SendFlags.NONE);

                    // Receive response
                    int received = dealer.recv(receiveBuffer, RecvFlags.NONE);
                    if (received != messageSize) {
                        throw new RuntimeException("Received unexpected size: " + received);
                    }

                    long endNanos = System.nanoTime();
                    long latencyNanos = endNanos - startNanos;

                    if (collectStats) {
                        latencies.add(latencyNanos);
                    }
                    totalLatency += latencyNanos;
                }

                routerThread.join(5000);

                if (!collectStats) {
                    return new LatencyResult(0, 0, 0, 0, 0, messageSize, iterations);
                }

                return calculateLatencyStatistics(latencies, messageSize, iterations);
            }
        }

        private LatencyResult calculateLatencyStatistics(List<Long> latencies, int messageSize, int iterations) {
            Collections.sort(latencies);

            long minNanos = latencies.get(0);
            long maxNanos = latencies.get(latencies.size() - 1);

            long totalNanos = 0;
            for (long latency : latencies) {
                totalNanos += latency;
            }
            double averageNanos = (double) totalNanos / latencies.size();

            int p50Index = (int) (latencies.size() * 0.50);
            int p99Index = (int) (latencies.size() * 0.99);

            long medianNanos = latencies.get(Math.min(p50Index, latencies.size() - 1));
            long p99Nanos = latencies.get(Math.min(p99Index, latencies.size() - 1));

            double averageMicros = averageNanos / 1000.0;
            double minMicros = minNanos / 1000.0;
            double maxMicros = maxNanos / 1000.0;
            double medianMicros = medianNanos / 1000.0;
            double p99Micros = p99Nanos / 1000.0;

            return new LatencyResult(averageMicros, minMicros, maxMicros, medianMicros, p99Micros, messageSize, iterations);
        }

        private void displayDealerRouterResults(LatencyResult result, String sizeLabel) {
            System.out.println();
            System.out.println("Results:");
            System.out.println("  Message Size:        " + sizeLabel + " (" + result.messageSize + " bytes)");
            System.out.println("  Iterations:          " + formatNumber(result.iterations));
            System.out.println("  Average Latency:     " + String.format("%.2f", result.averageLatency) + " μs");
            System.out.println("  Median Latency:      " + String.format("%.2f", result.medianLatency) + " μs");
            System.out.println("  Min Latency:         " + String.format("%.2f", result.minLatency) + " μs");
            System.out.println("  Max Latency:         " + String.format("%.2f", result.maxLatency) + " μs");
            System.out.println("  P99 Latency:         " + String.format("%.2f", result.p99Latency) + " μs");
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
     * Container for latency benchmark results.
     */
    private static class LatencyResult {
        final double averageLatency; // in microseconds
        final double minLatency;     // in microseconds
        final double maxLatency;     // in microseconds
        final double medianLatency;  // in microseconds
        final double p99Latency;     // in microseconds
        final int messageSize;
        final int iterations;

        LatencyResult(double averageLatency, double minLatency, double maxLatency,
                     double medianLatency, double p99Latency, int messageSize, int iterations) {
            this.averageLatency = averageLatency;
            this.minLatency = minLatency;
            this.maxLatency = maxLatency;
            this.medianLatency = medianLatency;
            this.p99Latency = p99Latency;
            this.messageSize = messageSize;
            this.iterations = iterations;
        }
    }
}
