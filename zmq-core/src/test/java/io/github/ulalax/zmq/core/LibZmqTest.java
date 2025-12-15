package io.github.ulalax.zmq.core;

import org.junit.jupiter.api.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for LibZmq FFM bindings.
 * Verifies low-level libzmq function calls work correctly.
 */
@Tag("Core")
@Tag("FFM")
class LibZmqTest {

    private Arena arena;
    private MemorySegment context;

    @BeforeEach
    void setUp() {
        arena = Arena.ofConfined();
        context = LibZmq.ctxNew();
        assertThat(context).isNotNull();
        assertThat(context.address()).isNotEqualTo(0);
    }

    @AfterEach
    void tearDown() {
        if (context != null && context.address() != 0) {
            LibZmq.ctxTerm(context);
        }
        if (arena != null) {
            arena.close();
        }
    }

    @Nested
    @DisplayName("Version Information")
    class VersionInformation {

        @Test
        @DisplayName("Should return valid ZeroMQ version")
        void should_Return_Valid_ZeroMQ_Version() {
            // When: Get ZMQ version
            int[] version = LibZmq.version();

            // Then: Should return valid version array with major >= 4
            assertThat(version).isNotNull()
                    .hasSize(3);
            assertThat(version[0]).as("Major version").isGreaterThanOrEqualTo(4);
            assertThat(version[1]).as("Minor version").isGreaterThanOrEqualTo(0);
            assertThat(version[2]).as("Patch version").isGreaterThanOrEqualTo(0);

            System.out.println("ZMQ Version: " + version[0] + "." + version[1] + "." + version[2]);
        }
    }

    @Nested
    @DisplayName("Context Management")
    class ContextManagement {

        @Test
        @DisplayName("Should create new context successfully")
        void should_Create_New_Context_Successfully() {
            // When: Create new context
            MemorySegment ctx = LibZmq.ctxNew();

            // Then: Should return valid context
            assertThat(ctx).isNotNull();
            assertThat(ctx.address()).isNotEqualTo(0);

            // Cleanup
            int rc = LibZmq.ctxTerm(ctx);
            assertThat(rc).as("Context termination").isEqualTo(0);
        }

