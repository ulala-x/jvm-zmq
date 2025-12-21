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
| ByteArray_SendRecv       |     64B |  1.51 Gbps |   2.94M |    3.40 ms |  1.00 |   1.83 MB |       1.00 |
| ArrayPool_SendRecv       |     64B | 922.62 Mbps |   1.80M |    5.55 ms |  1.63 | 490.97 KB |       0.26 |
| MessageZeroCopy_SendRecv |     64B | 14.13 Mbps |  27.59K |  362.46 ms | 106.69 |  11.00 MB |       6.00 |
| Message_SendRecv         |     64B | 614.22 Mbps |   1.20M |    8.34 ms |  2.45 |   7.71 MB |       4.21 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    512B |  6.55 Gbps |   1.60M |    6.26 ms |  1.00 |  10.53 MB |       1.00 |
| ArrayPool_SendRecv       |    512B |  6.19 Gbps |   1.51M |    6.62 ms |  1.06 | 649.20 KB |       0.06 |
| MessageZeroCopy_SendRecv |    512B | 104.48 Mbps |  25.51K |  392.05 ms | 62.66 |  11.14 MB |       1.06 |
| Message_SendRecv         |    512B |  4.31 Gbps |   1.05M |    9.50 ms |  1.52 |   7.71 MB |       0.73 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |     1KB |  9.47 Gbps |   1.16M |    8.65 ms |  1.00 |  20.30 MB |       1.00 |
| ArrayPool_SendRecv       |     1KB |  8.94 Gbps |   1.09M |    9.16 ms |  1.06 | 651.25 KB |       0.03 |
| MessageZeroCopy_SendRecv |     1KB | 204.37 Mbps |  24.95K |  400.83 ms | 46.35 |  11.25 MB |       0.55 |
| Message_SendRecv         |     1KB |  8.69 Gbps |   1.06M |    9.43 ms |  1.09 |   7.71 MB |       0.38 |
|                          |         |            |         |           |       |           |             |
| ByteArray_SendRecv       |    64KB |  5.19 GB/s |  79.13K |  126.37 ms |  1.00 |   1.22 GB |       1.00 |
| ArrayPool_SendRecv       |    64KB |  5.26 GB/s |  80.32K |  124.50 ms |  0.99 | 698.09 KB |       0.00 |
| MessageZeroCopy_SendRecv |    64KB |  1.16 GB/s |  17.74K |  563.85 ms |  4.46 |  11.71 MB |       0.01 |
| Message_SendRecv         |    64KB |  5.17 GB/s |  78.94K |  126.68 ms |  1.00 |   7.79 MB |       0.01 |

### Detailed Metrics

| Method                   | Size    | Score (ops/s) | Error      | StdDev    | Latency   | Gen0      |
|------------------------- |--------:|--------------:|-----------:|----------:|----------:|----------:|
| ByteArray_SendRecv       |     64B |       294.36 |   0.1038 ms |   0.0241 ms | 339.72 ns |   50.0000 |
| ArrayPool_SendRecv       |     64B |       180.20 |   0.2714 ms |   0.0630 ms | 554.94 ns |   30.0000 |
| MessageZeroCopy_SendRecv |     64B |         2.76 |  13.1408 ms |   3.0523 ms |  36.25 μs |    1.0000 |
| Message_SendRecv         |     64B |       119.97 |   0.0909 ms |   0.0211 ms | 833.57 ns |   46.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    512B |       159.82 |   0.0851 ms |   0.0198 ms | 625.69 ns |   88.0000 |
| ArrayPool_SendRecv       |    512B |       151.01 |   0.0596 ms |   0.0139 ms | 662.23 ns |   26.0000 |
| MessageZeroCopy_SendRecv |    512B |         2.55 |  20.7017 ms |   4.8086 ms |  39.20 μs |    1.0000 |
| Message_SendRecv         |    512B |       105.31 |   0.1804 ms |   0.0419 ms | 949.58 ns |   40.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |     1KB |       115.64 |   0.1907 ms |   0.0443 ms | 864.77 ns |  107.0000 |
| ArrayPool_SendRecv       |     1KB |       109.13 |   0.3631 ms |   0.0843 ms | 916.32 ns |   18.0000 |
| MessageZeroCopy_SendRecv |     1KB |         2.49 |  23.3933 ms |   5.4338 ms |  40.08 μs |    1.0000 |
| Message_SendRecv         |     1KB |       106.04 |   0.1806 ms |   0.0420 ms | 943.08 ns |   41.0000 |
|                          |         |               |            |           |           |           |
| ByteArray_SendRecv       |    64KB |         7.91 |   5.0892 ms |   1.1821 ms |  12.64 μs |  223.0000 |
| ArrayPool_SendRecv       |    64KB |         8.03 |   3.5545 ms |   0.8256 ms |  12.45 μs |    2.0000 |
| MessageZeroCopy_SendRecv |    64KB |         1.77 |  11.7885 ms |   2.7382 ms |  56.38 μs |    1.0000 |
| Message_SendRecv         |    64KB |         7.89 |   4.5665 ms |   1.0607 ms |  12.67 μs |    3.0000 |

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
- **Best for**: Small to medium messages where throughput is critical

