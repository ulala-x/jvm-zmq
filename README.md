# JVM-ZMQ

[![Build and Test](https://github.com/ulala-x/jvm-zmq/actions/workflows/build.yml/badge.svg)](https://github.com/ulala-x/jvm-zmq/actions/workflows/build.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ulalax/zmq.svg)](https://search.maven.org/artifact/io.github.ulalax/zmq)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Modern Java bindings for ZeroMQ using JDK 21+ FFM (Foreign Function & Memory) API.

## Features

- **Java 21 FFM API**: Direct native library binding without JNI overhead, using Foreign Function & Memory API
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

```kotlin
// build.gradle.kts
tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
```

Or via command line:

```bash
java --enable-native-access=ALL-UNNAMED -jar your-app.jar
```

## Quick Start

### REQ-REP Pattern

```java
import io.github.ulalax.zmq.*;

public class ReqRepExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.REP);
             Socket client = new Socket(ctx, SocketType.REQ)) {

            server.bind("tcp://*:5555");
            client.connect("tcp://localhost:5555");

            // Client sends request
            client.send("Hello");

            // Server receives and replies
            String request = server.recvString();
            System.out.println("Server received: " + request);
            server.send("World");

            // Client receives reply
            String reply = client.recvString();
            System.out.println("Client received: " + reply);
        }
    }
}
```

### PUB-SUB Pattern

```java
import io.github.ulalax.zmq.*;

// Publisher
try (Context ctx = new Context();
     Socket pub = new Socket(ctx, SocketType.PUB)) {

    pub.bind("tcp://*:5556");
    Thread.sleep(100); // Give time for subscribers to connect
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

### ROUTER-DEALER Pattern

```java
import io.github.ulalax.zmq.*;
import java.nio.charset.StandardCharsets;

try (Context ctx = new Context();
     Socket router = new Socket(ctx, SocketType.ROUTER);
     Socket dealer = new Socket(ctx, SocketType.DEALER)) {

    // Set dealer identity
    dealer.setOption(SocketOption.ROUTING_ID, "CLIENT1".getBytes(StandardCharsets.UTF_8));

    router.bind("tcp://127.0.0.1:5555");
    dealer.connect("tcp://127.0.0.1:5555");
    Thread.sleep(100);

    // Dealer sends to Router
    dealer.send("Hello from Dealer!");

    // Router receives (first frame = sender identity)
    byte[] identity = router.recvBytes();
    String message = router.recvString();

    // Router replies using sender's identity
    router.send(identity, SendFlags.SEND_MORE);
    router.send("Hello back from Router!");

    // Dealer receives reply
    String reply = dealer.recvString();
    System.out.println("Dealer received: " + reply);
}
```

### Polling Multiple Sockets

```java
import io.github.ulalax.zmq.*;

try (Context ctx = new Context();
     Socket socket1 = new Socket(ctx, SocketType.PULL);
     Socket socket2 = new Socket(ctx, SocketType.PULL)) {

    socket1.bind("tcp://*:5558");
    socket2.bind("tcp://*:5559");

    PollItem[] items = {
        new PollItem(socket1, PollEvents.IN),
        new PollItem(socket2, PollEvents.IN)
    };

    // Wait for events with 1 second timeout
    int ready = Poller.poll(items, 1000);

    if (items[0].isReadable()) {
        byte[] data = socket1.recvBytes();
        // Process data from socket1
    }

    if (items[1].isReadable()) {
        byte[] data = socket2.recvBytes();
        // Process data from socket2
    }
}
```

### Multipart Messages

```java
import io.github.ulalax.zmq.*;

try (Context ctx = new Context();
     Socket sender = new Socket(ctx, SocketType.PUSH);
     Socket receiver = new Socket(ctx, SocketType.PULL)) {

    sender.bind("tcp://*:5560");
    receiver.connect("tcp://localhost:5560");
    Thread.sleep(100);

    // Send multipart message
    MultipartMessage outMsg = new MultipartMessage();
    outMsg.add("Frame 1");
    outMsg.add("Frame 2");
    outMsg.add("Frame 3");
    sender.send(outMsg);

    // Receive multipart message
    MultipartMessage inMsg = receiver.recvMultipart();
    for (String frame : inMsg.asStrings()) {
        System.out.println("Received frame: " + frame);
    }
}
```

## Socket Types

| Type | Description |
|------|-------------|
| `SocketType.REQ` | Request socket for client-side REQ-REP |
| `SocketType.REP` | Reply socket for server-side REQ-REP |
| `SocketType.PUB` | Publish socket for PUB-SUB |
| `SocketType.SUB` | Subscribe socket for PUB-SUB |
| `SocketType.PUSH` | Push socket for PUSH-PULL pipeline |
| `SocketType.PULL` | Pull socket for PUSH-PULL pipeline |
| `SocketType.DEALER` | Async request socket |
| `SocketType.ROUTER` | Async reply socket with routing |
| `SocketType.PAIR` | Exclusive pair for inter-thread communication |
| `SocketType.XPUB` | Extended publish with subscription backflow |
| `SocketType.XSUB` | Extended subscribe |
| `SocketType.STREAM` | Raw TCP socket |

## API Overview

### Context

```java
// Create context
Context ctx = new Context();
Context ctx = new Context(ioThreads, maxSockets);

// Context options
ctx.setOption(ContextOption.IO_THREADS, 4);
int threads = ctx.getOption(ContextOption.IO_THREADS);

// Get ZMQ version
var (major, minor, patch) = Context.version();

// Check capability
boolean hasCurve = Context.has("curve");
```

### Socket

```java
Socket socket = new Socket(ctx, SocketType.REQ);

// Connection
socket.bind("tcp://*:5555");
socket.connect("tcp://localhost:5555");
socket.unbind("tcp://*:5555");
socket.disconnect("tcp://localhost:5555");

// Send operations
socket.send("Hello");                              // String
socket.send(byteArray);                            // byte[]
socket.send(data, SendFlags.SEND_MORE);           // With flags
boolean sent = socket.trySend(data);              // Non-blocking

// Receive operations
String str = socket.recvString();                  // String
byte[] data = socket.recvBytes();                  // byte[]
String str = socket.recvString(RecvFlags.DONTWAIT); // With flags
boolean received = socket.tryRecvString();         // Non-blocking

// Socket options
socket.setOption(SocketOption.LINGER, 0);
socket.setOption(SocketOption.RCVTIMEO, 5000);
int linger = socket.getOption(SocketOption.LINGER);

// Subscription (SUB socket)
socket.subscribe("topic");
socket.unsubscribe("topic");
```

### Message

```java
// Create and send message
try (Message msg = new Message("Hello World")) {
    socket.send(msg, SendFlags.NONE);
}

// Receive message
try (Message msg = socket.recvMessage()) {
    System.out.println(msg.toString());
    System.out.println("Size: " + msg.size());
    byte[] data = msg.data();
}

// Message properties
String routing = msg.get(MessageProperty.ROUTING_ID);
String userId = msg.get(MessageProperty.USER_ID);
```

### CURVE Security

```java
import io.github.ulalax.zmq.*;

// Generate keypair
Curve.KeyPair serverKeys = Curve.generateKeypair();
Curve.KeyPair clientKeys = Curve.generateKeypair();

// Server
try (Socket server = new Socket(ctx, SocketType.REP)) {
    server.setOption(SocketOption.CURVE_SERVER, 1);
    server.setOption(SocketOption.CURVE_SECRETKEY, serverKeys.secretKey());
    server.bind("tcp://*:5555");

    String request = server.recvString();
    server.send("Secure reply");
}

// Client
try (Socket client = new Socket(ctx, SocketType.REQ)) {
    client.setOption(SocketOption.CURVE_SERVERKEY, serverKeys.publicKey());
    client.setOption(SocketOption.CURVE_PUBLICKEY, clientKeys.publicKey());
    client.setOption(SocketOption.CURVE_SECRETKEY, clientKeys.secretKey());
    client.connect("tcp://localhost:5555");

    client.send("Secure request");
    String reply = client.recvString();
}
```

## Supported Platforms

Native libraries are bundled for the following platforms:

| OS | Architecture |
|----|--------------|
| Linux | x86_64, ARM64 (aarch64) |
| macOS | x86_64, ARM64 (Apple Silicon) |
| Windows | x86_64 |

Libraries are automatically extracted and loaded at runtime - no manual installation required.

## Requirements

- **JDK 21** or later
- No external native library installation required (bundled)

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
└── zmq/               # High-level API
    └── src/main/java/io/github/ulalax/zmq/
        ├── Context.java           # ZMQ context
        ├── Socket.java            # ZMQ socket
        ├── Message.java           # ZMQ message
        ├── MultipartMessage.java  # Multipart message utilities
        ├── Poller.java            # Polling utilities
        ├── PollItem.java          # Poll item
        ├── Curve.java             # CURVE security
        ├── Z85.java               # Z85 encoding
        └── Proxy.java             # Proxy utilities
```

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Related Projects

- [libzmq](https://github.com/zeromq/libzmq) - ZeroMQ core library
- [libzmq-native](https://github.com/ulala-x/libzmq-native) - Pre-built native libraries for multiple platforms
- [netzmq](https://github.com/ulala-x/netzmq) - .NET ZeroMQ bindings
