# JVM-ZMQ Performance Benchmarks

Comprehensive performance benchmarks for JVM-ZMQ using JMH (Java Microbenchmark Harness).

## Test Environment

- **OS**: Ubuntu 24.04 LTS
- **CPU**: x86_64
- **JVM**: Java HotSpot(TM) 64-Bit Server VM, JDK 22.0.2
- **JMH**: v1.37
- **Pattern**: Router-to-Router (inproc transport)
- **Messages per iteration**: 10,000
- **Warmup**: 3 iterations (2s each)
- **Measurement**: 5 iterations (5s each)

## Memory Strategy Benchmarks

Comparison of four memory management strategies for sending/receiving messages.

### Performance Overview

| Method                   | Size    | Throughput | Msg/sec | Mean      | Ratio | Allocated | Alloc Ratio |
|------------------------- |--------:|------------|--------:|----------:|------:|----------:|------------:|
| ByteArray_SendRecv       |     64B |  1.37 Gbps |   2.67M |    3.75 ms |  1.00 |   1.83 MB |       1.00 |
| ArrayPool_SendRecv       |     64B | 987.87 Mbps |   1.93M |    5.18 ms |  1.38 | 490.90 KB |       0.26 |
| MessageZeroCopy_SendRecv |     64B | 15.02 Mbps |  29.34K |  340.83 ms | 90.92 |  11.00 MB |       6.00 |
| Message_SendRecv         |     64B | 643.56 Mbps |   1.26M |    7.96 ms |  2.12 |   7.71 MB |       4.21 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |     1KB | 10.93 Gbps | 910.61K |   10.98 ms |  1.00 |  29.46 MB |       1.00 |
| ArrayPool_SendRecv       |     1KB | 11.05 Gbps | 921.11K |   10.86 ms |  0.99 | 653.10 KB |       0.02 |
| MessageZeroCopy_SendRecv |     1KB | 307.15 Mbps |  25.60K |  390.69 ms | 35.58 |  11.18 MB |       0.38 |
| Message_SendRecv         |     1KB | 11.60 Gbps | 967.02K |   10.34 ms |  0.94 |   7.71 MB |       0.26 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    64KB |  5.55 GB/s |  84.73K |  118.02 ms |  1.00 |   1.22 GB |       1.00 |
| ArrayPool_SendRecv       |    64KB |  4.76 GB/s |  72.56K |  137.82 ms |  1.17 | 696.43 KB |       0.00 |
| MessageZeroCopy_SendRecv |    64KB |  1.14 GB/s |  17.46K |  572.76 ms |  4.85 |  11.63 MB |       0.01 |
| Message_SendRecv         |    64KB |  4.66 GB/s |  71.12K |  140.61 ms |  1.19 |   7.79 MB |       0.01 |

### Detailed Metrics

| Method                   | Size    | Score (ops/s) | Error      | StdDev    | Latency   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| ByteArray_SendRecv       |     64B |       266.77 |   0.3339 ms |   0.0776 ms | 374.86 ns |   46.0000 |
| ArrayPool_SendRecv       |     64B |       192.94 |   0.2643 ms |   0.0614 ms | 518.29 ns |   33.0000 |
| MessageZeroCopy_SendRecv |     64B |         2.93 |  19.2949 ms |   4.4818 ms |  34.08 μs |    1.0000 |
| Message_SendRecv         |     64B |       125.70 |   0.4513 ms |   0.1048 ms | 795.58 ns |   49.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |     1KB |        91.06 |   1.2963 ms |   0.3011 ms |   1.10 μs |  118.0000 |
| ArrayPool_SendRecv       |     1KB |        92.11 |   0.4259 ms |   0.0989 ms |   1.09 μs |   14.0000 |
| MessageZeroCopy_SendRecv |     1KB |         2.56 |  51.3251 ms |  11.9218 ms |  39.07 μs |    1.0000 |
| Message_SendRecv         |     1KB |        96.70 |   0.6075 ms |   0.1411 ms |   1.03 μs |   37.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    64KB |         8.47 |  18.2434 ms |   4.2376 ms |  11.80 μs |  276.0000 |
| ArrayPool_SendRecv       |    64KB |         7.26 |  10.2885 ms |   2.3898 ms |  13.78 μs |    2.0000 |
| MessageZeroCopy_SendRecv |    64KB |         1.75 |  32.6346 ms |   7.5804 ms |  57.28 μs |    1.0000 |
| Message_SendRecv         |    64KB |         7.11 |  27.9916 ms |   6.5019 ms |  14.06 μs |    2.0000 |

