# jvm-zmq Development Checklist

This checklist provides a complete step-by-step guide for porting netzmq (C#) to jvm-zmq (Java FFM). Each task is detailed enough to be executed independently, even after context reset.

## Legend
- [ ] Not Started
- [P] In Progress
- [X] Completed

---

## Phase 1: Project Setup

### 1.1 Gradle Multi-Module Setup
- [X] **Task**: Create root project structure
  - **Action**: Create `/settings.gradle.kts` with module definitions
  - **Details**: Include modules: `zmq-core`, `zmq`
  - **Reference**: `01-project-setup.md` Section 1
  - **Files to create**:
    - `/settings.gradle.kts`
    - `/build.gradle.kts` (root)
    - `/gradle.properties`

- [X] **Task**: Configure zmq-core module
  - **Action**: Create `zmq-core/build.gradle.kts`
  - **Details**: Native bindings layer (no external dependencies except JDK)
  - **Package**: `io.github.ulalax.zmq.core`
  - **Reference**: `01-project-setup.md` Section 2

- [X] **Task**: Configure zmq module
  - **Action**: Create `zmq/build.gradle.kts`
  - **Details**: High-level API (depends on zmq-core)
  - **Package**: `io.github.ulalax.zmq`
  - **Reference**: `01-project-setup.md` Section 3

### 1.2 Native Library Bundling
- [X] **Task**: Download platform-specific libzmq binaries
  - **Action**: Download from https://github.com/ulala-x/libzmq-native/releases
  - **Platforms**: Windows (x64), Linux (x64, arm64), macOS (x64, arm64)
  - **Target**: `zmq-core/src/main/resources/native/{os}/{arch}/`
  - **Files**:
    - Windows: `libzmq.dll`
    - Linux: `libzmq.so`
    - macOS: `libzmq.dylib`

- [X] **Task**: Implement NativeLoader.java
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
- [X] **Task**: Create ZmqConstants.java
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
- [X] **Task**: Create ZmqMsg MemoryLayout
  - **Action**: Define 64-byte zmq_msg_t structure using FFM
  - **Location**: `zmq-core/src/main/java/io/github/ulalax/zmq/core/ZmqStructs.java`
  - **Reference**: `02-ffm-bindings.md` Section 3
  - **Layout**: 64 bytes aligned on 8-byte boundary
  - **Pattern**: Use `MemoryLayout.structLayout()` with 8 longs

- [X] **Task**: Create ZmqPollItem MemoryLayout
  - **Action**: Define zmq_pollitem_t for Windows and Unix
  - **Location**: Same file as above
  - **Reference**: `02-ffm-bindings.md` Section 3
  - **Platform differences**:
    - Windows: socket (ptr), fd (long), events (short), revents (short)
    - Unix: socket (ptr), fd (int), events (short), revents (short)
  - **Pattern**: Use `MemoryLayout.structLayout()` with platform detection

### 2.3 LibZmq Function Bindings
- [X] **Task**: Create LibZmq.java interface
  - **Action**: Port all P/Invoke declarations from `LibZmq.cs`
  - **Location**: `zmq-core/src/main/java/io/github/ulalax/zmq/core/LibZmq.java`
  - **Reference**: `02-ffm-bindings.md` Section 1
  - **Pattern**: Use `Linker.nativeLinker().downcallHandle()` for each function

- [X] **Task**: Implement error handling functions
  - **Functions**: `zmq_errno()`, `zmq_strerror()`
  - **C# Reference**: Lines 17-34 of LibZmq.cs

- [X] **Task**: Implement version function
  - **Function**: `zmq_version()`
  - **C# Reference**: Lines 41-42 of LibZmq.cs
  - **Output**: Three integers (major, minor, patch)

- [X] **Task**: Implement context management functions
  - **Functions**:
    - `zmq_ctx_new()` - Create context
    - `zmq_ctx_term()` - Terminate context
    - `zmq_ctx_shutdown()` - Shutdown context
    - `zmq_ctx_set()` - Set context option
    - `zmq_ctx_get()` - Get context option
  - **C# Reference**: Lines 49-74 of LibZmq.cs

- [X] **Task**: Implement socket management functions
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

- [X] **Task**: Implement message functions
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

- [X] **Task**: Implement polling functions
  - **Functions**:
    - `zmq_poll()` for Windows (with long fd)
    - `zmq_poll()` for Unix (with int fd)
  - **C# Reference**: Lines 343-421 of LibZmq.cs
  - **Platform detection**: Use `System.getProperty("os.name")`
  - **Pattern**: Create wrapper that handles platform differences

- [X] **Task**: Implement utility functions
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
- [X] **Task**: Create ZmqException.java
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
- [X] **Task**: Create Context.java
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
- [X] **Task**: Create Socket.java
  - **Action**: Port from Socket.cs
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Socket.java`
  - **Reference**: `03-high-level-api.md` Section 2
  - **C# Reference**: Socket.cs (lines 1-785)
  - **Sub-tasks**:
    - [X] Constructor and handle management
    - [X] Bind/Connect/Unbind/Disconnect methods
    - [X] Send methods (byte[], ByteBuffer, String)
    - [X] TrySend methods (non-blocking variants)
    - [X] Recv methods (byte[], ByteBuffer)
    - [X] TryRecv methods (non-blocking variants)
    - [X] RecvString methods
    - [X] Socket options (get/set)
    - [X] Subscribe/Unsubscribe methods
    - [X] Monitor method
    - [X] `hasMore()` property
    - [X] Implement `AutoCloseable`

### 3.3 Message Class
- [X] **Task**: Create Message.java
  - **Action**: Port from Message.cs with Arena management
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Message.java`
  - **Reference**: `03-high-level-api.md` Section 3
  - **C# Reference**: Message.cs (lines 1-384)
  - **FFM Pattern**: Use `Arena` for memory management
  - **Sub-tasks**:
    - [X] Constructor variations (empty, size, data, external)
    - [X] Arena management (allocate 64 bytes for zmq_msg_t)
    - [X] `data()` method returning MemorySegment
    - [X] `size()` method
    - [X] `more()` property
    - [X] `getProperty()` / `setProperty()`
    - [X] `getMetadata()`
    - [X] `toByteArray()` / `toString()`
    - [X] `rebuild()` methods
    - [X] `move()` / `copy()` methods
    - [X] Internal `send()` / `recv()` methods
    - [X] Implement `AutoCloseable`
    - [X] Cleaner-based finalization

- [X] **Task**: Handle free callback for external data
  - **Action**: Implement upcall stub for zmq_free_fn
  - **Reference**: Message.cs lines 76-152
  - **FFM Pattern**: Use `Linker.upcallStub()` with Arena
  - **Signature**: `void free_fn(void* data, void* hint)`

### 3.4 Poller Class
- [X] **Task**: Create Poller.java
  - **Action**: Port static polling functions
  - **Location**: `zmq/src/main/java/io/github/ulalax/zmq/Poller.java`
  - **Reference**: `03-high-level-api.md` Section 4
  - **C# Reference**: Poller.cs (lines 1-114)
  - **Features**:
    - Static `poll()` method with PollItem array
    - Static `poll()` with timeout variations
    - Single-socket polling optimization
    - ArrayPool equivalent using thread-local arrays

- [X] **Task**: Create PollItem class
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
- [X] **Task**: Create SocketType.java
- [X] **Task**: Create SendFlags.java
- [X] **Task**: Create RecvFlags.java
- [X] **Task**: Create PollEvents.java
- [X] **Task**: Create ContextOption.java
- [X] **Task**: Create SocketOption.java
- [X] **Task**: Create MessageProperty.java
- [X] **Task**: Create SocketMonitorEvent.java

### 3.6 Utility Classes
- [X] **Task**: Create Curve.java
- [X] **Task**: Create Z85.java
- [X] **Task**: Create Proxy.java

### 3.7 Advanced Features
- [X] **Task**: Create MultipartMessage.java

---

## Phase 4: Testing

### 4.1 Unit Tests (zmq-core)
- [X] **Task**: Test native library loading
- [X] **Task**: Test LibZmq bindings

### 4.2 Integration Tests (zmq)
- [X] **Task**: Test Context creation and options
- [X] **Task**: Test Socket operations (REQ-REP, PUB-SUB, PUSH-PULL, ROUTER-DEALER, XPUB-XSUB, PAIR)
- [X] **Task**: Test Message operations
- [X] **Task**: Test Polling
- [X] **Task**: Test Utilities

### 4.3 Performance Tests
- [ ] **Task**: Benchmark message throughput
- [ ] **Task**: Benchmark latency

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
  - **Publish**: GitHub Packages

---

## Progress Tracking

**Overall Progress**: 43/48 tasks completed

**Phase 1 (Setup)**: 5/5 completed ✅
**Phase 2 (Core)**: 12/12 completed ✅
**Phase 3 (API)**: 15/15 completed ✅
**Phase 4 (Testing)**: 7/9 completed (Performance tests pending)
**Phase 5 (Docs)**: 0/5 completed
**Phase 6 (Release)**: 0/4 completed

---

## Quick Reference

- **netzmq source**: `/home/ulalax/project/ulalax/libzmq/netzmq/`
- **jvm-zmq target**: `/home/ulalax/project/ulalax/libzmq/jvm-zmq/`
- **Native libs**: https://github.com/ulala-x/libzmq-native
- **JDK version**: 21 (for FFM API)
- **Base package**: `io.github.ulalax.zmq`
- **Test count**: 386 tests (all passing)

This checklist is designed to be comprehensive and self-contained. Each task can be completed independently with references to the detailed documentation in the other markdown files.
