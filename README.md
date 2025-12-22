[![English](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](README.ko.md)

# JVM-ZMQ

[![CI - Build and Test](https://github.com/ulala-x/jvm-zmq/actions/workflows/ci.yml/badge.svg)](https://github.com/ulala-x/jvm-zmq/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/ulala-x/jvm-zmq)](https://github.com/ulala-x/jvm-zmq/releases)
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

### GitHub Packages

This library is published to GitHub Packages. Add the repository and dependency:

#### Gradle

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/ulala-x/jvm-zmq")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("io.github.ulalax:zmq:0.1")
}
```

#### Maven

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/ulala-x/jvm-zmq</url>
    </repository>
</repositories>

<dependency>
    <groupId>io.github.ulalax</groupId>
    <artifactId>zmq</artifactId>
    <version>0.1</version>
</dependency>
```

Add credentials to `~/.m2/settings.xml`:
```xml
<servers>
    <server>
        <id>github</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
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

Performance benchmarks on Router-to-Router pattern (Ubuntu 24.04 LTS, JDK 22.0.2, 10,000 messages per iteration):

### Memory Strategy Performance

**Small Messages (64 bytes):**
- **ByteArray**: 2.94M msg/sec (1.51 Gbps) - **Best for small messages**
- ArrayPool: 1.80M msg/sec (923 Mbps, 74% less allocation vs ByteArray)
- Message: 1.20M msg/sec (614 Mbps)
- ❌ MessageZeroCopy: 28K msg/sec (severe degradation)

**Medium Messages (512 bytes):**
- **ByteArray**: 1.60M msg/sec (6.55 Gbps) - **Best throughput**
- ArrayPool: 1.51M msg/sec (6.19 Gbps, 94% less allocation vs ByteArray)
- Message: 1.05M msg/sec (4.31 Gbps)
- ❌ MessageZeroCopy: 26K msg/sec

**Medium Messages (1,024 bytes):**
- **ByteArray**: 1.16M msg/sec (9.47 Gbps) - **Best throughput**
- ArrayPool: 1.09M msg/sec (8.94 Gbps, 97% less allocation vs ByteArray)
- Message: 1.06M msg/sec (8.69 Gbps)
- ❌ MessageZeroCopy: 25K msg/sec

**Large Messages (65,536 bytes):**
- **ArrayPool**: 80K msg/sec (5.26 GB/s, >99% less allocation) - **Best for large messages**
- ByteArray: 79K msg/sec (5.19 GB/s)
- Message: 79K msg/sec (5.17 GB/s)
- ❌ MessageZeroCopy: 18K msg/sec

**Recommendations:**
- **Small messages (<512B)**: Use `socket.send(byte[])` for maximum throughput (2.94M msg/sec @ 64B)
- **Medium messages (512B-1KB)**: Use `ByteArray` or `ArrayPool` - similar performance with 94-97% less GC
- **Large messages (>8KB)**: Use `ArrayPool` pattern to reduce GC pressure (>99% less allocation)
- **Avoid**: `MessageZeroCopy` - shows 63-107x slowdown due to Arena allocation overhead

### Receive Mode Performance

| Message Size | Blocking | Poller | NonBlocking |
|--------------|----------|--------|-------------|
| **64 B** | 1.44M msg/sec | **1.43M msg/sec** | 1.37M msg/sec |
| **512 B** | 1.36M msg/sec | **1.33M msg/sec** | 1.23M msg/sec |
| **1,024 B** | 1.06M msg/sec | **1.07M msg/sec** | 977K msg/sec |
| **65,536 B** | 67K msg/sec | **70K msg/sec** | 34K msg/sec |

**Recommendations:**
- **Single socket**: Use `Blocking` mode (`socket.recv()`) for simplest implementation
- **Multiple sockets**: Use `Poller` for event-driven programming - matches or exceeds blocking performance (98-104%)
- **Avoid**: `NonBlocking` with busy-wait/sleep - not recommended for production (2x slower for large messages)

### Running Benchmarks

```bash
# Run all JMH benchmarks
./gradlew :zmq:jmh

# Run specific benchmark
./gradlew :zmq:jmh -PjmhIncludes='.*MemoryStrategyBenchmark.*'
./gradlew :zmq:jmh -PjmhIncludes='.*ReceiveModeBenchmark.*'

# Format results in .NET BenchmarkDotNet style
cd zmq && python3 scripts/format_jmh_dotnet_style.py
```

Results are saved to `zmq/build/reports/jmh/results.json` (JSON) and `results-formatted.txt` (human-readable).

For complete benchmark analysis, implementation details, and optimization guidelines, see **[Performance Benchmarks](docs/BENCHMARKS.md)**.

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

# Install to local Maven repository (for local development/testing)
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

## Documentation

- **[API Documentation](https://ulala-x.github.io/jvm-zmq/)** - Complete Javadoc API reference
- **[Performance Benchmarks](docs/BENCHMARKS.md)** - Detailed benchmark results and analysis
- **[Sample Code](zmq-samples/)** - 13 sample applications

## Changelog

### v0.2 (Upcoming)
- **Breaking Change**: Simplified Socket API to .NET style
  - `send()` now returns `boolean` (true=success, false=EAGAIN)
  - `recv()` now returns `int` (bytes received, -1=EAGAIN)
  - Real errors throw `ZmqException`
- Added `tryRecv()` convenience methods for non-blocking operations
- Removed `SendResult` and `RecvResult` wrapper classes
- Removed `recvBytes()` methods (use `recv(buffer)` instead)
- Performance: No regression, cleaner API

### v0.1
- Initial release
- Java 22 FFM API bindings for ZeroMQ
- All socket types supported (REQ, REP, PUB, SUB, PUSH, PULL, DEALER, ROUTER, PAIR, XPUB, XSUB, STREAM)
- CURVE security support
- Cross-platform native libraries (Windows, Linux, macOS - x64/ARM64)
- Comprehensive benchmarks and samples

## License

MIT License - see [LICENSE](LICENSE) for details.

## Related Projects

- [libzmq](https://github.com/zeromq/libzmq) - ZeroMQ core library
- [libzmq-native](https://github.com/ulala-x/libzmq-native) - Cross-platform native binaries for Windows/Linux/macOS (x64/ARM64)
- [net-zmq](https://github.com/ulala-x/net-zmq) - .NET 8+ ZeroMQ bindings with cppzmq-style API
