[![English](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](README.ko.md)

# JVM-ZMQ

[![CI - Build and Test](https://github.com/ulala-x/jvm-zmq/actions/workflows/ci.yml/badge.svg)](https://github.com/ulala-x/jvm-zmq/actions/workflows/ci.yml)
[![GitHub Release](https://img.shields.io/github/v/release/ulala-x/jvm-zmq)](https://github.com/ulala-x/jvm-zmq/releases)
[![changelog](https://img.shields.io/badge/changelog-view-blue)](CHANGELOG.md)
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

    // Blocking receive
    String request = server.recvString();
    System.out.println("Received: " + request);
    server.send("World");
}

// Client
try (Context ctx = new Context();
     Socket client = new Socket(ctx, SocketType.REQ)) {
    client.connect("tcp://localhost:5555");

    // Send returns boolean (true=success, false=would block)
    client.send("Hello");

    // Blocking receive
    String reply = client.recvString();
    System.out.println("Received: " + reply);
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

    String message = sub.recvString();
    System.out.println("Received: " + message);
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

    // Peer A receives (first frame = sender identity)
    byte[] senderId = new byte[256];
    int idLen = peerA.recv(senderId, RecvFlags.NONE);
    String message = peerA.recvString();
    System.out.println("Received from peer: " + message);

    // Peer A replies using sender's identity
    peerA.send(senderId, 0, idLen, SendFlags.SEND_MORE);
    peerA.send("Hello back from Peer A!");
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

### Non-blocking I/O

```java
import io.github.ulalax.zmq.*;

try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.DEALER)) {
    socket.connect("tcp://localhost:5555");

    // Non-blocking send: returns false if would block
    boolean sent = socket.send(data, SendFlags.DONT_WAIT);
    if (!sent) {
        System.out.println("Socket not ready - would block");
    }

    // Non-blocking receive: returns -1 if would block
    byte[] buffer = new byte[1024];
    int bytes = socket.recv(buffer, RecvFlags.DONT_WAIT);
    if (bytes == -1) {
        System.out.println("No message available");
    } else {
        System.out.println("Received " + bytes + " bytes");
    }

    // Convenience method with Optional
    socket.tryRecvString().ifPresent(msg ->
        System.out.println("Received: " + msg)
    );
}
```

## Performance

Based on JMH benchmarks (Router-to-Router pattern, 10,000 messages per iteration):

### Message Buffer Strategy

| Strategy | 64B | 512B | 1KB | 64KB | GC Pressure |
|----------|-----|------|-----|------|-------------|
| **ArrayPool (Recommended)** | 1.29M msg/s | 1.28M msg/s | 985K msg/s | 78K msg/s | Low |
| ByteArray | 1.71M msg/s | 1.33M msg/s | 1.04M msg/s | 72K msg/s | High |
| Message | 1.06M msg/s | 994K msg/s | 997K msg/s | 78K msg/s | Medium |

**Recommended: `PooledByteBufAllocator` (Netty)**
- Consistent ~178KB memory allocation across all message sizes
- **99.99% less memory** than ByteArray for 64KB messages
- Ideal for long-running servers

### Receive Mode

| Mode | 64B | 512B | 1KB | 64KB |
|------|-----|------|-----|------|
| **PureBlocking** | 1.48M msg/s | 1.36M msg/s | 1.10M msg/s | 70K msg/s |
| **Poller** | 1.48M msg/s | 1.34M msg/s | 1.10M msg/s | 68K msg/s |
| NonBlocking | 1.38M msg/s | 1.27M msg/s | 943K msg/s | 44K msg/s |

**Recommended:**
- Single socket → Blocking `recv()`
- Multiple sockets → `Poller`

### Recommended Pattern

High-performance receiver pattern for production:

> **Note**: Using `Message` object receives exactly the sent size, but when using `byte[]` buffer,
> you must pre-allocate for the maximum expected message size. `recv()` returns actual bytes received.

```java
import io.github.ulalax.zmq.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;

public class HighPerformanceReceiver {
    private static final PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
    private static final int MAX_MESSAGE_SIZE = 65536;

    public void receiveMessages(Socket socket) {
        // When receiving to byte[], pre-allocate for max message size
        byte[] recvBuffer = new byte[MAX_MESSAGE_SIZE];

        while (running) {
            // Blocking receive - best for single socket
            int size = socket.recv(recvBuffer, RecvFlags.NONE);

            // Use pooled buffer for processing (avoids GC pressure)
            ByteBuf buf = allocator.buffer(size);
            try {
                buf.writeBytes(recvBuffer, 0, size);
                processMessage(buf);
            } finally {
                buf.release();
            }
        }
    }
}
```

For multiple sockets, use Poller:

```java
try (Poller poller = new Poller()) {
    poller.register(socket1, PollEvents.IN);
    poller.register(socket2, PollEvents.IN);

    while (running) {
        poller.poll(-1);  // Wait for events

        if (poller.isReadable(0)) {
            int size = socket1.recv(buffer, RecvFlags.NONE);
            // process...
        }
        if (poller.isReadable(1)) {
            int size = socket2.recv(buffer, RecvFlags.NONE);
            // process...
        }
    }
}
```

For detailed benchmark results, methodology, and alternative strategies, see [Performance Benchmarks](docs/BENCHMARKS.md).

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

// Send - returns boolean (true=success, false=would block)
socket.send("Hello");                           // blocking
socket.send(byteArray);                         // blocking
socket.send(data, SendFlags.SEND_MORE);         // multipart
socket.send(data, SendFlags.DONT_WAIT);         // non-blocking

// Receive - returns int (bytes received, -1=would block)
String str = socket.recvString();               // blocking
int bytes = socket.recv(buffer, RecvFlags.NONE);      // to buffer
int bytes = socket.recv(buffer, RecvFlags.DONT_WAIT); // non-blocking

// Convenience methods
socket.tryRecvString();                         // Optional<String>
socket.tryRecv(buffer);                         // non-blocking to buffer

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

## License

MIT License - see [LICENSE](LICENSE) for details.

## Related Projects

- [libzmq](https://github.com/zeromq/libzmq) - ZeroMQ core library
- [libzmq-native](https://github.com/ulala-x/libzmq-native) - Cross-platform native binaries for Windows/Linux/macOS (x64/ARM64)
- [net-zmq](https://github.com/ulala-x/net-zmq) - .NET 8+ ZeroMQ bindings with cppzmq-style API
