# JMH Benchmarks

JVM-ZMQ includes JMH (Java Microbenchmark Harness) benchmarks for performance testing.

## Location

Benchmark code is in `zmq/src/jmh/java/` (not `src/main/java`).

## Available Benchmarks

1. **LatencyBenchmark** - Round-trip latency measurements
   - REQ-REP pattern
   - DEALER-ROUTER pattern
   - Various message sizes (1B, 128B, 1KB, 64KB)

2. **ThroughputBenchmark** - Message throughput measurements
   - PUB-SUB pattern
   - PUSH-PULL pattern

## Running Benchmarks

### Quick Test (2-3 minutes)
```bash
./run-benchmark.sh
```

### Full Benchmarks (20-30 minutes)
```bash
./gradlew :zmq:jmh
```

### Custom Parameters
```bash
# Specific benchmark
./gradlew :zmq:jmh -Pjmh.includes=".*reqRepLatency.*"

# Specific message size
./gradlew :zmq:jmh -Pjmh.params="messageSize=1024"

# With GC profiling
./gradlew :zmq:jmh -Pjmh.profilers=gc
```

## Results

Results are saved to: `zmq/build/reports/jmh/results.json`

## Benchmark Parameters

Current configuration (in `build.gradle.kts`):
- Warmup: 3 iterations × 2 seconds
- Measurement: 5 iterations × 5 seconds
- Forks: 1
- Threads: 1
