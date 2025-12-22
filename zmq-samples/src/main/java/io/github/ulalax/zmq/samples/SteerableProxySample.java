package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.Proxy;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;
import io.github.ulalax.zmq.core.ZmqException;

/**
 * Steerable Proxy Pattern Sample
 *
 * <p>Demonstrates the ZeroMQ steerable proxy pattern with dynamic control capabilities.
 * A steerable proxy allows runtime control through PAUSE, RESUME, and TERMINATE commands.</p>
 *
 * <p>Architecture:</p>
 * <pre>
 *   Publishers -> XSub (Frontend) -> Proxy -> XPub (Backend) -> Subscribers
 *                                      ^
 *                                      |
 *                                  Control (PAIR)
 * </pre>
 *
 * <p>This sample demonstrates:</p>
 * <ul>
 *   <li>Steerable proxy with control socket (PAIR)</li>
 *   <li>PAUSE command - pauses message flow (messages are queued)</li>
 *   <li>RESUME command - resumes message flow (queued messages are delivered)</li>
 *   <li>TERMINATE command - gracefully terminates the proxy</li>
 *   <li>Dynamic proxy control at runtime</li>
 * </ul>
 *
 * <p>The steerable proxy is useful for:</p>
 * <ul>
 *   <li>Implementing backpressure mechanisms</li>
 *   <li>Maintenance windows (pause traffic)</li>
 *   <li>Graceful shutdown scenarios</li>
 *   <li>Traffic control and flow management</li>
 * </ul>
 *
 * <p>Key Components:</p>
 * <ul>
 *   <li>Frontend (XSub): Receives messages from publishers on tcp://*:5564</li>
 *   <li>Backend (XPub): Distributes messages to subscribers on tcp://*:5565</li>
 *   <li>Control (PAIR): Receives control commands on inproc://proxy-control</li>
 * </ul>
 *
 * <p>Control Commands:</p>
 * <ul>
 *   <li>{@code PAUSE} - Pauses the proxy, queuing incoming messages</li>
 *   <li>{@code RESUME} - Resumes the proxy, delivering queued messages</li>
 *   <li>{@code TERMINATE} - Terminates the proxy gracefully</li>
 * </ul>
 */
public class SteerableProxySample {

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ Steerable Proxy Sample");
        System.out.println("=============================");
        System.out.println();
        System.out.println("This sample demonstrates:");
        System.out.println("  - Steerable proxy with control socket");
        System.out.println("  - PAUSE/RESUME/TERMINATE commands");
        System.out.println("  - Dynamic proxy control at runtime");
        System.out.println();

