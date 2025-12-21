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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

/**
 * Compares four memory management strategies for ZMQ message sending:
 * 1. ByteArray: Allocate new byte[] every time (baseline, high GC pressure)
 * 2. ArrayPool: Use Netty PooledByteBufAllocator (low GC pressure)
 * 3. Message: Use Message objects with native memory (medium GC pressure)
 * 4. MessageZeroCopy: Use msgInitData for true zero-copy (low GC pressure)
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = {"-XX:+UseG1GC", "-Xms2g", "-Xmx2g"})
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class MemoryStrategyBenchmark {

    @State(Scope.Thread)
    public static class RouterState {
        Context ctx;
        Socket router1;
        Socket router2;
        byte[] router2Id;
        byte[] sourceData;
        byte[] recvBuffer;
        byte[] identityBuffer;

        // === Reusable buffers for ArrayPool benchmark ===
        byte[] reusableSendBuffer;    // 재사용 가능한 송신 버퍼
        byte[] reusableRecvBuffer;    // 재사용 가능한 수신 버퍼

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

            sourceData = new byte[messageSize];
            Arrays.fill(sourceData, (byte) 'M');
            recvBuffer = new byte[messageSize];
            identityBuffer = new byte[64];

            // Initialize reusable buffers for ArrayPool (current messageSize)
            reusableSendBuffer = new byte[messageSize];  // 현재 메시지 크기에 맞춤
            reusableRecvBuffer = new byte[messageSize];  // 현재 메시지 크기에 맞춤

            // === PooledByteBufAllocator Pre-warming ===
            // Pre-warm for all three message sizes (64, 1500, 65536)
            // to eliminate lazy initialization overhead in benchmark iterations
            int[] allMessageSizes = {64, 1500, 65536};
            warmupByteBufAllocator(
                    allMessageSizes
            );
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
         *   <li>Expected 5-25% improvement in ArrayPool_SendRecv benchmark</li>
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
            for (int size : messageSizes) {
                for (int i = 0; i < 100; i++) {
                    ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(size);
                    warmupBuffers.add(buf);
                    buf.writeBytes(new byte[size]);
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
                int n = 0;

                while (n < state.messageCount) {
                    // First message: blocking wait
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE).value();
                    int size = state.router2.recv(state.recvBuffer, RecvFlags.NONE).value();

                    // Simulate external delivery: allocate new array (GC pressure!)
                    byte[] outputBuffer = new byte[size];
                    System.arraycopy(state.recvBuffer, 0, outputBuffer, 0, size);
                    // External consumer would use outputBuffer here

                    state.receiverLatch.countDown();
                    n++;

                    // Batch receive available messages (reduces syscalls)
                    while (n < state.messageCount) {
                        RecvResult<Integer> idResult = state.router2.recv(state.identityBuffer, RecvFlags.DONT_WAIT);
                        if (idResult.wouldBlock()) break;  // No more available

                        size = state.router2.recv(state.recvBuffer, RecvFlags.NONE).value();

                        // Simulate external delivery: create new output buffer (GC pressure!)
                        outputBuffer = new byte[size];
                        System.arraycopy(state.recvBuffer, 0, outputBuffer, 0, size);
                        // External consumer would use outputBuffer here

                        state.receiverLatch.countDown();
                        n++;
                    }
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

                byte[] sendBuffer = new byte[state.messageSize];
                System.arraycopy(state.sourceData, 0, sendBuffer, 0, state.messageSize);
                state.router1.send(sendBuffer, SendFlags.DONT_WAIT);
            }

            awaitCompletion(receiver, state);
        } catch (Exception e) {
            throw new RuntimeException("Benchmark failed", e);
        }
    }

    @Benchmark
    public void ArrayPool_SendRecv(RouterState state) {
        state.receiverLatch = new CountDownLatch(state.messageCount);
        state.receiverError = false;

        PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

        Thread receiver = new Thread(() -> {
            try {
                int n = 0;

                while (n < state.messageCount) {
                    // First message: blocking wait
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE).value();
                    int size = state.router2.recv(state.recvBuffer, RecvFlags.NONE).value();

                    // Simulate external delivery: rent from pool (minimal GC!)
                    ByteBuf outputBuf = allocator.buffer(size);
                    try {
                        outputBuf.writeBytes(state.recvBuffer, 0, size);
                        // ✅ 재사용 버퍼로 복사 (외부 소비자가 사용)
                        outputBuf.getBytes(0, state.reusableRecvBuffer, 0, size);
                    } finally {
                        outputBuf.release();
                    }

                    state.receiverLatch.countDown();
                    n++;

                    // Batch receive available messages (reduces syscalls)
                    while (n < state.messageCount) {
                        RecvResult<Integer> idResult = state.router2.recv(state.identityBuffer, RecvFlags.DONT_WAIT);
                        if (idResult.wouldBlock()) break;  // No more available

                        size = state.router2.recv(state.recvBuffer, RecvFlags.NONE).value();

                        // Simulate external delivery: rent from pool (minimal GC!)
                        outputBuf = allocator.buffer(size);
                        try {
                            outputBuf.writeBytes(state.recvBuffer, 0, size);
                            // ✅ 재사용 버퍼로 복사 (외부 소비자가 사용)
                            outputBuf.getBytes(0, state.reusableRecvBuffer, 0, size);
                        } finally {
                            outputBuf.release();
                        }

                        state.receiverLatch.countDown();
                        n++;
                    }
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

                ByteBuf sendBuf = allocator.buffer(state.messageSize);
                try {
                    sendBuf.writeBytes(state.sourceData, 0, state.messageSize);

                    // ✅ 재사용 버퍼 사용 (매번 할당 안 함!)
                    sendBuf.getBytes(0, state.reusableSendBuffer, 0, state.messageSize);
                    state.router1.send(state.reusableSendBuffer, SendFlags.DONT_WAIT);
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
                int n = 0;

                while (n < state.messageCount) {
                    // First message: blocking wait
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE).value();
                    try (Message msg = new Message()) {
                        state.router2.recv(msg, RecvFlags.NONE).value();
                        // Use msg.data() directly (no copy to managed memory)
                        // External consumer would use msg.data() here
                    }

                    state.receiverLatch.countDown();
                    n++;

                    // Batch receive available messages (reduces syscalls)
                    while (n < state.messageCount) {
                        RecvResult<Integer> idResult = state.router2.recv(state.identityBuffer, RecvFlags.DONT_WAIT);
                        if (idResult.wouldBlock()) break;  // No more available

                        // Receive into Message (native memory allocation)
                        try (Message msg = new Message()) {
                            state.router2.recv(msg, RecvFlags.NONE).value();
                            // Use msg.data() directly (no copy to managed memory)
                            // External consumer would use msg.data() here
                        }

                        state.receiverLatch.countDown();
                        n++;
                    }
                }
            } catch (Exception e) {
                state.receiverError = true;
                state.receiverException = e;
            }
        });
        receiver.start();

        try {
            for (int i = 0; i < state.messageCount; i++) {
                try (Message idMsg = new Message(state.router2Id);
                     Message payloadMsg = new Message(state.sourceData)) {
                    state.router1.send(idMsg, SendFlags.SEND_MORE);
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
                int n = 0;

                while (n < state.messageCount) {
                    // First message: blocking wait
                    state.router2.recv(state.identityBuffer, RecvFlags.NONE).value();
                    try (Message msg = new Message()) {
                        state.router2.recv(msg, RecvFlags.NONE).value();
                        // Use msg.data() directly (no copy to managed memory)
                        // External consumer would use msg.data() here
                    }

                    state.receiverLatch.countDown();
                    n++;

                    // Batch receive available messages (reduces syscalls)
                    while (n < state.messageCount) {
                        RecvResult<Integer> idResult = state.router2.recv(state.identityBuffer, RecvFlags.DONT_WAIT);
                        if (idResult.wouldBlock()) break;  // No more available

                        // Receive into Message (already zero-copy from ZMQ side)
                        try (Message msg = new Message()) {
                            state.router2.recv(msg, RecvFlags.NONE).value();
                            // Use msg.data() directly (no copy to managed memory)
                            // External consumer would use msg.data() here
                        }

                        state.receiverLatch.countDown();
                        n++;
                    }
                }
            } catch (Exception e) {
                state.receiverError = true;
                state.receiverException = e;
            }
        });
        receiver.start();

        try {
            for (int i = 0; i < state.messageCount; i++) {
                try (Message idMsg = new Message(state.router2Id)) {
                    state.router1.send(idMsg, SendFlags.SEND_MORE);
                }

                // MUST use shared arena - ZMQ callback runs on internal thread (Thread-3)
                // Confined arenas can only be closed by owner thread, would throw WrongThreadException
                // See: CALLBACK_THREAD_ANALYSIS.md for thread safety analysis
                Arena dataArena = Arena.ofShared();
                MemorySegment dataSeg = dataArena.allocate(state.messageSize);

                MemorySegment.copy(state.sourceData, 0, dataSeg,
                    JAVA_BYTE, 0, state.messageSize);

                Message payloadMsg = new Message(dataSeg, state.messageSize, data -> {
                    dataArena.close();
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
