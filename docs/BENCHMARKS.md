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

Comparison of five buffer management strategies for sending/receiving messages.

### Performance Overview

| Strategy | 64B | 512B | 1KB | 64KB | 128KB | 256KB |
|----------|-----|------|-----|------|-------|-------|
| **ByteArray** | 5,745 ops/s | 5,307 ops/s | 4,031 ops/s | - | - | - |
| **ArrayPool_Heap** | 5,147 ops/s | 3,416 ops/s | 4,375 ops/s | 824 ops/s | 512 ops/s | 287 ops/s |
| **ArrayPool_Direct** | 4,581 ops/s | 4,693 ops/s | 4,446 ops/s | 821 ops/s | 443 ops/s | 280 ops/s |
| **Message** | - | - | - | - | - | - |
| **MessageZeroCopy** | - | - | - | - | - | - |

### Memory Allocation

| Strategy | Allocation per iteration |
|----------|-------------------------|
| **ByteArray** | Scales with message size |
| **ArrayPool_Heap** | ~17KB (constant) |
| **ArrayPool_Direct** | ~44KB (constant) |
| **Message** | - |
| **MessageZeroCopy** | - |

### Key Findings

**Recommended: ArrayPool_Heap for production use**
- Consistent low memory allocation (~17KB) regardless of message size
- Good performance across all message sizes
- Lower GC pressure compared to Direct allocation
- ArrayPool_Heap often outperforms ArrayPool_Direct in many scenarios

### Strategy Descriptions

#### 1. ByteArray_SendRecv (Baseline)
```java
// Sending
byte[] sendBuffer = new byte[messageSize];
System.arraycopy(sourceData, 0, sendBuffer, 0, messageSize);
socket.send(sendBuffer, SendFlags.DONT_WAIT);

// Receiving - uses fixed buffer
socket.recv(recvBuffer, RecvFlags.NONE);
byte[] outputBuffer = new byte[size];
System.arraycopy(recvBuffer, 0, outputBuffer, 0, size);
```

**Characteristics:**
- Allocates new byte arrays for every send/receive
- Highest GC pressure - scales with message size
- Simple implementation
- **Best for**: Small messages only (<1KB)

**Performance:**
- 64B: 5,745 ops/s
- 512B: 5,307 ops/s
- 1KB: 4,031 ops/s
- Not suitable for large messages (64KB+)

#### 2. ArrayPool_Heap (RECOMMENDED)
```java
// Sending
ByteBuf sendBuf = heapAllocator.buffer(messageSize);
try {
    sendBuf.writeBytes(sourceData, 0, messageSize);
    sendBuf.getBytes(0, reusableSendBuffer, 0, messageSize);
    socket.send(reusableSendBuffer, SendFlags.DONT_WAIT);
} finally {
    sendBuf.release();
}

// Receiving
socket.recv(recvBuffer, RecvFlags.NONE);
ByteBuf outputBuf = heapAllocator.buffer(size);
try {
    outputBuf.writeBytes(recvBuffer, 0, size);
    outputBuf.getBytes(0, reusableRecvBuffer, 0, size);
} finally {
    outputBuf.release();
}
```

**Characteristics:**
- Uses Netty PooledByteBufAllocator with heap buffers
- **Constant low memory allocation (~17KB)** regardless of message size
- Lower GC pressure compared to Direct allocation
- **Best for**: Production use, all message sizes

**Performance:**
- 64B: 5,147 ops/s
- 512B: 3,416 ops/s
- 1KB: 4,375 ops/s
- 64KB: 824 ops/s
- 128KB: 512 ops/s
- 256KB: 287 ops/s

#### 3. ArrayPool_Direct
```java
// Same as ArrayPool_Heap but uses direct buffers
ByteBuf sendBuf = directAllocator.buffer(messageSize);
```

**Characteristics:**
- Uses Netty PooledByteBufAllocator with direct buffers
- Higher allocation overhead (~44KB vs ~17KB for Heap)
- Direct buffers may be faster for very large messages in some cases
- **Use only if**: Profiling shows benefit for your specific workload

**Performance:**
- 64B: 4,581 ops/s
- 512B: 4,693 ops/s
- 1KB: 4,446 ops/s
- 64KB: 821 ops/s
- 128KB: 443 ops/s
- 256KB: 280 ops/s

#### 4. Message_SendRecv
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
    // Use msg.data() directly
}
```

**Characteristics:**
- Uses ZMQ native message objects
- Direct memory access via MemorySegment
- **Best for**: When native ZMQ Message API is preferred

#### 5. MessageZeroCopy_SendRecv (REMOVED)
This strategy has been removed due to severe performance issues with Arena allocation overhead.

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
| **Production servers** | **ArrayPool_Heap** | Constant ~17KB allocation, minimal GC pressure |
| Small messages (<1KB) | ByteArray or ArrayPool_Heap | Both perform well, ArrayPool has lower GC pressure |
| Large messages (>64KB) | **ArrayPool_Heap** | Essential for memory efficiency |
| Native ZMQ API preference | Message | Direct MemorySegment access |
| Direct buffers needed | ArrayPool_Direct | Only if profiling shows benefit |

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
# Message buffer strategy only
./gradlew :zmq:jmh -PjmhIncludes='.*MessageBufferStrategyBenchmark.*'

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

1. **Message Buffer Strategy** (5 strategies tested):
   - **Use `ArrayPool_Heap` for production** - constant ~17KB allocation regardless of message size
   - Small messages (<1KB): `ByteArray` and `ArrayPool_Heap` both perform well (~4,000-5,700 ops/s)
   - Large messages (>64KB): `ArrayPool_Heap` is essential for memory efficiency
   - `ArrayPool_Heap` often outperforms `ArrayPool_Direct` due to lower allocation overhead
   - `MessageZeroCopy` has been removed due to severe performance issues

2. **Receive Buffer**:
   - Always use pre-allocated fixed buffers for receiving
   - Avoid allocating new `byte[]` for each receive operation
   - This applies to all strategies for minimizing GC pressure

3. **Receive Mode** (4 strategies tested):
   - Single socket (simple): Use `PureBlocking` (simplest, 1.48M msg/sec @ 64B)
   - Single socket (optimized): Use `BlockingBatch` (reduces syscalls)
   - Multiple sockets: Use `Poller` (100% of PureBlocking performance, multi-socket support)
   - Avoid `NonBlocking` with sleep (37% slower for large messages)

4. **Memory Allocation**:
   - ArrayPool_Heap: ~17KB (constant)
   - ArrayPool_Direct: ~44KB (constant)
   - ByteArray: Scales linearly with message size
   - Heap allocation is more efficient than Direct in most cases

5. **Latency**:
   - Small messages: Sub-microsecond latency (584-942 ns)
   - Medium messages: ~750 ns - 1.02 μs (512B-1KB)
   - Large messages: 12-14 microseconds (64KB)
