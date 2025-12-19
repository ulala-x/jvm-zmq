package io.github.ulalax.zmq;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Smoke test to verify the Result API refactoring works correctly.
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
        SendResult sendResult = sender.send(data);

        // Then
        assertThat(sendResult.isPresent()).isTrue();
        assertThat(sendResult.value()).isEqualTo(data.length);
        assertThat(sendResult.wouldBlock()).isFalse();

        // When - Blocking receive
        RecvResult<byte[]> recvResult = receiver.recvBytes();

        // Then
        assertThat(recvResult.isPresent()).isTrue();
        assertThat(recvResult.value()).isEqualTo(data);
        assertThat(recvResult.wouldBlock()).isFalse();
    }

    @Test
    void testBlockingSendReceiveString() {
        // Given
        String message = "Hello Result API";

        // When - Blocking send
        SendResult sendResult = sender.send(message);

        // Then
        assertThat(sendResult.isPresent()).isTrue();
        assertThat(sendResult.value()).isGreaterThan(0);

        // When - Blocking receive
        RecvResult<String> recvResult = receiver.recvString();

        // Then
        assertThat(recvResult.isPresent()).isTrue();
        assertThat(recvResult.value()).isEqualTo(message);
    }

    @Test
    void testNonBlockingWouldBlock() {
        // When - Non-blocking receive on empty socket
        RecvResult<byte[]> recvResult = receiver.recvBytes(RecvFlags.DONT_WAIT);

        // Then - Should return empty result (would block)
        assertThat(recvResult.isPresent()).isFalse();
        assertThat(recvResult.wouldBlock()).isTrue();
    }

    @Test
    void testNonBlockingSendReceive() {
        // Given
        byte[] data = "Non-blocking test".getBytes();

        // When - Non-blocking send
        SendResult sendResult = sender.send(data, SendFlags.DONT_WAIT);

        // Then
        assertThat(sendResult.isPresent()).isTrue();
        assertThat(sendResult.value()).isEqualTo(data.length);

        // Give message time to arrive
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When - Non-blocking receive
        RecvResult<byte[]> recvResult = receiver.recvBytes(RecvFlags.DONT_WAIT);

        // Then
        assertThat(recvResult.isPresent()).isTrue();
        assertThat(recvResult.value()).isEqualTo(data);
    }

    @Test
    void testResultApiFunctionalStyle() {
        // Given
        String message = "Functional API Test";
        sender.send(message);

        // When - Using ifPresent
        final String[] received = new String[1];
        receiver.recvString().ifPresent(msg -> received[0] = msg);

        // Then
        assertThat(received[0]).isEqualTo(message);
    }

    @Test
    void testResultApiMap() {
        // Given
        sender.send("12345");

        // When - Transform received string to its length
        RecvResult<Integer> lengthResult = receiver.recvString()
            .map(String::length);

        // Then
        assertThat(lengthResult.isPresent()).isTrue();
        assertThat(lengthResult.value()).isEqualTo(5);
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
            RecvResult<Integer> result = receiver.recv(msg, RecvFlags.NONE);

            // Then
            assertThat(result.isPresent()).isTrue();
            assertThat(result.value()).isEqualTo(data.length);

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
        RecvResult<Integer> result = receiver.recv(buffer);

        // Then
        assertThat(result.isPresent()).isTrue();
        assertThat(result.value()).isEqualTo(data.length);

        byte[] received = new byte[result.value()];
        System.arraycopy(buffer, 0, received, 0, result.value());
        assertThat(received).isEqualTo(data);
    }

    @Test
    void testMultipleMessagesInSequence() {
        // Given
        String[] messages = {"First", "Second", "Third"};

        // When - Send multiple messages
        for (String msg : messages) {
            SendResult result = sender.send(msg);
            assertThat(result.isPresent()).isTrue();
        }

        // Then - Receive and verify all messages
        for (String expected : messages) {
            RecvResult<String> result = receiver.recvString();
            assertThat(result.isPresent()).isTrue();
            assertThat(result.value()).isEqualTo(expected);
        }
    }
}
