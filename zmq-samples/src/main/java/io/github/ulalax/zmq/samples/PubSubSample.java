package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;
import io.github.ulalax.zmq.core.ZmqException;

/**
 * PUB-SUB Pattern Sample
 *
 * <p>Demonstrates the ZeroMQ publish-subscribe pattern with topic filtering.
 * The publisher broadcasts messages on different topics (weather, sports, news),
 * and the subscriber receives only messages matching subscribed topics (weather, news).</p>
 *
 * <p>This pattern is ideal for:</p>
 * <ul>
 *   <li>Broadcasting updates to multiple subscribers</li>
 *   <li>Event notification systems</li>
 *   <li>Real-time data distribution</li>
 *   <li>Topic-based message filtering</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>Publisher mode: java PubSubSample pub</li>
 *   <li>Subscriber mode: java PubSubSample sub</li>
 *   <li>Both (default): java PubSubSample both</li>
 * </ul>
 *
 * <p>Note: In PUB-SUB pattern, subscribers may miss initial messages if they connect
 * after the publisher has started sending. This is expected behavior due to the
 * asynchronous nature of the pattern.</p>
 */
public class PubSubSample {

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ PUB-SUB Sample");
        System.out.println("=====================");
        System.out.println();

        String mode = args.length > 0 ? args[0].toLowerCase() : "both";

        if (mode.equals("pub") || mode.equals("both")) {
            if (mode.equals("both")) {
                // Run publisher in a separate thread
                Thread publisherThread = new Thread(PubSubSample::runPublisher);
                publisherThread.start();

                // Wait for publisher to start and bind
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while waiting for publisher to start");
                    return;
                }
            } else {
                runPublisher();
                return;
            }
        }

        if (mode.equals("sub") || mode.equals("both")) {
            runSubscriber();
        }
    }

    /**
     * Runs the publisher that broadcasts messages on multiple topics.
     *
     * <p>The publisher:</p>
     * <ul>
     *   <li>Creates a PUB socket and binds to port 5556</li>
     *   <li>Sends 10 messages across 3 topics (weather, sports, news)</li>
     *   <li>Each message is prefixed with the topic name</li>
     * </ul>
     */
    private static void runPublisher() {
        System.out.println("[Publisher] Starting...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.PUB)) {

            socket.setOption(SocketOption.LINGER, 0);
            socket.bind("tcp://*:5556");
            System.out.println("[Publisher] Binding to tcp://*:5556");

            // Allow subscribers to connect
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Publisher] Interrupted during startup delay");
                return;
            }

            String[] topics = {"weather", "sports", "news"};
            for (int i = 0; i < 10; i++) {
                String topic = topics[i % topics.length];
                String message = topic + " Update #" + (i + 1);
                socket.send(message);
                System.out.println("[Publisher] Sent: " + message);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("[Publisher] Interrupted during message sending");
                    break;
                }
            }

            System.out.println("[Publisher] Done");

        } catch (Exception e) {
            System.err.println("[Publisher] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the subscriber that receives messages from specific topics.
     *
     * <p>The subscriber:</p>
     * <ul>
     *   <li>Creates a SUB socket and connects to the publisher</li>
     *   <li>Subscribes to "weather" and "news" topics (filters out "sports")</li>
     *   <li>Sets a 2-second receive timeout to prevent indefinite blocking</li>
     *   <li>Receives and displays filtered messages</li>
     * </ul>
     *
     * <p>Note: The subscriber only receives messages that match the subscribed
     * topic prefixes. Messages starting with "sports" will be filtered out.</p>
     */
    private static void runSubscriber() {
        System.out.println("[Subscriber] Starting...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.SUB)) {

            socket.setOption(SocketOption.LINGER, 0);
            socket.setOption(SocketOption.RCVTIMEO, 2000);
            socket.connect("tcp://localhost:5556");

            // Subscribe to specific topics
            socket.subscribe("weather");
            socket.subscribe("news");
            System.out.println("[Subscriber] Subscribed to 'weather' and 'news' topics");

            for (int i = 0; i < 10; i++) {
                // With Result API, wouldBlock() is cleaner than catching exceptions
                var result = socket.recvString();
                if (result.wouldBlock()) {
                    System.out.println("[Subscriber] Timeout, no message received");
                    break;
                }
                System.out.println("[Subscriber] Received: " + result.value());
            }

            System.out.println("[Subscriber] Done");

        } catch (Exception e) {
            System.err.println("[Subscriber] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
