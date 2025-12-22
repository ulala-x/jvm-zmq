package io.github.ulalax.zmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.foreign.ValueLayout;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Smoke test to verify the simplified .NET-style API works correctly.
 * Tests basic send/receive operations in both blocking and non-blocking modes.
 */
class ResultApiSmokeTest {

    private Context context;
    private Socket sender;
    private Socket receiver;

    @BeforeEach
    void setUp() {
        context = new Context();
        sender = new Socket(context, SocketType.PUSH);
        receiver = new Socket(context, SocketType.PULL);

        String endpoint = "inproc://result-api-smoke-test";
        sender.bind(endpoint);
        receiver.connect(endpoint);

        // Give sockets time to connect
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    void tearDown() {
        if (sender != null) {
            sender.close();
        }
        if (receiver != null) {
            receiver.close();
        }
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testBlockingSendReceiveBytes() {
        // Given
        byte[] data = "Hello World".getBytes();

        // When - Blocking send
        boolean sent = sender.send(data);

        // Then
        assertThat(sent).isTrue();

        // When - Blocking receive
        byte[] buffer = new byte[1024];
        int bytes = receiver.recv(buffer);

        // Then
        assertThat(bytes).isEqualTo(data.length);
        byte[] received = new byte[bytes];
        System.arraycopy(buffer, 0, received, 0, bytes);
        assertThat(received).isEqualTo(data);
    }

    @Test
    void testBlockingSendReceiveString() {
        // Given
        String message = "Hello Result API";

        // When - Blocking send
        boolean sent = sender.send(message);

        // Then
        assertThat(sent).isTrue();

        // When - Blocking receive
        String received = receiver.recvString();

        // Then
        assertThat(received).isEqualTo(message);
    }

    @Test
    void testNonBlockingWouldBlock() {
        // When - Non-blocking receive on empty socket
        byte[] buffer = new byte[1024];
        int bytes = receiver.recv(buffer, RecvFlags.DONT_WAIT);

        // Then - Should return -1 (would block)
        assertThat(bytes).isEqualTo(-1);
    }

    @Test
    void testNonBlockingSendReceive() {
        // Given
        byte[] data = "Non-blocking test".getBytes();

        // When - Non-blocking send
        boolean sent = sender.send(data, SendFlags.DONT_WAIT);

        // Then
        assertThat(sent).isTrue();

        // Give message time to arrive
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When - Non-blocking receive
        byte[] buffer = new byte[1024];
        int bytes = receiver.recv(buffer, RecvFlags.DONT_WAIT);

        // Then
        assertThat(bytes).isEqualTo(data.length);
        byte[] received = new byte[bytes];
        System.arraycopy(buffer, 0, received, 0, bytes);
        assertThat(received).isEqualTo(data);
    }

    @Test
    void testResultApiFunctionalStyle() {
        // Given
        String message = "Functional API Test";
        sender.send(message);

        // When - Using Optional.ifPresent with non-blocking receive
        final String[] received = new String[1];
        receiver.recvString(RecvFlags.DONT_WAIT).ifPresent(msg -> received[0] = msg);

        // Then
        assertThat(received[0]).isEqualTo(message);
    }

    @Test
    void testResultApiMap() {
        // Given
        sender.send("12345");

        // When - Transform received string to its length
        Optional<Integer> lengthResult = receiver.recvString(RecvFlags.DONT_WAIT)
            .map(String::length);

        // Then
        assertThat(lengthResult).isPresent();
        assertThat(lengthResult.get()).isEqualTo(5);
    }

    @Test
    void testResultApiOrElse() {
        // Given - Empty socket

        // When - Use orElse with default value
        String result = receiver.recvString(RecvFlags.DONT_WAIT)
            .orElse("default");

        // Then
        assertThat(result).isEqualTo("default");
    }

    @Test
    void testMessageRecvWithResult() {
        // Given
        byte[] data = "Message API Test".getBytes();
        sender.send(data);

        // When - Receive into Message object
        try (Message msg = new Message()) {
            int bytes = receiver.recv(msg, RecvFlags.NONE);

            // Then
            assertThat(bytes).isEqualTo(data.length);

            // Extract data from message
            byte[] received = msg.data().toArray(ValueLayout.JAVA_BYTE);
            assertThat(received).isEqualTo(data);
        }
    }

    @Test
    void testByteBufferRecvWithResult() {
        // Given
        byte[] data = "Buffer Test".getBytes();
        sender.send(data);

        // When - Receive into byte buffer
        byte[] buffer = new byte[1024];
        int bytes = receiver.recv(buffer);

        // Then
        assertThat(bytes).isEqualTo(data.length);

        byte[] received = new byte[bytes];
        System.arraycopy(buffer, 0, received, 0, bytes);
        assertThat(received).isEqualTo(data);
    }

    @Test
    void testMultipleMessagesInSequence() {
        // Given
        String[] messages = {"First", "Second", "Third"};

        // When - Send multiple messages
        for (String msg : messages) {
            boolean sent = sender.send(msg);
            assertThat(sent).isTrue();
        }

        // Then - Receive and verify all messages
        for (String expected : messages) {
            String received = receiver.recvString();
            assertThat(received).isEqualTo(expected);
        }
    }

    @Test
    void testTryRecvMethods() {
        // Given
        String message = "TryRecv Test";
        sender.send(message);

        // Give message time to arrive
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When - Use tryRecvString (non-blocking convenience method)
        Optional<String> result = receiver.tryRecvString();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(message);
    }

    @Test
    void testTryRecvBuffer() {
        // Given
        byte[] data = "TryRecv Buffer Test".getBytes();
        sender.send(data);

        // Give message time to arrive
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When - Use tryRecv (non-blocking convenience method)
        byte[] buffer = new byte[1024];
        int bytes = receiver.tryRecv(buffer);

        // Then
        assertThat(bytes).isEqualTo(data.length);
        byte[] received = new byte[bytes];
        System.arraycopy(buffer, 0, received, 0, bytes);
        assertThat(received).isEqualTo(data);
    }

    @Test
    void testTryRecvWouldBlock() {
        // Given - Empty socket

        // When - Use tryRecvString on empty socket
        Optional<String> result = receiver.tryRecvString();

        // Then - Should be empty
        assertThat(result).isEmpty();
    }
}
