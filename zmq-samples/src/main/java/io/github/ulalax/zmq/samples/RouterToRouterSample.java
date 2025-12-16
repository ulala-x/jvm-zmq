package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Router-to-Router Pattern Examples using MultipartMessage API
 *
 * <p>Router-to-Router is an advanced ZeroMQ pattern used for:</p>
 * <ul>
 *   <li>Peer-to-peer communication where both sides need routing control</li>
 *   <li>Building message brokers and proxies</li>
 *   <li>Network topologies where nodes need to address each other directly</li>
 * </ul>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Both sockets must have explicit identities set</li>
 *   <li>Messages must include target identity as first frame</li>
 *   <li>Received messages include sender identity as first frame</li>
 *   <li>Fully asynchronous bidirectional communication</li>
 * </ul>
 *
 * <p>This example demonstrates the MultipartMessage API for cleaner code.</p>
 */
public class RouterToRouterSample {

    public static void main(String[] args) {
        System.out.println("=== JVM-ZMQ Router-to-Router Examples (MultipartMessage API) ===");
        System.out.println();

        // Example 1: Basic peer-to-peer communication
        basicPeerToPeerExample();

        // Example 2: Hub and spoke pattern with multiple peers
        hubAndSpokeExample();

        // Example 3: Broker pattern (Router-Router-Router)
        brokerPatternExample();

        System.out.println();
        System.out.println("All examples completed!");
    }