**Performance:**
- 64B: 2.94M msg/sec (highest)
- 512B: 1.60M msg/sec (highest)
- 1KB: 1.16M msg/sec (competitive)
- 64KB: 79K msg/sec (highest)

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
- Significantly reduced GC pressure (6% allocation @ 512B, 3% @ 1KB)
- Requires buffer management discipline
- **Best for**: Medium to large messages where GC pressure matters

**Performance:**
- 64B: 1.80M msg/sec (61% of ByteArray)
- 512B: 1.51M msg/sec (94% of ByteArray, 94% less allocation)
- 1KB: 1.09M msg/sec (94% of ByteArray, 97% less allocation)
- 64KB: 80K msg/sec (101% of ByteArray, >99% less allocation)

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
- Medium GC pressure (4.21x @ 64B, 0.73x @ 512B, 0.38x @ 1KB)
- Direct memory access via MemorySegment
- **Best for**: Medium messages (512B-8KB)

**Performance:**
- 64B: 1.20M msg/sec (41% of ByteArray)
- 512B: 1.05M msg/sec (66% of ByteArray)
- 1KB: 1.06M msg/sec (92% of ByteArray)
- 64KB: 79K msg/sec (100% of ByteArray)

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
- **Severe performance degradation** (107x slower @ 64B, 63x @ 512B)
- Arena allocation overhead dominates any zero-copy benefit
- **Avoid in production**

**Performance:**
- 64B: 27K msg/sec (0.9% of ByteArray) ❌
- 512B: 25K msg/sec (1.6% of ByteArray) ❌
- 1KB: 25K msg/sec (2.1% of ByteArray) ❌
- 64KB: 18K msg/sec (22% of ByteArray) ❌

### Recommendations by Message Size

| Message Size | Recommended Strategy | Throughput | Reason |
|--------------|---------------------|------------|--------|
| < 512 B | **ByteArray** | 2.94M msg/sec | Highest throughput, simple implementation |
| 512 B - 1 KB | **ByteArray** or **ArrayPool** | ~1.5M msg/sec | Similar performance, choose based on GC requirements |
| 1-8 KB | **Message** or **ArrayPool** | ~1M msg/sec | Lower GC pressure with competitive throughput |
| > 8 KB | **ArrayPool** | 80K msg/sec | >99% less GC pressure with best throughput |

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
   - Small messages (<512B): Use `ByteArray` (2.94M msg/sec @ 64B)
   - Medium messages (512B-1KB): Use `ByteArray` or `ArrayPool` (1.5M msg/sec @ 512B)
   - Large messages (>8KB): Use `ArrayPool` for reduced GC (>99% less allocation)
   - **Never use** `MessageZeroCopy` (63-107x slower)

2. **Receive Mode**:
   - Single socket: Use `Blocking` (simplest)
   - Multiple sockets: Use `Poller` (98-104% of Blocking performance)
   - **Never use** `NonBlocking` with sleep (2x slower for large messages)

3. **GC Pressure**:
   - ArrayPool reduces allocation by 94-99% vs ByteArray
   - Trade-off: Slightly lower throughput for medium messages (6-12%)
   - Consider ArrayPool when GC pauses are problematic

4. **Latency**:
   - Small messages: Sub-microsecond latency (340-830 ns)
   - Medium messages: ~600-950 ns (512B-1KB)
   - Large messages: 12-15 microseconds (64KB)
