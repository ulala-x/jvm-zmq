package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;

/**
 * REQ-REP Pattern Sample
 *
 * <p>Demonstrates the ZeroMQ request-reply pattern with a simple client-server model.
 * The server listens on port 5555 and responds to client requests.</p>
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>Server mode: java ReqRepSample server</li>
 *   <li>Client mode: java ReqRepSample client</li>
 *   <li>Both (default): java ReqRepSample both</li>
 * </ul>
 */
public class ReqRepSample {

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ REQ-REP Sample");
        System.out.println("=====================");
        System.out.println();

        String mode = args.length > 0 ? args[0].toLowerCase() : "both";

        if (mode.equals("server") || mode.equals("both")) {
            if (mode.equals("both")) {
                // Run server in a separate thread
                Thread serverThread = new Thread(ReqRepSample::runServer);
                serverThread.start();

                // Wait for server to start
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Interrupted while waiting for server to start");
                    return;
                }
            } else {
                runServer();
                return;
            }
        }

        if (mode.equals("client") || mode.equals("both")) {
            runClient();
        }
    }

    /**
     * Runs the server that listens for requests and sends replies.
     */
    private static void runServer() {
        System.out.println("[Server] Starting...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.REP)) {

            socket.setOption(SocketOption.LINGER, 0);
            socket.bind("tcp://*:5555");
            System.out.println("[Server] Listening on tcp://*:5555");

            for (int i = 0; i < 5; i++) {
                String request = socket.recvString().value();
                System.out.println("[Server] Received: " + request);

                // Simulate processing time
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("[Server] Interrupted during processing");
                    break;
                }

                String reply = "Reply #" + (i + 1);
                socket.send(reply);
                System.out.println("[Server] Sent: " + reply);
            }

            System.out.println("[Server] Done");

        } catch (Exception e) {
            System.err.println("[Server] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the client that sends requests and receives replies.
     */
    private static void runClient() {
        System.out.println("[Client] Starting...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.REQ)) {

            socket.setOption(SocketOption.LINGER, 0);
            socket.connect("tcp://localhost:5555");
            System.out.println("[Client] Connected to tcp://localhost:5555");

            for (int i = 0; i < 5; i++) {
                String request = "Hello #" + (i + 1);
                socket.send(request);
                System.out.println("[Client] Sent: " + request);

                String reply = socket.recvString().value();
                System.out.println("[Client] Received: " + reply);
            }

            System.out.println("[Client] Done");

        } catch (Exception e) {
            System.err.println("[Client] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
