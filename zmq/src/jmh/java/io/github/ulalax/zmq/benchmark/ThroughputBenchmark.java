package io.github.ulalax.zmq.benchmark;

import io.github.ulalax.zmq.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JMH Throughput Benchmarks for ZMQ messaging patterns.
 * <p>
 * Measures message processing throughput (messages/sec and MB/sec)
 * using PUSH-PULL and PUB-SUB patterns with various message sizes.
 * <p>
 * Run with: ./gradlew :zmq:jmh
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ThroughputBenchmark {

    /**
     * State class for PUSH-PULL pattern benchmarks.
     */
    @State(Scope.Thread)
    public static class PushPullState {
        Context ctx;
        Socket pusher;
        Socket puller;
        byte[] messageData;
        byte[] receiveBuffer;
        Thread receiverThread;
        volatile boolean stopReceiver = false;

        @Param({"1", "128", "1024", "65536"})
        int messageSize;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            ctx = new Context();
            pusher = new Socket(ctx, SocketType.PUSH);
            puller = new Socket(ctx, SocketType.PULL);

            // Configure sockets for maximum throughput
            pusher.setOption(SocketOption.SNDHWM, 0);
            puller.setOption(SocketOption.RCVHWM, 0);
            pusher.setOption(SocketOption.LINGER, 0);

            // Bind and connect
            puller.bind("tcp://127.0.0.1:0");
            String endpoint = puller.getOptionString(SocketOption.LAST_ENDPOINT);
            pusher.connect(endpoint);

            Thread.sleep(100); // Allow connection to establish

            // Prepare message data
            messageData = new byte[messageSize];
            receiveBuffer = new byte[messageSize];
            Arrays.fill(messageData, (byte) 'A');

            // Start background receiver thread
            startBackgroundReceiver();
        }

        private void startBackgroundReceiver() {
            CountDownLatch receiverReady = new CountDownLatch(1);
            stopReceiver = false;

            receiverThread = new Thread(() -> {
                try {
                    receiverReady.countDown();
                    byte[] buffer = new byte[messageSize];

                    while (!stopReceiver) {
                        try {
                            puller.recv(buffer, RecvFlags.DONT_WAIT);
                        } catch (Exception e) {
                            // Ignore EAGAIN errors
                            Thread.sleep(1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "ThroughputReceiver");

            receiverThread.start();
            try {
                receiverReady.await(5, TimeUnit.SECONDS);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            stopReceiver = true;
            if (receiverThread != null) {
                receiverThread.join(5000);
            }

            if (pusher != null) pusher.close();
            if (puller != null) puller.close();
            if (ctx != null) ctx.close();
        }
    }

    /**
     * State class for PUB-SUB pattern benchmarks.
     */
    @State(Scope.Thread)
    public static class PubSubState {
        Context ctx;
        Socket publisher;
        Socket subscriber;
        byte[] messageData;
        Thread subscriberThread;
        volatile boolean stopSubscriber = false;

        @Param({"1024"})
        int messageSize;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            ctx = new Context();
            publisher = new Socket(ctx, SocketType.PUB);
            subscriber = new Socket(ctx, SocketType.SUB);

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

            // Prepare message data
            messageData = new byte[messageSize];
            Arrays.fill(messageData, (byte) 'B');

            // Start background subscriber thread
            startBackgroundSubscriber();
        }

        private void startBackgroundSubscriber() {
            CountDownLatch subscriberReady = new CountDownLatch(1);
            stopSubscriber = false;

            subscriberThread = new Thread(() -> {
                try {
                    subscriberReady.countDown();
                    byte[] buffer = new byte[messageSize];

                    while (!stopSubscriber) {
                        try {
                            subscriber.recv(buffer, RecvFlags.DONT_WAIT);
                        } catch (Exception e) {
                            // Ignore EAGAIN errors
                            Thread.sleep(1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "PubSubSubscriber");

            subscriberThread.start();
            try {
                subscriberReady.await(5, TimeUnit.SECONDS);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            stopSubscriber = true;
            if (subscriberThread != null) {
                subscriberThread.join(5000);
            }

            if (publisher != null) publisher.close();
            if (subscriber != null) subscriber.close();
            if (ctx != null) ctx.close();
        }
    }

    /**
     * Benchmark for PUSH-PULL throughput.
     * <p>
     * Measures how many messages can be sent per second.
     */
    @Benchmark
    public void pushPullThroughput(PushPullState state, Blackhole blackhole) {
        try {
            state.pusher.send(state.messageData, SendFlags.NONE);
            blackhole.consume(state.messageData);
        } catch (Exception e) {
            throw new RuntimeException("Send failed", e);
        }
    }

    /**
     * Benchmark for PUB-SUB throughput.
     * <p>
     * Measures how many messages can be published per second.
     */
    @Benchmark
    public void pubSubThroughput(PubSubState state, Blackhole blackhole) {
        try {
            state.publisher.send(state.messageData, SendFlags.NONE);
            blackhole.consume(state.messageData);
        } catch (Exception e) {
            throw new RuntimeException("Publish failed", e);
        }
    }
}
