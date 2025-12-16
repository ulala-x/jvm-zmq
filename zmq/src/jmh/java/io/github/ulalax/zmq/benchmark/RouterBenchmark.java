package io.github.ulalax.zmq.benchmark;

import io.github.ulalax.zmq.*;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for ZMQ Router-to-Router pattern.
 * <p>
 * Measures throughput (messages/sec) for Router-to-Router pattern
 * with parallel send/receive operations using separate threads.
 * <p>
 * This benchmark follows the C# ThroughputBenchmarks.cs pattern:
 * - Main thread sends messageCount messages
 * - Receiver thread receives messageCount messages in parallel
 * - Measures true throughput, not round-trip latency
 * <p>
 * Run with: ./gradlew :zmq:jmh
 */
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class RouterBenchmark {

    /**
     * State class for Router-to-Router throughput benchmarks.
     */
    @State(Scope.Thread)
    public static class RouterState {
        Context ctx;
        Socket router1;
        Socket router2;
        byte[] router2Id;
        byte[] messageData;

        Thread receiverThread;
        volatile CountDownLatch receiverLatch;
        volatile boolean receiverError = false;
        volatile Exception receiverException;

        @Param({"64", "1500", "65536"})
        int messageSize;

        @Param({"10000"})
        int messageCount;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            ctx = new Context();
            router1 = new Socket(ctx, SocketType.ROUTER);
            router2 = new Socket(ctx, SocketType.ROUTER);

            // Set routing IDs
            byte[] router1Id = "r1".getBytes(StandardCharsets.UTF_8);
            router2Id = "r2".getBytes(StandardCharsets.UTF_8);
            router1.setOption(SocketOption.ROUTING_ID, router1Id);
            router2.setOption(SocketOption.ROUTING_ID, router2Id);

            // Configure for max throughput
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

            Thread.sleep(100); // Allow connection

            // Handshake: Router2 -> Router1
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

            // Prepare message data
            messageData = new byte[messageSize];
            Arrays.fill(messageData, (byte) 'R');
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            // Close sockets
            if (router1 != null) {
                router1.setOption(SocketOption.LINGER, 0);
                router1.close();
            }
            if (router2 != null) {
                router2.setOption(SocketOption.LINGER, 0);
                router2.close();
            }

            // Close context
            if (ctx != null) ctx.close();
        }
    }

    /**
     * Router-to-Router throughput benchmark.
     * <p>
     * Measures throughput by sending messageCount 2-frame messages (identity + payload)
     * from router1 to router2 in parallel with a receiver thread.
     * <p>
     * Pattern:
     * 1. Start receiver thread that receives messageCount messages
     * 2. Main thread sends messageCount messages
     * 3. Wait for receiver thread to complete
     * <p>
     * This measures true throughput, not round-trip latency.
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void routerBenchmarkMessage(RouterState state) {
        try {
            // Create latch for this iteration
            state.receiverLatch = new CountDownLatch(state.messageCount);
            state.receiverError = false;
            state.receiverException = null;

            // Start receiver thread
            state.receiverThread = new Thread(() -> {
                try {
                    Message identityBuffer = new Message();
                    Message contentBuffer = new Message();

                    for (int i = 0; i < state.messageCount; i++) {
                        // Receive 2-frame message: identity + content
                        state.router2.recv(identityBuffer, RecvFlags.NONE);
                        state.router2.recv(contentBuffer, RecvFlags.NONE);

                        // Clear messages for reuse
                        identityBuffer.close();
                        contentBuffer.close();
                        identityBuffer = new Message();
                        contentBuffer = new Message();

                        state.receiverLatch.countDown();
                    }

                    identityBuffer.close();
                    contentBuffer.close();
                } catch (Exception e) {
                    state.receiverError = true;
                    state.receiverException = e;
                }
            });

            state.receiverThread.start();

            // Send messageCount 2-frame messages
            for (int i = 0; i < state.messageCount; i++) {
                try (Message sendId = new Message(state.router2Id);
                     Message sendContent = new Message(state.messageData)) {
                    state.router1.send(sendId, SendFlags.SEND_MORE);
                    state.router1.send(sendContent, SendFlags.NONE);
                }
            }

            // Wait for receiver thread to finish
            if (!state.receiverLatch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Receiver thread timeout - only received " +
                    (state.messageCount - state.receiverLatch.getCount()) + " messages");
            }

            state.receiverThread.join();

            // Check for receiver errors
            if (state.receiverError) {
                throw new RuntimeException("Receiver thread failed", state.receiverException);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Benchmark interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Router benchmark failed", e);
        }
    }

}
