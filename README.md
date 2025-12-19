# JVM-ZMQ

[![Build and Test](https://github.com/ulala-x/jvm-zmq/actions/workflows/build.yml/badge.svg)](https://github.com/ulala-x/jvm-zmq/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ulalax/zmq.svg)](https://search.maven.org/artifact/io.github.ulalax/zmq)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API Documentation](https://img.shields.io/badge/API-Documentation-blue)](https://ulala-x.github.io/jvm-zmq/)

A modern Java binding for ZeroMQ (libzmq) using JDK 22+ FFM (Foreign Function & Memory) API.

## Features

- **Java 22 FFM API**: Direct native library binding without JNI overhead, using stable Foreign Function & Memory API
- **Type-Safe API**: Strongly-typed enums, socket options, and message handling
- **Resource-Safe**: AutoCloseable resources with Cleaner-based automatic finalization
- **Cross-Platform**: Bundled native libraries for Windows, Linux, and macOS (x86_64 and ARM64)
- **Complete ZMQ Support**: All socket types, patterns, and advanced features including CURVE security
- **Zero Native Dependencies**: Native libzmq libraries automatically extracted and loaded at runtime

## Installation

### Gradle

```kotlin
dependencies {
    implementation("io.github.ulalax:zmq:1.0.0-SNAPSHOT")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.ulalax</groupId>
    <artifactId>zmq</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Important**: Enable native access when running your application:

```bash
java --enable-native-access=ALL-UNNAMED -jar your-app.jar
```

## Quick Start

### REQ-REP Pattern

```java
import io.github.ulalax.zmq.*;

// Server
try (Context ctx = new Context();
     Socket server = new Socket(ctx, SocketType.REP)) {
    server.bind("tcp://*:5555");

    // Blocking receive - returns RecvResult
    RecvResult<String> result = server.recvString();
    result.ifPresent(request -> {
        System.out.println("Received: " + request);
        server.send("World");
    });
}

// Client
try (Context ctx = new Context();
     Socket client = new Socket(ctx, SocketType.REQ)) {
    client.connect("tcp://localhost:5555");

    // Send returns SendResult
    SendResult sendResult = client.send("Hello");
    sendResult.ifPresent(bytes ->
        System.out.println("Sent " + bytes + " bytes")
    );

    // Receive with Result API
    client.recvString().ifPresent(reply ->
        System.out.println("Received: " + reply)
    );
}
```

### PUB-SUB Pattern

```java
import io.github.ulalax.zmq.*;

// Publisher
try (Context ctx = new Context();
     Socket pub = new Socket(ctx, SocketType.PUB)) {
    pub.bind("tcp://*:5556");
    pub.send("topic1 Hello subscribers!");
}

// Subscriber
try (Context ctx = new Context();
     Socket sub = new Socket(ctx, SocketType.SUB)) {
    sub.connect("tcp://localhost:5556");
    sub.subscribe("topic1");

    // Receive with Result API
    sub.recvString().ifPresent(message ->
        System.out.println("Received: " + message)
    );
}
```

### Router-to-Router Pattern

```java
import io.github.ulalax.zmq.*;
import java.nio.charset.StandardCharsets;

try (Context ctx = new Context();
     Socket peerA = new Socket(ctx, SocketType.ROUTER);
     Socket peerB = new Socket(ctx, SocketType.ROUTER)) {

    // Set explicit identities for Router-to-Router
    peerA.setOption(SocketOption.ROUTING_ID, "PEER_A".getBytes(StandardCharsets.UTF_8));
    peerB.setOption(SocketOption.ROUTING_ID, "PEER_B".getBytes(StandardCharsets.UTF_8));

    peerA.bind("tcp://127.0.0.1:5555");
    peerB.connect("tcp://127.0.0.1:5555");

    // Peer B sends to Peer A (first frame = target identity)
    peerB.send("PEER_A".getBytes(StandardCharsets.UTF_8), SendFlags.SEND_MORE);
    peerB.send("Hello from Peer B!");

    // Peer A receives (first frame = sender identity) - using Result API
    RecvResult<byte[]> senderIdResult = peerA.recvBytes();
    RecvResult<String> messageResult = peerA.recvString();

    if (senderIdResult.isPresent() && messageResult.isPresent()) {
        byte[] senderId = senderIdResult.value();
        String message = messageResult.value();
        System.out.println("Received from peer: " + message);

        // Peer A replies using sender's identity
        peerA.send(senderId, SendFlags.SEND_MORE);
        peerA.send("Hello back from Peer A!");
    }
}
```

### Polling

```java
import io.github.ulalax.zmq.*;

try (Poller poller = new Poller()) {
    int idx1 = poller.register(socket1, PollEvents.IN);
    int idx2 = poller.register(socket2, PollEvents.IN);

    if (poller.poll(1000) > 0) {
        if (poller.isReadable(idx1)) { /* handle socket1 */ }
        if (poller.isReadable(idx2)) { /* handle socket2 */ }
    }
}
```

### Non-blocking I/O with Result API

```java
import io.github.ulalax.zmq.*;

try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.DEALER)) {
    socket.connect("tcp://localhost:5555");

    // Non-blocking send
    SendResult sendResult = socket.send(data, SendFlags.DONT_WAIT);
    if (sendResult.wouldBlock()) {
        System.out.println("Socket not ready - would block");
    } else {
        System.out.println("Sent " + sendResult.value() + " bytes");
    }

    // Non-blocking receive with functional style
    socket.recvString(RecvFlags.DONT_WAIT)
        .ifPresent(msg -> System.out.println("Received: " + msg));

    // Transform received data
    RecvResult<Integer> length = socket.recvBytes(RecvFlags.DONT_WAIT)
        .map(bytes -> bytes.length);
    length.ifPresent(len -> System.out.println("Length: " + len));
}
```

## Performance

Benchmark results on Router-to-Router pattern (10,000 messages per iteration):

### Memory Strategy Performance

Different memory strategies show varying performance characteristics depending on message size:

| Message Size | ByteArray | ArrayPool | Message | MessageZeroCopy |
|--------------|-----------|-----------|---------|-----------------|
| **64 B** | **3.71M msg/sec** | 1.83M msg/sec | 1.15M msg/sec | 26K msg/sec |
| **1,500 B** | 835K msg/sec | 842K msg/sec | **861K msg/sec** | 24K msg/sec |
| **65,536 B** | 89K msg/sec | **91K msg/sec** | 70K msg/sec | 16K msg/sec |

**Throughput = ops/sec × 10,000 messages*

**Recommendations:**
- **Small messages (< 1KB)**: Use `ByteArray` (`socket.send(byte[])`) for maximum throughput
- **Medium messages (1-8KB)**: Use `Message` or `ByteArray` - similar performance
- **Large messages (> 8KB)**: Use `ArrayPool` pattern to reduce GC pressure
- **Avoid**: `MessageZeroCopy` shows severe performance degradation due to Arena allocation overhead

### Receive Mode Performance

Three receive strategies compared for event-driven applications:

| Message Size | Blocking | Poller | NonBlocking |
|--------------|----------|--------|-------------|
| **64 B** | 1.45M msg/sec | 1.43M msg/sec | 1.37M msg/sec |
| **1,500 B** | 840K msg/sec | 824K msg/sec | 764K msg/sec |
| **65,536 B** | 71K msg/sec | 65K msg/sec | 30K msg/sec |

**Recommendations:**
- **Single socket**: Use `Blocking` mode (`socket.recv()`) for simplest implementation
- **Multiple sockets**: Use `Poller` for event-driven programming with ~98% of blocking performance
- **Avoid**: `NonBlocking` with `Thread.sleep()` - not recommended for production

### Running Benchmarks

```bash
# Run all JMH benchmarks
./gradlew :zmq:jmh

