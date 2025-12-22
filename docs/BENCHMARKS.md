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

## Memory Strategy Benchmarks

Comparison of four memory management strategies for sending/receiving messages.

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

#### 2. ArrayPool_SendRecv ✅ RECOMMENDED
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
    socket.recv(msg, RecvFlags.NONE).value();
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

#### 4. MessageZeroCopy_SendRecv ❌ NOT RECOMMENDED
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
- 64B: 28K msg/sec (1.6% of ByteArray) ❌
- 512B: 26K msg/sec (2.0% of ByteArray) ❌
- 1KB: 26K msg/sec (2.5% of ByteArray) ❌
- 64KB: 19K msg/sec (26% of ByteArray) ❌

### Receive Buffer Best Practice

> **⚠️ Important**: Always use a pre-allocated fixed buffer for receiving messages.

```java
// ✅ GOOD: Pre-allocate receive buffer once
byte[] recvBuffer = new byte[maxMessageSize];  // Allocate once at setup

while (running) {
    int size = socket.recv(recvBuffer, RecvFlags.NONE).value();
    // Process recvBuffer[0..size-1]
}
```

```java
// ❌ BAD: Allocate new buffer for each receive
while (running) {
    byte[] buffer = new byte[maxMessageSize];  // GC pressure!
    socket.recv(buffer, RecvFlags.NONE);
}
```

This practice is essential for minimizing GC pressure in high-throughput applications.

### Recommendations

| Use Case | Recommended Strategy | Reason |
|----------|---------------------|--------|
| **Production servers** | ✅ **ArrayPool** | Constant ~178KB allocation, minimal GC pressure |
| Maximum throughput (small messages) | ByteArray | Highest msg/sec for <512B |
| Native ZMQ API preference | Message | Direct MemorySegment access |
| Zero-copy requirements | ❌ Never use MessageZeroCopy | Arena.ofShared() overhead too high |

## Receive Mode Benchmarks

Comparison of three receive strategies: Blocking, Poller, and NonBlocking.

### Performance Overview

| Method                   | Size    | Throughput | Msg/sec | Mean      | Ratio | Allocated | Alloc Ratio |
|------------------------- |--------:|------------|--------:|----------:|------:|----------:|------------:|
| Blocking_RouterToRouter  |     64B | 735.17 Mbps |   1.44M |    6.96 ms |  1.00 |   5.34 MB |       1.00 |
| NonBlocking_RouterToRouter |     64B | 699.81 Mbps |   1.37M |    7.32 ms |  1.05 |   5.34 MB |       1.00 |
| Poller_RouterToRouter    |     64B | 730.11 Mbps |   1.43M |    7.01 ms |  1.01 |   5.34 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |    512B |  5.57 Gbps |   1.36M |    7.36 ms |  1.00 |   5.50 MB |       1.00 |
| NonBlocking_RouterToRouter |    512B |  5.04 Gbps |   1.23M |    8.12 ms |  1.10 |   5.49 MB |       1.00 |
| Poller_RouterToRouter    |    512B |  5.45 Gbps |   1.33M |    7.52 ms |  1.02 |   5.50 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |     1KB |  8.69 Gbps |   1.06M |    9.42 ms |  1.00 |   5.50 MB |       1.00 |
| NonBlocking_RouterToRouter |     1KB |  8.00 Gbps | 976.90K |   10.24 ms |  1.09 |   5.49 MB |       1.00 |
| Poller_RouterToRouter    |     1KB |  8.76 Gbps |   1.07M |    9.35 ms |  0.99 |   5.50 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |    64KB |  4.41 GB/s |  67.33K |  148.51 ms |  1.00 |   5.57 MB |       1.00 |
| NonBlocking_RouterToRouter |    64KB |  2.23 GB/s |  34.10K |  293.29 ms |  1.97 |   5.50 MB |       0.99 |
| Poller_RouterToRouter    |    64KB |  4.59 GB/s |  70.02K |  142.82 ms |  0.96 |   5.57 MB |       1.00 |

### Detailed Metrics

| Method                   | Size    | Score (ops/s) | Error      | StdDev    | Latency   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| Blocking_RouterToRouter  |     64B |       143.59 |   0.0627 ms |   0.0146 ms | 696.43 ns |   55.0000 |
| NonBlocking_RouterToRouter |     64B |       136.68 |   0.1011 ms |   0.0235 ms | 731.62 ns |   53.0000 |
| Poller_RouterToRouter    |     64B |       142.60 |   0.1237 ms |   0.0287 ms | 701.27 ns |   55.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |    512B |       135.92 |   0.1854 ms |   0.0431 ms | 735.75 ns |   52.0000 |
| NonBlocking_RouterToRouter |    512B |       123.12 |   0.1267 ms |   0.0294 ms | 812.22 ns |   47.0000 |
| Poller_RouterToRouter    |    512B |       132.97 |   0.1580 ms |   0.0367 ms | 752.05 ns |   50.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |     1KB |       106.11 |   0.2328 ms |   0.0541 ms | 942.44 ns |   40.0000 |
| NonBlocking_RouterToRouter |     1KB |        97.69 |   0.1830 ms |   0.0425 ms |   1.02 μs |   37.0000 |
| Poller_RouterToRouter    |     1KB |       106.95 |   0.2135 ms |   0.0496 ms | 935.04 ns |   41.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |    64KB |         6.73 |   3.1031 ms |   0.7208 ms |  14.85 μs |    2.0000 |
| NonBlocking_RouterToRouter |    64KB |         3.41 |  12.4926 ms |   2.9018 ms |  29.33 μs |    2.0000 |
| Poller_RouterToRouter    |    64KB |         7.00 |   2.2215 ms |   0.5160 ms |  14.28 μs |    2.0000 |

