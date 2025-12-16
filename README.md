# JVM-ZMQ

[![Build and Test](https://github.com/ulala-x/jvm-zmq/actions/workflows/build.yml/badge.svg)](https://github.com/ulala-x/jvm-zmq/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ulalax/zmq.svg)](https://search.maven.org/artifact/io.github.ulalax/zmq)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

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

    String request = server.recvString();
    server.send("World");
}

// Client
try (Context ctx = new Context();
     Socket client = new Socket(ctx, SocketType.REQ)) {
    client.connect("tcp://localhost:5555");
    client.send("Hello");
    String reply = client.recvString();
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
    byte[] senderId = peerA.recvBytes();
    String message = peerA.recvString();

    // Peer A replies using sender's identity
    peerA.send(senderId, SendFlags.SEND_MORE);
    peerA.send("Hello back from Peer A!");
}
```

### Polling

```java
import io.github.ulalax.zmq.*;

PollItem[] items = new PollItem[] {
    new PollItem(socket1, PollEvents.IN),
    new PollItem(socket2, PollEvents.IN)
};

if (Poller.poll(items, 1000) > 0) {
    if (items[0].isReadable()) { /* handle socket1 */ }
    if (items[1].isReadable()) { /* handle socket2 */ }
}
```

### Message API

```java
import io.github.ulalax.zmq.*;

// Create and send message
try (Message msg = new Message("Hello World")) {
    socket.send(msg, SendFlags.NONE);
}

// Receive message
try (Message reply = socket.recvMessage()) {
    System.out.println(reply.toString());
}
```

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
| Windows | x64 |
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
├── zmq/               # High-level API
│   └── src/main/java/io/github/ulalax/zmq/
│       ├── Context.java           # ZMQ context
│       ├── Socket.java            # ZMQ socket
│       ├── Message.java           # ZMQ message
│       ├── MultipartMessage.java  # Multipart utilities
│       ├── Poller.java            # Polling utilities
│       ├── Curve.java             # CURVE security
│       └── Proxy.java             # Proxy utilities
│
├── zmq-samples/       # Sample applications
│   └── src/main/java/io/github/ulalax/zmq/samples/
│       └── *.java                 # 13 sample programs
│
└── zmq-benchmark/     # JMH benchmarks
    └── src/jmh/java/io/github/ulalax/zmq/benchmark/
        └── *.java                 # Performance benchmarks
```

## License

MIT License - see [LICENSE](LICENSE) for details.

## Related Projects

- [libzmq](https://github.com/zeromq/libzmq) - ZeroMQ core library
- [libzmq-native](https://github.com/ulala-x/libzmq-native) - Native binaries
- [netzmq](https://github.com/ulala-x/netzmq) - .NET ZeroMQ bindings