# Run specific benchmark
./gradlew :zmq:jmh -Pjmh.includes=MemoryStrategyBenchmark
./gradlew :zmq:jmh -Pjmh.includes=ReceiveModeBenchmark

# Format results
./gradlew :zmq:formatJmhResults
```

Results are saved to `zmq/build/reports/jmh/results-formatted.txt`

For detailed benchmark results, performance analysis, and implementation patterns, see [docs/BENCHMARKS.md](docs/BENCHMARKS.md).

## Socket Types

| Type | Description |
|------|-------------|
| `SocketType.REQ` | Request socket (client) |
| `SocketType.REP` | Reply socket (server) |
| `SocketType.PUB` | Publish socket |
| `SocketType.SUB` | Subscribe socket |
| `SocketType.PUSH` | Push socket (pipeline) |
| `SocketType.PULL` | Pull socket (pipeline) |
| `SocketType.DEALER` | Async request |
| `SocketType.ROUTER` | Async reply |
| `SocketType.PAIR` | Exclusive pair |
| `SocketType.XPUB` | Extended publish |
| `SocketType.XSUB` | Extended subscribe |
| `SocketType.STREAM` | Raw TCP socket |

## API Reference

### Context

```java
Context ctx = new Context();                              // Default
Context ctx = new Context(ioThreads, maxSockets);         // Custom

