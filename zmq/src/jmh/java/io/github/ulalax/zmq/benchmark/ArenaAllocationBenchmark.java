package io.github.ulalax.zmq.benchmark;

import io.github.ulalax.zmq.Message;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmark to measure Arena allocation overhead and Message creation costs.
 * Compares Arena.ofShared() vs Arena.ofConfined() and Message with/without callbacks.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 5)
@Fork(1)
@State(Scope.Thread)
public class ArenaAllocationBenchmark {

    private byte[] sourceData;

    @Setup(Level.Trial)
    public void setup() {
        sourceData = new byte[64];
        for (int i = 0; i < 64; i++) {
            sourceData[i] = (byte) i;
        }
    }

    /**
     * Baseline: Arena.ofShared() creation + allocation + close
     */
    @Benchmark
    public void arenaSharedAllocation() {
        Arena arena = Arena.ofShared();
        MemorySegment seg = arena.allocate(64);
        arena.close();
    }

    /**
     * Baseline: Arena.ofConfined() creation + allocation + close
     */
    @Benchmark
    public void arenaConfinedAllocation() {
        Arena arena = Arena.ofConfined();
        MemorySegment seg = arena.allocate(64);
        arena.close();
    }

    /**
     * Message creation with callback (current MessageZeroCopy pattern)
     */
    @Benchmark
    public void messageWithCallback() {
        Arena dataArena = Arena.ofShared();
        MemorySegment dataSeg = dataArena.allocate(64);
        MemorySegment.copy(sourceData, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, 64);

        Message msg = new Message(dataSeg, 64, data -> {
            dataArena.close();
        });
        msg.close();
    }

    /**
     * Message creation without callback
     */
    @Benchmark
    public void messageWithoutCallback() {
        Arena dataArena = Arena.ofShared();
        MemorySegment dataSeg = dataArena.allocate(64);
        MemorySegment.copy(sourceData, 0, dataSeg, ValueLayout.JAVA_BYTE, 0, 64);

        Message msg = new Message(dataSeg, 64, null);
        msg.close();
        dataArena.close();
    }

    /**
     * Multiple Arena allocations (simulating overhead)
     */
    @Benchmark
    public void multipleArenaAllocation() {
        // Data arena
        Arena dataArena = Arena.ofShared();
        MemorySegment dataSeg = dataArena.allocate(64);

        // msgSegment arena
        Arena msgArena = Arena.ofConfined();
        MemorySegment msgSeg = msgArena.allocate(64);

        // hint arena
        Arena hintArena = Arena.ofShared();
        MemorySegment hintSeg = hintArena.allocate(8);

        hintArena.close();
        msgArena.close();
        dataArena.close();
    }

    /**
     * Single Arena with multiple allocations (optimal pattern)
     */
    @Benchmark
    public void singleArenaMultipleAllocation() {
        Arena arena = Arena.ofShared();
        MemorySegment dataSeg = arena.allocate(64);
        MemorySegment msgSeg = arena.allocate(64);
        MemorySegment hintSeg = arena.allocate(8);
        arena.close();
    }
}
