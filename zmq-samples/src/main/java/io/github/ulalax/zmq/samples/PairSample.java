package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;

/**
 * PAIR Pattern Sample
 *
 * <p>Demonstrates 1:1 bidirectional communication using ZeroMQ PAIR sockets.
 * PAIR sockets are used for exclusive pair communication where one socket
 * connects to exactly one other socket.</p>
 *
 * <p>This pattern is ideal for:</p>
 * <ul>
 *   <li>Inproc communication between threads in the same process</li>
 *   <li>Exclusive bidirectional data exchange</li>
 *   <li>Simple peer-to-peer messaging without routing</li>
 *   <li>Coordination between two specific components</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>
 * java PairSample
 * </pre>
 *
 * <p>Note: PAIR sockets do not perform any load balancing or routing.
 * They are designed for 1:1 exclusive connections only. For many-to-many
 * communication, consider using PUB-SUB or PUSH-PULL patterns.</p>
 */
public class PairSample {

    private static final String ENDPOINT = "inproc://pair-example";

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ PAIR Socket Sample");
        System.out.println("==========================");
        System.out.println();
        System.out.println("Demonstrating 1:1 bidirectional communication using inproc transport");
        System.out.println();

        // Create a shared context for inproc communication
        // Note: For inproc transport, both sockets must share the same context
        try (Context ctx = new Context()) {
            // Create two threads with Pair sockets communicating via inproc
            Thread thread1 = new Thread(() -> runPairA(ctx));
            Thread thread2 = new Thread(() -> runPairB(ctx));

            thread1.start();
            thread2.start();

            // Wait for both threads to complete
            try {
                thread1.join();
                thread2.join();

                System.out.println();
                System.out.println("Sample completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Main thread interrupted");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs Pair-A which binds to the inproc endpoint and initiates the conversation.
     *
     * <p>Pair-A:</p>
     * <ul>
     *   <li>Creates a PAIR socket and binds to inproc://pair-example</li>
     *   <li>Sends the first message to Pair-B</li>
     *   <li>Receives response from Pair-B</li>
     *   <li>Continues bidirectional message exchange</li>
     * </ul>
     *
     * @param ctx The shared ZeroMQ context
     */
    private static void runPairA(Context ctx) {
        System.out.println("[Pair-A] Starting...");

        try (Socket socket = new Socket(ctx, SocketType.PAIR)) {

            socket.setOption(SocketOption.LINGER, 0);
            socket.bind(ENDPOINT);
            System.out.println("[Pair-A] Bound to " + ENDPOINT);

            // Allow time for the other socket to connect
            sleep(100);

            // Send initial message
            String message1 = "Hello from Pair-A";
            socket.send(message1);
            System.out.println("[Pair-A] Sent: " + message1);

            // Receive response
            String received1 = socket.recvString().value();
            System.out.println("[Pair-A] Received: " + received1);

            // Send another message
            String message2 = "How are you, Pair-B?";
            socket.send(message2);
            System.out.println("[Pair-A] Sent: " + message2);

            // Receive final response
            String received2 = socket.recvString().value();
            System.out.println("[Pair-A] Received: " + received2);

            // Send final message
            String message3 = "Goodbye from Pair-A";
            socket.send(message3);
            System.out.println("[Pair-A] Sent: " + message3);

            System.out.println("[Pair-A] Done");

        } catch (Exception e) {
            System.err.println("[Pair-A] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs Pair-B which connects to the inproc endpoint and responds to Pair-A.
     *
     * <p>Pair-B:</p>
     * <ul>
     *   <li>Creates a PAIR socket and connects to inproc://pair-example</li>
     *   <li>Receives messages from Pair-A</li>
     *   <li>Sends responses back to Pair-A</li>
     *   <li>Participates in bidirectional conversation</li>
     * </ul>
     *
     * @param ctx The shared ZeroMQ context
     */
    private static void runPairB(Context ctx) {
        System.out.println("[Pair-B] Starting...");

        try (Socket socket = new Socket(ctx, SocketType.PAIR)) {

            socket.setOption(SocketOption.LINGER, 0);

            // Wait briefly to ensure Pair-A has bound
            sleep(50);

            socket.connect(ENDPOINT);
            System.out.println("[Pair-B] Connected to " + ENDPOINT);

            // Receive first message
            String received1 = socket.recvString().value();
            System.out.println("[Pair-B] Received: " + received1);

            // Send response
            String message1 = "Hi Pair-A, this is Pair-B";
            socket.send(message1);
            System.out.println("[Pair-B] Sent: " + message1);

            // Receive second message
            String received2 = socket.recvString().value();
            System.out.println("[Pair-B] Received: " + received2);

            // Send final response
            String message2 = "I'm doing great! Thanks for asking.";
            socket.send(message2);
            System.out.println("[Pair-B] Sent: " + message2);

            // Receive final message
            String received3 = socket.recvString().value();
            System.out.println("[Pair-B] Received: " + received3);

            System.out.println("[Pair-B] Done");

        } catch (Exception e) {
            System.err.println("[Pair-B] Error: " + e.getMessage());
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