ctx.setOption(ContextOption.IO_THREADS, 4);
int threads = ctx.getOption(ContextOption.IO_THREADS);

int[] version = Context.version();                        // Get ZMQ version
boolean hasCurve = Context.has("curve");                  // Check capability
```

### Socket

```java
Socket socket = new Socket(ctx, SocketType.REQ);

// Connection
socket.bind("tcp://*:5555");
socket.connect("tcp://localhost:5555");
socket.unbind("tcp://*:5555");
socket.disconnect("tcp://localhost:5555");

// Send
socket.send("Hello");
socket.send(byteArray);
socket.send(data, SendFlags.SEND_MORE);
boolean sent = socket.trySend(data);

// Receive
String str = socket.recvString();
byte[] data = socket.recvBytes();
boolean received = socket.tryRecvString();

// Options
socket.setOption(SocketOption.LINGER, 0);
int linger = socket.getOption(SocketOption.LINGER);
```

## Samples

The `zmq-samples` module contains 13 sample applications demonstrating all ZeroMQ patterns:

| Sample | Pattern | Description |
|--------|---------|-------------|
| ReqRepSample | REQ-REP | Synchronous request-reply |
| PubSubSample | PUB-SUB | Publish-subscribe with topic filtering |
| PushPullSample | PUSH-PULL | Pipeline (ventilator-worker-sink) |
| PairSample | PAIR | Exclusive 1:1 bidirectional |
| RouterDealerSample | ROUTER-DEALER | Async broker pattern |
| RouterToRouterSample | ROUTER-ROUTER | Peer-to-peer, hub-spoke |
| ProxySample | XPUB-XSUB | Proxy for pub-sub forwarding |
| SteerableProxySample | Steerable Proxy | Controllable proxy (PAUSE/RESUME) |
| PollerSample | Polling | Multi-socket polling |
| MonitorSample | Monitor | Socket event monitoring |
| CurveSecuritySample | CURVE | Encrypted communication |
| MultipartSample | Multipart | Multipart message handling |
| RouterBenchmarkSample | Benchmark | Performance testing |

### Running Samples

```bash
# Run specific sample
./gradlew :zmq-samples:runReqRep
./gradlew :zmq-samples:runPubSub
./gradlew :zmq-samples:runPushPull
./gradlew :zmq-samples:runPair
./gradlew :zmq-samples:runRouterDealer
./gradlew :zmq-samples:runRouterToRouter
./gradlew :zmq-samples:runProxy
./gradlew :zmq-samples:runSteerableProxy
./gradlew :zmq-samples:runPoller
./gradlew :zmq-samples:runMonitor
./gradlew :zmq-samples:runCurveSecurity
./gradlew :zmq-samples:runMultipart
./gradlew :zmq-samples:runRouterBenchmark
```

## Supported Platforms

| OS | Architecture |
|----|--------------|
| Windows | x64, ARM64 |
| Linux | x64, ARM64 |
| macOS | x64, ARM64 |

## Requirements

- JDK 22 or later
- Native libzmq library (automatically provided)

## Building from Source

```bash
# Clone repository
git clone https://github.com/ulala-x/jvm-zmq.git
cd jvm-zmq

# Build
./gradlew build

# Run tests
./gradlew test

# Install to local Maven repository
./gradlew publishToMavenLocal
```

## Project Structure

```
jvm-zmq/
├── zmq-core/          # Low-level FFM bindings
│   └── src/main/java/io/github/ulalax/zmq/core/
│       ├── LibZmq.java        # FFM function bindings
│       ├── ZmqConstants.java  # ZMQ constants
│       ├── ZmqStructs.java    # Memory layouts
│       ├── ZmqException.java  # Exception handling
│       └── NativeLoader.java  # Native library loader
│
├── zmq/               # High-level API & JMH benchmarks
│   ├── src/main/java/io/github/ulalax/zmq/
│   │   ├── Context.java           # ZMQ context
│   │   ├── Socket.java            # ZMQ socket
│   │   ├── Message.java           # ZMQ message
│   │   ├── MultipartMessage.java  # Multipart utilities
│   │   ├── Poller.java            # Instance-based polling
│   │   ├── Curve.java             # CURVE security
│   │   └── Proxy.java             # Proxy utilities
│   └── src/jmh/java/io/github/ulalax/zmq/benchmark/
│       └── *.java                 # Performance benchmarks
│
└── zmq-samples/       # Sample applications
    └── src/main/java/io/github/ulalax/zmq/samples/
        └── *.java                 # 13 sample programs
