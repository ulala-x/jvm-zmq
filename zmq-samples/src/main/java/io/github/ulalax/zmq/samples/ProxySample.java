package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.Proxy;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;
import io.github.ulalax.zmq.core.ZmqException;

/**
 * XPub-XSub Proxy Pattern Sample
 *
 * <p>Demonstrates the ZeroMQ proxy pattern with XPub-XSub sockets for building
 * a message broker that connects multiple publishers to multiple subscribers.</p>
 *
 * <p>Architecture:</p>
 * <pre>
 *   Publishers -> XSub (Frontend) -> Proxy -> XPub (Backend) -> Subscribers
 * </pre>
 *
 * <p>This sample demonstrates:</p>
 * <ul>
 *   <li>XSub socket receiving from multiple publishers</li>
 *   <li>XPub socket distributing to multiple subscribers</li>
 *   <li>Built-in Proxy forwarding messages and subscriptions</li>
 *   <li>Dynamic subscription handling</li>
 *   <li>Topic-based message filtering through the proxy</li>
 * </ul>
 *
 * <p>The proxy pattern is ideal for:</p>
 * <ul>
 *   <li>Building message brokers and queue devices</li>
 *   <li>Decoupling publishers from subscribers</li>
 *   <li>Load balancing across multiple publishers/subscribers</li>
 *   <li>Creating scalable pub-sub architectures</li>
 * </ul>
 *
 * <p>In this sample:</p>
 * <ul>
 *   <li>Publisher-1 sends messages on "weather" topic</li>
 *   <li>Publisher-2 sends messages on "sports" topic</li>
 *   <li>Subscriber-1 receives only "weather" messages</li>
 *   <li>Subscriber-2 receives only "sports" messages</li>
 *   <li>Subscriber-3 receives both "weather" and "sports" messages</li>
 * </ul>
 *
 * <p>The proxy automatically forwards subscription requests from subscribers
 * back to publishers, enabling efficient message filtering.</p>
 */
public class ProxySample {

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ XPub-XSub Proxy Pattern Sample");
        System.out.println("======================================");
        System.out.println();
        System.out.println("Architecture:");
        System.out.println("  Publishers -> XSub (Frontend) -> Proxy -> XPub (Backend) -> Subscribers");
        System.out.println();
        System.out.println("This sample demonstrates:");
        System.out.println("  - XSub socket receiving from multiple publishers");
        System.out.println("  - XPub socket distributing to multiple subscribers");
        System.out.println("  - Built-in Proxy forwarding messages and subscriptions");
        System.out.println("  - Dynamic subscription handling");
        System.out.println();

