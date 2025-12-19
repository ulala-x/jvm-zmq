package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.MultipartMessage;
import io.github.ulalax.zmq.RecvResult;
import io.github.ulalax.zmq.SendFlags;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;

import java.util.ArrayList;
import java.util.List;

/**
 * Multipart Message Extensions Sample
 *
 * <p>Demonstrates the Socket extension methods for convenient multipart message handling
 * using the new Result API pattern. Multipart messages allow you to send multiple frames
 * as a single atomic message. All frames are delivered together or not at all.</p>
 *
 * <p>This sample includes six examples:</p>
 * <ul>
 *   <li>Example 1: Send multipart using the new SendFlags API</li>
 *   <li>Example 2: Send multipart with byte arrays using SendFlags</li>
 *   <li>Example 3: Send multipart using MultipartMessage container</li>
 *   <li>Example 4: Receive multipart using recvMultipart with Result API</li>
 *   <li>Example 5: Non-blocking receive with Result API - demonstrates wouldBlock() and ifPresent()</li>
 *   <li>Example 6: Router-Dealer pattern with SendFlags.SEND_MORE</li>
 * </ul>
 *
 * <p><b>Key API Changes Demonstrated:</b></p>
 * <ul>
 *   <li>SendFlags.SEND_MORE instead of deprecated sendMore() method</li>
 *   <li>RecvResult API with isPresent(), wouldBlock(), and functional-style ifPresent()</li>
 *   <li>Type-safe multipart message handling</li>
 * </ul>
 */
public class MultipartSample {

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ Multipart Extensions Sample");
        System.out.println("====================================");
        System.out.println();

        // Run all examples
        example1_SendMultipartWithStrings();
        example2_SendMultipartWithByteArrays();
        example3_SendMultipartWithMultipartMessage();
        example4_RecvMultipart();
        example5_TryRecvMultipart();
        example6_RouterDealerWithExtensions();

