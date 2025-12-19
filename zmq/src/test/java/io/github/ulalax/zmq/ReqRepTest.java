package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for REQ-REP pattern.
 */
@Tag("Socket")
@Tag("ReqRep")
class ReqRepTest {

    @Nested
    @DisplayName("Request-Response")
    class RequestResponse {

        @Test
        @DisplayName("Should exchange messages between REQ and REP sockets")
        void should_Exchange_Messages_Between_Req_And_Rep_Sockets() throws Exception {
            // Given: A REP server and REQ client connected on the same endpoint
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.bind("inproc://test-reqrep");
                client.connect("inproc://test-reqrep");

                // Give time for connection
                Thread.sleep(50);

                // When: Client sends request
                String request = "Hello";
                client.send(request);

                // Then: Server receives the request
                byte[] received = server.recvBytes().value();
                assertThat(received).isNotNull();
                assertThat(new String(received, StandardCharsets.UTF_8))
                        .as("Received request")
                        .isEqualTo(request);

                // When: Server sends reply
                String reply = "World";
                server.send(reply);

                // Then: Client receives the reply
                byte[] response = client.recvBytes().value();
                assertThat(response).isNotNull();
                assertThat(new String(response, StandardCharsets.UTF_8))
                        .as("Received reply")
                        .isEqualTo(reply);
            }
        }

        @Test
        @DisplayName("Should handle multiple request-response exchanges")
        void should_Handle_Multiple_Request_Response_Exchanges() throws Exception {
            // Given: A REP server and REQ client connected
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.bind("inproc://test-multi");
                client.connect("inproc://test-multi");
                Thread.sleep(50);

                // When: Perform multiple exchanges
                // Then: Each exchange should succeed
                for (int i = 0; i < 5; i++) {
                    String request = "Request-" + i;
                    client.send(request);

                    byte[] received = server.recvBytes().value();
                    assertThat(new String(received, StandardCharsets.UTF_8))
                            .as("Request #" + i)
                            .isEqualTo(request);

                    String reply = "Reply-" + i;
                    server.send(reply);

                    byte[] response = client.recvBytes().value();
                    assertThat(new String(response, StandardCharsets.UTF_8))
                            .as("Reply #" + i)
                            .isEqualTo(reply);
                }
            }
        }
    }

    @Nested
    @DisplayName("Binary Data Exchange")
    class BinaryDataExchange {

        @Test
        @DisplayName("Should exchange binary data correctly")
        void should_Exchange_Binary_Data_Correctly() throws Exception {
            // Given: A REP server and REQ client connected
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.bind("inproc://test-binary");
                client.connect("inproc://test-binary");
                Thread.sleep(50);

                // When: Send binary data with all byte values (0-255)
                byte[] binaryData = new byte[256];
                for (int i = 0; i < 256; i++) {
                    binaryData[i] = (byte) i;
                }

                client.send(binaryData);

                // Then: Server should receive exact binary data
                byte[] received = server.recvBytes().value();
                assertThat(received)
                        .as("Received binary data")
                        .isEqualTo(binaryData);

                // When: Server echoes the data back
                server.send(binaryData);

                // Then: Client should receive exact binary data
                byte[] response = client.recvBytes().value();
                assertThat(response)
                        .as("Echoed binary data")
                        .isEqualTo(binaryData);
            }
        }
    }
}
