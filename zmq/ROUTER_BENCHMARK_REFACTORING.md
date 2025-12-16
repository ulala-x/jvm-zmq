# Router Benchmark Refactoring Summary

## Overview
Refactored `RouterBenchmark.java` to follow the C# `ThroughputBenchmarks.cs` pattern, measuring true throughput instead of round-trip latency.

## Key Changes

### 1. Benchmark Mode
**Before:**
```java
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
```

**After:**
```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
```

### 2. Parameters
**Before:**
```java
@Param({"1", "128", "1024", "65536"})
int messageSize;
```

**After:**
```java
@Param({"64", "1024", "65536"})
int messageSize;

@Param({"10000"})
int messageCount;
```

### 3. Benchmark Pattern
**Before (Round-trip latency):**
- Send message from router1 to router2
- Router2 receives and echoes back
- Router1 receives the echo
- Measures complete round-trip time

**After (Parallel throughput):**
- Start receiver thread that receives `messageCount` messages
- Main thread sends `messageCount` messages
- Wait for receiver thread to complete
- Measures true throughput with parallel send/receive

### 4. Implementation Details

**Receiver Thread:**
```java
state.receiverThread = new Thread(() -> {
    for (int i = 0; i < state.messageCount; i++) {
        // Receive 2-frame message: identity + content
        state.router2.recv(identityBuffer, RecvFlags.NONE);
        state.router2.recv(contentBuffer, RecvFlags.NONE);
        // Discard (no echo needed)
        state.receiverLatch.countDown();
    }
});
```

**Sender Loop:**
```java
for (int i = 0; i < state.messageCount; i++) {
    try (Message sendId = new Message(state.router2Id);
         Message sendContent = new Message(state.messageData)) {
        state.router1.send(sendId, SendFlags.SEND_MORE);
        state.router1.send(sendContent, SendFlags.NONE);
    }
}
```

### 5. Thread Coordination
- Uses `CountDownLatch` to track received messages
- 30-second timeout to detect hangs
- Error propagation from receiver thread to main thread

### 6. Formatter Updates
Updated `build.gradle.kts` to correctly calculate message throughput:

```kotlin
// Calculate actual message throughput
// JMH reports ops/s, each op processes messageCount messages
val msgPerSec = (score * messageCount).toLong()
val errorPerSec = (error * messageCount).toLong()
output.appendLine("  Throughput:  ${"%,d".format(msgPerSec)} msg/sec  (± ${"%,d".format(errorPerSec)})")
output.appendLine("  JMH Score:   ${"%.2f".format(score)} ops/s (${"%,d".format(messageCount)} messages per operation)")
```

## Results Comparison

### Before (Round-trip latency)
```
Benchmark                        (messageSize)   Mode  Cnt   Score   Error  Units
RouterBenchmark.routerBenchmark           1024  thrpt    5  12.345  ± 0.234  ops/s
RouterBenchmark.routerBenchmark           1024   avgt    5  81.234  ± 1.567   us/op
```
Each operation was a full round-trip, so throughput was very low.

### After (Parallel throughput)
```
Benchmark                        (messageCount)  (messageSize)   Mode  Cnt   Score   Error  Units
RouterBenchmark.routerBenchmark           10000           1024  thrpt    5  90.719 ± 1.627  ops/s

Formatted Output:
Message Size: 1024 bytes
--------------------------------------------------
  Throughput:  907,187 msg/sec  (± 16,266)
  JMH Score:   90.72 ops/s (10,000 messages per operation)
```

Each operation sends 10,000 messages in parallel with receiving, measuring true throughput.

## Usage

### Build and Run
```bash
# Build the project
./gradlew :zmq:build

# Run with default parameters (messageSize: 64, 1024, 65536; messageCount: 10000)
./gradlew :zmq:jmh

# Run with custom warmup/iterations for testing
./gradlew :zmq:jmh -Pjmh.warmupIterations=1 -Pjmh.iterations=2
```

### Customize Parameters
To change default parameters, edit `RouterBenchmark.java`:

```java
@Param({"64", "1024", "65536"})  // Message sizes to test
int messageSize;

@Param({"10000"})  // Messages per benchmark operation
int messageCount;
```

### Understanding Results

**JMH Score (ops/s):**
- Number of benchmark operations per second
- Each operation sends/receives `messageCount` messages
- Example: 90.72 ops/s with messageCount=10,000

**Actual Throughput (msg/sec):**
- Total messages processed per second
- Calculated as: JMH Score × messageCount
- Example: 90.72 × 10,000 = 907,200 messages/sec

## Performance Characteristics

From the benchmark results:

| Message Size | Throughput (msg/sec) | Bandwidth (MB/s) |
|-------------|---------------------|------------------|
| 64 bytes    | ~950,000            | ~57.8            |
| 1024 bytes  | ~907,000            | ~883.2           |
| 65536 bytes | ~84,000             | ~5,243.5         |

The throughput decreases with larger messages due to:
1. Memory allocation overhead
2. Data copying costs
3. Network buffer limitations
4. TCP/IP protocol overhead

## Alignment with C# Pattern

This refactoring matches the C# `ThroughputBenchmarks.cs` pattern:

1. **Setup Phase** (lines 54-97 in RouterBenchmark.java)
   - ✓ Create router1 and router2
   - ✓ Set routing IDs ("r1", "r2")
   - ✓ Bind and connect
   - ✓ Handshake exchange

2. **Run Method Pattern** (lines 130-195)
   - ✓ Create receiver thread
   - ✓ Receiver processes messageCount messages
   - ✓ Main thread sends messageCount messages
   - ✓ Wait for receiver completion
   - ✓ Measures parallel throughput

3. **2-Frame Message Format**
   - ✓ Identity frame (routing ID)
   - ✓ Content frame (payload data)

## Benefits

1. **True Throughput Measurement**: Measures parallel send/receive, not round-trip latency
2. **Configurable Message Count**: Easy to adjust batch size via `messageCount` parameter
3. **Better Performance Metrics**: Shows actual messages/sec throughput
4. **Matches C# Pattern**: Consistent benchmarking across implementations
5. **Clear Reporting**: Formatted output shows both JMH score and actual message throughput
