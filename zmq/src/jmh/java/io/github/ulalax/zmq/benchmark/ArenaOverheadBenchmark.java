package io.github.ulalax.zmq.benchmark;

import org.openjdk.jmh.annotations.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark to measure Arena.ofShared() creation overhead.
 * Compares Arena creation alone vs Arena creation + allocation.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(1)
@State(Scope.Thread)
public class ArenaOverheadBenchmark {

    /**
     * Baseline: Only Arena.ofShared() creation + close
     * This measures pure Arena lifecycle overhead
     */
    @Benchmark
    public void arenaCreationOnly() {
        Arena arena = Arena.ofShared();
        arena.close();
    }

    /**
     * Arena.ofShared() + 64B allocation + close
     */
    @Benchmark
    public void arenaWith64ByteAllocation() {
        Arena arena = Arena.ofShared();
        MemorySegment seg = arena.allocate(64);
        arena.close();
    }

    /**
     * Arena.ofShared() + 1KB allocation + close
     */
    @Benchmark
    public void arenaWith1KBAllocation() {
        Arena arena = Arena.ofShared();
        MemorySegment seg = arena.allocate(1024);
        arena.close();
    }

    /**
     * Arena.ofShared() + 64KB allocation + close
     */
    @Benchmark
    public void arenaWith64KBAllocation() {
        Arena arena = Arena.ofShared();
        MemorySegment seg = arena.allocate(65536);
        arena.close();
    }

    /**
     * Arena.ofConfined() creation + close (for comparison)
     * Expected to be much faster than ofShared()
     */
    @Benchmark
    public void arenaConfinedCreationOnly() {
        Arena arena = Arena.ofConfined();
        arena.close();
    }

    /**
     * Arena.ofConfined() + 64KB allocation + close
     */
    @Benchmark
    public void arenaConfinedWith64KBAllocation() {
        Arena arena = Arena.ofConfined();
        MemorySegment seg = arena.allocate(65536);
        arena.close();
    }

    /**
     * Reuse single Arena.ofShared() for multiple allocations
     * This simulates the optimal pattern
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 1, batchSize = 10000)
    public void reuseSharedArena10000Allocations() {
        Arena arena = Arena.ofShared();
        for (int i = 0; i < 10000; i++) {
            MemorySegment seg = arena.allocate(64);
        }
        arena.close();
    }

    /**
     * Create new Arena.ofShared() for each allocation (current MessageZeroCopy pattern)
     * This simulates the problematic pattern
     */
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 1, batchSize = 10000)
    public void createSharedArenaPerAllocation10000Times() {
        for (int i = 0; i < 10000; i++) {
            Arena arena = Arena.ofShared();
            MemorySegment seg = arena.allocate(64);
            arena.close();
        }
    }
}
