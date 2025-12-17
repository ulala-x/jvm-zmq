# JVM-ZMQ Performance Benchmarks

Comprehensive performance analysis of jvm-zmq using JMH (Java Microbenchmark Harness) on Router-to-Router messaging pattern.

## Test Environment

- **CPU**: (System dependent)
- **Memory**: 2GB heap (-Xms2g -Xmx2g)
- **JVM**: OpenJDK 22+ with G1GC (-XX:+UseG1GC)
- **OS**: Linux/Windows/macOS
- **ZMQ Version**: libzmq 4.3.5
- **Pattern**: ROUTER-to-ROUTER
- **Messages per iteration**: 10,000
- **JMH Settings**:
  - Warmup: 2 iterations × 2 seconds
  - Measurement: 3 iterations × 5 seconds
  - Forks: 1

## Memory Strategy Benchmarks

Comparison of four memory management approaches for ZMQ message sending.

### Complete Results

| Benchmark | Message Size | Throughput (msg/sec) | Latency (μs) | JMH Score (ops/s) |
|-----------|--------------|---------------------|--------------|-------------------|
| **ByteArray_SendRecv** | 64 B | 3,712,301 ±154K | 0.27 ±0.01 | **371.23 ±15.43** |
| **ByteArray_SendRecv** | 1,500 B | 834,710 ±32K | 1.20 ±0.05 | **83.47 ±3.23** |
| **ByteArray_SendRecv** | 65,536 B | 88,855 ±1K | 11.25 ±0.15 | **8.89 ±0.12** |
| **ArrayPool_SendRecv** | 64 B | 1,833,300 ±43K | 0.55 ±0.01 | **183.33 ±4.30** |
| **ArrayPool_SendRecv** | 1,500 B | 842,413 ±24K | 1.19 ±0.03 | **84.24 ±2.43** |
| **ArrayPool_SendRecv** | 65,536 B | 91,126 ±1K | 10.97 ±0.18 | **9.11 ±0.15** |
| **Message_SendRecv** | 64 B | 1,152,431 ±18K | 0.87 ±0.01 | **115.24 ±1.90** |
| **Message_SendRecv** | 1,500 B | 861,393 ±56K | 1.16 ±0.08 | **86.14 ±5.61** |
| **Message_SendRecv** | 65,536 B | 70,024 ±18K | 14.28 ±3.74 | **7.00 ±1.84** |
| **MessageZeroCopy_SendRecv** | 64 B | 26,343 ±1K | 37.96 ±2.63 | **2.63 ±0.18** |
| **MessageZeroCopy_SendRecv** | 1,500 B | 23,872 ±772 | 41.89 ±1.35 | **2.39 ±0.08** |
| **MessageZeroCopy_SendRecv** | 65,536 B | 15,733 ±821 | 63.56 ±3.32 | **1.57 ±0.08** |

*JMH Score = ops/sec (each operation sends 10,000 messages)*
*Throughput = JMH Score × 10,000*

### Performance by Message Size

#### Small Messages (64 bytes)
```
ByteArray:        3.71M msg/sec  ████████████████████████████████████  (100% - FASTEST)
ArrayPool:        1.83M msg/sec  ██████████████████                    ( 49%)
Message:          1.15M msg/sec  ███████████                           ( 31%)
MessageZeroCopy:  26K msg/sec    ▏                                     (  1% - SLOWEST)
```

**Winner**: ByteArray (3.2× faster than Message)

#### Medium Messages (1,500 bytes)
```
Message:          861K msg/sec   ████████████████████████████████████  (100% - FASTEST)
ArrayPool:        842K msg/sec   ████████████████████████████████████  ( 98%)
ByteArray:        835K msg/sec   ███████████████████████████████████   ( 97%)
MessageZeroCopy:  24K msg/sec    ▏                                     (  3% - SLOWEST)
```

**Winner**: Message/ArrayPool/ByteArray (nearly identical)

#### Large Messages (65,536 bytes)
```
ArrayPool:        91K msg/sec    ████████████████████████████████████  (100% - FASTEST)
ByteArray:        89K msg/sec    ████████████████████████████████████  ( 98%)
Message:          70K msg/sec    ███████████████████████████           ( 77%)
MessageZeroCopy:  16K msg/sec    ██████                                ( 17% - SLOWEST)
```

**Winner**: ArrayPool (reduces GC pressure for large allocations)

### Analysis: Why ByteArray Outperforms Message for Small Messages

**Root Cause**: Buffer reuse strategy

#### ByteArray Path (Socket.java)
```java
// Socket maintains ONE reusable buffer
private Arena ioArena = Arena.ofConfined();      // Created once in constructor
private MemorySegment sendBuffer;                // Reused across all sends
private int sendBufferSize = 8192;

public int send(byte[] data, SendFlags flags) {
    // Expand buffer only if needed
    if (data.length > sendBufferSize) {
        sendBuffer = ioArena.allocate(data.length);
        sendBufferSize = data.length;
    }
    // Copy heap → native (1 operation)
    MemorySegment.copy(data, 0, sendBuffer, JAVA_BYTE, 0, data.length);
    // Send
    return LibZmq.send(getHandle(), sendBuffer, data.length, flags.getValue());
}
```

