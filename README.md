# jvm-zmq

Modern Java bindings for ZeroMQ using JDK 21+ FFM (Foreign Function & Memory) API.

## Features

- **Pure Java FFM** - No JNI, direct native library binding using Java 21+ FFM API
- **Type-safe API** - Strongly typed enums, options, and message handling
- **Resource-safe** - AutoCloseable resources with Cleaner-based finalization
- **Cross-platform** - Windows, Linux, macOS support (x86_64 and aarch64)
- **Complete ZMQ support** - All socket types, patterns, and features including CURVE security

## Requirements

- **JDK 21** or later
- **Gradle 8.5** or later (wrapper included)

## Quick Start

### 1. Add Dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.ulalax:zmq:1.0.0-SNAPSHOT")
}
```

### 2. Enable Native Access

```kotlin
// build.gradle.kts
tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
```

### 3. Example: REQ-REP Pattern

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

## Socket Types

| Type | Description |
|------|-------------|
| `REQ` | Request socket for client-side REQ-REP |
| `REP` | Reply socket for server-side REQ-REP |
| `PUB` | Publish socket for PUB-SUB |
| `SUB` | Subscribe socket for PUB-SUB |
| `PUSH` | Push socket for PUSH-PULL pipeline |
| `PULL` | Pull socket for PUSH-PULL pipeline |
| `DEALER` | Async request socket |
| `ROUTER` | Async reply socket with routing |
| `PAIR` | Exclusive pair for inter-thread |
| `XPUB` | Extended publish with subscription backflow |
| `XSUB` | Extended subscribe |
| `STREAM` | Raw TCP socket |

## Examples

### PUB-SUB Pattern

```java
try (Context ctx = new Context();
     Socket pub = new Socket(ctx, SocketType.PUB);
     Socket sub = new Socket(ctx, SocketType.SUB)) {

    pub.bind("tcp://*:5556");

    // Subscribe to all messages
    sub.subscribe("");
    sub.connect("tcp://localhost:5556");

    // Give time for subscription to propagate
    Thread.sleep(100);

    // Publish message
    pub.send("Hello Subscribers!");

    // Receive message
    String message = sub.recvString();
    System.out.println("Received: " + message);
}
```

### Topic Filtering

```java
// Subscribe to specific topic
sub.subscribe("news.");

// Only receives messages starting with "news."
pub.send("news.sports Breaking news!");  // Received
pub.send("weather.today Sunny");          // Filtered out
```

### PUSH-PULL Pipeline

```java
try (Context ctx = new Context();
     Socket push = new Socket(ctx, SocketType.PUSH);
     Socket pull = new Socket(ctx, SocketType.PULL)) {

    push.bind("tcp://*:5557");
    pull.connect("tcp://localhost:5557");

    // Push work items
    for (int i = 0; i < 10; i++) {
        push.send("Work item " + i);
    }

    // Pull and process
    for (int i = 0; i < 10; i++) {
        String work = pull.recvString();
        System.out.println("Processing: " + work);
    }
}
```

### Socket Options

```java
Socket socket = new Socket(ctx, SocketType.REQ);

// Set timeout
socket.setOption(SocketOption.RCVTIMEO, 5000);  // 5 second receive timeout
socket.setOption(SocketOption.SNDTIMEO, 5000);  // 5 second send timeout

// Set linger (wait time for pending messages on close)
socket.setOption(SocketOption.LINGER, 0);       // Don't wait

// High water mark (message queue size)
socket.setOption(SocketOption.SNDHWM, 1000);    // Send buffer
socket.setOption(SocketOption.RCVHWM, 1000);    // Receive buffer
```

### Polling Multiple Sockets

```java
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
│       ├── Context.java       # ZMQ context
│       ├── Socket.java        # ZMQ socket
│       ├── Message.java       # ZMQ message
│       ├── Poller.java        # Polling utilities
│       ├── Curve.java         # CURVE security
│       ├── Z85.java           # Z85 encoding
│       └── Proxy.java         # Proxy utilities
│
└── plan/              # Architecture documentation
```

## Native Libraries

Native libzmq libraries are bundled for:
- `linux/x86_64`
- `linux/aarch64`
- `windows/x86_64`
- `macos/x86_64`
- `macos/aarch64`

Libraries are automatically extracted and loaded at runtime.

## License

MIT License - see [LICENSE](LICENSE) file.

## Related Projects

- [libzmq](https://github.com/zeromq/libzmq) - ZeroMQ core library
- [libzmq-native](https://github.com/ulala-x/libzmq-native) - Pre-built native libraries
- [netzmq](https://github.com/ulala-x/netzmq) - C# ZeroMQ bindings (original reference)
