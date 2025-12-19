# Migration Guide: v1.x → v2.0

This guide provides comprehensive information for migrating from jvm-zmq v1.x to v2.0, which introduces a Result-based API following the cppzmq design pattern.

## Table of Contents

- [Overview](#overview)
- [Why the Change?](#why-the-change)
- [Breaking Changes Summary](#breaking-changes-summary)
- [API Changes](#api-changes)
  - [Send Operations](#send-operations)
  - [Receive Operations](#receive-operations)
  - [Multipart Messages](#multipart-messages)
- [Migration Patterns](#migration-patterns)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)
- [FAQ](#faq)

## Overview

Version 2.0 introduces a fundamental change in how send and receive operations are handled. Instead of returning values directly or using boolean `try*` methods, all operations now return `SendResult` and `RecvResult` objects that explicitly represent three possible states:

1. **Success** - Operation completed successfully, result contains value
2. **Would Block (EAGAIN)** - Non-blocking operation cannot complete immediately (empty result, not an error)
3. **Error** - Real ZMQ error occurred (throws `ZmqException`)

## Why the Change?

The Result-based API provides several advantages:

1. **Type Safety** - Eliminates null checks and boolean flags
2. **Explicit Error Handling** - Clear distinction between would-block and errors
3. **Functional Programming** - Support for map, ifPresent, orElse patterns
4. **Consistency** - Unified API for blocking and non-blocking operations
5. **cppzmq Alignment** - Follows the proven design pattern from the official C++ binding

## Breaking Changes Summary

### Removed Methods

All methods listed below have been removed. Use the new Result-based API instead.

| Removed Method | Replacement | Notes |
|----------------|-------------|-------|
| `int send(byte[] data, SendFlags)` | `SendResult send(byte[] data, SendFlags)` | Returns Result instead of int |
| `int send(String text, SendFlags)` | `SendResult send(String text, SendFlags)` | Returns Result instead of int |
| `int send(Message msg, SendFlags)` | `SendResult send(Message msg, SendFlags)` | Returns Result instead of int |
| `boolean trySend(byte[], SendFlags)` | `SendResult send(byte[], SendFlags)` | Check `.wouldBlock()` |
| `boolean trySend(String, SendFlags)` | `SendResult send(String, SendFlags)` | Check `.wouldBlock()` |
| `int recv(byte[], RecvFlags)` | `RecvResult<Integer> recv(byte[], RecvFlags)` | Returns Result<Integer> |
| `int recv(Message, RecvFlags)` | `RecvResult<Integer> recv(Message, RecvFlags)` | Returns Result<Integer> |
| `String recvString(RecvFlags)` | `RecvResult<String> recvString(RecvFlags)` | Returns Result<String> |
| `byte[] recvBytes(RecvFlags)` | `RecvResult<byte[]> recvBytes(RecvFlags)` | Returns Result<byte[]> |
| `int tryRecv(byte[], RecvFlags)` | `RecvResult<Integer> recv(byte[], RecvFlags)` | Check `.wouldBlock()` |
| `String tryRecvString(RecvFlags)` | `RecvResult<String> recvString(RecvFlags)` | Check `.wouldBlock()` |
| `byte[] tryRecvBytes(RecvFlags)` | `RecvResult<byte[]> recvBytes(RecvFlags)` | Check `.wouldBlock()` |
| `void sendMore(byte[] data)` | `send(data, SendFlags.SEND_MORE)` | Use explicit flag |
| `void sendMore(String text)` | `send(text, SendFlags.SEND_MORE)` | Use explicit flag |

### Changed Return Types

All send/recv methods now return `SendResult` or `RecvResult<T>` instead of primitive types or booleans.

## API Changes

### Send Operations

#### Blocking Send

**Old API:**
```java
// Returned number of bytes sent
int bytesSent = socket.send(data);
int bytesSent = socket.send("Hello");
int bytesSent = socket.send(message, SendFlags.NONE);
```

**New API:**
```java
// Returns SendResult - extract value with .value()
SendResult result = socket.send(data);
int bytesSent = result.value();

// Or use functional style
socket.send("Hello").ifPresent(bytes ->
    System.out.println("Sent " + bytes + " bytes")
);

// Message send
socket.send(message, SendFlags.NONE)
    .ifPresent(bytes -> log.info("Sent {} bytes", bytes));
```

#### Non-blocking Send

**Old API:**
```java
// Returned boolean indicating success
boolean sent = socket.trySend(data, SendFlags.DONT_WAIT);
if (!sent) {
    // Would block - retry later
    retryQueue.add(data);
}

boolean sent = socket.trySend("Hello", SendFlags.DONT_WAIT);
```

**New API:**
```java
// Returns SendResult - check wouldBlock()
SendResult result = socket.send(data, SendFlags.DONT_WAIT);
if (result.wouldBlock()) {
    // Would block - retry later
    retryQueue.add(data);
} else {
    System.out.println("Sent " + result.value() + " bytes");
}

// Functional style
socket.send("Hello", SendFlags.DONT_WAIT)
    .ifPresent(bytes -> metrics.recordSend(bytes));
```

#### Multipart Send (SEND_MORE)

**Old API:**
```java
// Using sendMore() convenience methods
socket.sendMore("header");
socket.sendMore("body");
socket.send("footer");  // Last frame
```

**New API:**
```java
// Use explicit SEND_MORE flag
socket.send("header", SendFlags.SEND_MORE);
socket.send("body", SendFlags.SEND_MORE);
socket.send("footer");  // Last frame, no flag

// Or use MultipartMessage utility
MultipartMessage msg = MultipartMessage.builder()
    .add("header")
    .add("body")
    .add("footer")
    .build();
socket.sendMultipart(msg);
```

### Receive Operations

#### Blocking Receive

**Old API:**
```java
// Returned value directly
String message = socket.recvString();
byte[] data = socket.recvBytes();
int bytesRead = socket.recv(buffer);
int bytesRead = socket.recv(message);
```

**New API:**
```java
// Returns RecvResult - extract value
RecvResult<String> result = socket.recvString();
String message = result.value();

// Functional style (recommended)
socket.recvString().ifPresent(msg ->
    processMessage(msg)
);

// Receive into buffer
socket.recv(buffer).ifPresent(bytesRead ->
    processBuffer(buffer, bytesRead)
);

// Receive into Message
socket.recv(message).ifPresent(bytesRead ->
    processMessage(message)
);
```

#### Non-blocking Receive

**Old API:**
```java
// Returned null or -1 on would-block
String msg = socket.tryRecvString(RecvFlags.DONT_WAIT);
if (msg == null) {
    // Would block
    return;
}
processMessage(msg);

byte[] data = socket.tryRecvBytes(RecvFlags.DONT_WAIT);
if (data == null) {
    // Would block
}

int bytes = socket.tryRecv(buffer, RecvFlags.DONT_WAIT);
if (bytes == -1) {
    // Would block
}
```

**New API:**
```java
// Returns RecvResult - check wouldBlock()
RecvResult<String> result = socket.recvString(RecvFlags.DONT_WAIT);
if (result.wouldBlock()) {
    // Would block
    return;
}
processMessage(result.value());

// Functional style
socket.recvString(RecvFlags.DONT_WAIT)
    .ifPresent(msg -> processMessage(msg));

// Receive bytes
RecvResult<byte[]> dataResult = socket.recvBytes(RecvFlags.DONT_WAIT);
if (dataResult.isPresent()) {
    processData(dataResult.value());
} else {
    // Would block - do other work
}

// Receive into buffer
socket.recv(buffer, RecvFlags.DONT_WAIT)
    .ifPresent(bytes -> processBuffer(buffer, bytes));
```

#### Transforming Received Data

The new `RecvResult` supports functional transformations with `map()`:

**Old API:**
```java
byte[] data = socket.recvBytes();
int length = data.length;
String hex = bytesToHex(data);
```

**New API:**
```java
// Transform with map()
int length = socket.recvBytes()
    .map(bytes -> bytes.length)
    .orElse(0);

String hex = socket.recvBytes()
    .map(this::bytesToHex)
    .orElse("");

// Chain multiple transformations
socket.recvString()
    .map(String::trim)
    .map(String::toUpperCase)
    .ifPresent(this::processCleanedMessage);
```

### Multipart Messages

**Old API:**
```java
// Manual multipart receive
List<byte[]> parts = new ArrayList<>();
do {
    byte[] frame = socket.recvBytes();
    parts.add(frame);
} while (socket.hasMore());
```

**New API:**
```java
// Using Result API
List<byte[]> parts = new ArrayList<>();
do {
    socket.recvBytes().ifPresent(parts::add);
} while (socket.hasMore());

// Or use MultipartMessage utility (recommended)
socket.recvMultipart().ifPresent(msg -> {
    for (byte[] frame : msg) {
        processFrame(frame);
    }
});
```

## Migration Patterns

### Pattern 1: Simple Blocking Request-Reply

**Old Code:**
```java
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.REP)) {
    socket.bind("tcp://*:5555");

    while (true) {
        String request = socket.recvString();
        System.out.println("Received: " + request);
        socket.send("World");
    }
}
```

**New Code:**
```java
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.REP)) {
    socket.bind("tcp://*:5555");

    while (true) {
        socket.recvString().ifPresent(request -> {
            System.out.println("Received: " + request);
            socket.send("World");
        });
    }
}
```

### Pattern 2: Non-blocking Event Loop

**Old Code:**
```java
while (running) {
    String msg = socket.tryRecvString(RecvFlags.DONT_WAIT);
    if (msg != null) {
        processMessage(msg);
    }

    if (!sendQueue.isEmpty()) {
        byte[] data = sendQueue.peek();
        boolean sent = socket.trySend(data, SendFlags.DONT_WAIT);
        if (sent) {
            sendQueue.poll();
        }
    }

    Thread.sleep(1);
}
```

**New Code:**
```java
while (running) {
    socket.recvString(RecvFlags.DONT_WAIT)
        .ifPresent(msg -> processMessage(msg));

    if (!sendQueue.isEmpty()) {
        byte[] data = sendQueue.peek();
        SendResult result = socket.send(data, SendFlags.DONT_WAIT);
        if (result.isPresent()) {
            sendQueue.poll();
        }
    }

    Thread.sleep(1);
}
```

### Pattern 3: Poller-based Server

**Old Code:**
```java
try (Poller poller = new Poller()) {
    int idx = poller.register(socket, PollEvents.IN);

    while (running) {
        if (poller.poll(1000) > 0) {
            if (poller.isReadable(idx)) {
                byte[] data = socket.recvBytes();
                processData(data);
            }
        }
    }
}
```

**New Code:**
```java
try (Poller poller = new Poller()) {
    int idx = poller.register(socket, PollEvents.IN);

    while (running) {
        if (poller.poll(1000) > 0) {
            if (poller.isReadable(idx)) {
                socket.recvBytes().ifPresent(data ->
                    processData(data)
                );
            }
        }
    }
}
```

### Pattern 4: Error Handling

**Old Code:**
```java
try {
    String msg = socket.recvString();
    processMessage(msg);
} catch (ZmqException e) {
    if (e.getErrorCode() == ZmqConstants.EAGAIN) {
        // Would block
        handleWouldBlock();
    } else {
        // Real error
        handleError(e);
    }
}
```

**New Code:**
```java
try {
    RecvResult<String> result = socket.recvString(RecvFlags.DONT_WAIT);
    if (result.wouldBlock()) {
        // Would block - not an error
        handleWouldBlock();
    } else {
        processMessage(result.value());
    }
} catch (ZmqException e) {
    // Real error - EAGAIN is handled by Result
    handleError(e);
}
```

### Pattern 5: Publisher-Subscriber

**Old Code:**
```java
// Publisher
try (Socket pub = new Socket(ctx, SocketType.PUB)) {
    pub.bind("tcp://*:5556");
    while (running) {
        String msg = generateMessage();
        pub.send("topic " + msg);
    }
}

// Subscriber
try (Socket sub = new Socket(ctx, SocketType.SUB)) {
    sub.connect("tcp://localhost:5556");
    sub.subscribe("topic");
    while (running) {
        String msg = sub.recvString();
        processMessage(msg);
    }
}
```

**New Code:**
```java
// Publisher - no changes to send logic for simple cases
try (Socket pub = new Socket(ctx, SocketType.PUB)) {
    pub.bind("tcp://*:5556");
    while (running) {
        String msg = generateMessage();
        pub.send("topic " + msg);
    }
}

// Subscriber - use Result API
try (Socket sub = new Socket(ctx, SocketType.SUB)) {
    sub.connect("tcp://localhost:5556");
    sub.subscribe("topic");
    while (running) {
        sub.recvString().ifPresent(msg ->
            processMessage(msg)
        );
    }
}
```

### Pattern 6: Router-Dealer

**Old Code:**
```java
try (Socket router = new Socket(ctx, SocketType.ROUTER)) {
    router.bind("tcp://*:5555");

    // Receive identity and message
    byte[] identity = router.recvBytes();
    byte[] empty = router.recvBytes();
    byte[] data = router.recvBytes();

    // Send reply
    router.sendMore(identity);
    router.sendMore(new byte[0]);
    router.send(processRequest(data));
}
```

**New Code:**
```java
try (Socket router = new Socket(ctx, SocketType.ROUTER)) {
    router.bind("tcp://*:5555");

    // Receive identity and message using Result API
    RecvResult<byte[]> identityResult = router.recvBytes();
    RecvResult<byte[]> emptyResult = router.recvBytes();
    RecvResult<byte[]> dataResult = router.recvBytes();

    if (identityResult.isPresent() && dataResult.isPresent()) {
        byte[] identity = identityResult.value();
        byte[] data = dataResult.value();

        // Send reply using explicit flags
        router.send(identity, SendFlags.SEND_MORE);
        router.send(new byte[0], SendFlags.SEND_MORE);
        router.send(processRequest(data));
    }
}
```

## Best Practices

### 1. Use Functional Style for Simple Cases

**Good:**
```java
socket.recvString().ifPresent(msg -> processMessage(msg));
```

**Avoid:**
```java
RecvResult<String> result = socket.recvString();
if (result.isPresent()) {
    String msg = result.value();
    processMessage(msg);
}
```

### 2. Check wouldBlock() Explicitly for Non-blocking I/O

**Good:**
```java
SendResult result = socket.send(data, SendFlags.DONT_WAIT);
if (result.wouldBlock()) {
    retryQueue.add(data);
} else {
    metrics.recordSend(result.value());
}
```

**Avoid:**
```java
SendResult result = socket.send(data, SendFlags.DONT_WAIT);
if (!result.isPresent()) {
    retryQueue.add(data);
}
```

### 3. Use map() for Transformations

**Good:**
```java
int length = socket.recvBytes()
    .map(bytes -> bytes.length)
    .orElse(0);
```

**Avoid:**
```java
RecvResult<byte[]> result = socket.recvBytes();
int length = 0;
if (result.isPresent()) {
    length = result.value().length;
}
```

### 4. Use orElse() for Default Values

**Good:**
```java
String msg = socket.recvString(RecvFlags.DONT_WAIT)
    .orElse("default message");
```

**Avoid:**
```java
RecvResult<String> result = socket.recvString(RecvFlags.DONT_WAIT);
String msg;
if (result.isPresent()) {
    msg = result.value();
} else {
    msg = "default message";
}
```

### 5. Handle Multipart with Utilities

**Good:**
```java
MultipartMessage msg = MultipartMessage.builder()
    .add("part1")
    .add("part2")
    .build();
socket.sendMultipart(msg);

socket.recvMultipart().ifPresent(received ->
    received.forEach(this::processFrame)
);
```

**Avoid:**
```java
socket.send("part1", SendFlags.SEND_MORE);
socket.send("part2");

List<byte[]> parts = new ArrayList<>();
do {
    socket.recvBytes().ifPresent(parts::add);
} while (socket.hasMore());
```

## Troubleshooting

### Issue: Compilation Error "cannot find symbol: method send(byte[])"

**Cause:** The method signature changed to return `SendResult`.

**Solution:** Update variable declaration to `SendResult`:
```java
// Before
int bytes = socket.send(data);

// After
SendResult result = socket.send(data);
int bytes = result.value();
```

### Issue: NullPointerException after migration

**Cause:** Trying to use `.value()` without checking `.isPresent()` first.

**Solution:** Always check before calling `.value()` or use functional style:
```java
// Safe approach 1
RecvResult<String> result = socket.recvString();
if (result.isPresent()) {
    String msg = result.value();
    processMessage(msg);
}

// Safe approach 2
socket.recvString().ifPresent(msg -> processMessage(msg));
```

### Issue: Code checking for null on non-blocking receive

**Cause:** Old API returned null on would-block, new API never returns null.

**Solution:** Use `.wouldBlock()` or `.isPresent()`:
```java
// Before
String msg = socket.tryRecvString(RecvFlags.DONT_WAIT);
if (msg == null) {
    // Would block
}

// After
RecvResult<String> result = socket.recvString(RecvFlags.DONT_WAIT);
if (result.wouldBlock()) {
    // Would block
}
```

### Issue: Performance degradation after migration

**Cause:** Creating unnecessary intermediate Result objects.

**Solution:** Use functional style to avoid temporary variables:
```java
// Less efficient
RecvResult<byte[]> result = socket.recvBytes();
if (result.isPresent()) {
    byte[] data = result.value();
    int length = data.length;
    process(data, length);
}

// More efficient
socket.recvBytes().ifPresent(data -> process(data, data.length));
```

### Issue: NoSuchElementException when calling .value()

**Cause:** Calling `.value()` on an empty Result (would-block case).

**Solution:** Always check `.isPresent()` first or use `.orElse()`:
```java
// Before (throws exception)
RecvResult<String> result = socket.recvString(RecvFlags.DONT_WAIT);
String msg = result.value();  // Throws if would block

// After (safe)
String msg = result.orElse("default");

// Or
if (result.isPresent()) {
    String msg = result.value();
}
```

## FAQ

### Q: Do I need to migrate immediately?

**A:** Yes. Version 2.0 removes the old API completely. However, the migration is straightforward with clear patterns.

### Q: Will this break my existing code?

**A:** Yes, this is a breaking change. All send/recv methods have new signatures. Follow the migration patterns in this guide.

### Q: What are the performance implications?

**A:** The Result API has negligible overhead. The JVM optimizes these patterns well, especially when using functional style.

### Q: Can I gradually migrate?

**A:** No, you must migrate all send/recv operations at once since the old API is removed. However, you can migrate module by module in a multi-module project.

### Q: Why not use Optional<T>?

**A:** `Optional` doesn't clearly distinguish between would-block and actual missing data. `Result` types are more explicit and provide better semantics for ZeroMQ operations.

### Q: Do I need to change my Context and Socket creation code?

**A:** No, `Context` and `Socket` creation remains unchanged. Only send/recv operations are affected.

### Q: What about socket options and configuration?

**A:** Socket options (`setOption`, `getOption`) are unchanged.

### Q: Are there any changes to Poller?

**A:** No changes to Poller API. Only the recv operations inside poll handlers need updating.

### Q: How do I handle existing try-catch blocks?

**A:** Real errors still throw `ZmqException`. The Result API only affects EAGAIN (would-block), which is no longer an exception.

### Q: Can I use the old boolean pattern?

**A:** No, but you can achieve similar semantics:
```java
// Old
if (socket.trySend(data, SendFlags.DONT_WAIT)) {
    // Success
}

// New
if (socket.send(data, SendFlags.DONT_WAIT).isPresent()) {
    // Success
}
```

### Q: Is there a automated migration tool?

**A:** Not currently. The migration is straightforward enough to do manually with IDE refactoring support. Use find-replace for common patterns.

### Q: Where can I get help?

**A:**
- Check the [API Documentation](https://ulala-x.github.io/jvm-zmq/)
- Review code examples in the [zmq-samples](zmq-samples/) module
- Open an issue on [GitHub](https://github.com/ulala-x/jvm-zmq/issues)

## Summary

The Result-based API provides a cleaner, type-safe approach to ZeroMQ operations. While migration requires updating all send/recv operations, the patterns are consistent and the benefits are significant:

- **Type Safety** - No null checks or magic return values
- **Explicit Semantics** - Clear distinction between success, would-block, and errors
- **Functional Style** - Modern Java patterns with map, ifPresent, orElse
- **Better Error Handling** - EAGAIN is not an exception anymore
- **cppzmq Alignment** - Consistent with the official C++ binding

The migration effort is offset by improved code quality and maintainability. Follow the patterns in this guide, and you'll have cleaner, more robust ZeroMQ applications.