### Mode Descriptions

#### 1. Blocking (Baseline)
```java
while (n < messageCount) {
    // Blocking recv - thread waits until message arrives
    socket.recv(identityBuffer, RecvFlags.NONE).value();
    socket.recv(recvBuffer, RecvFlags.NONE).value();
    n++;
}
```

**Characteristics:**
- Thread blocks until message available
- Simplest implementation
- **Best for**: Single socket applications

**Performance:**
- 64B: 1.44M msg/sec
- 512B: 1.36M msg/sec
- 1KB: 1.06M msg/sec
- 64KB: 67K msg/sec

#### 2. Poller (Recommended for Multiple Sockets)
```java
Poller poller = new Poller();
int idx = poller.register(socket, PollEvents.IN);

while (n < messageCount) {
    if (poller.poll(1000) > 0) {
        if (poller.isReadable(idx)) {
            socket.recv(identityBuffer, RecvFlags.NONE).value();
            socket.recv(recvBuffer, RecvFlags.NONE).value();
            n++;
        }
    }
}
```

**Characteristics:**
- Event-driven I/O
- Can monitor multiple sockets
- **Best for**: Multi-socket applications

**Performance:**
- 64B: 1.43M msg/sec (99% of Blocking)
- 512B: 1.33M msg/sec (98% of Blocking)
- 1KB: 1.07M msg/sec (101% of Blocking)
- 64KB: 70K msg/sec (104% of Blocking)

**Verdict**: Poller matches or exceeds Blocking performance while providing multi-socket capability.

#### 3. NonBlocking with Sleep ❌ NOT RECOMMENDED
```java
while (n < messageCount) {
    RecvResult<Integer> idResult = socket.recv(identityBuffer, RecvFlags.DONT_WAIT);
    if (idResult.wouldBlock()) {
        Thread.sleep(1); // Busy-wait with sleep
        continue;
    }
    socket.recv(recvBuffer, RecvFlags.NONE).value();
    n++;
}
```

**Characteristics:**
- Non-blocking recv with sleep-based retry
- Inefficient CPU usage
- **Avoid in production**

**Performance:**
- 64B: 1.37M msg/sec (95% of Blocking)
- 512B: 1.23M msg/sec (90% of Blocking)
- 1KB: 977K msg/sec (92% of Blocking)
- 64KB: 34K msg/sec (51% of Blocking) ❌

**Verdict**: Significantly worse for large messages. Use Poller instead.

### Recommendations by Use Case

| Use Case | Recommended Mode | Reason |
|----------|-----------------|--------|
| Single socket | **Blocking** | Simplest, no overhead |
| Multiple sockets | **Poller** | Event-driven, matches Blocking performance |
| High-frequency polling | **Poller** | Avoid busy-wait overhead |

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

1. **Memory Strategy**:
   - ✅ **Use `ArrayPool` for production** - constant ~178KB allocation regardless of message size
   - Small messages (<512B): `ByteArray` offers highest throughput (1.71M msg/sec @ 64B)
   - Large messages (>8KB): `ArrayPool` is essential (7,000x less allocation than ByteArray)
   - ❌ **Never use** `MessageZeroCopy` (40-62x slower due to Arena.ofShared() overhead)

2. **Receive Buffer**:
   - ⚠️ Always use pre-allocated fixed buffers for receiving
   - Avoid allocating new `byte[]` for each receive operation
   - This applies to all strategies for minimizing GC pressure

3. **Receive Mode**:
   - Single socket: Use `Blocking` (simplest)
   - Multiple sockets: Use `Poller` (98-104% of Blocking performance)
   - ❌ **Never use** `NonBlocking` with sleep (2x slower for large messages)

4. **GC Pressure** (at 64KB messages):
   - ArrayPool: 177KB (constant)
   - Message: 7.55MB (constant)
   - ByteArray: 1.22GB (scales with message size)
   - ArrayPool reduces allocation by **99.99%** vs ByteArray at 64KB

5. **Latency**:
   - Small messages: Sub-microsecond latency (584-942 ns)
   - Medium messages: ~750 ns - 1.02 μs (512B-1KB)
   - Large messages: 12-14 microseconds (64KB)
