package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.*;
import io.github.ulalax.zmq.core.ZmqException;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

/**
 * ROUTER-DEALER Async Broker Pattern Sample
 *
 * <p>Demonstrates the asynchronous broker pattern using ZeroMQ ROUTER-DEALER sockets.
 * This pattern implements a message routing system where:</p>
 * <ul>
 *   <li>Broker - Routes messages between clients and workers using ROUTER sockets</li>
 *   <li>Clients - Send requests via DEALER sockets</li>
 *   <li>Workers - Process requests and send replies via DEALER sockets</li>
 * </ul>
 *
 * <p>The broker maintains queues for pending requests and available workers,
 * implementing a load-balancing pattern that matches requests to workers.</p>
 *
 * <p>Message Format:</p>
 * <ul>
 *   <li>Client to Broker: [empty delimiter][request]</li>
 *   <li>Broker to Worker: [empty delimiter][client-identity][empty delimiter][request]</li>
 *   <li>Worker to Broker: [empty delimiter][client-identity][empty delimiter][reply]</li>
 *   <li>Broker to Client: [empty delimiter][reply]</li>
 * </ul>
 */
public class RouterDealerSample {

    // Address configuration
    private static final String FRONTEND_ADDRESS = "tcp://*:5555";
    private static final String BACKEND_ADDRESS = "tcp://*:5556";
    private static final String FRONTEND_CONNECT_ADDRESS = "tcp://localhost:5555";
    private static final String BACKEND_CONNECT_ADDRESS = "tcp://localhost:5556";

    // Client configuration
    private static final int CLIENT_COUNT = 2;
    private static final int REQUESTS_PER_CLIENT = 3;

    // Worker configuration
    private static final int WORKER_COUNT = 2;

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ ROUTER-DEALER Async Broker Sample");
        System.out.println("==========================================");
        System.out.println();
        System.out.println("This sample demonstrates the async broker pattern:");
        System.out.println("- Broker with ROUTER frontend and ROUTER backend");
        System.out.println("- Multiple DEALER clients sending requests");
        System.out.println("- Multiple DEALER workers processing requests");
        System.out.println();

        // Start broker
        Thread brokerThread = new Thread(RouterDealerSample::runBroker);
        brokerThread.start();
        sleep(500); // Give broker time to bind

        // Start workers
        Thread[] workerThreads = new Thread[WORKER_COUNT];
        for (int i = 0; i < WORKER_COUNT; i++) {
            int workerId = i + 1;
            workerThreads[i] = new Thread(() -> runWorker(workerId));
            workerThreads[i].start();
        }
        sleep(500); // Give workers time to connect

        // Start clients
        Thread[] clientThreads = new Thread[CLIENT_COUNT];
        for (int i = 0; i < CLIENT_COUNT; i++) {
            int clientId = i + 1;
            clientThreads[i] = new Thread(() -> runClient(clientId));
            clientThreads[i].start();
        }

        // Wait for clients to complete
        try {
            for (Thread clientThread : clientThreads) {
                clientThread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting for clients");
        }

        // Give workers time to process remaining messages
        sleep(1000);

        System.out.println();
        System.out.println("All clients completed. Press Ctrl+C to exit.");
        System.out.println();

        // Keep broker and workers running
        // They will be terminated when the main thread exits
    }