        System.out.println();
        System.out.println("All examples completed successfully!");
    }

    /**
     * Example 1: Send multipart using the new SendFlags API.
     *
     * <p>Demonstrates the modern approach to sending multipart messages using SendFlags.SEND_MORE
     * instead of the deprecated sendMore() method. The last frame uses SendFlags.NONE to signal
     * the end of the multipart message.</p>
     */
    private static void example1_SendMultipartWithStrings() {
        System.out.println("Example 1: Send multipart with SendFlags.SEND_MORE");
        System.out.println("--------------------------------------------------");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.setOption(SocketOption.LINGER, 0);
            receiver.setOption(SocketOption.LINGER, 0);

            receiver.bind("tcp://127.0.0.1:20001");
            sender.connect("tcp://127.0.0.1:20001");

            sleep(100);

            // NEW API: Use SendFlags.SEND_MORE for all frames except the last
            // OLD API: sendMore("Header"); sendMore("Body"); send("Footer");
            sender.send("Header", SendFlags.SEND_MORE);
            sender.send("Body", SendFlags.SEND_MORE);
            sender.send("Footer", SendFlags.NONE);  // Last frame uses NONE
            System.out.println("Sent 3-frame message: Header, Body, Footer");

            // Receive using frame-by-frame method with Result API
            // In blocking mode, these will always succeed (or throw)
            String header = receiver.recvString().value();
            String body = receiver.recvString().value();
            String footer = receiver.recvString().value();

            System.out.println("Received: " + header + ", " + body + ", " + footer);
            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 1: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 2: Send multipart using byte arrays with dynamic SendFlags selection.
     *
     * <p>Shows how to programmatically choose between SendFlags.SEND_MORE and SendFlags.NONE
     * when iterating over frames. This is useful for variable-length multipart messages.</p>
     */
    private static void example2_SendMultipartWithByteArrays() {
        System.out.println("Example 2: Send multipart with byte arrays and dynamic flags");
        System.out.println("-------------------------------------------------------------");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.setOption(SocketOption.LINGER, 0);
            receiver.setOption(SocketOption.LINGER, 0);

            receiver.bind("tcp://127.0.0.1:20002");
            sender.connect("tcp://127.0.0.1:20002");

            sleep(100);

            // Prepare binary frames
            List<byte[]> frames = new ArrayList<>();
            frames.add(new byte[]{0x01, 0x02, 0x03});
            frames.add(new byte[]{0x04, 0x05});
            frames.add(new byte[]{0x06, 0x07, 0x08, 0x09});

            // Send multipart message - use SEND_MORE for all except the last frame
            // This pattern is useful when frame count is dynamic
            for (int i = 0; i < frames.size(); i++) {
                boolean isLast = (i == frames.size() - 1);
                SendFlags flags = isLast ? SendFlags.NONE : SendFlags.SEND_MORE;
                sender.send(frames.get(i), flags);
            }
            System.out.println("Sent " + frames.size() + " binary frames");

            // Receive frames with Result API
            for (int i = 0; i < frames.size(); i++) {
                byte[] frame = receiver.recvBytes().value();  // .value() extracts from RecvResult
                System.out.print("Frame " + (i + 1) + ": [");
                for (int j = 0; j < frame.length; j++) {
                    System.out.print(String.format("0x%02X", frame[j]));
                    if (j < frame.length - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println("]");
            }

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 2: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 3: Send multipart using MultipartMessage container.
     */
    private static void example3_SendMultipartWithMultipartMessage() {
        System.out.println("Example 3: Send multipart with MultipartMessage");
        System.out.println("-----------------------------------------------");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.setOption(SocketOption.LINGER, 0);
            receiver.setOption(SocketOption.LINGER, 0);

            receiver.bind("tcp://127.0.0.1:20003");
            sender.connect("tcp://127.0.0.1:20003");

            sleep(100);

            // Build multipart message with mixed content types
            MultipartMessage message = new MultipartMessage();
            message.addString("Command");
            message.add(new byte[0]); // Empty delimiter frame
            message.add(new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}); // Binary data
            message.addString("Payload");

            sender.sendMultipart(message);
            System.out.println("Sent MultipartMessage with " + message.size() + " frames");

            // Receive and display - using .value() to extract from RecvResult
            String cmd = receiver.recvString().value();
            byte[] delimiter = receiver.recvBytes().value();
            byte[] binary = receiver.recvBytes().value();
            String payload = receiver.recvString().value();

            System.out.println("Command: " + cmd);
            System.out.println("Delimiter: " + (delimiter.length == 0 ? "empty" : "not empty"));
            System.out.print("Binary: [");
            for (int i = 0; i < binary.length; i++) {
                System.out.print(String.format("0x%02X", binary[i]));
                if (i < binary.length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
            System.out.println("Payload: " + payload);
            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 3: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 4: Receive multipart using the new Result API (blocking mode).
     *
     * <p>Demonstrates using RecvResult with functional-style processing. In blocking mode,
     * recvMultipart() will always succeed (or throw on error), so the Result will always
     * be present. However, using the Result API provides consistency and future-proofing.</p>
     */
    private static void example4_RecvMultipart() {
        System.out.println("Example 4: recvMultipart with Result API (blocking)");
        System.out.println("----------------------------------------------------");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.setOption(SocketOption.LINGER, 0);
            receiver.setOption(SocketOption.LINGER, 0);

            receiver.bind("tcp://127.0.0.1:20004");
            sender.connect("tcp://127.0.0.1:20004");

            sleep(100);

            // Send multipart message
            MultipartMessage sendMsg = new MultipartMessage();
            sendMsg.addString("Part1");
            sendMsg.addString("Part2");
            sendMsg.addString("Part3");
            sender.sendMultipart(sendMsg);
            System.out.println("Sent: Part1, Part2, Part3");

            // NEW API: recvMultipart() returns RecvResult<MultipartMessage>
            // In blocking mode, this will block until a message arrives
            RecvResult<MultipartMessage> result = receiver.recvMultipart();

            // Functional-style processing with ifPresent()
            result.ifPresent(msg -> {
                System.out.println("Received " + msg.size() + " frames:");
                for (int i = 0; i < msg.size(); i++) {
                    String frameText = msg.getString(i);
                    System.out.println("  Frame " + (i + 1) + ": " + frameText);
                }
            });

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 4: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 5: Non-blocking receive with the Result API.
     *
     * <p>This is where the Result API truly shines! Demonstrates the key benefits:</p>
     * <ul>
     *   <li>Type-safe distinction between "would block" and success</li>
     *   <li>Functional-style processing with ifPresent()</li>
     *   <li>Explicit wouldBlock() checks for clarity</li>
     *   <li>No null pointer risks</li>
     * </ul>
     *
     * <p>Compare this to the old API which returned null for would-block,
     * requiring null checks and lacking semantic clarity.</p>
     */
    private static void example5_TryRecvMultipart() {
        System.out.println("Example 5: Result API for non-blocking receive");
        System.out.println("------------------------------------------------");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.setOption(SocketOption.LINGER, 0);
            receiver.setOption(SocketOption.LINGER, 0);

            receiver.bind("tcp://127.0.0.1:20005");
            sender.connect("tcp://127.0.0.1:20005");

            sleep(100);

            // Try to receive when no message is available
            // NEW API: Returns RecvResult instead of nullable MultipartMessage
            RecvResult<MultipartMessage> result1 = receiver.recvMultipart();

            // Pattern 1: Use wouldBlock() for explicit checking
            if (result1.wouldBlock()) {
                System.out.println("First try: Would block (no message available)");
            } else {
                System.out.println("First try: Received message");
            }

            // Send a message
            MultipartMessage sendMsg = new MultipartMessage();
            sendMsg.addString("Data1");
            sendMsg.addString("Data2");
            sender.sendMultipart(sendMsg);

            sleep(50);

            // Try to receive when message is available
            RecvResult<MultipartMessage> result2 = receiver.recvMultipart();

            // Pattern 2: Use isPresent() for checking
            System.out.println("Second try: " + (result2.isPresent() ? "Success" : "Would block"));

            // Pattern 3: Functional-style processing with ifPresent()
            // This is the recommended approach - clean and concise
            result2.ifPresent(msg -> {
                System.out.println("Received " + msg.size() + " frames:");
                for (int i = 0; i < msg.size(); i++) {
                    String frameText = msg.getString(i);
                    System.out.println("  Frame " + (i + 1) + ": " + frameText);
                }
            });

            // Pattern 4: You can also chain transformations with map()
            RecvResult<Integer> frameCount = result2.map(MultipartMessage::size);
            frameCount.ifPresent(count ->
                System.out.println("Total frames in message: " + count));

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 5: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 6: Router-Dealer pattern with the new SendFlags API and Result API.
     *
     * <p>Demonstrates a complete request-reply cycle using modern APIs:</p>
     * <ul>
     *   <li>SendFlags.SEND_MORE for multipart message construction</li>
     *   <li>RecvResult with functional-style processing</li>
     *   <li>Router socket identity envelope handling</li>
     * </ul>
     *
     * <p>This pattern is commonly used in broker architectures where the Router
     * maintains connection identity and routes messages to specific Dealers.</p>
     */
    private static void example6_RouterDealerWithExtensions() {
        System.out.println("Example 6: Router-Dealer with modern API");
        System.out.println("-----------------------------------------");

        try (Context ctx = new Context();
             Socket router = new Socket(ctx, SocketType.ROUTER);
             Socket dealer = new Socket(ctx, SocketType.DEALER)) {

            router.setOption(SocketOption.LINGER, 0);
            dealer.setOption(SocketOption.LINGER, 0);

            router.bind("tcp://127.0.0.1:20006");
            dealer.connect("tcp://127.0.0.1:20006");

            sleep(100);

            // Dealer sends request using MultipartMessage (unchanged)
            MultipartMessage request = new MultipartMessage();
            request.addString("REQUEST");
            request.addString("GetData");
            dealer.sendMultipart(request);
            System.out.println("Dealer sent: REQUEST, GetData");

            // Router receives with Result API
            RecvResult<MultipartMessage> requestResult = router.recvMultipart();

            // Process the received request
            requestResult.ifPresent(receivedRequest -> {
                System.out.println("Router received " + receivedRequest.size() + " frames:");
                System.out.println("  Identity: " + receivedRequest.get(0).length + " bytes");
                System.out.println("  Frame 1: " + receivedRequest.getString(1));
                System.out.println("  Frame 2: " + receivedRequest.getString(2));

                // Router replies by echoing identity and adding response
                // Using SendFlags.SEND_MORE for explicit frame control
                byte[] identity = receivedRequest.get(0);

                try {
                    // NEW API: Use SendFlags.SEND_MORE instead of sendMore()
                    router.send(identity, SendFlags.SEND_MORE);
                    router.send("RESPONSE", SendFlags.SEND_MORE);
                    router.send("DataPayload", SendFlags.NONE);
                    System.out.println("Router replied with RESPONSE, DataPayload");
                } catch (Exception e) {
                    System.err.println("Error sending router reply: " + e.getMessage());
                }
            });

            // Small delay to ensure router sends before dealer receives
            sleep(50);

            // Dealer receives response (identity stripped by Router)
            RecvResult<MultipartMessage> responseResult = dealer.recvMultipart();

            // Pattern: Chain map() and ifPresent() for transformation + action
            responseResult
                .map(msg -> {
                    // Transform to a simple string representation
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < msg.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(msg.getString(i));
                    }
                    return sb.toString();
                })
                .ifPresent(frames ->
                    System.out.println("Dealer received response: " + frames));

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 6: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to sleep without throwing checked exceptions.
     *
     * @param millis Milliseconds to sleep
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Sleep interrupted");
        }
    }
}
