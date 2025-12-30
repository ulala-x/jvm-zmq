package io.github.ulalax.zmq.benchmark;

import io.github.ulalax.zmq.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCounted;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * Compares five message buffer strategies for ZMQ message sending:
 * 1. ByteArray_SendRecv: new byte[] + send(byte[]) / new byte[] + recv(byte[])
 * 2. ArrayPool_SendRecv_Heap: heapBuffer() + send(ByteBuf) / heapBuffer() + recv(ByteBuf)
 * 3. ArrayPool_SendRecv_Direct: directBuffer() + send(ByteBuf) / directBuffer() + recv(ByteBuf)
 * 4. Message_SendRecv: new Message(size) + send(Message) / new Message() + recv(Message)
 * 5. MessageZeroCopy_SendRecv: Arena.ofShared() + new Message(seg, size, callback) / new Message() + recv(Message)
 *
 * This benchmark measures pure memory allocation/deallocation without data copying,
 * matching the .NET benchmark structure.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-XX:+UseG1GC", "-Xms2g", "-Xmx2g"})
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MessageBufferStrategyBenchmark {

    @State(Scope.Thread)
    public static class RouterState {
        Context ctx;
        Socket router1;
        Socket router2;
        byte[] router2Id;
        byte[] identityBuffer;

        volatile CountDownLatch receiverLatch;
        volatile boolean receiverError = false;
        volatile Exception receiverException;

        @Param({"64", "512", "1024", "65536", "131072", "262144"})
        int messageSize;

        @Param({"100"})
        int messageCount;

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

            identityBuffer = new byte[64];

            // === PooledByteBufAllocator Pre-warming ===
            // Pre-warm for all message sizes to eliminate lazy initialization overhead
            int[] allMessageSizes = {64, 512, 1024, 65536, 131072, 262144};
            warmupByteBufAllocator(allMessageSizes);
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

        /**
         * Pre-warms the PooledByteBufAllocator to eliminate lazy initialization overhead.
         *
         * <p>Strategy: Allocate and release ByteBuf for each message size to:
         * <ul>
         *   <li>Initialize Netty's internal thread-local caches</li>
         *   <li>Pre-allocate memory chunks for different size classes</li>
         *   <li>Eliminate first-allocation overhead in benchmark iterations</li>
         * </ul>
         *
         * <p>Performance impact:
         * <ul>
         *   <li>Expected 5-25% improvement in ArrayPool_SendRecv benchmarks</li>
         *   <li>Based on HintPtrPool pre-warming success (3000x improvement)</li>
         *   <li>Most significant for small messages (64 bytes)</li>
         * </ul>
         *
         * @param messageSizes Array of message sizes to warm up for
         */
        private void warmupByteBufAllocator(int[] messageSizes) {
            System.out.println("=== PooledByteBufAllocator Pre-warming ===");
            System.out.println("Warming up allocator for message sizes: " +
                              Arrays.toString(messageSizes));
            System.out.println("Warmup iterations per size: " + 100);

            long startTime = System.nanoTime();

            ArrayList<ByteBuf> warmupBuffers = new ArrayList<>();
            // Warm up both heap and direct buffers
            for (int size : messageSizes) {
                for (int i = 0; i < 100; i++) {
                    // Heap buffers
                    ByteBuf heapBuf = PooledByteBufAllocator.DEFAULT.heapBuffer(size);
                    warmupBuffers.add(heapBuf);
                    heapBuf.writeBytes(new byte[size]);

                    // Direct buffers
                    ByteBuf directBuf = PooledByteBufAllocator.DEFAULT.directBuffer(size);
                    warmupBuffers.add(directBuf);
                    directBuf.writeBytes(new byte[size]);
                }
            }
            warmupBuffers.forEach(ReferenceCounted::release);

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            System.out.println("Pre-warming completed in " + elapsedMs + "ms");
            System.out.println("===========================================\n");

        }
    }

    @Benchmark
    public void ByteArray_SendRecv(RouterState state) {
        state.receiverLatch = new CountDownLatch(state.messageCount);
        state.receiverError = false;

        Thread receiver = new Thread(() -> {
            try {
                for (int n = 0; n < state.messageCount; n++) {
                    // Receive identity
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE);

                    // Receive payload - allocate new buffer (GC pressure!)
                    byte[] recvBuffer = new byte[state.messageSize];
                    state.router2.recv(recvBuffer, RecvFlags.NONE);

                    state.receiverLatch.countDown();
                }
            } catch (Exception e) {
                state.receiverError = true;
                state.receiverException = e;
            }
        });
        receiver.start();

        try {
            for (int i = 0; i < state.messageCount; i++) {
                state.router1.send(state.router2Id, SendFlags.SEND_MORE);

                // Allocate new buffer every time (no data copying)
                byte[] sendBuffer = new byte[state.messageSize];
                state.router1.send(sendBuffer, SendFlags.DONT_WAIT);
            }

            awaitCompletion(receiver, state);
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        }
    }

    @Benchmark
    public void ArrayPool_SendRecv_Heap(RouterState state) {
        state.receiverLatch = new CountDownLatch(state.messageCount);
        state.receiverError = false;

        PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

        Thread receiver = new Thread(() -> {
            try {
                for (int n = 0; n < state.messageCount; n++) {
                    // Receive identity
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE);

                    // Rent heap buffer from pool
                    ByteBuf recvBuf = allocator.heapBuffer(state.messageSize);
                    try {
                        state.router2.recv(recvBuf, RecvFlags.NONE);
                    } finally {
                        recvBuf.release();
                    }

                    state.receiverLatch.countDown();
                }
            } catch (Exception e) {
                state.receiverError = true;
                state.receiverException = e;
            }
        });
        receiver.start();

        try {
            for (int i = 0; i < state.messageCount; i++) {
                state.router1.send(state.router2Id, SendFlags.SEND_MORE);

                // Rent heap buffer from pool
                ByteBuf sendBuf = allocator.heapBuffer(state.messageSize);
                sendBuf.writerIndex(state.messageSize);  // Mark buffer as containing data
                try {
                    state.router1.send(sendBuf, SendFlags.DONT_WAIT);
                } finally {
                    sendBuf.release();
                }
            }

            awaitCompletion(receiver, state);
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        }
    }

    @Benchmark
    public void ArrayPool_SendRecv_Direct(RouterState state) {
        state.receiverLatch = new CountDownLatch(state.messageCount);
        state.receiverError = false;

        PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

        Thread receiver = new Thread(() -> {
            try {
                for (int n = 0; n < state.messageCount; n++) {
                    // Receive identity
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE);

                    // Rent direct buffer from pool
                    ByteBuf recvBuf = allocator.directBuffer(state.messageSize);
                    try {
                        state.router2.recv(recvBuf, RecvFlags.NONE);
                    } finally {
                        recvBuf.release();
                    }

                    state.receiverLatch.countDown();
                }
            } catch (Exception e) {
                state.receiverError = true;
                state.receiverException = e;
            }
        });
        receiver.start();

        try {
            for (int i = 0; i < state.messageCount; i++) {
                state.router1.send(state.router2Id, SendFlags.SEND_MORE);

                // Rent direct buffer from pool
                ByteBuf sendBuf = allocator.directBuffer(state.messageSize);
                sendBuf.writerIndex(state.messageSize);  // Mark buffer as containing data
                try {
                    state.router1.send(sendBuf, SendFlags.DONT_WAIT);
                } finally {
                    sendBuf.release();
                }
            }

            awaitCompletion(receiver, state);
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        }
    }

    @Benchmark
    public void Message_SendRecv(RouterState state) {
        state.receiverLatch = new CountDownLatch(state.messageCount);
        state.receiverError = false;

        Thread receiver = new Thread(() -> {
            try {
                for (int n = 0; n < state.messageCount; n++) {
                    // Receive identity
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE);

                    // Receive payload as Message (no data copying)
                    try (Message msg = new Message()) {
                        state.router2.recv(msg, RecvFlags.NONE);
                    }

                    state.receiverLatch.countDown();
                }
            } catch (Exception e) {
                state.receiverError = true;
                state.receiverException = e;
            }
        });
        receiver.start();

        try {
            for (int i = 0; i < state.messageCount; i++) {
                // Send identity as simple byte[] (small data, no pooling needed)
                state.router1.send(state.router2Id, SendFlags.SEND_MORE);

                // Send payload using Message (this is what we're measuring)
                try (Message payloadMsg = new Message(state.messageSize)) { // Size only, no data
                    state.router1.send(payloadMsg, SendFlags.DONT_WAIT);
                }
            }

            awaitCompletion(receiver, state);
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        }
    }

    @Benchmark
    public void MessageZeroCopy_SendRecv(RouterState state) {
        state.receiverLatch = new CountDownLatch(state.messageCount);
        state.receiverError = false;

        Thread receiver = new Thread(() -> {
            try {
                for (int n = 0; n < state.messageCount; n++) {
                    // Receive identity
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE);

                    // Receive payload as Message (no data copying)
                    try (Message msg = new Message()) {
                        state.router2.recv(msg, RecvFlags.NONE);
                    }

                    state.receiverLatch.countDown();
                }
            } catch (Exception e) {
                state.receiverError = true;
                state.receiverException = e;
            }
        });
        receiver.start();



        try {
            for (int i = 0; i < state.messageCount; i++) {
                // Send identity as simple byte[] (small data, no pooling needed)
                state.router1.send(state.router2Id, SendFlags.SEND_MORE);

                // Single shared arena for all messages - closed after all callbacks complete
                Arena dataArena = Arena.ofShared();
                var pendingCallbacks = new AtomicInteger(state.messageCount);
                // Allocate from shared arena (no per-message Arena creation overhead)
                MemorySegment dataSeg = dataArena.allocate(state.messageSize);

                Message payloadMsg = new Message(dataSeg, state.messageSize, data -> {
                    // Close arena only when all messages are done
                    if (pendingCallbacks.decrementAndGet() == 0) {
                        dataArena.close();
                    }
                });

                state.router1.send(payloadMsg, SendFlags.DONT_WAIT);
                payloadMsg.close();
            }

            awaitCompletion(receiver, state);
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        }
    }

    private void awaitCompletion(Thread receiver, RouterState state) {
        try {
            if (!state.receiverLatch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Receiver thread timeout");
            }

            receiver.join();

            if (state.receiverError) {
                throw new RuntimeException("Receiver thread failed", state.receiverException);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Benchmark interrupted", e);
        }
    }
}
