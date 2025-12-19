package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.PollEvents;
import io.github.ulalax.zmq.Poller;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;

import java.time.Duration;

/**
 * Instance-based Poller Sample
 *
 * <p>Demonstrates the new instance-based Poller API optimized for high-frequency polling.
 * This sample shows how to:</p>
 * <ul>
 *   <li>Create and reuse a Poller instance for multiple poll operations</li>
 *   <li>Register multiple sockets with different events</li>
 *   <li>Poll efficiently without reallocating native memory</li>
 *   <li>Dynamically modify polling events</li>
 *   <li>Unregister sockets when no longer needed</li>
 * </ul>
 *
 * <h2>Performance Benefits</h2>
 * <p>Unlike the static Poller.poll() API which allocates native memory on every call,
 * the instance-based approach maintains Arena and MemorySegment across poll operations.
 * This significantly reduces allocation overhead in high-frequency polling scenarios.</p>
 */
public class InstancePollerSample {

    private static final String RECEIVER1_ADDRESS = "tcp://*:5571";
    private static final String RECEIVER2_ADDRESS = "tcp://*:5572";
    private static final String SENDER1_ADDRESS = "tcp://localhost:5571";
    private static final String SENDER2_ADDRESS = "tcp://localhost:5572";

    private static final int SENDER1_INTERVAL_MS = 200;
    private static final int SENDER2_INTERVAL_MS = 400;
    private static final int MAX_MESSAGES = 30;
    private static final int MESSAGES_PER_SENDER = 15;

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ Instance-based Poller Sample");
        System.out.println("====================================");
        System.out.println();
        System.out.println("This sample demonstrates:");
        System.out.println("  - Instance-based Poller API for high-frequency polling");
        System.out.println("  - Efficient native memory reuse across poll operations");
        System.out.println("  - Dynamic socket registration and modification");
        System.out.println("  - AutoCloseable resource management");
        System.out.println();

        try (Context context = new Context();
             Socket receiver1 = new Socket(context, SocketType.PULL);
             Socket receiver2 = new Socket(context, SocketType.PULL);
             Poller poller = new Poller()) { // Instance-based Poller with try-with-resources

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

            // Register sockets with the poller instance
            int idx1 = poller.register(receiver1, PollEvents.IN);
            int idx2 = poller.register(receiver2, PollEvents.IN);

            System.out.println("[Main] Registered " + poller.size() + " sockets with poller");
            System.out.println("[Main] Socket 1 index: " + idx1);
            System.out.println("[Main] Socket 2 index: " + idx2);
            System.out.println("[Main] Starting to poll...");
            System.out.println();

            int totalMessages = 0;
            int receiver1Count = 0;
            int receiver2Count = 0;

            while (totalMessages < MAX_MESSAGES) {
                // Poll with Duration API (100ms timeout)
                // Native memory is NOT reallocated here - very efficient!
                int ready = poller.poll(Duration.ofMillis(100));

                if (ready == 0) {
                    System.out.println("[Main] Poll timeout - no messages");
                    continue;
                }

                if (ready < 0) {
                    System.err.println("[Main] Poll error");
                    break;
                }

                // Check receiver 1 using index
                if (poller.isReadable(idx1)) {
                    String msg = receiver1.recvString().value();
                    receiver1Count++;
                    System.out.println("[Receiver-1] (#" + receiver1Count + ") " + msg);
                    totalMessages++;
                }

                // Check receiver 2 using index
                if (poller.isReadable(idx2)) {
                    String msg = receiver2.recvString().value();
                    receiver2Count++;
                    System.out.println("[Receiver-2] (#" + receiver2Count + ") " + msg);
                    totalMessages++;

                    // Demonstrate dynamic event modification after 5 messages
                    if (receiver2Count == 5) {
                        System.out.println();
                        System.out.println("[Main] Modifying receiver2 to poll for OUT events (demo only)");
                        poller.modify(idx2, PollEvents.OUT);
                        // Immediately change it back for the demo to continue
                        poller.modify(idx2, PollEvents.IN);
                        System.out.println("[Main] Changed back to IN events");
                        System.out.println();
                    }
                }

                // Show poll event details
                if (ready > 0 && totalMessages % 10 == 0) {
                    System.out.println("[Main] Progress: " + totalMessages + "/" + MAX_MESSAGES +
                            " messages (R1: " + receiver1Count + ", R2: " + receiver2Count + ")");
                }
            }

            System.out.println();
            System.out.println("=== Statistics ===");
            System.out.println("Total messages: " + totalMessages);
            System.out.println("Receiver 1: " + receiver1Count + " messages");
            System.out.println("Receiver 2: " + receiver2Count + " messages");
            System.out.println("Poller size: " + poller.size() + " sockets");
            System.out.println();
            System.out.println("[Main] Done - Poller will be closed automatically");

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
