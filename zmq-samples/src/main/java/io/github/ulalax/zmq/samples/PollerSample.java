package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.PollEvents;
import io.github.ulalax.zmq.PollItem;
import io.github.ulalax.zmq.Poller;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;

/**
 * Poller Sample
 *
 * <p>Demonstrates polling multiple sockets simultaneously using ZeroMQ Poller.
 * This sample shows how to:</p>
 * <ul>
 *   <li>Monitor multiple sockets for incoming messages</li>
 *   <li>Handle non-blocking receives with timeout</li>
 *   <li>Process messages from different sources efficiently</li>
 * </ul>
 *
 * <p>The sample creates two PULL sockets listening on different ports,
 * and two sender threads that send messages at different intervals.
 * The main thread uses Poller to efficiently wait for messages from
 * either socket without blocking.</p>
 */
public class PollerSample {

    private static final String RECEIVER1_ADDRESS = "tcp://*:5561";
    private static final String RECEIVER2_ADDRESS = "tcp://*:5562";
    private static final String SENDER1_ADDRESS = "tcp://localhost:5561";
    private static final String SENDER2_ADDRESS = "tcp://localhost:5562";

    private static final int SENDER1_INTERVAL_MS = 300;
    private static final int SENDER2_INTERVAL_MS = 500;
    private static final int MAX_MESSAGES = 20;
    private static final int MESSAGES_PER_SENDER = 10;

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ Poller Sample");
        System.out.println("====================");
        System.out.println();
        System.out.println("This sample demonstrates:");
        System.out.println("  - Polling multiple sockets simultaneously");
        System.out.println("  - Non-blocking receive with timeout");
        System.out.println("  - Handling multiple message sources");
        System.out.println();

        try (Context context = new Context();
             Socket receiver1 = new Socket(context, SocketType.PULL);
             Socket receiver2 = new Socket(context, SocketType.PULL)) {

            // Bind receivers
            receiver1.bind(RECEIVER1_ADDRESS);
            receiver2.bind(RECEIVER2_ADDRESS);

            System.out.println("[Main] Receiver 1 bound to " + RECEIVER1_ADDRESS);
            System.out.println("[Main] Receiver 2 bound to " + RECEIVER2_ADDRESS);
            System.out.println();

            // Start sender threads
            Thread sender1 = new Thread(() ->
                    runSender(context, "Sender-1", SENDER1_ADDRESS, SENDER1_INTERVAL_MS));
            Thread sender2 = new Thread(() ->
                    runSender(context, "Sender-2", SENDER2_ADDRESS, SENDER2_INTERVAL_MS));

            sender1.setDaemon(true);
            sender2.setDaemon(true);
            sender1.start();
            sender2.start();

            // Allow senders to connect
            sleep(500);

            // Create poll items
            PollItem[] pollItems = {
                    new PollItem(receiver1, PollEvents.IN),
                    new PollItem(receiver2, PollEvents.IN)
            };

            System.out.println("[Main] Starting to poll both receivers...");
            System.out.println();

            int totalMessages = 0;

            while (totalMessages < MAX_MESSAGES) {
                // Poll with 1 second timeout
                int ready = Poller.poll(pollItems, 1000);

                if (ready == 0) {
                    System.out.println("[Main] Poll timeout - no messages");
                    continue;
                }

                // Check receiver 1
                if (pollItems[0].isReadable()) {
                    String msg = receiver1.recvString();
                    System.out.println("[Receiver-1] " + msg);
                    totalMessages++;
                }

                // Check receiver 2
                if (pollItems[1].isReadable()) {
                    String msg = receiver2.recvString();
                    System.out.println("[Receiver-2] " + msg);
                    totalMessages++;
                }
            }

            System.out.println();
            System.out.println("[Main] Received " + totalMessages + " total messages");
            System.out.println("[Main] Done");

        } catch (Exception e) {
            System.err.println("[Main] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs a sender thread that pushes messages to a receiver.
     *
     * @param ctx The ZeroMQ context
     * @param name The sender name for logging
     * @param endpoint The endpoint to connect to
     * @param intervalMs The interval between messages in milliseconds
     */
    private static void runSender(Context ctx, String name, String endpoint, int intervalMs) {
        try (Socket socket = new Socket(ctx, SocketType.PUSH)) {
            socket.setOption(SocketOption.LINGER, 0);
            socket.connect(endpoint);

            System.out.println("[" + name + "] Connected to " + endpoint);

            for (int i = 1; i <= MESSAGES_PER_SENDER; i++) {
                String message = "Message #" + i + " from " + name;
                socket.send(message);
                sleep(intervalMs);
            }

            System.out.println("[" + name + "] Done sending");

        } catch (Exception e) {
            System.err.println("[" + name + "] Error: " + e.getMessage());
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