        @Test
        @DisplayName("Should set and get context options")
        void should_Set_And_Get_Context_Options() {
            // When: Set IO threads to 2
            int rc = LibZmq.ctxSet(context, ZmqConstants.ZMQ_IO_THREADS, 2);
            assertThat(rc).as("Setting IO threads").isEqualTo(0);

            // Then: Should retrieve the set value
            int ioThreads = LibZmq.ctxGet(context, ZmqConstants.ZMQ_IO_THREADS);
            assertThat(ioThreads).as("Retrieved IO threads").isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Socket Operations")
    class SocketOperations {

        @Test
        @DisplayName("Should create socket successfully")
        void should_Create_Socket_Successfully() {
            // When: Create a REQ socket
            MemorySegment socket = LibZmq.socket(context, ZmqConstants.ZMQ_REQ);

            // Then: Should return valid socket
            assertThat(socket).isNotNull();
            assertThat(socket.address()).isNotEqualTo(0);

            // Cleanup
            int rc = LibZmq.close(socket);
            assertThat(rc).as("Socket close").isEqualTo(0);
        }

        @Test
        @DisplayName("Should bind and connect sockets")
        void should_Bind_And_Connect_Sockets() {
            // Given: A REP server and REQ client
            MemorySegment server = LibZmq.socket(context, ZmqConstants.ZMQ_REP);
            MemorySegment client = LibZmq.socket(context, ZmqConstants.ZMQ_REQ);

            try {
                // When: Bind server and connect client
                int rc = LibZmq.bind(server, "inproc://test");
                assertThat(rc).as("Bind operation").isEqualTo(0);

                rc = LibZmq.connect(client, "inproc://test");
                assertThat(rc).as("Connect operation").isEqualTo(0);

                // Then: Sockets should be ready for communication
                assertThat(server.address()).isNotEqualTo(0);
                assertThat(client.address()).isNotEqualTo(0);

            } finally {
                LibZmq.close(client);
                LibZmq.close(server);
            }
        }
    }

    @Nested
    @DisplayName("Message Handling")
    class MessageHandling {

        @Test
        @DisplayName("Should initialize and close empty message")
        void should_Initialize_And_Close_Empty_Message() {
            // Given: Message structure
            MemorySegment msg = arena.allocate(ZmqStructs.ZMQ_MSG_SIZE);

            // When: Initialize message
            int rc = LibZmq.msgInit(msg);
            assertThat(rc).as("Message init").isEqualTo(0);

            // Then: Message should be valid with size 0
            long size = LibZmq.msgSize(msg);
            assertThat(size).as("Empty message size").isEqualTo(0);

            // Cleanup
            rc = LibZmq.msgClose(msg);
            assertThat(rc).as("Message close").isEqualTo(0);
        }

        @Test
        @DisplayName("Should initialize message with size")
        void should_Initialize_Message_With_Size() {
            // Given: Message structure
            MemorySegment msg = arena.allocate(ZmqStructs.ZMQ_MSG_SIZE);

            // When: Initialize message with size 100
            int rc = LibZmq.msgInitSize(msg, 100);
            assertThat(rc).as("Message init with size").isEqualTo(0);

            // Then: Message should have correct size
            long size = LibZmq.msgSize(msg);
            assertThat(size).as("Message size").isEqualTo(100);

            // And: Message data should be accessible
            MemorySegment data = LibZmq.msgData(msg);
            assertThat(data).isNotNull();
            assertThat(data.address()).isNotEqualTo(0);

            // Cleanup
            LibZmq.msgClose(msg);
        }
    }

    @Nested
    @DisplayName("Send and Receive")
    class SendAndReceive {

        @Test
        @DisplayName("Should send and receive messages between REQ-REP sockets")
        void should_Send_And_Receive_Messages_Between_Req_Rep_Sockets() {
            // Given: REQ-REP pair connected
            MemorySegment server = LibZmq.socket(context, ZmqConstants.ZMQ_REP);
            MemorySegment client = LibZmq.socket(context, ZmqConstants.ZMQ_REQ);

            try {
                LibZmq.bind(server, "inproc://test-sendrecv");
                LibZmq.connect(client, "inproc://test-sendrecv");

                // Give sockets time to connect
                Thread.sleep(100);

                // When: Client sends message
                String testMsg = "Hello ZMQ";
                byte[] data = testMsg.getBytes();
                MemorySegment sendBuf = arena.allocateFrom(java.lang.foreign.ValueLayout.JAVA_BYTE, data);

                int rc = LibZmq.send(client, sendBuf, data.length, 0);
                assertThat(rc).as("Bytes sent").isGreaterThan(0);

                // Then: Server should receive the message
                MemorySegment recvBuf = arena.allocate(100);
                rc = LibZmq.recv(server, recvBuf, 100, 0);
                assertThat(rc).as("Bytes received")
                        .isGreaterThan(0)
                        .isEqualTo(data.length);

                // And: Received data should match sent data
                byte[] received = recvBuf.asSlice(0, rc).toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
                assertThat(received).as("Received message").isEqualTo(data);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Test interrupted");
            } finally {
                LibZmq.close(client);
                LibZmq.close(server);
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle invalid socket operations")
        void should_Handle_Invalid_Socket_Operations() {
            // Given: Invalid socket (NULL)
            MemorySegment invalidSocket = MemorySegment.NULL;

            // When: Try to close invalid socket
            int rc = LibZmq.close(invalidSocket);

            // Then: Should return error code
            assertThat(rc).as("Error return code").isEqualTo(-1);

            // And: Error number should be set
            int errno = LibZmq.errno();
            assertThat(errno).as("Error number").isNotEqualTo(0);

            // And: Error message should be available
            String errMsg = LibZmq.strerror(errno);
            assertThat(errMsg)
                    .as("Error message")
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Capability Detection")
    class CapabilityDetection {

        @Test
        @DisplayName("Should check for standard capabilities")
        void should_Check_For_Standard_Capabilities() {
            // When: Check for standard capabilities
            int hasCurve = LibZmq.has("curve");
            int hasIpc = LibZmq.has("ipc");

            // Then: Should return 0 or 1
            assertThat(hasCurve).as("CURVE capability").isBetween(0, 1);
            assertThat(hasIpc).as("IPC capability").isBetween(0, 1);
        }
    }
}
