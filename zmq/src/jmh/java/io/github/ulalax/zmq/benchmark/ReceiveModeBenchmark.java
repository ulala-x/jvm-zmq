package io.github.ulalax.zmq.benchmark;

import io.github.ulalax.zmq.*;
import io.github.ulalax.zmq.core.*;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Compares receive strategies for ROUTER-to-ROUTER multipart messaging:
 *
 * 1. Blocking: Thread blocks on Recv() until message available
 *    - Highest throughput (baseline)
 *    - Simplest implementation
 *    - Thread dedicated to single socket
 *
 * 2. NonBlocking (Sleep 1ms): TryRecv() with Thread.Sleep(1ms) fallback
 *    - No blocking, but Thread.Sleep() adds overhead
 *    - Slower than Blocking/Poller
 *    - Not recommended for production
 *
 * 3. Poller: Event-driven with zmq_poll()
 *    - Similar to Blocking performance
 *    - Multi-socket support
 *    - Recommended for production use
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-XX:+UseG1GC", "-Xms2g", "-Xmx2g"})
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class ReceiveModeBenchmark {

    public enum ReceiveMode {
        BLOCKING,
        NON_BLOCKING,
        POLLER
    }

    @State(Scope.Thread)
    public static class RouterState {
        Context ctx;
        Socket router1;
        Socket router2;
        byte[] router2Id;
        byte[] messageData;
        byte[] recvBuffer;
        byte[] identityBuffer;

        volatile CountDownLatch receiverLatch;
        volatile boolean receiverError = false;
        volatile Exception receiverException;

        @Param({"64", "1500", "65536"})
        int messageSize;

        @Param({"10000"})
        int messageCount;

        @Param({"BLOCKING", "NON_BLOCKING", "POLLER"})
        ReceiveMode mode;

        @Setup(Level.Trial)
        public void setup() throws Exception {
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

            messageData = new byte[messageSize];
            Arrays.fill(messageData, (byte) 'R');
            recvBuffer = new byte[65536];
            identityBuffer = new byte[256];
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            if (router1 != null) {
                router1.setOption(SocketOption.LINGER, 0);
                router1.close();
            }
            if (router2 != null) {
                router2.setOption(SocketOption.LINGER, 0);
                router2.close();
            }
            if (ctx != null) ctx.close();
        }
    }

    /**
     * Blocking receive mode - highest performance, simplest implementation.
     * Uses blocking Recv() for first message, then batch-processes available messages
     * with TryRecv() to minimize syscall overhead.
     */
    private void receiveBlocking(Socket socket, RouterState state) {
        try {
            int n = 0;
            while (n < state.messageCount) {
                // First message: blocking wait (maintains Blocking semantics)
                socket.recv(state.identityBuffer, RecvFlags.NONE);
                socket.recv(state.recvBuffer, RecvFlags.NONE);
                state.receiverLatch.countDown();
                n++;

                // Batch receive available messages (reduces syscalls)
                while (n < state.messageCount) {
                    int idSize = socket.tryRecv(state.identityBuffer, RecvFlags.NONE);
                    if (idSize == -1) break;  // No more available

                    socket.tryRecv(state.recvBuffer, RecvFlags.NONE);
                    state.receiverLatch.countDown();
                    n++;
                }
            }
        } catch (Exception e) {
            state.receiverError = true;
            state.receiverException = e;
        }
    }

    /**
     * Non-blocking receive mode with Thread.Sleep(1ms) fallback.
     * Slower than Blocking/Poller due to polling overhead.
     * Not recommended for production use.
     */
    private void receiveNonBlocking(Socket socket, RouterState state) {
        try {
            int n = 0;
            while (n < state.messageCount) {
                try {
                    socket.recv(state.identityBuffer, RecvFlags.DONT_WAIT);
                    socket.recv(state.recvBuffer, RecvFlags.DONT_WAIT);
                    state.receiverLatch.countDown();
                    n++;

                    // Batch receive without sleep
                    while (n < state.messageCount) {
                        try {
                            socket.recv(state.identityBuffer, RecvFlags.DONT_WAIT);
                            socket.recv(state.recvBuffer, RecvFlags.DONT_WAIT);
                            state.receiverLatch.countDown();
                            n++;
                        } catch (ZmqException e) {
                            if (!e.isAgain()) throw e;
                            break;
                        }
                    }
                } catch (ZmqException e) {
                    if (!e.isAgain()) throw e;
                    Thread.sleep(1);  // Wait before retry
                }
            }
        } catch (Exception e) {
            state.receiverError = true;
            state.receiverException = e;
        }
    }

    /**
     * Poller-based receive mode - event-driven approach using zmq_poll().
     * Achieves 98-99% of Blocking performance with multi-socket support.
     * Recommended for production use.
     */
    private void receivePoller(Socket socket, RouterState state) {
        try (Poller poller = new Poller()) {
            int idx = poller.register(socket, PollEvents.IN);
            int n = 0;

            while (n < state.messageCount) {
                poller.poll(-1);  // Wait for events

                // Batch receive all available messages
                while (n < state.messageCount) {
                    int idSize = socket.tryRecv(state.identityBuffer, RecvFlags.NONE);
                    if (idSize == -1) break;  // No more available

                    socket.tryRecv(state.recvBuffer, RecvFlags.NONE);
                    state.receiverLatch.countDown();
                    n++;
                }
            }
        } catch (Exception e) {
            state.receiverError = true;
            state.receiverException = e;
        }
    }

    @Benchmark
    public void routerBenchmark(RouterState state) {
        state.receiverLatch = new CountDownLatch(state.messageCount);
        state.receiverError = false;
        state.receiverException = null;

        Thread receiverThread = new Thread(() -> {
            switch (state.mode) {
                case BLOCKING -> receiveBlocking(state.router2, state);
                case NON_BLOCKING -> receiveNonBlocking(state.router2, state);
                case POLLER -> receivePoller(state.router2, state);
            }
        });

        receiverThread.start();

        try {
            // Sender
            for (int i = 0; i < state.messageCount; i++) {
                try (Message sendId = new Message(state.router2Id);
                     Message sendContent = new Message(state.messageData)) {
                    state.router1.send(sendId, SendFlags.SEND_MORE);
                    state.router1.send(sendContent, SendFlags.DONT_WAIT);
                }
            }

            if (!state.receiverLatch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Receiver timeout");
            }

            receiverThread.join();

            if (state.receiverError) {
                throw new RuntimeException("Receiver error", state.receiverException);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        }
    }
}