**Operations per send**: 3 (check size → copy → send)

#### Message Path (Message.java)
```java
public Message(byte[] data) {
    // Creates NEW Arena for EVERY message
    this.arena = Arena.ofConfined();                           // 1. Arena allocation
    this.msgSegment = arena.allocate(ZMQ_MSG_LAYOUT);          // 2. msgSegment allocation
    int result = LibZmq.msgInitSize(msgSegment, data.length);  // 3. Native initialization
    MemorySegment.copy(data, 0, data(), JAVA_BYTE, 0, data.length); // 4. Copy
    this.cleanable = CLEANER.register(this, ...);              // 5. Cleaner registration
    // ... later: msgClose() + Arena.close()                   // 6-7. Cleanup
}
```

**Operations per send**: 7+ (Arena create → msgSegment → msgInitSize → copy → Cleaner → send → msgClose → Arena.close)

#### Overhead Comparison

| Message Size | Copy Cost | Allocation/Init Overhead | Winner |
|--------------|-----------|-------------------------|---------|
| **64 B** | Low | **Dominates** | ByteArray (overhead >> copy) |
| **1,500 B** | Medium | Medium | Equal (overhead ≈ copy) |
| **65,536 B** | High | Low | ArrayPool (copy >> overhead, pool efficiency) |

### Recommendations

| Message Size Range | Recommended Strategy | Reason |
|--------------------|---------------------|---------|
| **< 1 KB** | `socket.send(byte[])` | Maximum throughput, minimal overhead |
| **1-8 KB** | `socket.send(byte[])` or `Message` | Similar performance |
| **> 8 KB** | `ArrayPool` pattern | Reduces GC pressure |
| **All sizes** | **AVOID** `MessageZeroCopy` | Severe performance degradation |

## Receive Mode Benchmarks

Comparison of three receive strategies for event-driven applications.

### Complete Results

| Benchmark | Mode | Message Size | Throughput (msg/sec) | Latency (μs) | JMH Score (ops/s) |
|-----------|------|--------------|---------------------|--------------|-------------------|
| **routerBenchmark** | BLOCKING | 64 B | 1,445,847 ±30K | 0.69 ±0.01 | **144.58 ±3.02** |
| **routerBenchmark** | BLOCKING | 1,500 B | 840,031 ±31K | 1.19 ±0.04 | **84.00 ±3.11** |
| **routerBenchmark** | BLOCKING | 65,536 B | 71,279 ±5K | 14.03 ±1.06 | **7.13 ±0.54** |
| **routerBenchmark** | POLLER | 64 B | 1,426,541 ±15K | 0.70 ±0.01 | **142.65 ±1.51** |
| **routerBenchmark** | POLLER | 1,500 B | 823,902 ±29K | 1.21 ±0.04 | **82.39 ±2.98** |
| **routerBenchmark** | POLLER | 65,536 B | 64,614 ±2K | 15.48 ±0.58 | **6.46 ±0.24** |
| **routerBenchmark** | NON_BLOCKING | 64 B | 1,368,165 ±12K | 0.73 ±0.01 | **136.82 ±1.20** |
| **routerBenchmark** | NON_BLOCKING | 1,500 B | 763,556 ±34K | 1.31 ±0.06 | **76.36 ±3.46** |
| **routerBenchmark** | NON_BLOCKING | 65,536 B | 30,378 ±3K | 32.92 ±3.65 | **3.04 ±0.34** |

### Performance by Mode

#### Blocking Mode
```java
// Simplest implementation - blocks until message available
socket.recv(buffer, RecvFlags.NONE);
```

**Characteristics**:
- Highest throughput (baseline)
- Thread dedicated to single socket
- Simplest implementation

#### Poller Mode
```java
// Event-driven - waits for events on multiple sockets
try (Poller poller = new Poller()) {
    int idx = poller.register(socket, PollEvents.IN);
    poller.poll(-1);  // Wait for events
    if (poller.isReadable(idx)) {
        socket.tryRecv(buffer, RecvFlags.NONE);
    }
}
```

**Characteristics**:
- **98-99%** of Blocking performance
- Multi-socket support
- **Recommended for production**

#### NonBlocking Mode
```java
// Polling with sleep fallback
try {
    socket.recv(buffer, RecvFlags.DONT_WAIT);
} catch (ZmqException e) {
    if (e.isAgain()) Thread.sleep(1);
}
```

**Characteristics**:
- Slowest (especially for large messages)
- Thread.sleep() overhead
- **Not recommended for production**

### Performance Comparison

