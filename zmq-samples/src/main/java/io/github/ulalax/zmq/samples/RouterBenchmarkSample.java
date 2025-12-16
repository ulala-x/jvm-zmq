package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.*;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Router-to-Router Performance Benchmark Sample
 *
 * <p>This sample demonstrates:</p>
 * <ul>
 *   <li>Router-to-Router one-way routing performance measurement</li>
 *   <li>High-throughput message passing with identity-based routing</li>
 *   <li>Inproc transport for maximum performance (in-process communication)</li>
 *   <li>Message frame handling with SendFlags.SEND_MORE</li>
 * </ul>
 *
 * <p>Benchmark Configuration:</p>
 * <ul>
 *   <li>Transport: inproc (in-process, lowest latency)</li>
 *   <li>Message count: 10,000</li>
 *   <li>Message size: 64 bytes (random data)</li>
 *   <li>Pattern: One-way routing (router1 → router2)</li>
 * </ul>
 *
 * <p>Message Format (Router-to-Router):</p>
 * <ul>
 *   <li>Frame 1: Target router identity (SEND_MORE flag)</li>
 *   <li>Frame 2: Message payload (64 bytes)</li>
 * </ul>
 *
 * <p>Reception Format:</p>
 * <ul>
 *   <li>Frame 1: Sender router identity</li>
 *   <li>Frame 2+: Message payload frames</li>
 * </ul>
 *
 * <p>Performance Metrics:</p>
 * <ul>
 *   <li>Total elapsed time (milliseconds)</li>
 *   <li>Average time per message (microseconds)</li>
 *   <li>Messages per second (throughput)</li>
 * </ul>
 */
public class RouterBenchmarkSample {

    private static final int MESSAGE_COUNT = 10_000;
    private static final int MESSAGE_SIZE = 64;
    private static final String ENDPOINT = "inproc://router-bench";

    public static void main(String[] args) {
        System.out.println("=== JVM-ZMQ Router-to-Router Performance Benchmark ===");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Transport:     inproc (in-process)");
        System.out.println("  Message count: " + String.format("%,d", MESSAGE_COUNT));
        System.out.println("  Message size:  " + MESSAGE_SIZE + " bytes");
        System.out.println("  Pattern:       One-way routing (router1 → router2)");
        System.out.println();

        runBenchmark();
    }

    /**
     * Run the Router-to-Router benchmark.
     * Measures the performance of one-way message routing between two ROUTER sockets.
     */
    private static void runBenchmark() {
        try (Context context = new Context();
             Socket router1 = new Socket(context, SocketType.ROUTER);
             Socket router2 = new Socket(context, SocketType.ROUTER)) {

            // Set routing identities for both routers
            byte[] router1Id = "router1".getBytes(StandardCharsets.UTF_8);
            byte[] router2Id = "router2".getBytes(StandardCharsets.UTF_8);

            router1.setOption(SocketOption.ROUTING_ID, router1Id);
            router2.setOption(SocketOption.ROUTING_ID, router2Id);

            // Configure sockets for benchmark
            router1.setOption(SocketOption.LINGER, 0);
            router2.setOption(SocketOption.LINGER, 0);

            // Bind and connect
            router1.bind(ENDPOINT);
            router2.connect(ENDPOINT);

            // Wait for connection to establish
            sleep(10);

            // Prepare random message payload
            byte[] message = new byte[MESSAGE_SIZE];
            new Random().nextBytes(message);

            System.out.println("Starting benchmark...");
            System.out.println();

            // Start benchmark
            long startTime = System.nanoTime();

            for (int i = 0; i < MESSAGE_COUNT; i++) {
                // Send: router1 → router2
                // Frame 1: Target identity (router2)
                // Frame 2: Message payload
                router1.send(router2Id, SendFlags.SEND_MORE);
                router1.send(message);

                // Receive at router2
                // Frame 1: Sender identity (router1)
                Message identityMsg = new Message();
                router2.recv(identityMsg, RecvFlags.NONE);

                // Receive remaining frames (message payload)
                while (router2.hasMore()) {
                    Message frameMsg = new Message();
                    router2.recv(frameMsg, RecvFlags.NONE);
                    frameMsg.close();
                }
                identityMsg.close();

                // Progress indicator
                if ((i + 1) % 10_000 == 0) {
                    System.out.println(String.format("  Progress: %,d/%,d", i + 1, MESSAGE_COUNT));
                }
            }

            // End benchmark
            long endTime = System.nanoTime();
            long elapsedNanos = endTime - startTime;
            long elapsedMillis = elapsedNanos / 1_000_000;
            double avgMicrosPerOp = elapsedNanos / 1_000.0 / MESSAGE_COUNT;
            double messagesPerSecond = MESSAGE_COUNT / (elapsedNanos / 1_000_000_000.0);

            // Print results
            System.out.println();
            System.out.println("=== Benchmark Results ===");
            System.out.println(String.format("Total time:       %,d ms", elapsedMillis));
            System.out.println(String.format("Average per msg:  %.3f μs/op", avgMicrosPerOp));
            System.out.println(String.format("Throughput:       %,.0f msg/s", messagesPerSecond));
            System.out.println();
            System.out.println("Benchmark completed successfully!");

        } catch (Exception e) {
            System.err.println("Error during benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to sleep without throwing checked exceptions.
     *
     * @param millis Milliseconds to sleep
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Sleep interrupted");
        }
    }
}
