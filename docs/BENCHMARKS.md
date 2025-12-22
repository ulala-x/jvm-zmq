[![English](https://img.shields.io/badge/lang-en-red.svg)](BENCHMARKS.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](BENCHMARKS.ko.md)

# JVM-ZMQ Performance Benchmarks

Comprehensive performance benchmarks for JVM-ZMQ using JMH (Java Microbenchmark Harness).

## Test Environment

- **OS**: Ubuntu 24.04 LTS
- **CPU**: x86_64
- **JVM**: Java HotSpot(TM) 64-Bit Server VM, JDK 22.0.2
- **JMH**: v1.37
- **Pattern**: Router-to-Router (tcp transport)
- **Messages per iteration**: 10,000
- **Warmup**: 3 iterations (2s each)
- **Measurement**: 5 iterations (5s each)

## Message Buffer Strategy Benchmarks

Comparison of four buffer management strategies for sending/receiving messages.

### Performance Overview

| Method                   | Size    | Throughput | Msg/sec | Mean      | Ratio | Allocated | Alloc Ratio |
|------------------------- |--------:|------------|--------:|----------:|------:|----------:|------------:|
| ByteArray_SendRecv       |     64B | 876.11 Mbps |   1.71M |    5.84 ms |  1.00 |   1.53 MB |       1.00 |
| ArrayPool_SendRecv       |     64B | 660.21 Mbps |   1.29M |    7.76 ms |  1.33 | 178.36 KB |       0.11 |
| MessageZeroCopy_SendRecv |     64B | 14.21 Mbps |  27.75K |  360.30 ms | 61.65 |  10.86 MB |       7.11 |
| Message_SendRecv         |     64B | 543.56 Mbps |   1.06M |    9.42 ms |  1.61 |   7.55 MB |       4.95 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    512B |  5.45 Gbps |   1.33M |    7.51 ms |  1.00 |  10.07 MB |       1.00 |
| ArrayPool_SendRecv       |    512B |  5.26 Gbps |   1.28M |    7.78 ms |  1.04 | 179.11 KB |       0.02 |
| MessageZeroCopy_SendRecv |    512B | 106.89 Mbps |  26.10K |  383.19 ms | 51.00 |  11.14 MB |       1.11 |
| Message_SendRecv         |    512B |  4.07 Gbps | 993.51K |   10.07 ms |  1.34 |   7.55 MB |       0.75 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |     1KB |  8.51 Gbps |   1.04M |    9.63 ms |  1.00 |  19.84 MB |       1.00 |
| ArrayPool_SendRecv       |     1KB |  8.07 Gbps | 984.60K |   10.16 ms |  1.06 | 179.62 KB |       0.01 |
| MessageZeroCopy_SendRecv |     1KB | 210.75 Mbps |  25.73K |  388.70 ms | 40.38 |  11.20 MB |       0.56 |
| Message_SendRecv         |     1KB |  8.17 Gbps | 997.05K |   10.03 ms |  1.04 |   7.55 MB |       0.38 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    64KB |  4.74 GB/s |  72.35K |  138.21 ms |  1.00 |   1.22 GB |       1.00 |
| ArrayPool_SendRecv       |    64KB |  5.10 GB/s |  77.88K |  128.40 ms |  0.93 | 177.15 KB |       0.00 |
| MessageZeroCopy_SendRecv |    64KB |  1.23 GB/s |  18.74K |  533.52 ms |  3.86 |  11.24 MB |       0.01 |
| Message_SendRecv         |    64KB |  5.10 GB/s |  77.83K |  128.48 ms |  0.93 |   7.55 MB |       0.01 |

### Detailed Metrics

| Method                   | Size    | Score (ops/s) | Error      | StdDev    | Latency   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| ByteArray_SendRecv       |     64B |       171.11 |   0.1014 ms |   0.0236 ms | 584.40 ns |   29.0000 |
| ArrayPool_SendRecv       |     64B |       128.95 |   1.2621 ms |   0.2932 ms | 775.51 ns |   21.0000 |
| MessageZeroCopy_SendRecv |     64B |         2.78 |  16.6015 ms |   3.8562 ms |  36.03 μs |    1.0000 |
| Message_SendRecv         |     64B |       106.16 |   0.0954 ms |   0.0222 ms | 941.93 ns |   40.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    512B |       133.10 |   1.0116 ms |   0.2350 ms | 751.29 ns |   73.0000 |
| ArrayPool_SendRecv       |    512B |       128.46 |   0.0989 ms |   0.0230 ms | 778.47 ns |   21.0000 |
| MessageZeroCopy_SendRecv |    512B |         2.61 |   9.1644 ms |   2.1287 ms |  38.32 μs |    1.0000 |
| Message_SendRecv         |    512B |        99.35 |   0.0757 ms |   0.0176 ms |   1.01 μs |   38.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |     1KB |       103.89 |   0.5087 ms |   0.1182 ms | 962.53 ns |   96.0000 |
| ArrayPool_SendRecv       |     1KB |        98.46 |   0.1869 ms |   0.0434 ms |   1.02 μs |   16.0000 |
| MessageZeroCopy_SendRecv |     1KB |         2.57 |  48.1970 ms |  11.1952 ms |  38.87 μs |    1.0000 |
| Message_SendRecv         |     1KB |        99.70 |   0.0772 ms |   0.0179 ms |   1.00 μs |   38.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    64KB |         7.24 |  10.6799 ms |   2.4807 ms |  13.82 μs |  244.0000 |
| ArrayPool_SendRecv       |    64KB |         7.79 |  10.4213 ms |   2.4207 ms |  12.84 μs |    1.0000 |
| MessageZeroCopy_SendRecv |    64KB |         1.87 |  22.4202 ms |   5.2078 ms |  53.35 μs |    1.0000 |
| Message_SendRecv         |    64KB |         7.78 |   4.0053 ms |   0.9303 ms |  12.85 μs |    3.0000 |

### Key Finding

**ArrayPool is recommended for production use:**
- Maintains constant ~178KB allocation regardless of message size
- ByteArray scales linearly with message size (1.22GB at 64KB vs 178KB)
- At 64KB messages: ArrayPool uses **7,000x less memory** than ByteArray

### Strategy Descriptions

#### 1. ByteArray_SendRecv (Baseline)
```java
// Sending
byte[] sendBuffer = new byte[messageSize];
System.arraycopy(sourceData, 0, sendBuffer, 0, messageSize);
socket.send(sendBuffer, SendFlags.DONT_WAIT);

// Receiving - uses fixed buffer
socket.recv(recvBuffer, RecvFlags.NONE);  // recvBuffer is pre-allocated
byte[] outputBuffer = new byte[size];     // New allocation for each message
System.arraycopy(recvBuffer, 0, outputBuffer, 0, size);
```

**Characteristics:**
- Allocates new byte arrays for every send/receive
- Highest GC pressure (baseline allocation = 1.0)
- Simple implementation
- **Best for**: Small messages where throughput is critical

**Performance:**
- 64B: 1.71M msg/sec (highest)
- 512B: 1.33M msg/sec (highest)
- 1KB: 1.04M msg/sec
- 64KB: 72K msg/sec

#### 2. ArrayPool_SendRecv (RECOMMENDED)
```java
// Sending
ByteBuf sendBuf = allocator.buffer(messageSize);
try {
    sendBuf.writeBytes(sourceData, 0, messageSize);
    sendBuf.getBytes(0, reusableSendBuffer, 0, messageSize);  // Fixed buffer
    socket.send(reusableSendBuffer, SendFlags.DONT_WAIT);
} finally {
    sendBuf.release();
}

// Receiving - uses fixed buffer
socket.recv(recvBuffer, RecvFlags.NONE);  // recvBuffer is pre-allocated
ByteBuf outputBuf = allocator.buffer(size);
try {
    outputBuf.writeBytes(recvBuffer, 0, size);
    outputBuf.getBytes(0, reusableRecvBuffer, 0, size);  // Fixed buffer
} finally {
    outputBuf.release();
}
```

**Characteristics:**
- Uses Netty PooledByteBufAllocator for buffer pooling
- **Dramatically reduced GC pressure** (2% allocation @ 512B, 1% @ 1KB)
- Constant memory allocation regardless of message size (~178KB)
- **Best for**: Production use, long-running servers

**Performance:**
- 64B: 1.29M msg/sec (75% of ByteArray, 89% less allocation)
- 512B: 1.28M msg/sec (96% of ByteArray, 98% less allocation)
- 1KB: 984K msg/sec (95% of ByteArray, 99% less allocation)
- 64KB: 78K msg/sec (108% of ByteArray, **99.99% less allocation**)

#### 3. Message_SendRecv
```java
// Sending
try (Message idMsg = new Message(router2Id);
     Message payloadMsg = new Message(sourceData)) {
    socket.send(idMsg, SendFlags.SEND_MORE);
    socket.send(payloadMsg, SendFlags.DONT_WAIT);
}

// Receiving
try (Message msg = new Message()) {
    socket.recv(msg, RecvFlags.NONE);
    // Use msg.data() directly (no copy to managed memory)
}
```

**Characteristics:**
- Uses ZMQ native message objects
- Medium GC pressure (constant ~7.55MB regardless of message size)
- Direct memory access via MemorySegment
- **Best for**: When native ZMQ Message API is preferred

**Performance:**
- 64B: 1.06M msg/sec (62% of ByteArray)
- 512B: 993K msg/sec (75% of ByteArray)
- 1KB: 997K msg/sec (96% of ByteArray)
- 64KB: 78K msg/sec (108% of ByteArray)

#### 4. MessageZeroCopy_SendRecv (NOT RECOMMENDED)
```java
// Sending with zero-copy callback
Arena dataArena = Arena.ofShared();
MemorySegment dataSeg = dataArena.allocate(messageSize);
MemorySegment.copy(sourceData, 0, dataSeg, JAVA_BYTE, 0, messageSize);

Message payloadMsg = new Message(dataSeg, messageSize, data -> {
    dataArena.close();
});
socket.send(payloadMsg, SendFlags.DONT_WAIT);
```

**Characteristics:**
- Attempts true zero-copy with Arena allocation
- **Severe performance degradation** (62x slower @ 64B, 51x @ 512B)
- `Arena.ofShared()` overhead (~31μs per creation) dominates any zero-copy benefit
- **Never use in production**

**Performance:**
- 64B: 28K msg/sec (1.6% of ByteArray)
- 512B: 26K msg/sec (2.0% of ByteArray)
- 1KB: 26K msg/sec (2.5% of ByteArray)
- 64KB: 19K msg/sec (26% of ByteArray)

### Receive Buffer Best Practice

> **Important**: Always use a pre-allocated fixed buffer for receiving messages.

```java
// GOOD: Pre-allocate receive buffer once
byte[] recvBuffer = new byte[maxMessageSize];  // Allocate once at setup

while (running) {
    int size = socket.recv(recvBuffer, RecvFlags.NONE);
    // Process recvBuffer[0..size-1]
}
```

```java
// BAD: Allocate new buffer for each receive
while (running) {
    byte[] buffer = new byte[maxMessageSize];  // GC pressure!
    socket.recv(buffer, RecvFlags.NONE);
}
```

This practice is essential for minimizing GC pressure in high-throughput applications.

### Recommendations

| Use Case | Recommended Strategy | Reason |
|----------|---------------------|--------|
| **Production servers** | **ArrayPool** | Constant ~178KB allocation, minimal GC pressure |
| Maximum throughput (small messages) | ByteArray | Highest msg/sec for <512B |
| Native ZMQ API preference | Message | Direct MemorySegment access |
| Zero-copy requirements | Avoid MessageZeroCopy | Arena.ofShared() overhead too high |

## Receive Mode Benchmarks

Comparison of four receive strategies: PureBlocking, BlockingBatch, NonBlocking, and Poller.

### Performance Overview

| Mode | 64B (msg/sec) | 512B (msg/sec) | 1KB (msg/sec) | 64KB (msg/sec) |
|------|---------------|----------------|---------------|----------------|
| **PureBlocking** | 1.48M | 1.36M | 1.10M | 70K |
| **BlockingBatch** | 1.46M | 1.35M | 1.03M | 70K |
| **NonBlocking** | 1.38M | 1.27M | 943K | 44K (slow) |
| **Poller** | 1.48M | 1.34M | 1.10M | 68K |

### Detailed Metrics

| Mode | Size | Score (ops/s) | Latency |
|------|------|---------------|---------|
| PureBlocking | 64B | 147.79 | 0.68 μs |
| BlockingBatch | 64B | 145.54 | 0.69 μs |
| NonBlocking | 64B | 137.74 | 0.73 μs |
| Poller | 64B | 147.88 | 0.68 μs |
| | | | |
| PureBlocking | 512B | 135.77 | 0.74 μs |
| BlockingBatch | 512B | 135.17 | 0.74 μs |
| NonBlocking | 512B | 126.58 | 0.79 μs |
| Poller | 512B | 133.89 | 0.75 μs |
| | | | |
| PureBlocking | 1KB | 110.18 | 0.91 μs |
| BlockingBatch | 1KB | 102.93 | 0.97 μs |
| NonBlocking | 1KB | 94.31 | 1.06 μs |
| Poller | 1KB | 110.04 | 0.91 μs |
| | | | |
| PureBlocking | 64KB | 6.98 | 14.33 μs |
| BlockingBatch | 64KB | 7.02 | 14.25 μs |
| NonBlocking | 64KB | 4.38 | 22.83 μs |
| Poller | 64KB | 6.84 | 14.63 μs |

### Mode Descriptions

#### 1. PureBlocking (Baseline)
```java
while (n < messageCount) {
    // Blocking recv - thread waits until message arrives
    socket.recv(identityBuffer, RecvFlags.NONE);
    socket.recv(recvBuffer, RecvFlags.NONE);
    n++;
}
```

**Characteristics:**
- Thread blocks until message available
- Simplest implementation, one syscall per message
- **Best for**: Single socket applications

**Performance:**
- 64B: 1.48M msg/sec
- 512B: 1.36M msg/sec
- 1KB: 1.10M msg/sec
- 64KB: 70K msg/sec

#### 2. BlockingBatch (Optimized Throughput)
```java
while (n < messageCount) {
    // First message: blocking wait
    socket.recv(identityBuffer, RecvFlags.NONE);
    socket.recv(recvBuffer, RecvFlags.NONE);
    n++;

    // Batch receive available messages (reduces syscalls)
    while (n < messageCount) {
        int bytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
        if (bytes == -1) break;  // No more available (EAGAIN)

        socket.recv(recvBuffer, RecvFlags.NONE);
        n++;
    }
}
```

**Characteristics:**
- First message uses blocking wait, then batch-processes with non-blocking recv
- Reduces syscall overhead
- **Best for**: High-throughput single socket applications

**Performance:**
- 64B: 1.46M msg/sec (99% of PureBlocking)
- 512B: 1.35M msg/sec (99% of PureBlocking)
- 1KB: 1.03M msg/sec (94% of PureBlocking)
- 64KB: 70K msg/sec (100% of PureBlocking)

#### 3. Poller (Recommended for Multiple Sockets)
```java
try (Poller poller = new Poller()) {
    int idx = poller.register(socket, PollEvents.IN);

    while (n < messageCount) {
        poller.poll(-1);  // Wait for events

        // Batch receive all available messages
        while (n < messageCount) {
            int bytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
            if (bytes == -1) break;  // No more available (EAGAIN)

            socket.recv(recvBuffer, RecvFlags.NONE);
            n++;
        }
    }
}
```

**Characteristics:**
- Event-driven I/O with batch processing
- Can monitor multiple sockets
- **Best for**: Multi-socket applications

**Performance:**
- 64B: 1.48M msg/sec (100% of PureBlocking)
- 512B: 1.34M msg/sec (99% of PureBlocking)
- 1KB: 1.10M msg/sec (100% of PureBlocking)
- 64KB: 68K msg/sec (97% of PureBlocking)

**Verdict**: Poller matches PureBlocking performance while providing multi-socket capability.

#### 4. NonBlocking with Sleep (NOT RECOMMENDED)
```java
while (n < messageCount) {
    int bytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
    if (bytes != -1) {
        socket.recv(recvBuffer, RecvFlags.DONT_WAIT);
        n++;

        // Batch receive without sleep
        while (n < messageCount) {
            int batchBytes = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
            if (batchBytes == -1) break;
            socket.recv(recvBuffer, RecvFlags.DONT_WAIT);
            n++;
        }
    } else {
        Thread.sleep(1);  // Wait before retry
    }
}
```

**Characteristics:**
- Non-blocking recv with sleep-based retry
- Inefficient CPU usage due to sleep overhead
- **Avoid in production**

**Performance:**
- 64B: 1.38M msg/sec (93% of PureBlocking)
- 512B: 1.27M msg/sec (93% of PureBlocking)
- 1KB: 943K msg/sec (86% of PureBlocking)
- 64KB: 44K msg/sec (63% of PureBlocking)

**Verdict**: Significantly worse for large messages. Use Poller instead.

### Recommendations by Use Case

| Use Case | Recommended Mode | Reason |
|----------|-----------------|--------|
| Single socket (simple) | **PureBlocking** | Simplest, no overhead |
| Single socket (high throughput) | **BlockingBatch** | Reduces syscalls |
| Multiple sockets | **Poller** | Event-driven, multi-socket support |
| Never use | ~~NonBlocking~~ | Sleep overhead degrades performance |

## Running Benchmarks

### Run All Benchmarks
```bash
./gradlew :zmq:jmh
```

### Run Specific Benchmark
```bash
# Memory strategy only
./gradlew :zmq:jmh -PjmhIncludes='.*MemoryStrategyBenchmark.*'

# Receive mode only
./gradlew :zmq:jmh -PjmhIncludes='.*ReceiveModeBenchmark.*'
```

### Format Results
```bash
# Human-readable format
./gradlew :zmq:formatJmhResults

# .NET BenchmarkDotNet style
cd zmq && python3 scripts/format_jmh_dotnet_style.py
```

### Output Files
- **JSON**: `zmq/build/reports/jmh/results.json`
- **Formatted**: `zmq/build/reports/jmh/results-formatted.txt`

## Key Takeaways

1. **Message Buffer Strategy**:
   - **Use `ArrayPool` for production** - constant ~178KB allocation regardless of message size
   - Small messages (<512B): `ByteArray` offers highest throughput (1.71M msg/sec @ 64B)
   - Large messages (>8KB): `ArrayPool` is essential (7,000x less allocation than ByteArray)
   - Avoid `MessageZeroCopy` (40-62x slower due to Arena.ofShared() overhead)

2. **Receive Buffer**:
   - Always use pre-allocated fixed buffers for receiving
   - Avoid allocating new `byte[]` for each receive operation
   - This applies to all strategies for minimizing GC pressure

3. **Receive Mode** (4 strategies tested):
   - Single socket (simple): Use `PureBlocking` (simplest, 1.48M msg/sec @ 64B)
   - Single socket (optimized): Use `BlockingBatch` (reduces syscalls)
   - Multiple sockets: Use `Poller` (100% of PureBlocking performance, multi-socket support)
   - Avoid `NonBlocking` with sleep (37% slower for large messages)

4. **GC Pressure** (at 64KB messages):
   - ArrayPool: 177KB (constant)
   - Message: 7.55MB (constant)
   - ByteArray: 1.22GB (scales with message size)
   - ArrayPool reduces allocation by **99.99%** vs ByteArray at 64KB

5. **Latency**:
   - Small messages: Sub-microsecond latency (584-942 ns)
   - Medium messages: ~750 ns - 1.02 μs (512B-1KB)
   - Large messages: 12-14 microseconds (64KB)