    /**
     * Example 1: Basic Peer-to-Peer Communication
     * Two Router sockets communicating directly with each other
     * Using MultipartMessage for cleaner send/receive
     */
    private static void basicPeerToPeerExample() {
        System.out.println("--- Example 1: Basic Peer-to-Peer (MultipartMessage) ---");

        try (Context ctx = new Context();
             Socket peerA = new Socket(ctx, SocketType.ROUTER);
             Socket peerB = new Socket(ctx, SocketType.ROUTER)) {

            // Configure sockets
            peerA.setOption(SocketOption.LINGER, 0);
            peerB.setOption(SocketOption.LINGER, 0);
            peerA.setOption(SocketOption.RCVTIMEO, 1000);
            peerB.setOption(SocketOption.RCVTIMEO, 1000);

            // IMPORTANT: Set explicit identities for Router-to-Router
            peerA.setOption(SocketOption.ROUTING_ID, "PEER_A");
            peerB.setOption(SocketOption.ROUTING_ID, "PEER_B");

            // Peer A binds, Peer B connects
            peerA.bind("tcp://127.0.0.1:15700");
            peerB.connect("tcp://127.0.0.1:15700");
            sleep(100);

            // Peer B sends to Peer A using MultipartMessage
            // Frame 1: Target identity (who to send to)
            // Frame 2: Message content
            System.out.println("Peer B sending message to Peer A...");
            MultipartMessage outMsg = new MultipartMessage();
            outMsg.addString("PEER_A");           // Target identity
            outMsg.addString("Hello from Peer B!");
            outMsg.send(peerB);

            // Peer A receives using MultipartMessage.recv
            // Frame 1: Sender identity (who sent this)
            // Frame 2: Message content
            MultipartMessage inMsg = MultipartMessage.recv(peerA);
            String senderId = inMsg.getString(0);
            String message = inMsg.getString(1);
            System.out.println("Peer A received from [" + senderId + "]: " + message);

            // Peer A replies back using sender's identity
            System.out.println("Peer A replying to Peer B...");
            MultipartMessage reply = new MultipartMessage();
            reply.addString(senderId);            // Use sender's identity for reply
            reply.addString("Hello back from Peer A!");
            reply.send(peerA);

            // Peer B receives reply
            MultipartMessage replyMsg = MultipartMessage.recv(peerB);
            String replyFrom = replyMsg.getString(0);
            String replyText = replyMsg.getString(1);
            System.out.println("Peer B received from [" + replyFrom + "]: " + replyText);

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in basicPeerToPeerExample: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 2: Hub and Spoke Pattern
     * One central hub Router connecting to multiple peer Routers
     * Using MultipartMessage for cleaner broadcast operations
     */
    private static void hubAndSpokeExample() {
        System.out.println("--- Example 2: Hub and Spoke Pattern (MultipartMessage) ---");

        try (Context ctx = new Context();
             Socket hub = new Socket(ctx, SocketType.ROUTER);
             Socket spoke1 = new Socket(ctx, SocketType.ROUTER);
             Socket spoke2 = new Socket(ctx, SocketType.ROUTER);
             Socket spoke3 = new Socket(ctx, SocketType.ROUTER)) {

            // Configure all sockets
            for (Socket socket : new Socket[]{hub, spoke1, spoke2, spoke3}) {
                socket.setOption(SocketOption.LINGER, 0);
                socket.setOption(SocketOption.RCVTIMEO, 1000);
            }

            // Set identities
            hub.setOption(SocketOption.ROUTING_ID, "HUB");
            spoke1.setOption(SocketOption.ROUTING_ID, "SPOKE1");
            spoke2.setOption(SocketOption.ROUTING_ID, "SPOKE2");
            spoke3.setOption(SocketOption.ROUTING_ID, "SPOKE3");

            // Hub binds, spokes connect
            hub.bind("tcp://127.0.0.1:15701");
            spoke1.connect("tcp://127.0.0.1:15701");
            spoke2.connect("tcp://127.0.0.1:15701");
            spoke3.connect("tcp://127.0.0.1:15701");
            sleep(200);

            // All spokes send registration message to hub
            System.out.println("Spokes sending registration to Hub...");
            sendMultipart(spoke1, "HUB", "REGISTER:SPOKE1");
            sendMultipart(spoke2, "HUB", "REGISTER:SPOKE2");
            sendMultipart(spoke3, "HUB", "REGISTER:SPOKE3");

            sleep(100);

            // Hub receives and processes registrations
            List<String> registeredPeers = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                MultipartMessage regMsg = MultipartMessage.recv(hub);
                String peerId = regMsg.getString(0);
                String regContent = regMsg.getString(1);
                registeredPeers.add(peerId);
                System.out.println("Hub received: [" + peerId + "] -> " + regContent);
            }

            // Hub broadcasts message to all registered peers
            System.out.println();
            System.out.println("Hub broadcasting to all spokes...");
            for (String peer : registeredPeers) {
                sendMultipart(hub, peer, "Welcome " + peer + "! You are connected.");
            }

            // Each spoke receives its message
            for (Socket spoke : new Socket[]{spoke1, spoke2, spoke3}) {
                MultipartMessage msg = MultipartMessage.recv(spoke);
                String from = msg.getString(0);
                String content = msg.getString(1);
                String spokeName = getSpokeName(spoke, spoke1, spoke2, spoke3);
                System.out.println(spokeName + " received from [" + from + "]: " + content);
            }
            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in hubAndSpokeExample: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example 3: Broker Pattern
     * A broker (Router) that routes messages between clients (Routers)
     * Clients can send messages to each other through the broker
     * Using MultipartMessage for complex multi-frame routing
     */
    private static void brokerPatternExample() {
        System.out.println("--- Example 3: Broker Pattern (MultipartMessage) ---");

        try (Context ctx = new Context();
             Socket broker = new Socket(ctx, SocketType.ROUTER);
             Socket client1 = new Socket(ctx, SocketType.ROUTER);
             Socket client2 = new Socket(ctx, SocketType.ROUTER)) {

            // Configure all sockets
            for (Socket socket : new Socket[]{broker, client1, client2}) {
                socket.setOption(SocketOption.LINGER, 0);
                socket.setOption(SocketOption.RCVTIMEO, 1000);
            }

            // Set identities
            broker.setOption(SocketOption.ROUTING_ID, "BROKER");
            client1.setOption(SocketOption.ROUTING_ID, "CLIENT1");
            client2.setOption(SocketOption.ROUTING_ID, "CLIENT2");

            broker.bind("tcp://127.0.0.1:15702");
            client1.connect("tcp://127.0.0.1:15702");
            client2.connect("tcp://127.0.0.1:15702");
            sleep(200);

            // Client1 sends a message to be forwarded to Client2
            // Message format: [BROKER][TARGET_CLIENT][actual message]
            System.out.println("Client1 sending message to Client2 via Broker...");
            sendMultipart(client1, "BROKER", "CLIENT2", "Hello Client2, this is Client1!");

            // Broker receives and forwards
            MultipartMessage inMsg = MultipartMessage.recv(broker);
            String senderId = inMsg.getString(0);
            String targetId = inMsg.getString(1);
            String forwardMsg = inMsg.getString(2);

            System.out.println("Broker received from [" + senderId + "]: forward to [" + targetId + "] -> " + forwardMsg);

            // Broker forwards to target, including original sender info
            sendMultipart(broker, targetId, senderId, forwardMsg);

            // Client2 receives
            MultipartMessage recvMsg = MultipartMessage.recv(client2);
            String brokerFrom = recvMsg.getString(0);
            String originalSender = recvMsg.getString(1);
            String received = recvMsg.getString(2);

            System.out.println("Client2 received from [" + originalSender + "] (via " + brokerFrom + "): " + received);

            // Client2 replies back to Client1 via broker
            System.out.println();
            System.out.println("Client2 replying to Client1 via Broker...");
            sendMultipart(client2, "BROKER", "CLIENT1", "Got your message! Reply from Client2.");

            // Broker forwards reply
            MultipartMessage replyIn = MultipartMessage.recv(broker);
            String replySenderId = replyIn.getString(0);
            String replyTargetId = replyIn.getString(1);
            String replyForwardMsg = replyIn.getString(2);

            sendMultipart(broker, replyTargetId, replySenderId, replyForwardMsg);

            // Client1 receives reply
            MultipartMessage finalMsg = MultipartMessage.recv(client1);
            String finalBrokerFrom = finalMsg.getString(0);
            String finalOriginalSender = finalMsg.getString(1);
            String finalReceived = finalMsg.getString(2);

            System.out.println("Client1 received from [" + finalOriginalSender + "] (via " + finalBrokerFrom + "): " + finalReceived);

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error in brokerPatternExample: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to send a multipart message with variable number of string frames.
     *
     * @param socket The socket to send on
     * @param frames The string frames to send
     */
    private static void sendMultipart(Socket socket, String... frames) {
        MultipartMessage msg = new MultipartMessage();
        for (String frame : frames) {
            msg.addString(frame);
        }
        msg.send(socket);
    }

    /**
     * Helper method to get spoke name by reference.
     *
     * @param spoke The spoke socket
     * @param spoke1 Reference to spoke1
     * @param spoke2 Reference to spoke2
     * @param spoke3 Reference to spoke3
     * @return Spoke name
     */
    private static String getSpokeName(Socket spoke, Socket spoke1, Socket spoke2, Socket spoke3) {
        if (spoke == spoke1) return "SPOKE1";
        if (spoke == spoke2) return "SPOKE2";
        if (spoke == spoke3) return "SPOKE3";
        return "UNKNOWN";
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