        // Create shared context for all sockets
        try (Context context = new Context()) {
            // Start proxy in background thread
            Thread proxyThread = new Thread(() -> runSteerableProxy(context));
            proxyThread.setDaemon(true);
            proxyThread.setName("Proxy-Thread");
            proxyThread.start();

            // Allow proxy to initialize
            sleep(500);

            // Start publisher in background thread
            Thread pubThread = new Thread(() -> runPublisher(context));
            pubThread.setDaemon(true);
            pubThread.setName("Publisher-Thread");
            pubThread.start();

            // Allow publisher to initialize
            sleep(500);

            // Start subscriber in background thread
            Thread subThread = new Thread(() -> runSubscriber(context));
            subThread.setDaemon(true);
            subThread.setName("Subscriber-Thread");
            subThread.start();

            // Run control commands (main thread)
            runController(context);

            // Wait for subscriber to complete (with timeout)
            try {
                subThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Main thread interrupted");
            }

            System.out.println();
            System.out.println("[Main] Done");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the steerable proxy.
     *
     * <p>The steerable proxy:</p>
     * <ul>
     *   <li>Creates an XSub frontend socket for publishers (tcp://*:5564)</li>
     *   <li>Creates an XPub backend socket for subscribers (tcp://*:5565)</li>
     *   <li>Creates a PAIR control socket (inproc://proxy-control)</li>
     *   <li>Forwards messages and subscriptions between frontend and backend</li>
     *   <li>Responds to control commands (PAUSE, RESUME, TERMINATE)</li>
     * </ul>
     *
     * <p>The proxy blocks until it receives a TERMINATE command or an error occurs.</p>
     *
     * @param ctx The ZeroMQ context shared across all sockets
     */
    private static void runSteerableProxy(Context ctx) {
        System.out.println("[Proxy] Starting steerable proxy...");

        try (Socket frontend = new Socket(ctx, SocketType.XSUB);
             Socket backend = new Socket(ctx, SocketType.XPUB);
             Socket control = new Socket(ctx, SocketType.PAIR)) {

            // Bind frontend to receive from publishers
            frontend.bind("tcp://*:5564");
            System.out.println("[Proxy] Frontend XSub: tcp://*:5564");

            // Bind backend to send to subscribers
            backend.bind("tcp://*:5565");
            System.out.println("[Proxy] Backend XPub:  tcp://*:5565");

            // Bind control socket
            control.bind("inproc://proxy-control");
            System.out.println("[Proxy] Control:       inproc://proxy-control");
            System.out.println();

            try {
                // Start steerable proxy - blocks until TERMINATE command
                Proxy.startSteerable(frontend, backend, control);
            } catch (Exception ex) {
                System.out.println("[Proxy] Stopped: " + ex.getMessage());
            }

            System.out.println("[Proxy] Terminated");

        } catch (Exception e) {
            System.err.println("[Proxy] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the controller that sends commands to the steerable proxy.
     *
     * <p>The controller demonstrates:</p>
     * <ul>
     *   <li>Connecting to the proxy's control socket</li>
     *   <li>Sending PAUSE to temporarily stop message flow</li>
     *   <li>Sending RESUME to restart message flow</li>
     *   <li>Sending TERMINATE to gracefully stop the proxy</li>
     * </ul>
     *
     * <p>Command sequence:</p>
     * <ol>
     *   <li>Wait 2 seconds - let some messages flow</li>
     *   <li>Send PAUSE - messages will be queued</li>
     *   <li>Wait 2 seconds - demonstrate paused state</li>
     *   <li>Send RESUME - queued messages will be delivered</li>
     *   <li>Wait 2 seconds - let messages flow again</li>
     *   <li>Send TERMINATE - gracefully shutdown proxy</li>
     * </ol>
     *
     * @param ctx The ZeroMQ context
     */
    private static void runController(Context ctx) {
        System.out.println("[Controller] Starting...");

        try (Socket control = new Socket(ctx, SocketType.PAIR)) {
            // Connect to proxy control socket
            control.connect("inproc://proxy-control");
            System.out.println("[Controller] Connected to proxy control socket");
            System.out.println();

            // Let some messages flow
            sleep(2000);

            // Pause the proxy
            System.out.println("[Controller] >>> Sending PAUSE command");
            control.send(Proxy.Commands.PAUSE);
            System.out.println("[Controller] Proxy paused - messages will be queued");
            sleep(2000);

            // Resume the proxy
            System.out.println("[Controller] >>> Sending RESUME command");
            control.send(Proxy.Commands.RESUME);
            System.out.println("[Controller] Proxy resumed - queued messages will flow");
            sleep(2000);

            // Terminate the proxy
            System.out.println("[Controller] >>> Sending TERMINATE command");
            control.send(Proxy.Commands.TERMINATE);
            System.out.println("[Controller] Proxy termination requested");

            System.out.println("[Controller] Done");

        } catch (Exception e) {
            System.err.println("[Controller] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the publisher that sends messages through the proxy.
     *
     * <p>The publisher:</p>
     * <ul>
     *   <li>Creates a PUB socket</li>
     *   <li>Connects to the proxy's frontend XSub socket (tcp://localhost:5564)</li>
     *   <li>Publishes 15 messages on the "news" topic</li>
     *   <li>Sends a message every 500ms</li>
     * </ul>
     *
     * <p>During the PAUSE period, messages will be queued and delivered
     * when the proxy is resumed.</p>
     *
     * @param ctx The ZeroMQ context
     */
    private static void runPublisher(Context ctx) {
        System.out.println("[Publisher] Starting...");

        try (Socket socket = new Socket(ctx, SocketType.PUB)) {
            socket.setOption(SocketOption.LINGER, 0);

            // Connect to proxy frontend
            socket.connect("tcp://localhost:5564");

            // Allow time for connection
            sleep(500);

            // Publish messages
            for (int i = 1; i <= 15; i++) {
                String message = "news Message #" + i;
                try {
                    socket.send(message);
                    System.out.println("[Publisher] Sent: " + message);
                } catch (Exception e) {
                    System.err.println("[Publisher] Error sending: " + e.getMessage());
                    break;
                }
                sleep(500);
            }

            System.out.println("[Publisher] Done");

        } catch (Exception e) {
            System.err.println("[Publisher] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the subscriber that receives messages through the proxy.
     *
     * <p>The subscriber:</p>
     * <ul>
     *   <li>Creates a SUB socket</li>
     *   <li>Connects to the proxy's backend XPub socket (tcp://localhost:5565)</li>
     *   <li>Subscribes to the "news" topic</li>
     *   <li>Receives messages with a 1-second timeout</li>
     *   <li>Stops after 3 consecutive timeouts</li>
     * </ul>
     *
     * <p>The subscriber will notice the pause when messages stop arriving,
     * and will see a burst of messages when the proxy is resumed.</p>
     *
     * @param ctx The ZeroMQ context
     */
    private static void runSubscriber(Context ctx) {
        System.out.println("[Subscriber] Starting...");

        try (Socket socket = new Socket(ctx, SocketType.SUB)) {
            socket.setOption(SocketOption.LINGER, 0);
            socket.setOption(SocketOption.RCVTIMEO, 1000); // 1 second timeout

            // Connect to proxy backend
            socket.connect("tcp://localhost:5565");

            // Subscribe to "news" topic
            socket.subscribe("news");
            System.out.println("[Subscriber] Subscribed to 'news'");
            System.out.println();

            // Allow subscription to propagate
            sleep(500);

            int received = 0;
            int timeouts = 0;

            while (timeouts < 3) {
                try {
                    String message = socket.recvString();
                    System.out.println("[Subscriber] Received: " + message);
                    received++;
                    timeouts = 0; // Reset timeout counter on successful receive
                } catch (ZmqException ex) {
                    // EAGAIN indicates timeout (no message received)
                    if (ex.isAgain()) {
                        timeouts++;
                        if (timeouts < 3) {
                            System.out.println("[Subscriber] Waiting for messages...");
                        }
                    } else {
                        throw ex; // Re-throw if it's a different error
                    }
                } catch (Exception e) {
                    System.err.println("[Subscriber] Error: " + e.getMessage());
                    break;
                }
            }

            System.out.println("[Subscriber] Received " + received + " messages total");
            System.out.println("[Subscriber] Done");

        } catch (Exception e) {
            System.err.println("[Subscriber] Error: " + e.getMessage());
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