    /**
     * Runs the broker that routes messages between clients and workers.
     * Uses ROUTER sockets for both frontend (clients) and backend (workers).
     */
    private static void runBroker() {
        System.out.println("[Broker] Starting...");

        try (Context ctx = new Context();
             Socket frontend = new Socket(ctx, SocketType.ROUTER);
             Socket backend = new Socket(ctx, SocketType.ROUTER);
             Poller poller = new Poller()) {

            // Configure sockets
            frontend.setOption(SocketOption.LINGER, 0);
            backend.setOption(SocketOption.LINGER, 0);

            // Bind frontend for clients
            frontend.bind(FRONTEND_ADDRESS);
            System.out.println("[Broker] Frontend listening on " + FRONTEND_ADDRESS);

            // Bind backend for workers
            backend.bind(BACKEND_ADDRESS);
            System.out.println("[Broker] Backend listening on " + BACKEND_ADDRESS);
            System.out.println("[Broker] Polling started...");

            // Queues for managing requests and workers
            Queue<ClientRequest> clientRequests = new LinkedList<>();
            Queue<byte[]> availableWorkers = new LinkedList<>();

            // Register sockets with poller
            int frontendIdx = poller.register(frontend, PollEvents.IN);
            int backendIdx = poller.register(backend, PollEvents.IN);

            while (true) {
                try {
                    // Poll with 100ms timeout
                    poller.poll(100);

                    // Check frontend (client requests)
                    if (poller.isReadable(frontendIdx)) {
                        // Receive from client: [client-identity][empty][request]
                        byte[] clientIdentity = recvBytes(frontend);
                        byte[] empty = recvBytes(frontend);
                        byte[] request = recvBytes(frontend);

                        String clientId = new String(clientIdentity, StandardCharsets.UTF_8);
                        String requestText = new String(request, StandardCharsets.UTF_8);
                        System.out.println("[Broker] Client " + clientId + " -> Request: " + requestText);

                        // Queue the request
                        clientRequests.add(new ClientRequest(clientIdentity, request));

                        // Try to route if worker available
                        if (!availableWorkers.isEmpty() && !clientRequests.isEmpty()) {
                            byte[] workerIdentity = availableWorkers.poll();
                            ClientRequest req = clientRequests.poll();

                            // Send to worker: [worker-identity][empty][client-identity][empty][request]
                            backend.send(workerIdentity, SendFlags.SEND_MORE);
                            backend.send(new byte[0], SendFlags.SEND_MORE);
                            backend.send(req.clientIdentity, SendFlags.SEND_MORE);
                            backend.send(new byte[0], SendFlags.SEND_MORE);
                            backend.send(req.request, SendFlags.NONE);

                            String workerId = new String(workerIdentity, StandardCharsets.UTF_8);
                            String reqClientId = new String(req.clientIdentity, StandardCharsets.UTF_8);
                            System.out.println("[Broker] Routed to Worker " + workerId + " for Client " + reqClientId);
                        }
                    }

                    // Check backend (worker responses)
                    if (poller.isReadable(backendIdx)) {
                        // Receive from worker: [worker-identity][empty][client-identity][empty][reply]
                        byte[] workerIdentity = recvBytes(backend);
                        byte[] empty1 = recvBytes(backend);
                        byte[] clientIdentity = recvBytes(backend);
                        byte[] empty2 = recvBytes(backend);
                        byte[] reply = recvBytes(backend);

                        String workerId = new String(workerIdentity, StandardCharsets.UTF_8);
                        String clientId = new String(clientIdentity, StandardCharsets.UTF_8);
                        String replyText = new String(reply, StandardCharsets.UTF_8);

                        if ("READY".equals(replyText)) {
                            // Worker is ready
                            System.out.println("[Broker] Worker " + workerId + " is ready");
                            availableWorkers.add(workerIdentity);

                            // Try to route queued request
                            if (!clientRequests.isEmpty()) {
                                ClientRequest req = clientRequests.poll();

                                backend.send(workerIdentity, SendFlags.SEND_MORE);
                                backend.send(new byte[0], SendFlags.SEND_MORE);
                                backend.send(req.clientIdentity, SendFlags.SEND_MORE);
                                backend.send(new byte[0], SendFlags.SEND_MORE);
                                backend.send(req.request, SendFlags.NONE);

                                String clientIdStr = new String(req.clientIdentity, StandardCharsets.UTF_8);
                                System.out.println("[Broker] Routed to Worker " + workerId + " for Client " + clientIdStr);
                            }
                        } else {
                            // Forward reply to client: [client-identity][empty][reply]
                            System.out.println("[Broker] Worker " + workerId + " -> Client " + clientId + ": " + replyText);

                            frontend.send(clientIdentity, SendFlags.SEND_MORE);
                            frontend.send(new byte[0], SendFlags.SEND_MORE);
                            frontend.send(reply, SendFlags.NONE);

                            // Worker is available again
                            availableWorkers.add(workerIdentity);

                            // Try to route queued request
                            if (!clientRequests.isEmpty()) {
                                ClientRequest req = clientRequests.poll();

                                backend.send(workerIdentity, SendFlags.SEND_MORE);
                                backend.send(new byte[0], SendFlags.SEND_MORE);
                                backend.send(req.clientIdentity, SendFlags.SEND_MORE);
                                backend.send(new byte[0], SendFlags.SEND_MORE);
                                backend.send(req.request, SendFlags.NONE);

                                String clientIdStr = new String(req.clientIdentity, StandardCharsets.UTF_8);
                                System.out.println("[Broker] Routed to Worker " + workerId + " for Client " + clientIdStr);
                            }
                        }
                    }
                } catch (ZmqException ex) {
                    System.err.println("[Broker] Error: " + ex.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[Broker] Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs a client that sends requests to the broker.
     *
     * @param id The unique identifier for this client
     */
    private static void runClient(int id) {
        String clientId = "client-" + id;
        System.out.println("[" + clientId + "] Starting...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.DEALER)) {

            // Set routing ID for this client
            socket.setOption(SocketOption.ROUTING_ID, clientId);
            socket.setOption(SocketOption.LINGER, 0);

            socket.connect(FRONTEND_CONNECT_ADDRESS);
            System.out.println("[" + clientId + "] Connected to broker");

            for (int i = 0; i < REQUESTS_PER_CLIENT; i++) {
                String request = "Request #" + (i + 1) + " from " + clientId;

                // Send to broker: [empty][request]
                socket.send(new byte[0], SendFlags.SEND_MORE);
                socket.send(request, SendFlags.NONE);
                System.out.println("[" + clientId + "] Sent: " + request);

                // Receive reply: [empty][reply]
                byte[] empty = recvBytes(socket);
                String reply = socket.recvString().value();
                System.out.println("[" + clientId + "] Received: " + reply);

                sleep(500); // Simulate work
            }

            System.out.println("[" + clientId + "] Done");

        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs a worker that processes requests from the broker.
     *
     * @param id The unique identifier for this worker
     */
    private static void runWorker(int id) {
        String workerId = "worker-" + id;
        System.out.println("[" + workerId + "] Starting...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.DEALER)) {

            // Set routing ID for this worker
            socket.setOption(SocketOption.ROUTING_ID, workerId);
            socket.setOption(SocketOption.LINGER, 0);

            socket.connect(BACKEND_CONNECT_ADDRESS);
            System.out.println("[" + workerId + "] Connected to broker");

            // Send READY message: [empty][client-identity][empty][READY]
            socket.send(new byte[0], SendFlags.SEND_MORE);
            socket.send("READY", SendFlags.SEND_MORE);
            socket.send(new byte[0], SendFlags.SEND_MORE);
            socket.send("READY", SendFlags.NONE);

            while (true) {
                try {
                    // Receive from broker: [empty][client-identity][empty][request]
                    byte[] empty1 = recvBytes(socket);
                    byte[] clientIdentity = recvBytes(socket);
                    byte[] empty2 = recvBytes(socket);
                    byte[] request = recvBytes(socket);

                    String clientId = new String(clientIdentity, StandardCharsets.UTF_8);
                    String requestText = new String(request, StandardCharsets.UTF_8);
                    System.out.println("[" + workerId + "] Processing request from " + clientId + ": " + requestText);

                    // Simulate work
                    sleep(300);

                    String reply = "Processed by " + workerId;

                    // Send reply to broker: [empty][client-identity][empty][reply]
                    socket.send(new byte[0], SendFlags.SEND_MORE);
                    socket.send(clientIdentity, SendFlags.SEND_MORE);
                    socket.send(new byte[0], SendFlags.SEND_MORE);
                    socket.send(reply, SendFlags.NONE);
                    System.out.println("[" + workerId + "] Sent reply to " + clientId + ": " + reply);

                } catch (ZmqException ex) {
                    System.err.println("[" + workerId + "] Error: " + ex.getMessage());
                    break;
                }
            }

            System.out.println("[" + workerId + "] Done");

        } catch (Exception e) {
            System.err.println("[" + workerId + "] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to receive a byte array from a socket.
     * Handles multi-part messages by reading one frame at a time.
     *
     * @param socket The socket to receive from
     * @return The received byte array
     */
    private static byte[] recvBytes(Socket socket) {
        return socket.recvBytes().value();  // Extract from RecvResult in blocking mode
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

    /**
     * Represents a client request waiting to be processed.
     */
    private static class ClientRequest {
        final byte[] clientIdentity;
        final byte[] request;

        ClientRequest(byte[] clientIdentity, byte[] request) {
            this.clientIdentity = clientIdentity;
            this.request = request;
        }
    }
}