### Strategy Descriptions

#### 1. ByteArray_SendRecv (Baseline)
```java
// Sending
byte[] sendBuffer = new byte[messageSize];
System.arraycopy(sourceData, 0, sendBuffer, 0, messageSize);
socket.send(sendBuffer, SendFlags.DONT_WAIT);

// Receiving
byte[] outputBuffer = new byte[size];
System.arraycopy(recvBuffer, 0, outputBuffer, 0, size);
```

**Characteristics:**
- Allocates new byte arrays for every send/receive
- Highest GC pressure (baseline allocation = 1.0)
- Simple implementation
- **Best for**: Small messages where throughput is critical

**Performance:**
- 64B: 2.67M msg/sec (highest)
- 1.5KB: 911K msg/sec (competitive)
- 64KB: 85K msg/sec (highest)

#### 2. ArrayPool_SendRecv
```java
// Sending
ByteBuf sendBuf = allocator.buffer(messageSize);
try {
    sendBuf.writeBytes(sourceData, 0, messageSize);
    sendBuf.getBytes(0, reusableSendBuffer, 0, messageSize);
    socket.send(reusableSendBuffer, SendFlags.DONT_WAIT);
} finally {
    sendBuf.release();
}

// Receiving
ByteBuf outputBuf = allocator.buffer(size);
try {
    outputBuf.writeBytes(recvBuffer, 0, size);
    outputBuf.getBytes(0, reusableRecvBuffer, 0, size);
} finally {
    outputBuf.release();
}
```

**Characteristics:**
- Uses Netty PooledByteBufAllocator for buffer pooling
- Significantly reduced GC pressure (26% allocation @ 64B, 2% @ 1.5KB)
- Requires buffer management discipline
- **Best for**: Large messages where GC pressure matters

**Performance:**
- 64B: 1.93M msg/sec (72% of ByteArray)
- 1.5KB: 921K msg/sec (competitive with ByteArray)
- 64KB: 73K msg/sec (86% of ByteArray, but 73% less allocation)

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
- Medium GC pressure (4.21x @ 64B, 0.26x @ 1.5KB)
- Direct memory access via MemorySegment
- **Best for**: Medium messages (1-8KB)

**Performance:**
- 64B: 1.26M msg/sec (47% of ByteArray)
- 1.5KB: 967K msg/sec (**highest**)
- 64KB: 71K msg/sec (84% of ByteArray)

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
- **Severe performance degradation** (90x slower @ 64B)
- Arena allocation overhead dominates any zero-copy benefit
- **Avoid in production**

**Performance:**
- 64B: 29K msg/sec (1% of ByteArray) ❌
- 1.5KB: 26K msg/sec (3% of ByteArray) ❌
- 64KB: 17K msg/sec (20% of ByteArray) ❌

### Recommendations by Message Size

| Message Size | Recommended Strategy | Throughput | Reason |
|--------------|---------------------|------------|--------|
| < 1 KB | **ByteArray** | 2.67M msg/sec | Highest throughput, simple implementation |
| 1-8 KB | **Message** or **ByteArray** | ~900K msg/sec | Similar performance, choose based on API preference |
| > 8 KB | **ArrayPool** | 73K msg/sec | 73% less GC pressure with competitive throughput |

## Receive Mode Benchmarks

Comparison of three receive strategies: Blocking, Poller, and NonBlocking.

### Performance Overview

