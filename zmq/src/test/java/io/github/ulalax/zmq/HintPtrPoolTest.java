package io.github.ulalax.zmq;

import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HintPtrPool memory reuse functionality.
 * Verifies that the pool eliminates Arena.ofShared() overhead without memory leaks.
 */
class HintPtrPoolTest {

    @Test
    void testHintPtrPoolReuse() throws Exception {
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.PULL);
             Socket client = new Socket(ctx, SocketType.PUSH)) {

            server.bind("tcp://127.0.0.1:15558");
            client.connect("tcp://127.0.0.1:15558");

            Thread.sleep(100); // Allow connection

            // Get pool initial state
            long initialAllocated = Message.HintPtrPool.getTotalAllocated();
            int initialPoolSize = Message.HintPtrPool.getPoolSize();

            System.out.println("Pool 초기 상태:");
            System.out.println("  총 할당: " + initialAllocated);
            System.out.println("  사용 가능: " + initialPoolSize);

            // Send 100 zero-copy messages
            int messageCount = 100;
            for (int i = 0; i < messageCount; i++) {
                Arena dataArena = Arena.ofShared();
                MemorySegment data = dataArena.allocate(64);
                data.setAtIndex(ValueLayout.JAVA_BYTE, 0, (byte) i);

                Message msg = new Message(data, 64, seg -> dataArena.close());
                client.send(msg, SendFlags.NONE);
                msg.close();

                Message received = new Message();
                server.recv(received, RecvFlags.NONE);
                assertEquals((byte) i, received.data().getAtIndex(ValueLayout.JAVA_BYTE, 0));
                received.close();
            }

            Thread.sleep(300); // Wait for callbacks to complete

            // Check pool state after messages
            long finalAllocated = Message.HintPtrPool.getTotalAllocated();
            int finalPoolSize = Message.HintPtrPool.getPoolSize();

            System.out.println("\n" + messageCount + "개 메시지 전송 후:");
            System.out.println("  총 할당: " + finalAllocated);
            System.out.println("  사용 가능: " + finalPoolSize);
            System.out.println("  추가 할당: " + (finalAllocated - initialAllocated));

            // Pool should reuse memory - minimal new allocations
            assertTrue(finalAllocated - initialAllocated < 10,
                "Pool should reuse memory (allocated: " + (finalAllocated - initialAllocated) + ")");

            // Pool size should be similar (segments returned)
            assertTrue(Math.abs(finalPoolSize - initialPoolSize) < messageCount,
                "Pool size should stabilize (diff: " + (finalPoolSize - initialPoolSize) + ")");
        }
    }

    @Test
    void testNoMemoryLeak() throws Exception {
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.PULL);
             Socket client = new Socket(ctx, SocketType.PUSH)) {

            server.bind("tcp://127.0.0.1:15559");
            client.connect("tcp://127.0.0.1:15559");

            Thread.sleep(100);

            long initialAllocated = Message.HintPtrPool.getTotalAllocated();

            // Send 1000 messages
            int messageCount = 1000;
            for (int i = 0; i < messageCount; i++) {
                Arena dataArena = Arena.ofShared();
                MemorySegment data = dataArena.allocate(64);

                Message msg = new Message(data, 64, seg -> dataArena.close());
                client.send(msg, SendFlags.NONE);
                msg.close();

                Message received = new Message();
                server.recv(received, RecvFlags.NONE);
                received.close();
            }

            Thread.sleep(500); // Wait for callbacks

            long finalAllocated = Message.HintPtrPool.getTotalAllocated();
            long additionalAllocations = finalAllocated - initialAllocated;

            System.out.println("\n메모리 누수 테스트 (" + messageCount + "개 메시지):");
            System.out.println("  초기 할당: " + initialAllocated);
            System.out.println("  최종 할당: " + finalAllocated);
            System.out.println("  추가 할당: " + additionalAllocations);

            // With initial pool of 1000, we should have almost no additional allocations
            assertTrue(additionalAllocations < 100,
                "Memory leak detected: " + additionalAllocations + " extra allocations");
        }
    }

    @Test
    void testPoolUnderLoad() throws Exception {
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.PULL);
             Socket client = new Socket(ctx, SocketType.PUSH)) {

            server.bind("tcp://127.0.0.1:15560");
            client.connect("tcp://127.0.0.1:15560");

            Thread.sleep(100);

            long startAllocated = Message.HintPtrPool.getTotalAllocated();
            int startPoolSize = Message.HintPtrPool.getPoolSize();

            // Send 2000 messages (exceeds initial pool size of 1000)
            int messageCount = 2000;
            for (int i = 0; i < messageCount; i++) {
                Arena dataArena = Arena.ofShared();
                MemorySegment data = dataArena.allocate(32);

                Message msg = new Message(data, 32, seg -> dataArena.close());
                client.send(msg, SendFlags.NONE);
                msg.close();

                Message received = new Message();
                server.recv(received, RecvFlags.NONE);
                received.close();
            }

            Thread.sleep(800); // Wait for all callbacks

            long endAllocated = Message.HintPtrPool.getTotalAllocated();
            int endPoolSize = Message.HintPtrPool.getPoolSize();

            System.out.println("\n부하 테스트 (" + messageCount + "개 메시지):");
            System.out.println("  시작 할당: " + startAllocated + ", 풀 크기: " + startPoolSize);
            System.out.println("  종료 할당: " + endAllocated + ", 풀 크기: " + endPoolSize);
            System.out.println("  추가 할당: " + (endAllocated - startAllocated));

            // Pool should grow to accommodate load, then stabilize
            assertTrue(endAllocated >= startAllocated,
                "Pool should grow under load");

            // After all messages processed, pool should have grown but not excessively
            assertTrue(endAllocated - startAllocated < messageCount,
                "Pool growth should be reasonable (grew by " + (endAllocated - startAllocated) + ")");
        }
    }

    @Test
    void testPoolBasicAllocation() {
        // Test basic pool operations
        long before = Message.HintPtrPool.getTotalAllocated();
        int sizeBefore = Message.HintPtrPool.getPoolSize();

        MemorySegment hint = Message.HintPtrPool.allocate();
        assertNotNull(hint);
        assertEquals(8, hint.byteSize(), "HintPtr should be 8 bytes (JAVA_LONG)");

        int sizeAfterAlloc = Message.HintPtrPool.getPoolSize();
        assertEquals(sizeBefore - 1, sizeAfterAlloc, "Pool size should decrease by 1");

        // Test write/read
        hint.set(ValueLayout.JAVA_LONG, 0, 12345L);
        assertEquals(12345L, hint.get(ValueLayout.JAVA_LONG, 0));

        // Return to pool
        Message.HintPtrPool.free(hint);
        int sizeAfterFree = Message.HintPtrPool.getPoolSize();
        assertEquals(sizeBefore, sizeAfterFree, "Pool size should be restored");
    }

    @Test
    void testPoolReuseSameSegment() {
        // Verify that freed segments are actually reused
        int poolSizeBefore = Message.HintPtrPool.getPoolSize();
        long allocatedBefore = Message.HintPtrPool.getTotalAllocated();

        // Allocate and free multiple segments
        MemorySegment hint1 = Message.HintPtrPool.allocate();
        hint1.set(ValueLayout.JAVA_LONG, 0, 999L);
        assertEquals(999L, hint1.get(ValueLayout.JAVA_LONG, 0));

        int poolSizeAfterAlloc = Message.HintPtrPool.getPoolSize();
        assertEquals(poolSizeBefore - 1, poolSizeAfterAlloc, "Pool size should decrease");

        Message.HintPtrPool.free(hint1);

        int poolSizeAfterFree = Message.HintPtrPool.getPoolSize();
        assertEquals(poolSizeBefore, poolSizeAfterFree, "Pool size should be restored");

        // Allocate again - should reuse from pool, not allocate new
        MemorySegment hint2 = Message.HintPtrPool.allocate();
        long allocatedAfter = Message.HintPtrPool.getTotalAllocated();

        assertEquals(allocatedBefore, allocatedAfter, "Should reuse segment, not allocate new");

        Message.HintPtrPool.free(hint2);
    }
}