        // Create shared context for all sockets
        try (Context context = new Context()) {
            // Start the proxy in a background thread
            Thread proxyThread = new Thread(() -> runProxy(context));
            proxyThread.setDaemon(true);
            proxyThread.setName("Proxy-Thread");
            proxyThread.start();

            // Allow proxy to initialize
            sleep(500);

            // Start publishers
            Thread publisher1Thread = new Thread(() ->
                runPublisher(context, "Publisher-1", "weather", 5557));
            publisher1Thread.setDaemon(true);
            publisher1Thread.setName("Publisher1-Thread");

            Thread publisher2Thread = new Thread(() ->
                runPublisher(context, "Publisher-2", "sports", 5558));
            publisher2Thread.setDaemon(true);
            publisher2Thread.setName("Publisher2-Thread");

            publisher1Thread.start();
            publisher2Thread.start();

            // Allow publishers to initialize
            sleep(500);

            // Start subscribers
            Thread subscriber1Thread = new Thread(() ->
                runSubscriber(context, "Subscriber-1", new String[]{"weather"}));
            subscriber1Thread.setDaemon(true);
            subscriber1Thread.setName("Subscriber1-Thread");

            Thread subscriber2Thread = new Thread(() ->
                runSubscriber(context, "Subscriber-2", new String[]{"sports"}));
            subscriber2Thread.setDaemon(true);
            subscriber2Thread.setName("Subscriber2-Thread");

            Thread subscriber3Thread = new Thread(() ->
                runSubscriber(context, "Subscriber-3", new String[]{"weather", "sports"}));
            subscriber3Thread.setDaemon(true);
            subscriber3Thread.setName("Subscriber3-Thread");

            subscriber1Thread.start();
            subscriber2Thread.start();
            subscriber3Thread.start();

            // Wait for subscribers to complete
            try {
                subscriber1Thread.join();
                subscriber2Thread.join();
                subscriber3Thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Main thread interrupted");
            }

            System.out.println();
            System.out.println("All subscribers completed. Press Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the XPub-XSub proxy.
     *
     * <p>The proxy:</p>
     * <ul>
     *   <li>Creates an XSub frontend socket for publishers to connect (tcp://*:5559)</li>
     *   <li>Creates an XPub backend socket for subscribers to connect (tcp://*:5560)</li>
     *   <li>Forwards messages from publishers to subscribers</li>
     *   <li>Forwards subscription requests from subscribers to publishers</li>
     * </ul>
     *
     * <p>Note: This method blocks until the context is terminated or an error occurs.</p>
     *
     * @param ctx The ZeroMQ context shared across all sockets
     */
    private static void runProxy(Context ctx) {
        System.out.println("[Proxy] Starting XPub-XSub proxy...");

        try (Socket frontend = new Socket(ctx, SocketType.XSUB);
             Socket backend = new Socket(ctx, SocketType.XPUB)) {

            // Create frontend XSub socket (for publishers)
            frontend.bind("tcp://*:5559");
            System.out.println("[Proxy] Frontend XSub bound to tcp://*:5559 (for publishers)");

            // Create backend XPub socket (for subscribers)
            backend.bind("tcp://*:5560");
            System.out.println("[Proxy] Backend XPub bound to tcp://*:5560 (for subscribers)");
            System.out.println("[Proxy] Proxy running - forwarding messages and subscriptions...");
            System.out.println();

            // Start the built-in proxy
            // This blocks and forwards messages from frontend to backend
            // and subscriptions from backend to frontend
            Proxy.start(frontend, backend);

        } catch (Exception e) {
            System.err.println("[Proxy] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs a publisher that sends messages on a specific topic.
     *
     * <p>The publisher:</p>
     * <ul>
     *   <li>Creates a PUB socket</li>
     *   <li>Connects to the proxy's frontend XSub socket</li>
     *   <li>Publishes 10 messages on the specified topic</li>
     * </ul>
     *
     * @param ctx The ZeroMQ context
     * @param name The publisher name for logging
     * @param topic The topic to publish on
     * @param directPort The direct port (not used in proxy pattern, kept for compatibility)
     */
    private static void runPublisher(Context ctx, String name, String topic, int directPort) {
        System.out.println("[" + name + "] Starting...");

        try (Socket socket = new Socket(ctx, SocketType.PUB)) {
            socket.setOption(SocketOption.LINGER, 0);

            // Connect to the proxy's frontend XSub
            socket.connect("tcp://localhost:5559");
            System.out.println("[" + name + "] Connected to proxy frontend (tcp://localhost:5559)");
            System.out.println("[" + name + "] Publishing topic: '" + topic + "'");
            System.out.println();

            // Allow time for connection
            sleep(1000);

            // Publish messages
            for (int i = 1; i <= 10; i++) {
                String message = topic + " Update #" + i + " from " + name;
                socket.send(message);
                System.out.println("[" + name + "] Sent: " + message);
                sleep(800);
            }

            System.out.println("[" + name + "] Publishing completed");

        } catch (Exception e) {
            System.err.println("[" + name + "] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs a subscriber that receives messages from specific topics.
     *
     * <p>The subscriber:</p>
     * <ul>
     *   <li>Creates a SUB socket</li>
     *   <li>Connects to the proxy's backend XPub socket</li>
     *   <li>Subscribes to the specified topics</li>
     *   <li>Receives and displays filtered messages</li>
     * </ul>
     *
     * <p>The subscriber will receive approximately 10 messages per subscribed topic.
     * It uses a 15-second timeout to detect when no more messages are available.</p>
     *
     * @param ctx The ZeroMQ context
     * @param name The subscriber name for logging
     * @param topics The topics to subscribe to
     */
    private static void runSubscriber(Context ctx, String name, String[] topics) {
        System.out.println("[" + name + "] Starting...");

        try (Socket socket = new Socket(ctx, SocketType.SUB)) {
            socket.setOption(SocketOption.LINGER, 0);
            socket.setOption(SocketOption.RCVTIMEO, 15000); // 15 second timeout

            // Connect to the proxy's backend XPub
            socket.connect("tcp://localhost:5560");
            System.out.println("[" + name + "] Connected to proxy backend (tcp://localhost:5560)");

            // Subscribe to topics
            for (String topic : topics) {
                socket.subscribe(topic);
                System.out.println("[" + name + "] Subscribed to topic: '" + topic + "'");
            }
            System.out.println();

            // Allow subscriptions to propagate
            sleep(500);

            int messageCount = 0;
            int maxMessages = 15; // Expect ~10 messages per topic

            while (messageCount < maxMessages) {
                try {
                    String message = socket.recvString();
                    messageCount++;
                    System.out.println("[" + name + "] Received: " + message);
                } catch (ZmqException ex) {
                    // EAGAIN indicates timeout (no message received)
                    if (ex.isAgain()) {
                        System.out.println("[" + name + "] Timeout - no more messages");
                        break;
                    }
                    throw ex; // Re-throw if it's a different error
                }
            }

            System.out.println("[" + name + "] Received " + messageCount + " messages. Unsubscribing...");

            // Unsubscribe from topics (demonstrates dynamic subscription handling)
            for (String topic : topics) {
                socket.unsubscribe(topic);
                System.out.println("[" + name + "] Unsubscribed from topic: '" + topic + "'");
            }

            System.out.println("[" + name + "] Completed");

        } catch (Exception e) {
            System.err.println("[" + name + "] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to sleep with proper exception handling.
     *
     * @param millis The number of milliseconds to sleep
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread interrupted during sleep");
        }
    }
}