| Method                   | Size    | Throughput | Msg/sec | Mean      | Ratio | Allocated | Alloc Ratio |
|------------------------- |--------:|------------|--------:|----------:|------:|----------:|------------:|
| Blocking_RouterToRouter  |     64B | 755.73 Mbps |   1.48M |    6.77 ms |  1.00 |   5.34 MB |       1.00 |
| NonBlocking_RouterToRouter |     64B | 742.83 Mbps |   1.45M |    6.89 ms |  1.02 |   5.34 MB |       1.00 |
| Poller_RouterToRouter    |     64B | 763.56 Mbps |   1.49M |    6.71 ms |  0.99 |   5.34 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |     1KB | 10.42 Gbps | 868.18K |   11.52 ms |  1.00 |   5.50 MB |       1.00 |
| NonBlocking_RouterToRouter |     1KB |  9.13 Gbps | 760.77K |   13.14 ms |  1.14 |   5.49 MB |       1.00 |
| Poller_RouterToRouter    |     1KB | 10.58 Gbps | 881.92K |   11.34 ms |  0.98 |   5.50 MB |       1.00 |
|                          |         |            |         |           |       |           |             |
| Blocking_RouterToRouter  |    64KB |  4.50 GB/s |  68.70K |  145.55 ms |  1.00 |   5.56 MB |       1.00 |
| NonBlocking_RouterToRouter |    64KB |  2.08 GB/s |  31.78K |  314.67 ms |  2.16 |   5.50 MB |       0.99 |
| Poller_RouterToRouter    |    64KB |  4.49 GB/s |  68.45K |  146.10 ms |  1.00 |   5.56 MB |       1.00 |

### Detailed Metrics

| Method                   | Size    | Score (ops/s) | Error      | StdDev    | Latency   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| Blocking_RouterToRouter  |     64B |       147.60 |   0.2040 ms |   0.0474 ms | 677.49 ns |   57.0000 |
| NonBlocking_RouterToRouter |     64B |       145.08 |   0.1462 ms |   0.0339 ms | 689.25 ns |   55.0000 |
| Poller_RouterToRouter    |     64B |       149.13 |   0.1954 ms |   0.0454 ms | 670.55 ns |   58.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |     1KB |        86.82 |   2.9687 ms |   0.6896 ms |   1.15 μs |   33.0000 |
| NonBlocking_RouterToRouter |     1KB |        76.08 |   1.1124 ms |   0.2584 ms |   1.31 μs |   29.0000 |
| Poller_RouterToRouter    |     1KB |        88.19 |   0.7164 ms |   0.1664 ms |   1.13 μs |   34.0000 |
|                          |         |               |            |           |           |           |
| Blocking_RouterToRouter  |    64KB |         6.87 |  28.0516 ms |   6.5158 ms |  14.56 μs |    2.0000 |
| NonBlocking_RouterToRouter |    64KB |         3.18 |  27.8433 ms |   6.4674 ms |  31.47 μs |    1.0000 |
| Poller_RouterToRouter    |    64KB |         6.84 |  22.7549 ms |   5.2855 ms |  14.61 μs |    2.0000 |

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
- 64B: 1.48M msg/sec
- 1.5KB: 868K msg/sec
- 64KB: 69K msg/sec

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
- 64B: 1.49M msg/sec (101% of Blocking)
- 1.5KB: 882K msg/sec (102% of Blocking)
- 64KB: 68K msg/sec (99% of Blocking)

**Verdict**: Poller matches Blocking performance while providing multi-socket capability.

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
- 64B: 1.45M msg/sec (98% of Blocking)
- 1.5KB: 761K msg/sec (88% of Blocking)
- 64KB: 32K msg/sec (46% of Blocking) ❌

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
   - Small messages: Use `ByteArray` (2.67M msg/sec @ 64B)
   - Medium messages: Use `Message` (967K msg/sec @ 1.5KB)
   - Large messages: Use `ArrayPool` for reduced GC (73% less allocation)
   - **Never use** `MessageZeroCopy` (90x slower)

2. **Receive Mode**:
   - Single socket: Use `Blocking` (simplest)
   - Multiple sockets: Use `Poller` (98-102% of Blocking performance)
   - **Never use** `NonBlocking` with sleep (2x slower for large messages)

3. **GC Pressure**:
   - ArrayPool reduces allocation by 73-98% vs ByteArray
   - Trade-off: Slightly lower throughput for large messages (14-17%)
   - Consider ArrayPool when GC pauses are problematic

4. **Latency**:
   - Small messages: Sub-microsecond latency (375-795 ns)
   - Medium messages: ~1 microsecond
   - Large messages: 12-15 microseconds
