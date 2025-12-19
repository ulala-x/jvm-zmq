package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.*;
import io.github.ulalax.zmq.core.ZmqException;

/**
 * CURVE Security Sample
 *
 * <p>Demonstrates ZeroMQ CURVE encryption for secure communication.
 * CURVE provides authentication and encryption based on CurveZMQ protocol.</p>
 *
 * <p>This sample demonstrates:</p>
 * <ul>
 *   <li>CURVE keypair generation</li>
 *   <li>Secure encrypted communication</li>
 *   <li>Server and client authentication setup</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>Both mode (default): java CurveSecuritySample</li>
 *   <li>Server mode: java CurveSecuritySample server</li>
 *   <li>Client mode: java CurveSecuritySample client</li>
 * </ul>
 */
public class CurveSecuritySample {

    private static final String ENDPOINT = "tcp://localhost:5563";
    private static final int PORT = 5563;

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ CURVE Security Sample");
        System.out.println("============================");
        System.out.println();
        System.out.println("This sample demonstrates:");
        System.out.println("  - CURVE keypair generation");
        System.out.println("  - Secure encrypted communication");
        System.out.println("  - Server and client authentication setup");
        System.out.println();

        // Check if CURVE is available
        if (!Context.has("curve")) {
            System.err.println("ERROR: CURVE security is not available in this build of libzmq");
            System.err.println("Please rebuild libzmq with libsodium support");
            return;
        }

        System.out.println("[Setup] CURVE security is available");
        System.out.println();

        // Generate server keypair
        Curve.KeyPair serverKeys = Curve.generateKeypair();
        System.out.println("[Server] Generated keypair:");
        System.out.println("  Public:  " + serverKeys.publicKey());
        System.out.println("  Secret:  " + serverKeys.secretKey());
        System.out.println();

        // Generate client keypair
        Curve.KeyPair clientKeys = Curve.generateKeypair();
        System.out.println("[Client] Generated keypair:");
        System.out.println("  Public:  " + clientKeys.publicKey());
        System.out.println("  Secret:  " + clientKeys.secretKey());
        System.out.println();

        // Demonstrate public key derivation
        String derivedPublic = Curve.derivePublicKey(clientKeys.secretKey());
        System.out.println("[Client] Derived public key from secret: " + derivedPublic);
        System.out.println("[Client] Keys match: " + derivedPublic.equals(clientKeys.publicKey()));
        System.out.println();

        String mode = args.length > 0 ? args[0].toLowerCase() : "both";

        if (mode.equals("server") || mode.equals("both")) {
            if (mode.equals("both")) {
                // Run server in a separate thread
                Thread serverThread = new Thread(() -> runSecureServer(serverKeys));
                serverThread.setDaemon(true);
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
                runSecureServer(serverKeys);
                return;
            }
        }

        if (mode.equals("client") || mode.equals("both")) {
            runSecureClient(clientKeys, serverKeys.publicKey());
        }

        if (mode.equals("both")) {
            System.out.println();
            System.out.println("[Main] Secure communication completed successfully!");
        }
    }

    /**
     * Runs the secure server with CURVE encryption.
     *
     * @param serverKeys The server's keypair
     */
    private static void runSecureServer(Curve.KeyPair serverKeys) {
        System.out.println("[Server] Starting secure server...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.REP)) {

            // Configure as CURVE server
            socket.setOption(SocketOption.CURVE_SERVER, 1);
            socket.setOption(SocketOption.CURVE_SECRETKEY, serverKeys.secretKey());

            socket.setOption(SocketOption.LINGER, 0);
            socket.setOption(SocketOption.RCVTIMEO, 5000);
            socket.bind("tcp://*:" + PORT);

            System.out.println("[Server] Bound to tcp://*:" + PORT + " with CURVE encryption");

            for (int i = 0; i < 3; i++) {
                try {
                    String request = socket.recvString().value();
                    System.out.println("[Server] Received encrypted: " + request);

                    String response = "Secure response #" + (i + 1);
                    socket.send(response);
                    System.out.println("[Server] Sent encrypted: " + response);
                } catch (ZmqException ex) {
                    if (ex.isAgain()) {
                        System.out.println("[Server] Timeout waiting for request");
                        break;
                    }
                    throw ex;
                }
            }

            System.out.println("[Server] Done");

        } catch (Exception e) {
            System.err.println("[Server] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the secure client with CURVE encryption.
     *
     * @param clientKeys The client's keypair
     * @param serverPublicKey The server's public key
     */
    private static void runSecureClient(Curve.KeyPair clientKeys, String serverPublicKey) {
        System.out.println("[Client] Starting secure client...");

        try (Context ctx = new Context();
             Socket socket = new Socket(ctx, SocketType.REQ)) {

            // Configure as CURVE client
            socket.setOption(SocketOption.CURVE_SERVERKEY, serverPublicKey);
            socket.setOption(SocketOption.CURVE_PUBLICKEY, clientKeys.publicKey());
            socket.setOption(SocketOption.CURVE_SECRETKEY, clientKeys.secretKey());

            socket.setOption(SocketOption.LINGER, 0);
            socket.setOption(SocketOption.RCVTIMEO, 5000);
            socket.connect(ENDPOINT);

            System.out.println("[Client] Connected to " + ENDPOINT + " with CURVE encryption");

            for (int i = 0; i < 3; i++) {
                String request = "Secure request #" + (i + 1);
                socket.send(request);
                System.out.println("[Client] Sent encrypted: " + request);

                try {
                    String response = socket.recvString().value();
                    System.out.println("[Client] Received encrypted: " + response);
                } catch (ZmqException ex) {
                    if (ex.isAgain()) {
                        System.out.println("[Client] Timeout waiting for response");
                        break;
                    }
                    throw ex;
                }

                // Small delay between requests
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("[Client] Interrupted during sleep");
                    break;
                }
            }

            System.out.println("[Client] Done");

        } catch (Exception e) {
            System.err.println("[Client] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
