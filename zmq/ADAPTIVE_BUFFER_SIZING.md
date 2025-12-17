# Adaptive Buffer Sizing Implementation

## Overview

The Socket class now implements dynamic buffer allocation that automatically adjusts buffer sizes based on actual message patterns, improving memory efficiency while maintaining performance.

## Implementation Details

### 1. Tracking Fields

Added to `/home/ulalax/project/ulalax/libzmq/jvm-zmq/zmq/src/main/java/io/github/ulalax/zmq/Socket.java`:

```java
// Buffer usage tracking for adaptive sizing
private static final int BUFFER_RESET_THRESHOLD_MULTIPLIER = 4;  // Reset if 4x larger than needed
private static final int BUFFER_USAGE_SAMPLE_SIZE = 100;  // Track last 100 operations
private int sendBufferUsageCount = 0;
private long sendBufferTotalUsed = 0;
private int recvBufferUsageCount = 0;
private long recvBufferTotalUsed = 0;
```

### 2. Buffer Management Logic

#### `shouldResetBuffer(long bufferSize, long avgUsed)` (line ~1128)
- Checks if a buffer should be reset to save memory
- Returns `true` only if:
  - Buffer is at least 4x larger than average usage
  - Average usage is at least 1KB (prevents thrashing on tiny messages)

#### Send Buffer Tracking (`trySend` method, line ~357-372)
After successful send operations:
1. Accumulates total bytes sent
2. Increments operation counter
3. Every 100 operations:
   - Calculates average message size
   - If buffer is oversized (4x larger than average):
     - Resets buffer to 2x average + 1KB padding
   - Resets tracking counters

#### Receive Buffer Tracking (`tryRecv` method, line ~579-594)
After successful receive operations:
1. Accumulates total bytes received
2. Increments operation counter
3. Every 100 operations:
   - Calculates average message size
   - If buffer is oversized (4x larger than average):
     - Resets buffer to 2x average + 1KB padding
   - Resets tracking counters

## Behavior & Benefits

### Adaptive Resizing Strategy

**Scenario 1: Large spike, then small messages**
```
Operations:    1      2-101
Message size:  32KB   1KB
Buffer size:   32KB → 3KB (after 100 small messages)
Memory saved:  ~29KB (91% reduction)
```

**Scenario 2: Consistently large messages**
```
Operations:    1-100
Message size:  10KB
Buffer size:   10KB (no reset - messages are consistently large)
```

**Scenario 3: Tiny messages (< 1KB)**
```
Operations:    1-100
Message size:  100 bytes
Buffer size:   No reset (below 1KB threshold, prevents thrashing)
```

### Key Features

1. **Low Overhead**: Only checks every 100 operations
2. **Prevents Thrashing**: Won't reset for messages < 1KB
3. **Conservative Threshold**: Requires 4x oversizing before reset
4. **Headroom**: Resets to 2x average + 1KB padding (not just average)
5. **Graceful Growth**: Buffers still expand as needed for large messages

### Memory Efficiency Gains

- **Before**: Buffers grow but never shrink
  - One 32KB message → buffer stays 32KB forever
  - 1000 sockets with 32KB buffers = 32MB memory

- **After**: Buffers adapt to typical usage
  - One 32KB message, then 1KB messages → buffer shrinks to ~3KB
  - 1000 sockets with 3KB buffers = 3MB memory
  - **Memory saved: 29MB (91% reduction)**

## Testing

Comprehensive test suite in `AdaptiveBufferSizingTest.java`:

### Test Cases

1. **testSendBufferAdaptiveSizing()**
   - Sends 1x 32KB message, then 100x 1KB messages
   - Verifies buffer shrinks to ~3-4KB
   - Confirms at least 4x reduction in buffer size

2. **testRecvBufferAdaptiveSizing()**
   - Receives 1x 32KB message, then 100x 1KB messages
   - Verifies buffer shrinks to ~3-4KB
   - Confirms at least 4x reduction in buffer size

3. **testNoResetForConsistentlyLargeMessages()**
   - Sends 100x 10KB messages
   - Verifies buffer remains large (no inappropriate shrinking)

4. **testNoResetForTinyMessages()**
   - Sends 1x 16KB message, then 100x 100-byte messages
   - Verifies buffer does NOT reset (below 1KB threshold)
   - Prevents thrashing on small message workloads

### Test Results

```bash
cd /home/ulalax/project/ulalax/libzmq/jvm-zmq
./gradlew :zmq:test --tests "AdaptiveBufferSizingTest"

BUILD SUCCESSFUL in 1s
4 tests passed
```

## Configuration

Current constants (can be tuned if needed):

```java
BUFFER_RESET_THRESHOLD_MULTIPLIER = 4    // Reset when 4x oversized
BUFFER_USAGE_SAMPLE_SIZE = 100           // Check every 100 operations
Minimum average for reset = 1024 bytes   // Prevent thrashing on tiny messages
New buffer size = (average * 2) + 1024   // 2x average + 1KB headroom
```

## Performance Impact

- **Overhead per operation**: ~3 integer operations (negligible)
- **Check frequency**: Every 100 operations
- **Memory allocation**: Only when resizing (rare)
- **No synchronization**: Single-threaded per socket (no locks needed)

## Backwards Compatibility

✅ Fully backwards compatible:
- No API changes
- No behavior changes for normal usage
- Only affects internal buffer management
- All existing tests pass

## Files Modified

1. `/home/ulalax/project/ulalax/libzmq/jvm-zmq/zmq/src/main/java/io/github/ulalax/zmq/Socket.java`
   - Added tracking fields (line ~55-61)
   - Added JavaDoc for buffer fields (line ~42-53)
   - Added `shouldResetBuffer()` method (line ~1128-1133)
   - Updated `trySend()` method (line ~357-372)
   - Updated `tryRecv()` method (line ~579-594)

2. `/home/ulalax/project/ulalax/libzmq/jvm-zmq/zmq/src/test/java/io/github/ulalax/zmq/AdaptiveBufferSizingTest.java`
   - New comprehensive test suite (4 test cases)

## Summary

This implementation provides automatic memory optimization for ZMQ sockets with:
- **Minimal overhead**: Simple tracking, rare checks
- **Safe defaults**: Conservative thresholds, anti-thrashing logic
- **Significant savings**: 90%+ memory reduction for spike-then-small workloads
- **Zero configuration**: Works automatically, no tuning needed
- **Fully tested**: Comprehensive test coverage
- **Production-ready**: All tests pass, backwards compatible