```

## Migration Guide (v1.x → v2.0)

Version 2.0 introduces a Result-based API following the cppzmq design pattern. This provides better type safety and clearer distinction between successful operations, would-block conditions, and errors.

### Breaking Changes

All `send()` and `recv()` methods now return `SendResult` and `RecvResult` instead of primitive types or throwing exceptions on EAGAIN.

**Old API (v1.x):**
```java
// Blocking operations returned values directly
int bytesSent = socket.send(data);
String message = socket.recvString();
byte[] data = socket.recvBytes();

// Non-blocking operations used try* methods returning boolean
boolean sent = socket.trySend(data, SendFlags.DONT_WAIT);
if (sent) {
    // Success
}

String msg = socket.tryRecvString(RecvFlags.DONT_WAIT);
if (msg != null) {
    // Success - msg contains data
}
```

**New API (v2.0):**
```java
// All operations return Result types
SendResult sendResult = socket.send(data);
RecvResult<String> messageResult = socket.recvString();
RecvResult<byte[]> dataResult = socket.recvBytes();

// Check success with isPresent() or use functional style
if (sendResult.isPresent()) {
    int bytesSent = sendResult.value();
}

// Functional style with ifPresent
messageResult.ifPresent(msg -> processMessage(msg));

// Non-blocking - check wouldBlock() explicitly
SendResult result = socket.send(data, SendFlags.DONT_WAIT);
if (result.wouldBlock()) {
    // Socket not ready
} else {
    // Success
    int bytes = result.value();
}
```

### Migration Patterns

| Old API | New API | Notes |
|---------|---------|-------|
| `int send(data)` | `SendResult send(data)` | Use `.value()` to get bytes sent |
| `String recvString()` | `RecvResult<String> recvString()` | Use `.value()` or `.ifPresent()` |
| `byte[] recvBytes()` | `RecvResult<byte[]> recvBytes()` | Use `.value()` or `.ifPresent()` |
| `boolean trySend(data, flags)` | `SendResult send(data, flags)` | Use `.wouldBlock()` instead of `!sent` |
| `String tryRecvString(flags)` | `RecvResult<String> recvString(flags)` | Use `.wouldBlock()` instead of `== null` |
| `byte[] tryRecvBytes(flags)` | `RecvResult<byte[]> recvBytes(flags)` | Use `.wouldBlock()` instead of `== null` |

### Common Migration Examples

**Blocking receive:**
```java
// Old
String msg = socket.recvString();
process(msg);

// New (Option 1 - Functional)
socket.recvString().ifPresent(msg -> process(msg));

// New (Option 2 - Traditional)
RecvResult<String> result = socket.recvString();
if (result.isPresent()) {
    process(result.value());
}
```

**Non-blocking with retry:**
```java
// Old
byte[] data = socket.tryRecvBytes(RecvFlags.DONT_WAIT);
if (data == null) {
    // Would block - retry later
    scheduleRetry();
} else {
    process(data);
}

// New
RecvResult<byte[]> result = socket.recvBytes(RecvFlags.DONT_WAIT);
if (result.wouldBlock()) {
    scheduleRetry();
} else {
    process(result.value());
}
```

**Transform received data:**
```java
// Old
byte[] data = socket.recvBytes();
int length = data.length;

// New
int length = socket.recvBytes()
    .map(bytes -> bytes.length)
    .orElse(0);
```

For detailed migration information, see [MIGRATION.md](MIGRATION.md).

## Documentation

- [API Documentation](https://ulala-x.github.io/jvm-zmq/) - Complete Javadoc API reference
- [Performance Benchmarks](docs/BENCHMARKS.md) - Detailed benchmark results, performance analysis, and optimization guidelines
- [Migration Guide](MIGRATION.md) - Comprehensive guide for upgrading from v1.x to v2.0

## License

MIT License - see [LICENSE](LICENSE) for details.

## Related Projects

- [libzmq](https://github.com/zeromq/libzmq) - ZeroMQ core library
- [libzmq-native](https://github.com/ulala-x/libzmq-native) - Native binaries
- [netzmq](https://github.com/ulala-x/netzmq) - .NET ZeroMQ bindings
