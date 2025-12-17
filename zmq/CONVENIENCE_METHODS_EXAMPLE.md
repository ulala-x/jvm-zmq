# cppzmq-style Convenience Methods

This document demonstrates the new convenience methods added to `Socket.java` that provide a more ergonomic API for multipart messaging, inspired by cppzmq.

## Overview

Five new convenience methods have been added to simplify multipart message sending:

1. **`sendMore(byte[] data)`** - Send a message frame with SEND_MORE flag
2. **`sendMore(String text)`** - Send a UTF-8 string with SEND_MORE flag
3. **`sendMore(Message msg)`** - Send a Message with SEND_MORE flag
4. **`trySendMore(byte[] data)`** - Non-blocking send with SEND_MORE flag
5. **`trySendMore(String text)`** - Non-blocking string send with SEND_MORE flag

## Before vs After

### Before (verbose)
```java
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.DEALER)) {

    socket.connect("tcp://localhost:5555");

    // Sending multipart message - verbose
    socket.send("header".getBytes(), SendFlags.SEND_MORE);
    socket.send("body1".getBytes(), SendFlags.SEND_MORE);
    socket.send("body2".getBytes(), SendFlags.NONE);
}
```

### After (clean)
```java
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.DEALER)) {

    socket.connect("tcp://localhost:5555");

    // Sending multipart message - clean and readable
    socket.sendMore("header");
    socket.sendMore("body1");
    socket.send("body2");
}
```

## Usage Examples

### Example 1: Basic Multipart Messaging

```java
try (Context ctx = new Context();
     Socket dealer = new Socket(ctx, SocketType.DEALER)) {

    dealer.connect("tcp://localhost:5555");

    // Send a 3-part message
    dealer.sendMore("identity");      // Part 1
    dealer.sendMore("request-type");  // Part 2
    dealer.send("payload");           // Part 3 (last)
}
```

### Example 2: Non-blocking Multipart Send

```java
try (Context ctx = new Context();
     Socket dealer = new Socket(ctx, SocketType.DEALER)) {

    dealer.connect("tcp://localhost:5555");

    // Non-blocking multipart send
    if (dealer.trySendMore("header")) {
        if (dealer.trySendMore("metadata")) {
            if (dealer.trySend("data", SendFlags.NONE)) {
                System.out.println("Complete message sent");
            } else {
                System.out.println("Failed on last part - handle partial send");
            }
        }
    } else {
        System.out.println("Would block - try again later");
    }
}
```

### Example 3: ROUTER Pattern (Identity + Payload)

```java
try (Context ctx = new Context();
     Socket router = new Socket(ctx, SocketType.ROUTER)) {

    router.bind("tcp://*:5555");

    // Receive request with identity
    byte[] identity = router.recvBytes();
    byte[] empty = router.recvBytes();
    byte[] request = router.recvBytes();

    // Process request...
    String response = processRequest(new String(request, UTF_8));

    // Send reply back with identity (simplified with sendMore)
    router.sendMore(identity);      // Identity frame
    router.sendMore(new byte[0]);   // Empty delimiter
    router.send(response);          // Response payload
}
```

### Example 4: PUB/SUB with Multipart Messages

```java
// Publisher
try (Context ctx = new Context();
     Socket pub = new Socket(ctx, SocketType.PUB)) {

    pub.bind("tcp://*:5556");

    while (running) {
        String topic = "stock.prices";
        String symbol = "AAPL";
        String price = "150.25";

        // Publish multipart message: [topic, symbol, price]
        pub.sendMore(topic);
        pub.sendMore(symbol);
        pub.send(price);

        Thread.sleep(1000);
    }
}
```

### Example 5: Message Object Variant

```java
try (Context ctx = new Context();
     Socket dealer = new Socket(ctx, SocketType.DEALER);
     Message header = new Message("AUTH".getBytes());
     Message token = new Message("secret-token".getBytes())) {

    dealer.connect("tcp://localhost:5555");

    // Send using Message objects
    dealer.sendMore(header);
    dealer.send(token, SendFlags.NONE);
}
```

## Performance Notes

- All convenience methods are **zero-cost abstractions** - they simply delegate to existing methods
- No additional allocations or overhead compared to manual flag specification
- `trySendMore()` variants are ideal for high-performance non-blocking scenarios
- Thread-safety: These methods maintain the same thread-safety guarantees as underlying methods (NOT thread-safe)

## API Consistency

These methods follow the established patterns in Socket.java:

| Pattern | Blocking | Non-blocking |
|---------|----------|--------------|
| Byte array | `sendMore(byte[])` | `trySendMore(byte[])` |
| String | `sendMore(String)` | `trySendMore(String)` |
| Message | `sendMore(Message)` | N/A |

## Error Handling

All methods maintain consistent error handling:

- **NullPointerException** - if data/text/msg is null
- **IllegalStateException** - if socket is closed
- **ZmqException** - if a ZMQ error occurs
- **trySendMore** returns `false` on EAGAIN (would block), throws on real errors

## See Also

- `send(byte[], SendFlags)` - Low-level send with explicit flags
- `trySend(byte[], SendFlags)` - Low-level non-blocking send
- `SendFlags.SEND_MORE` - Flag constant for multipart messaging
- `MultipartMessage` - Higher-level multipart message abstraction
