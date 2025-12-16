package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.MultipartMessage;
import io.github.ulalax.zmq.SendFlags;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;

import java.util.ArrayList;
import java.util.List;

/**
 * Multipart Message Extensions Sample
 *
 * <p>Demonstrates the Socket extension methods for convenient multipart message handling.
 * Multipart messages allow you to send multiple frames as a single atomic message.
 * All frames are delivered together or not at all.</p>
 *
 * <p>This sample includes six examples:</p>
 * <ul>
 *   <li>Example 1: Send multipart using string parameters (convenient for simple cases)</li>
 *   <li>Example 2: Send multipart using byte arrays</li>
 *   <li>Example 3: Send multipart using MultipartMessage container</li>
 *   <li>Example 4: Receive multipart using recvMultipart (blocking)</li>
 *   <li>Example 5: Receive multipart using tryRecvMultipart (non-blocking)</li>
 *   <li>Example 6: Router-Dealer pattern with multipart messages</li>
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
     * Example 1: Send multipart using string params (most convenient for simple cases).
     */
    private static void example1_SendMultipartWithStrings() {
        System.out.println("Example 1: Send multipart with string parts");
        System.out.println("--------------------------------------------");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.setOption(SocketOption.LINGER, 0);
            receiver.setOption(SocketOption.LINGER, 0);

            receiver.bind("tcp://127.0.0.1:20001");
            sender.connect("tcp://127.0.0.1:20001");

            sleep(100);

            // Send multipart message with simple string parts
            sender.send("Header", SendFlags.SEND_MORE);
            sender.send("Body", SendFlags.SEND_MORE);
            sender.send("Footer");
            System.out.println("Sent: Header, Body, Footer");

            // Receive using traditional method
            String header = receiver.recvString();
            String body = receiver.recvString();
            String footer = receiver.recvString();

            System.out.println("Received: " + header + ", " + body + ", " + footer);
            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 1: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 2: Send multipart using byte arrays.
     */
    private static void example2_SendMultipartWithByteArrays() {
        System.out.println("Example 2: Send multipart with byte arrays");
        System.out.println("------------------------------------------");

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

            // Send multipart message
            for (int i = 0; i < frames.size(); i++) {
                boolean isLast = (i == frames.size() - 1);
                SendFlags flags = isLast ? SendFlags.NONE : SendFlags.SEND_MORE;
                sender.send(frames.get(i), flags);
            }
            System.out.println("Sent " + frames.size() + " binary frames");

            // Receive frames
            for (int i = 0; i < frames.size(); i++) {
                byte[] frame = receiver.recvBytes();
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

            // Receive and display
            String cmd = receiver.recvString();
            byte[] delimiter = receiver.recvBytes();
            byte[] binary = receiver.recvBytes();
            String payload = receiver.recvString();

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
     * Example 4: Receive multipart using recvMultipart (blocking).
     */
    private static void example4_RecvMultipart() {
        System.out.println("Example 4: recvMultipart (blocking receive)");
        System.out.println("--------------------------------------------");

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

            // Receive complete message in one call
            MultipartMessage received = receiver.recvMultipart();
            System.out.println("Received " + received.size() + " frames:");

            for (int i = 0; i < received.size(); i++) {
                String frameText = received.getString(i);
                System.out.println("  Frame " + (i + 1) + ": " + frameText);
            }

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 4: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 5: Non-blocking receive with tryRecvMultipart.
     */
    private static void example5_TryRecvMultipart() {
        System.out.println("Example 5: tryRecvMultipart (non-blocking receive)");
        System.out.println("---------------------------------------------------");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            sender.setOption(SocketOption.LINGER, 0);
            receiver.setOption(SocketOption.LINGER, 0);

            receiver.bind("tcp://127.0.0.1:20005");
            sender.connect("tcp://127.0.0.1:20005");

            sleep(100);

            // Try to receive when no message is available
            MultipartMessage message1 = receiver.tryRecvMultipart();
            System.out.println("First try (no message): " + (message1 != null ? "Success" : "Would block"));

            // Send a message
            MultipartMessage sendMsg = new MultipartMessage();
            sendMsg.addString("Data1");
            sendMsg.addString("Data2");
            sender.sendMultipart(sendMsg);

            sleep(50);

            // Try to receive when message is available
            MultipartMessage message2 = receiver.tryRecvMultipart();
            System.out.println("Second try (message available): " + (message2 != null ? "Success" : "Would block"));

            if (message2 != null) {
                System.out.println("Received " + message2.size() + " frames:");
                for (int i = 0; i < message2.size(); i++) {
                    String frameText = message2.getString(i);
                    System.out.println("  Frame " + (i + 1) + ": " + frameText);
                }
            }

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in Example 5: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 6: Router-Dealer pattern with multipart extensions.
     * Demonstrates how extension methods simplify envelope handling.
     */
    private static void example6_RouterDealerWithExtensions() {
        System.out.println("Example 6: Router-Dealer with extension methods");
        System.out.println("------------------------------------------------");

        try (Context ctx = new Context();
             Socket router = new Socket(ctx, SocketType.ROUTER);
             Socket dealer = new Socket(ctx, SocketType.DEALER)) {

            router.setOption(SocketOption.LINGER, 0);
            dealer.setOption(SocketOption.LINGER, 0);

            router.bind("tcp://127.0.0.1:20006");
            dealer.connect("tcp://127.0.0.1:20006");

            sleep(100);

            // Dealer sends request
            MultipartMessage request = new MultipartMessage();
            request.addString("REQUEST");
            request.addString("GetData");
            dealer.sendMultipart(request);
            System.out.println("Dealer sent: REQUEST, GetData");

            // Router receives with automatic identity envelope
            MultipartMessage receivedRequest = router.recvMultipart();
            System.out.println("Router received " + receivedRequest.size() + " frames:");
            System.out.println("  Identity: " + receivedRequest.get(0).length + " bytes");
            System.out.println("  Frame 1: " + receivedRequest.getString(1));
            System.out.println("  Frame 2: " + receivedRequest.getString(2));

            // Router replies by echoing identity and adding response
            byte[] identity = receivedRequest.get(0);
            MultipartMessage reply = new MultipartMessage();
            reply.add(identity);
            reply.addString("RESPONSE");
            reply.addString("DataPayload");
            router.sendMultipart(reply);
            System.out.println("Router replied with RESPONSE, DataPayload");

            // Dealer receives response (identity stripped by Router)
            MultipartMessage response = dealer.recvMultipart();
            System.out.println("Dealer received " + response.size() + " frames:");
            for (int i = 0; i < response.size(); i++) {
                String frameText = response.getString(i);
                System.out.println("  Frame " + (i + 1) + ": " + frameText);
            }

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
