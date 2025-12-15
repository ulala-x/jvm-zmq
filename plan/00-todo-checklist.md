# jvm-zmq Development Checklist

This checklist provides a complete step-by-step guide for porting netzmq (C#) to jvm-zmq (Java FFM). Each task is detailed enough to be executed independently, even after context reset.

## Legend
- [ ] Not Started
- [P] In Progress
- [X] Completed

---

## Phase 1: Project Setup

### 1.1 Gradle Multi-Module Setup
- [ ] **Task**: Create root project structure
  - **Action**: Create `/settings.gradle.kts` with module definitions
  - **Details**: Include modules: `zmq-core`, `zmq`
  - **Reference**: `01-project-setup.md` Section 1
  - **Files to create**:
    - `/settings.gradle.kts`
    - `/build.gradle.kts` (root)
    - `/gradle.properties`

- [ ] **Task**: Configure zmq-core module
  - **Action**: Create `zmq-core/build.gradle.kts`
  - **Details**: Native bindings layer (no external dependencies except JDK)
  - **Package**: `io.github.ulalax.zmq.core`
  - **Reference**: `01-project-setup.md` Section 2

- [ ] **Task**: Configure zmq module
  - **Action**: Create `zmq/build.gradle.kts`
  - **Details**: High-level API (depends on zmq-core)
  - **Package**: `io.github.ulalax.zmq`
  - **Reference**: `01-project-setup.md` Section 3

### 1.2 Native Library Bundling
- [ ] **Task**: Download platform-specific libzmq binaries
  - **Action**: Download from https://github.com/ulala-x/libzmq-native/releases
  - **Platforms**: Windows (x64), Linux (x64, arm64), macOS (x64, arm64)
  - **Target**: `zmq-core/src/main/resources/native/{os}/{arch}/`
  - **Files**:
    - Windows: `libzmq.dll`
    - Linux: `libzmq.so`
    - macOS: `libzmq.dylib`

- [ ] **Task**: Implement NativeLoader.java
  - **Action**: Create resource extraction and loading logic
  - **Location**: `zmq-core/src/main/java/io/github/ulalax/zmq/core/NativeLoader.java`
  - **Reference**: `02-ffm-bindings.md` Section 5
  - **Key features**:
    - Detect OS and architecture
    - Extract from JAR to temp directory
    - Load with System.load()
    - Handle cleanup

---

## Phase 2: FFM Core Bindings (zmq-core module)

### 2.1 Constants and Enums
- [ ] **Task**: Create ZmqConstants.java
  - **Action**: Port all constants from `ZmqConstants.cs`
  - **Location**: `zmq-core/src/main/java/io/github/ulalax/zmq/core/ZmqConstants.java`
  - **Reference**: `02-ffm-bindings.md` Section 2
  - **Categories**:
    - Socket types (ZMQ_PAIR, ZMQ_PUB, etc.)
    - Socket options (ZMQ_AFFINITY, ZMQ_SUBSCRIBE, etc.)
    - Context options (ZMQ_IO_THREADS, etc.)
    - Send/recv flags (ZMQ_DONTWAIT, ZMQ_SNDMORE)
    - Poll events (ZMQ_POLLIN, ZMQ_POLLOUT, etc.)
    - Error codes (EAGAIN, ETERM, etc.)
    - Monitor events
    - Security mechanisms

### 2.2 Structures
- [ ] **Task**: Create ZmqMsg MemoryLayout
  - **Action**: Define 64-byte zmq_msg_t structure using FFM
  - **Location**: `zmq-core/src/main/java/io/github/ulalax/zmq/core/ZmqStructs.java`
  - **Reference**: `02-ffm-bindings.md` Section 3
  - **Layout**: 64 bytes aligned on 8-byte boundary
  - **Pattern**: Use `MemoryLayout.structLayout()` with 8 longs

- [ ] **Task**: Create ZmqPollItem MemoryLayout
  - **Action**: Define zmq_pollitem_t for Windows and Unix
  - **Location**: Same file as above
  - **Reference**: `02-ffm-bindings.md` Section 3
  - **Platform differences**:
    - Windows: socket (ptr), fd (long), events (short), revents (short)
    - Unix: socket (ptr), fd (int), events (short), revents (short)
  - **Pattern**: Use `MemoryLayout.structLayout()` with platform detection

### 2.3 LibZmq Function Bindings
- [ ] **Task**: Create LibZmq.java interface
  - **Action**: Port all P/Invoke declarations from `LibZmq.cs`
  - **Location**: `zmq-core/src/main/java/io/github/ulalax/zmq/core/LibZmq.java`
  - **Reference**: `02-ffm-bindings.md` Section 1
  - **Pattern**: Use `Linker.nativeLinker().downcallHandle()` for each function

- [ ] **Task**: Implement error handling functions
  - **Functions**: `zmq_errno()`, `zmq_strerror()`
  - **C# Reference**: Lines 17-34 of LibZmq.cs
  - **Java Pattern**:
    ```java
    MethodHandle zmq_errno = linker.downcallHandle(
        linker.defaultLookup().find("zmq_errno").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT)
    );
    ```

- [ ] **Task**: Implement version function
  - **Function**: `zmq_version()`
  - **C# Reference**: Lines 41-42 of LibZmq.cs
  - **Output**: Three integers (major, minor, patch)

- [ ] **Task**: Implement context management functions
  - **Functions**:
    - `zmq_ctx_new()` - Create context
    - `zmq_ctx_term()` - Terminate context
    - `zmq_ctx_shutdown()` - Shutdown context
    - `zmq_ctx_set()` - Set context option
    - `zmq_ctx_get()` - Get context option
  - **C# Reference**: Lines 49-74 of LibZmq.cs

- [ ] **Task**: Implement socket management functions
  - **Functions**:
    - `zmq_socket()` - Create socket
    - `zmq_close()` - Close socket
    - `zmq_bind()` - Bind to endpoint
    - `zmq_connect()` - Connect to endpoint
    - `zmq_unbind()` - Unbind from endpoint
    - `zmq_disconnect()` - Disconnect from endpoint
    - `zmq_setsockopt()` - Set socket option
    - `zmq_getsockopt()` - Get socket option
    - `zmq_send()` - Send data
    - `zmq_recv()` - Receive data
    - `zmq_socket_monitor()` - Monitor socket events
  - **C# Reference**: Lines 81-142 of LibZmq.cs

- [ ] **Task**: Implement message functions
  - **Functions**:
    - `zmq_msg_init()` - Initialize empty message
    - `zmq_msg_init_size()` - Initialize with size
    - `zmq_msg_init_data()` - Initialize with data
    - `zmq_msg_send()` - Send message
    - `zmq_msg_recv()` - Receive message
    - `zmq_msg_close()` - Close message
    - `zmq_msg_move()` - Move message content
    - `zmq_msg_copy()` - Copy message content
    - `zmq_msg_data()` - Get message data pointer
    - `zmq_msg_size()` - Get message size
    - `zmq_msg_more()` - Check for more parts
    - `zmq_msg_get()` - Get message property
    - `zmq_msg_set()` - Set message property
    - `zmq_msg_gets()` - Get message metadata
  - **C# Reference**: Lines 149-336 of LibZmq.cs
  - **Note**: Implement both ref-based and pointer-based versions

- [ ] **Task**: Implement polling functions
  - **Functions**:
    - `zmq_poll()` for Windows (with long fd)
    - `zmq_poll()` for Unix (with int fd)
  - **C# Reference**: Lines 343-421 of LibZmq.cs
  - **Platform detection**: Use `System.getProperty("os.name")`
  - **Pattern**: Create wrapper that handles platform differences

- [ ] **Task**: Implement utility functions
  - **Functions**:
    - `zmq_proxy()` - Start proxy
    - `zmq_proxy_steerable()` - Start steerable proxy
    - `zmq_has()` - Check capability
    - `zmq_z85_encode()` - Z85 encode
    - `zmq_z85_decode()` - Z85 decode
    - `zmq_curve_keypair()` - Generate CURVE keypair
    - `zmq_curve_public()` - Derive public key
  - **C# Reference**: Lines 428-467 of LibZmq.cs

### 2.4 Exception Handling
- [ ] **Task**: Create ZmqException.java
  - **Action**: Port exception class from C#
  - **Location**: `zmq-core/src/main/java/io/github/ulalax/zmq/core/ZmqException.java`
  - **Reference**: `02-ffm-bindings.md` Section 6
  - **C# Reference**: ZmqException.cs (Core)
  - **Features**:
    - Store error number
    - Get error message from `zmq_strerror()`
    - Static helpers: `throwIfError()`, `throwIfNull()`

---

## Phase 3: High-Level API (zmq module)

### 3.1 Context Class
- [ ] **Task**: Create Context.java
  - **Action**: Port from Context.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Context.java`
  - **Reference**: `03-high-level-api.md` Section 1
  - **C# Reference**: Context.cs (lines 1-119)
  - **Features**:
    - Constructor (default and with options)
    - `getOption()` / `setOption()`
    - `shutdown()`
    - Static `has()` method
    - Static `version()` method
    - Implement `AutoCloseable`
    - Use `Cleaner` for finalization

### 3.2 Socket Class
- [ ] **Task**: Create Socket.java
  - **Action**: Port from Socket.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Socket.java`
  - **Reference**: `03-high-level-api.md` Section 2
  - **C# Reference**: Socket.cs (lines 1-785)
  - **Sub-tasks**:
    - [ ] Constructor and handle management
    - [ ] Bind/Connect/Unbind/Disconnect methods
    - [ ] Send methods (byte[], ByteBuffer, String)
    - [ ] TrySend methods (non-blocking variants)
    - [ ] Recv methods (byte[], ByteBuffer)
    - [ ] TryRecv methods (non-blocking variants)
    - [ ] RecvString methods
    - [ ] Socket options (get/set)
    - [ ] Subscribe/Unsubscribe methods
    - [ ] Monitor method
    - [ ] `hasMore()` property
    - [ ] Implement `AutoCloseable`

### 3.3 Message Class
- [ ] **Task**: Create Message.java
  - **Action**: Port from Message.cs with Arena management
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Message.java`
  - **Reference**: `03-high-level-api.md` Section 3
  - **C# Reference**: Message.cs (lines 1-384)
  - **FFM Pattern**: Use `Arena` for memory management
  - **Sub-tasks**:
    - [ ] Constructor variations (empty, size, data, external)
    - [ ] Arena management (allocate 64 bytes for zmq_msg_t)
    - [ ] `data()` method returning MemorySegment
    - [ ] `size()` method
    - [ ] `more()` property
    - [ ] `getProperty()` / `setProperty()`
    - [ ] `getMetadata()`
    - [ ] `toByteArray()` / `toString()`
    - [ ] `rebuild()` methods
    - [ ] `move()` / `copy()` methods
    - [ ] Internal `send()` / `recv()` methods
    - [ ] Implement `AutoCloseable`
    - [ ] Cleaner-based finalization

- [ ] **Task**: Handle free callback for external data
  - **Action**: Implement upcall stub for zmq_free_fn
  - **Reference**: Message.cs lines 76-152
  - **FFM Pattern**: Use `Linker.upcallStub()` with Arena
  - **Signature**: `void free_fn(void* data, void* hint)`

### 3.4 Poller Class
- [ ] **Task**: Create Poller.java
  - **Action**: Port static polling functions
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Poller.java`
  - **Reference**: `03-high-level-api.md` Section 4
  - **C# Reference**: Poller.cs (lines 1-114)
  - **Features**:
    - Static `poll()` method with PollItem array
    - Static `poll()` with timeout variations
    - Single-socket polling optimization
    - ArrayPool equivalent using thread-local arrays

- [ ] **Task**: Create PollItem class
  - **Action**: Port PollItem struct
  - **Location**: Same file or separate
  - **Reference**: `03-high-level-api.md` Section 4
  - **C# Reference**: Poller.cs lines 9-35
  - **Fields**:
    - `socket` (Socket reference)
    - `fileDescriptor` (long/int)
    - `events` (PollEvents)
    - `returnedEvents` (PollEvents)
  - **Properties**: `isReadable()`, `isWritable()`, `hasError()`

### 3.5 Enum Classes
- [ ] **Task**: Create SocketType.java
  - **Action**: Port enum from SocketType.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/SocketType.java`
  - **C# Reference**: SocketType.cs
  - **Values**: PAIR, PUB, SUB, REQ, REP, DEALER, ROUTER, PULL, PUSH, XPUB, XSUB, STREAM

- [ ] **Task**: Create SendFlags.java
  - **Action**: Port enum from SendFlags.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/SendFlags.java`
  - **C# Reference**: SendFlags.cs
  - **Values**: NONE, DONT_WAIT, SEND_MORE
  - **Note**: Java doesn't have [Flags] attribute - use bitwise operations

- [ ] **Task**: Create RecvFlags.java
  - **Action**: Port enum from RecvFlags.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/RecvFlags.java`
  - **C# Reference**: RecvFlags.cs
  - **Values**: NONE, DONT_WAIT

- [ ] **Task**: Create PollEvents.java
  - **Action**: Port enum from PollEvents.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/PollEvents.java`
  - **C# Reference**: PollEvents.cs
  - **Values**: NONE, IN, OUT, ERR, PRI

- [ ] **Task**: Create ContextOption.java
  - **Action**: Port enum from ContextOption.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/ContextOption.java`
  - **C# Reference**: ContextOption.cs
  - **Values**: All context option constants

- [ ] **Task**: Create SocketOption.java
  - **Action**: Port enum from SocketOption.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/SocketOption.java`
  - **C# Reference**: SocketOption.cs
  - **Values**: All socket option constants (80+ options)

- [ ] **Task**: Create MessageProperty.java
  - **Action**: Port enum from MessageProperty.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/MessageProperty.java`
  - **C# Reference**: MessageProperty.cs
  - **Values**: MORE, SHARED

- [ ] **Task**: Create SocketMonitorEvent.java
  - **Action**: Port enum from SocketMonitorEvent.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/SocketMonitorEvent.java`
  - **C# Reference**: SocketMonitorEvent.cs
  - **Values**: All monitor event flags

### 3.6 Utility Classes
- [ ] **Task**: Create Curve.java
  - **Action**: Port CURVE key utilities
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Curve.java`
  - **Reference**: `04-utilities.md` Section 1
  - **C# Reference**: Curve.cs
  - **Methods**:
    - `generateKeypair()` returns KeyPair record
    - `derivePublicKey(String secretKey)` returns String

- [ ] **Task**: Create Z85.java
  - **Action**: Port Z85 encoding/decoding
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Z85.java`
  - **Reference**: `04-utilities.md` Section 2
  - **C# Reference**: Z85.cs
  - **Methods**:
    - `encode(byte[] data)` returns String
    - `decode(String encoded)` returns byte[]

- [ ] **Task**: Create Proxy.java
  - **Action**: Port proxy utilities
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Proxy.java`
  - **Reference**: `04-utilities.md` Section 3
  - **C# Reference**: Proxy.cs
  - **Methods**:
    - `start(Socket frontend, Socket backend, Socket capture)`
    - `startSteerable(Socket frontend, Socket backend, Socket control, Socket capture)`

### 3.7 Advanced Features
- [ ] **Task**: Create MultipartMessage.java (Optional)
  - **Action**: Implement multipart message helper
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/MultipartMessage.java`
  - **Reference**: `03-high-level-api.md` Section 5
  - **Features**:
    - List-based message frame storage
    - `add()`, `addString()`
    - `send()`, `recv()`
    - Iterator support

---

## Phase 4: Testing

### 4.1 Unit Tests (zmq-core)
- [ ] **Task**: Test native library loading
  - **Test**: NativeLoader successfully loads libzmq
  - **Platforms**: Windows, Linux, macOS (if available)

- [ ] **Task**: Test LibZmq bindings
  - **Test**: Call zmq_version() and verify result
  - **Test**: Create/destroy context
  - **Test**: Create/close socket
  - **Test**: Send/receive simple message

### 4.2 Integration Tests (zmq)
- [ ] **Task**: Test Context creation and options
  - **Test**: Create context with custom IO threads
  - **Test**: Get/set context options
  - **Test**: Check capabilities

- [ ] **Task**: Test Socket operations
  - **Test**: REQ-REP pattern
  - **Test**: PUB-SUB pattern with subscriptions
  - **Test**: PUSH-PULL pattern
  - **Test**: Socket options (linger, timeouts, etc.)

- [ ] **Task**: Test Message operations
  - **Test**: Create empty message
  - **Test**: Create message with data
  - **Test**: Message properties
  - **Test**: Multipart messages

- [ ] **Task**: Test Polling
  - **Test**: Poll single socket
  - **Test**: Poll multiple sockets
  - **Test**: Poll with timeout

- [ ] **Task**: Test Utilities
  - **Test**: Z85 encode/decode round-trip
  - **Test**: CURVE keypair generation (if supported)
  - **Test**: Proxy basic functionality

### 4.3 Performance Tests
- [ ] **Task**: Benchmark message throughput
  - **Test**: Measure messages/second for various sizes
  - **Compare**: Against netzmq if possible

- [ ] **Task**: Benchmark latency
  - **Test**: Measure round-trip latency
  - **Pattern**: REQ-REP ping-pong

---

## Phase 5: Documentation

### 5.1 API Documentation
- [ ] **Task**: Add Javadoc to all public classes
  - **Standard**: Comprehensive class-level documentation
  - **Include**: Code examples for common patterns

- [ ] **Task**: Add Javadoc to all public methods
  - **Include**: Parameter descriptions, return values, exceptions

### 5.2 User Guide
- [ ] **Task**: Write README.md
  - **Sections**:
    - Quick start
    - Installation
    - Basic examples
    - API overview
    - Building from source

- [ ] **Task**: Write EXAMPLES.md
  - **Include**:
    - REQ-REP pattern
    - PUB-SUB pattern
    - PUSH-PULL pattern
    - Polling example
    - CURVE security example

### 5.3 Developer Guide
- [ ] **Task**: Document FFM patterns used
  - **Topics**:
    - Arena memory management
    - MemorySegment usage
    - Function descriptor patterns
    - Upcall stubs

---

## Phase 6: Build and Release

### 6.1 Build Configuration
- [ ] **Task**: Configure Maven publishing
  - **Group**: io.github.ulalax
  - **Artifacts**: zmq-core, zmq
  - **Metadata**: POM with dependencies

- [ ] **Task**: Configure native library packaging
  - **Ensure**: All platforms included in JAR
  - **Test**: Extraction and loading on each platform

### 6.2 CI/CD
- [ ] **Task**: Setup GitHub Actions (if applicable)
  - **Jobs**:
    - Build on multiple platforms
    - Run tests
    - Publish artifacts

### 6.3 Release
- [ ] **Task**: Prepare release
  - **Version**: 0.0.1
  - **Changelog**: Document features and known issues
  - **Publish**:  GitHub Packages

---

## Common Patterns and References

### C# to Java FFM Translation Guide

#### 1. P/Invoke to FFM
**C# Pattern**:
```csharp
[LibraryImport(LibraryName, EntryPoint = "zmq_errno")]
internal static partial int Errno();
```

**Java FFM Pattern**:
```java
MethodHandle zmq_errno = linker.downcallHandle(
    linker.defaultLookup().find("zmq_errno").orElseThrow(),
    FunctionDescriptor.of(JAVA_INT)
);
```

#### 2. String Marshalling
**C# Pattern**:
```csharp
[LibraryImport(LibraryName, EntryPoint = "zmq_bind", StringMarshalling = StringMarshalling.Utf8)]
internal static partial int Bind(nint socket, string addr);
```

**Java FFM Pattern**:
```java
MethodHandle zmq_bind = linker.downcallHandle(
    linker.defaultLookup().find("zmq_bind").orElseThrow(),
    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS) // second ADDRESS is char*
);
// Usage: allocate UTF-8 string in Arena, pass MemorySegment
```

#### 3. Struct Marshalling
**C# Pattern**:
```csharp
[StructLayout(LayoutKind.Explicit, Size = 64)]
internal struct ZmqMsg { ... }
```

**Java FFM Pattern**:
```java
MemoryLayout ZMQ_MSG_LAYOUT = MemoryLayout.structLayout(
    JAVA_LONG.withName("p0"),
    JAVA_LONG.withName("p1"),
    // ... total 64 bytes
);
```

#### 4. Memory Management
**C# Pattern**:
```csharp
var ptr = Marshal.AllocHGlobal(64);
try { ... } finally { Marshal.FreeHGlobal(ptr); }
```

**Java FFM Pattern**:
```java
try (Arena arena = Arena.ofConfined()) {
    MemorySegment segment = arena.allocate(64);
    // ... automatic cleanup when arena closed
}
```

#### 5. Platform Detection
**C# Pattern**:
```csharp
if (OperatingSystem.IsWindows()) { ... }
```

**Java Pattern**:
```java
String os = System.getProperty("os.name").toLowerCase();
if (os.contains("win")) { ... }
```

---

## Notes and Best Practices

### FFM-Specific Considerations
1. **Arena Lifecycle**: Always use try-with-resources for Arena
2. **Thread Confinement**: Default Arena is confined to creating thread
3. **Memory Segments**: Never return MemorySegment from closed Arena
4. **Function Handles**: Cache MethodHandle instances (expensive to create)
5. **Upcalls**: Upcall stubs must remain reachable while native code may call them

### Platform Differences
1. **File Descriptor Type**: int on Unix, SOCKET (uint64) on Windows
2. **EAGAIN Error**: errno 11 on Linux/Windows, 35 on macOS
3. **Library Extension**: .dll (Windows), .so (Linux), .dylib (macOS)
4. **Path Separator**: System.getProperty("file.separator")

### Error Handling
1. Always check return codes from libzmq functions
2. Call zmq_errno() immediately after -1 return
3. Use zmq_strerror() for error messages
4. EAGAIN is not exceptional - use for non-blocking detection

### Memory Safety
1. Never access MemorySegment after Arena is closed
2. Always close zmq_msg_t with zmq_msg_close()
3. Use Cleaner for finalization, not finalizers
4. Be aware of native memory leaks - test with long-running processes

---

## Progress Tracking

**Overall Progress**: 0/X tasks completed

**Phase 1 (Setup)**: 0/5 completed
**Phase 2 (Core)**: 0/12 completed
**Phase 3 (API)**: 0/15 completed
**Phase 4 (Testing)**: 0/8 completed
**Phase 5 (Docs)**: 0/5 completed
**Phase 6 (Release)**: 0/3 completed

---

## Quick Reference

- **netzmq source**: `/home/ulalax/project/ulalax/libzmq/netzmq/`
- **jvm-zmq target**: `/home/ulalax/project/ulalax/libzmq/jvm-zmq/`
- **Native libs**: https://github.com/ulala-x/libzmq-native
- **JDK version**: 22 (for FFM API)
- **Base package**: `io.github.ulalax.zmq`

This checklist is designed to be comprehensive and self-contained. Each task can be completed independently with references to the detailed documentation in the other markdown files.