| Message Size | Blocking | Poller | NonBlocking |
|--------------|----------|--------|-------------|
| **64 B** | 1.45M msg/sec (100%) | 1.43M msg/sec (99%) | 1.37M msg/sec (95%) |
| **1,500 B** | 840K msg/sec (100%) | 824K msg/sec (98%) | 764K msg/sec (91%) |
| **65,536 B** | 71K msg/sec (100%) | 65K msg/sec (91%) | 30K msg/sec (43%) |

### Recommendations

| Use Case | Recommended Mode | Performance |
|----------|-----------------|-------------|
| **Single socket** | Blocking | 100% (baseline) |
| **Multiple sockets** | Poller | 98-99% of Blocking |
| **Non-blocking required** | Poller (NOT NonBlocking) | 98-99% of Blocking |
| **Avoid** | NonBlocking + sleep | 43-95% of Blocking |

## Implementation Patterns

### Optimal Small Message Pattern (< 1KB)

```java
// Sender
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.DEALER)) {
    socket.connect("tcp://localhost:5555");

    byte[] data = "small message".getBytes();
    socket.send(data, SendFlags.NONE);  // ByteArray - fastest
}

// Receiver (single socket)
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.DEALER)) {
    socket.bind("tcp://*:5555");

    byte[] buffer = new byte[1024];
    int size = socket.recv(buffer, RecvFlags.NONE);  // Blocking - simplest
}
```

### Optimal Large Message Pattern (> 8KB)

```java
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

// Sender
ByteBuf sendBuf = allocator.buffer(65536);
try {
    sendBuf.writeBytes(largeData);
    byte[] array = new byte[65536];
    sendBuf.getBytes(0, array);
    socket.send(array, SendFlags.NONE);  // ArrayPool pattern
} finally {
    sendBuf.release();
}
```

### Optimal Multi-Socket Pattern

```java
try (Poller poller = new Poller()) {
    int idx1 = poller.register(socket1, PollEvents.IN);
    int idx2 = poller.register(socket2, PollEvents.IN);

    while (running) {
        if (poller.poll(1000) > 0) {
            if (poller.isReadable(idx1)) {
                // Batch receive with tryRecv
                while (socket1.tryRecv(buffer, RecvFlags.NONE) != -1) {
                    process(buffer);
                }
            }
            if (poller.isReadable(idx2)) {
                while (socket2.tryRecv(buffer, RecvFlags.NONE) != -1) {
                    process(buffer);
                }
            }
        }
    }
}
```

## Running Benchmarks

### Run All Benchmarks
```bash
./gradlew :zmq:jmh
```

### Run Specific Benchmark
```bash
# Memory strategy comparison
./gradlew :zmq:jmh -Pjmh.includes=MemoryStrategyBenchmark

# Receive mode comparison
./gradlew :zmq:jmh -Pjmh.includes=ReceiveModeBenchmark

# Specific message size
./gradlew :zmq:jmh -Pjmh.includes=MemoryStrategyBenchmark -Pjmh.params='messageSize=64'
```

### Format Results
```bash
./gradlew :zmq:formatJmhResults
```

Results saved to: `zmq/build/reports/jmh/results-formatted.txt`

### Custom JMH Options
```bash
# Longer warmup/measurement
./gradlew :zmq:jmh -Pjmh.warmupIterations=5 -Pjmh.measurementIterations=10

# Multiple forks
./gradlew :zmq:jmh -Pjmh.forks=3

# Profilers (requires JMH async-profiler setup)
./gradlew :zmq:jmh -Pjmh.profilers=gc
```

## Benchmark Source Code

All benchmark implementations are available in the repository:

- **Memory Strategy**: `zmq/src/jmh/java/io/github/ulalax/zmq/benchmark/MemoryStrategyBenchmark.java`
- **Receive Mode**: `zmq/src/jmh/java/io/github/ulalax/zmq/benchmark/ReceiveModeBenchmark.java`
- **Formatter**: `zmq/scripts/format-jmh-results.py`

## Conclusion

### Key Findings

1. **ByteArray dominates for small messages** (< 1KB) due to socket-level buffer reuse eliminating allocation overhead
2. **Poller achieves 98-99% of Blocking performance** with multi-socket support - use for production
3. **MessageZeroCopy is severely broken** - avoid completely (141× slower than ByteArray for small messages)
4. **ArrayPool wins for large messages** (> 8KB) by reducing GC pressure

### Selection Guide

**For maximum throughput**:
- Small messages: `socket.send(byte[])`
- Large messages: `ArrayPool` pattern
- Receive: `Blocking` (single socket) or `Poller` (multi-socket)

**For production systems**:
- Use `Poller` for scalability (multi-socket, 98% performance)
- Use `ByteArray` or `Message` depending on message size
- Implement batch receive with `tryRecv()` after `poll()`
- Avoid `NonBlocking` mode with `Thread.sleep()`
