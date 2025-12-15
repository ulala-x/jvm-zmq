# JVM-ZMQ Examples

Complete, runnable examples demonstrating all ZeroMQ messaging patterns and features using JVM-ZMQ.

## Table of Contents

1. [REQ-REP Pattern](#1-req-rep-pattern)
2. [PUB-SUB Pattern](#2-pub-sub-pattern)
3. [PUSH-PULL Pattern](#3-push-pull-pattern)
4. [ROUTER-DEALER Pattern](#4-router-dealer-pattern)
5. [XPUB-XSUB Pattern](#5-xpub-xsub-pattern)
6. [PAIR Pattern](#6-pair-pattern)
7. [Polling](#7-polling)
8. [Multipart Messages](#8-multipart-messages)
9. [Socket Monitoring](#9-socket-monitoring)
10. [CURVE Security](#10-curve-security)

---

## 1. REQ-REP Pattern

The Request-Reply pattern is used for synchronous request-response communication. The client sends a request and waits for a reply, while the server receives requests and sends back responses.

### Basic Request-Response

```java
import io.github.ulalax.zmq.*;

public class ReqRepExample {
    public static void main(String[] args) throws Exception {
        // Create context (manages I/O threads and socket lifetime)
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.REP);
             Socket client = new Socket(ctx, SocketType.REQ)) {

            // Server binds to an endpoint
            server.bind("tcp://*:5555");

            // Client connects to the server
            client.connect("tcp://localhost:5555");

            // Allow time for connection to establish
            Thread.sleep(100);

            // Client sends a request
            String request = "Hello Server";
            client.send(request);
            System.out.println("Client sent: " + request);

            // Server receives the request
            String receivedRequest = server.recvString();
            System.out.println("Server received: " + receivedRequest);

            // Server sends a reply
            String reply = "Hello Client";
            server.send(reply);
            System.out.println("Server sent: " + reply);

            // Client receives the reply
            String receivedReply = client.recvString();
            System.out.println("Client received: " + receivedReply);
        }
    }
}
```

### Multiple Request-Response Cycles

```java
import io.github.ulalax.zmq.*;

public class MultipleReqRepExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.REP);
             Socket client = new Socket(ctx, SocketType.REQ)) {

            server.bind("tcp://*:5555");
            client.connect("tcp://localhost:5555");
            Thread.sleep(100);

            // Perform 5 request-response cycles
            for (int i = 0; i < 5; i++) {
                // Client sends request
                String request = "Request-" + i;
                client.send(request);

                // Server receives and processes
                String received = server.recvString();
                System.out.println("Server received: " + received);

                // Server sends reply
                String reply = "Reply-" + i;
                server.send(reply);

                // Client receives reply
                String response = client.recvString();
                System.out.println("Client received: " + response);
            }
        }
    }
}
```

---

## 2. PUB-SUB Pattern

The Publish-Subscribe pattern is used for one-to-many distribution where a publisher broadcasts messages to multiple subscribers.

### Basic Pub-Sub

```java
import io.github.ulalax.zmq.*;

public class PubSubExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket publisher = new Socket(ctx, SocketType.PUB);
             Socket subscriber = new Socket(ctx, SocketType.SUB)) {

            // Publisher binds to endpoint
            publisher.bind("tcp://*:5556");

            // Subscriber connects to publisher
            subscriber.connect("tcp://localhost:5556");

            // Subscribe to all messages (empty string = match all)
            subscriber.subscribe("");

            // Give time for subscription to propagate (slow joiner problem)
            Thread.sleep(100);

            // Set receive timeout to avoid blocking indefinitely
            subscriber.setOption(SocketOption.RCVTIMEO, 1000);

            // Publish messages
            for (int i = 0; i < 5; i++) {
                String message = "Message " + i;
                publisher.send(message);
                System.out.println("Published: " + message);
                Thread.sleep(50);
            }

            // Receive messages
            for (int i = 0; i < 5; i++) {
                String received = subscriber.recvString();
                System.out.println("Received: " + received);
            }
        }
    }
}
```

### Topic Filtering

```java
import io.github.ulalax.zmq.*;

public class TopicFilterExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket publisher = new Socket(ctx, SocketType.PUB);
             Socket newsSubscriber = new Socket(ctx, SocketType.SUB);
             Socket weatherSubscriber = new Socket(ctx, SocketType.SUB)) {

            publisher.bind("tcp://*:5556");

            // Subscribe to specific topics
            newsSubscriber.subscribe("news.");
            newsSubscriber.connect("tcp://localhost:5556");

            weatherSubscriber.subscribe("weather.");
            weatherSubscriber.connect("tcp://localhost:5556");

            Thread.sleep(100);

            newsSubscriber.setOption(SocketOption.RCVTIMEO, 1000);
            weatherSubscriber.setOption(SocketOption.RCVTIMEO, 1000);

            // Publish messages with different topics
            publisher.send("news.sports Breaking: Team wins championship!");
            publisher.send("weather.forecast Sunny today");
            publisher.send("news.tech New technology released");
            publisher.send("weather.alert Storm warning");

            Thread.sleep(100);

            // News subscriber receives only news messages
            System.out.println("News Subscriber:");
            try {
                while (true) {
                    String msg = newsSubscriber.recvString();
                    System.out.println("  " + msg);
                }
            } catch (Exception e) {
                // Timeout - no more messages
            }

            // Weather subscriber receives only weather messages
            System.out.println("\nWeather Subscriber:");
            try {
                while (true) {
                    String msg = weatherSubscriber.recvString();
                    System.out.println("  " + msg);
                }
            } catch (Exception e) {
                // Timeout - no more messages
            }
        }
    }
}
```

### Multiple Subscribers

```java
import io.github.ulalax.zmq.*;

public class MultipleSubscribersExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket publisher = new Socket(ctx, SocketType.PUB);
             Socket subscriber1 = new Socket(ctx, SocketType.SUB);
             Socket subscriber2 = new Socket(ctx, SocketType.SUB)) {

            publisher.bind("tcp://*:5556");

            // Both subscribers subscribe to all messages
            subscriber1.subscribe("");
            subscriber1.connect("tcp://localhost:5556");
            subscriber1.setOption(SocketOption.RCVTIMEO, 1000);

            subscriber2.subscribe("");
            subscriber2.connect("tcp://localhost:5556");
            subscriber2.setOption(SocketOption.RCVTIMEO, 1000);

            Thread.sleep(100);

            // Publish a broadcast message
            String message = "Broadcast to all subscribers";
            publisher.send(message);

            // Both subscribers receive the same message
            String received1 = subscriber1.recvString();
            String received2 = subscriber2.recvString();

            System.out.println("Subscriber 1 received: " + received1);
            System.out.println("Subscriber 2 received: " + received2);
        }
    }
}
```

---

## 3. PUSH-PULL Pattern

The Push-Pull pattern (also known as Pipeline) is used for distributing work among workers in a load-balanced manner.

### Basic Push-Pull

```java
import io.github.ulalax.zmq.*;

public class PushPullExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket pusher = new Socket(ctx, SocketType.PUSH);
             Socket puller = new Socket(ctx, SocketType.PULL)) {

            // Puller binds (sink)
            puller.bind("tcp://*:5557");

            // Pusher connects (producer)
            pusher.connect("tcp://localhost:5557");
            Thread.sleep(100);

            puller.setOption(SocketOption.RCVTIMEO, 1000);

            // Push work items
            for (int i = 0; i < 5; i++) {
                String workItem = "Task-" + i;
                pusher.send(workItem);
                System.out.println("Pushed: " + workItem);
            }

            // Pull and process work items
            for (int i = 0; i < 5; i++) {
                String task = puller.recvString();
                System.out.println("Pulled: " + task);
                // Process task here
            }
        }
    }
}
```

### Load Distribution (Multiple Workers)

```java
import io.github.ulalax.zmq.*;
import java.util.concurrent.*;

public class LoadDistributionExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context()) {
            // Create producer
            Thread producer = new Thread(() -> {
                try (Socket pusher = new Socket(ctx, SocketType.PUSH)) {
                    pusher.bind("tcp://*:5557");
                    Thread.sleep(200); // Let workers connect

                    // Push 10 work items
                    for (int i = 0; i < 10; i++) {
                        pusher.send("Work-" + i);
                        System.out.println("Producer: Pushed Work-" + i);
                        Thread.sleep(50);
                    }
                }
            });

            // Create two workers
            Thread worker1 = createWorker(ctx, "Worker-1");
            Thread worker2 = createWorker(ctx, "Worker-2");

            // Start all threads
            worker1.start();
            worker2.start();
            Thread.sleep(100); // Let workers connect
            producer.start();

            // Wait for completion
            producer.join();
            worker1.join(5000);
            worker2.join(5000);
        }
    }

    private static Thread createWorker(Context ctx, String name) {
        return new Thread(() -> {
            try (Socket puller = new Socket(ctx, SocketType.PULL)) {
                puller.connect("tcp://localhost:5557");
                puller.setOption(SocketOption.RCVTIMEO, 3000);

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String work = puller.recvString();
                        System.out.println(name + ": Received " + work);
                        Thread.sleep(100); // Simulate work
                    } catch (Exception e) {
                        break; // Timeout
                    }
                }
            }
        });
    }
}
```

### Pipeline Pattern (Multi-Stage)

```java
import io.github.ulalax.zmq.*;

public class PipelineExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             // Stage 1: Producer
             Socket producer = new Socket(ctx, SocketType.PUSH);
             // Stage 2: Worker (pulls from producer, processes, pushes to sink)
             Socket workerIn = new Socket(ctx, SocketType.PULL);
             Socket workerOut = new Socket(ctx, SocketType.PUSH);
             // Stage 3: Sink
             Socket sink = new Socket(ctx, SocketType.PULL)) {

            // Setup pipeline connections
            producer.bind("tcp://*:5557");
            workerIn.connect("tcp://localhost:5557");

            workerOut.bind("tcp://*:5558");
            sink.connect("tcp://localhost:5558");

            Thread.sleep(100);

            workerIn.setOption(SocketOption.RCVTIMEO, 1000);
            sink.setOption(SocketOption.RCVTIMEO, 1000);

            // Producer generates raw data
            String rawData = "raw-data";
            producer.send(rawData);
            System.out.println("Producer: " + rawData);

            // Worker receives, processes, and forwards
            String received = workerIn.recvString();
            String processed = received.toUpperCase() + "-PROCESSED";
            workerOut.send(processed);
            System.out.println("Worker: " + received + " -> " + processed);

            // Sink receives final result
            String result = sink.recvString();
            System.out.println("Sink: " + result);
        }
    }
}
```

---

## 4. ROUTER-DEALER Pattern

The Router-Dealer pattern enables asynchronous request-reply and advanced routing scenarios.

### Basic Router-Dealer Communication

```java
import io.github.ulalax.zmq.*;

public class RouterDealerExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket router = new Socket(ctx, SocketType.ROUTER);
             Socket dealer = new Socket(ctx, SocketType.DEALER)) {

            // Router binds (server side)
            router.bind("tcp://*:5559");

            // Dealer connects (client side)
            dealer.connect("tcp://localhost:5559");
            Thread.sleep(100);

            router.setOption(SocketOption.RCVTIMEO, 1000);
            dealer.setOption(SocketOption.RCVTIMEO, 1000);

            // Dealer sends message
            dealer.send("Hello from Dealer");

            // Router receives with identity frame
            // ROUTER sockets prepend sender identity to messages
            try (Message identity = new Message();
                 Message content = new Message()) {

                router.recv(identity, RecvFlags.NONE);
                router.recv(content, RecvFlags.NONE);

                System.out.println("Router received from identity: " +
                    bytesToHex(identity.toByteArray()));
                System.out.println("Router received content: " + content.toString());

                // Router replies using the identity
                try (Message replyId = new Message();
                     Message replyMsg = new Message("Hello from Router")) {

                    replyId.copy(identity);
                    router.send(replyId, SendFlags.SEND_MORE);
                    router.send(replyMsg, SendFlags.NONE);
                }
            }

            // Dealer receives reply
            String reply = dealer.recvString();
            System.out.println("Dealer received: " + reply);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
```

### Custom Identity

```java
import io.github.ulalax.zmq.*;

public class CustomIdentityExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket router = new Socket(ctx, SocketType.ROUTER);
             Socket dealer1 = new Socket(ctx, SocketType.DEALER);
             Socket dealer2 = new Socket(ctx, SocketType.DEALER)) {

            router.bind("tcp://*:5559");

            // Set custom identities before connecting
            dealer1.setOption(SocketOption.ROUTING_ID, "DEALER-1");
            dealer2.setOption(SocketOption.ROUTING_ID, "DEALER-2");

            dealer1.connect("tcp://localhost:5559");
            dealer2.connect("tcp://localhost:5559");
            Thread.sleep(100);

            router.setOption(SocketOption.RCVTIMEO, 1000);
            dealer1.setOption(SocketOption.RCVTIMEO, 1000);
            dealer2.setOption(SocketOption.RCVTIMEO, 1000);

            // Both dealers send messages
            dealer1.send("Message from Dealer 1");
            dealer2.send("Message from Dealer 2");

            // Router receives and routes replies
            for (int i = 0; i < 2; i++) {
                try (Message identity = new Message();
                     Message request = new Message()) {

                    router.recv(identity, RecvFlags.NONE);
                    router.recv(request, RecvFlags.NONE);

                    String identityStr = identity.toString();
                    System.out.println("Router received from: " + identityStr);

                    // Send targeted reply
                    try (Message replyId = new Message();
                         Message replyMsg = new Message("Reply to " + identityStr)) {

                        replyId.copy(identity);
                        router.send(replyId, SendFlags.SEND_MORE);
                        router.send(replyMsg, SendFlags.NONE);
                    }
                }
            }

            // Dealers receive their respective replies
            String reply1 = dealer1.recvString();
            String reply2 = dealer2.recvString();

            System.out.println("Dealer 1 received: " + reply1);
            System.out.println("Dealer 2 received: " + reply2);
        }
    }
}
```

### Async Request-Reply Broker

```java
import io.github.ulalax.zmq.*;

public class AsyncBrokerExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context()) {
            // Start client thread
            Thread client = new Thread(() -> {
                try (Socket dealer = new Socket(ctx, SocketType.DEALER)) {
                    dealer.connect("tcp://localhost:5559");
                    dealer.setOption(SocketOption.RCVTIMEO, 2000);

                    dealer.send("Client request");
                    String response = dealer.recvString();
                    System.out.println("Client received: " + response);
                }
            });

            // Start worker thread
            Thread worker = new Thread(() -> {
                try (Socket dealer = new Socket(ctx, SocketType.DEALER)) {
                    dealer.connect("tcp://localhost:5560");
                    dealer.setOption(SocketOption.RCVTIMEO, 2000);

                    String request = dealer.recvString();
                    System.out.println("Worker received: " + request);
                    dealer.send("Worker response");
                }
            });

            // Start broker
            Thread broker = new Thread(() -> {
                try (Socket frontend = new Socket(ctx, SocketType.ROUTER);
                     Socket backend = new Socket(ctx, SocketType.ROUTER)) {

                    frontend.bind("tcp://*:5559"); // Client-facing
                    backend.bind("tcp://*:5560");  // Worker-facing
                    Thread.sleep(100);

                    // Simple message forwarding (in production, use zmq_proxy)
                    PollItem[] items = {
                        new PollItem(frontend, PollEvents.IN),
                        new PollItem(backend, PollEvents.IN)
                    };

                    for (int i = 0; i < 2; i++) {
                        Poller.poll(items, 2000);

                        if (items[0].isReadable()) {
                            // Forward from frontend to backend
                            forwardMessage(frontend, backend);
                        }
                        if (items[1].isReadable()) {
                            // Forward from backend to frontend
                            forwardMessage(backend, frontend);
                        }
                    }
                }
            });

            broker.start();
            Thread.sleep(200);
            client.start();
            worker.start();

            broker.join(5000);
            client.join(5000);
            worker.join(5000);
        }
    }

    private static void forwardMessage(Socket from, Socket to) {
        try {
            MultipartMessage msg = from.recvMultipart();
            to.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 5. XPUB-XSUB Pattern

XPUB/XSUB is an extended pub-sub pattern that exposes subscription messages, useful for building message brokers.

### Subscription Messages

```java
import io.github.ulalax.zmq.*;

public class XPubXSubExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket xpub = new Socket(ctx, SocketType.XPUB);
             Socket subscriber = new Socket(ctx, SocketType.SUB)) {

            // XPUB binds (publisher side with subscription visibility)
            xpub.bind("tcp://*:5561");
            xpub.setOption(SocketOption.RCVTIMEO, 1000);
            xpub.setOption(SocketOption.LINGER, 0);

            // Regular SUB connects
            subscriber.connect("tcp://localhost:5561");
            subscriber.setOption(SocketOption.RCVTIMEO, 1000);
            subscriber.setOption(SocketOption.LINGER, 0);

            Thread.sleep(200);

            // Subscriber subscribes to a topic
            subscriber.subscribe("news");
            Thread.sleep(200);

            // XPUB receives the subscription message
            byte[] subMsg = xpub.recvBytes();

            // First byte: 0x01 = subscribe, 0x00 = unsubscribe
            if (subMsg[0] == 0x01) {
                String topic = new String(subMsg, 1, subMsg.length - 1);
                System.out.println("Subscription received for topic: " + topic);
            }

            // XPUB publishes message
            xpub.send("news Breaking news!");

            // Subscriber receives
            String message = subscriber.recvString();
            System.out.println("Subscriber received: " + message);

            // Unsubscribe
            subscriber.unsubscribe("news");
            Thread.sleep(200);

            // XPUB receives unsubscription message
            byte[] unsubMsg = xpub.recvBytes();
            if (unsubMsg[0] == 0x00) {
                String topic = new String(unsubMsg, 1, unsubMsg.length - 1);
                System.out.println("Unsubscription received for topic: " + topic);
            }
        }
    }
}
```

### Manual Subscription with XSUB

```java
import io.github.ulalax.zmq.*;

public class XSubExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket publisher = new Socket(ctx, SocketType.PUB);
             Socket xsub = new Socket(ctx, SocketType.XSUB)) {

            publisher.bind("tcp://*:5561");
            xsub.connect("tcp://localhost:5561");

            xsub.setOption(SocketOption.RCVTIMEO, 1000);
            xsub.setOption(SocketOption.LINGER, 0);

            Thread.sleep(200);

            // XSUB sends manual subscription message
            // 0x01 = subscribe, followed by topic (empty = all)
            byte[] subscribeMsg = new byte[]{0x01};
            xsub.send(subscribeMsg);

            Thread.sleep(300);

            // Publisher publishes
            publisher.send("Message from Publisher");
            Thread.sleep(100);

            // XSUB receives
            String message = xsub.recvString();
            System.out.println("XSUB received: " + message);

            // XSUB sends unsubscription message
            byte[] unsubscribeMsg = new byte[]{0x00};
            xsub.send(unsubscribeMsg);
        }
    }
}
```

---

## 6. PAIR Pattern

The PAIR pattern creates an exclusive connection between two sockets, useful for inter-thread communication.

### Basic Pair Communication

```java
import io.github.ulalax.zmq.*;

public class PairExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket socket1 = new Socket(ctx, SocketType.PAIR);
             Socket socket2 = new Socket(ctx, SocketType.PAIR)) {

            // PAIR sockets form exclusive connections
            socket1.bind("inproc://pair-example");
            socket2.connect("inproc://pair-example");
            Thread.sleep(50);

            socket1.setOption(SocketOption.RCVTIMEO, 1000);
            socket2.setOption(SocketOption.RCVTIMEO, 1000);

            // Bidirectional communication
            socket1.send("Hello from Socket 1");
            String msg1 = socket2.recvString();
            System.out.println("Socket 2 received: " + msg1);

            socket2.send("Hello from Socket 2");
            String msg2 = socket1.recvString();
            System.out.println("Socket 1 received: " + msg2);
        }
    }
}
```

### Inter-Thread Communication

```java
import io.github.ulalax.zmq.*;

public class InterThreadExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context()) {
            // Thread 1: Background worker
            Thread worker = new Thread(() -> {
                try (Socket socket = new Socket(ctx, SocketType.PAIR)) {
                    socket.bind("inproc://worker");
                    socket.setOption(SocketOption.RCVTIMEO, 2000);

                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            String task = socket.recvString();
                            System.out.println("Worker processing: " + task);

                            // Process task
                            Thread.sleep(100);

                            // Send result back
                            socket.send("Result: " + task.toUpperCase());
                        } catch (Exception e) {
                            break;
                        }
                    }
                }
            });

            worker.start();
            Thread.sleep(100);

            // Main thread: Task sender
            try (Socket socket = new Socket(ctx, SocketType.PAIR)) {
                socket.connect("inproc://worker");
                socket.setOption(SocketOption.RCVTIMEO, 1000);

                // Send tasks
                for (int i = 0; i < 3; i++) {
                    String task = "Task-" + i;
                    socket.send(task);
                    System.out.println("Main sent: " + task);

                    String result = socket.recvString();
                    System.out.println("Main received: " + result);
                }
            }

            worker.interrupt();
            worker.join();
        }
    }
}
```

---

## 7. Polling

Polling allows monitoring multiple sockets for events simultaneously.

### Basic Polling

```java
import io.github.ulalax.zmq.*;

public class PollingExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket socket1 = new Socket(ctx, SocketType.PULL);
             Socket socket2 = new Socket(ctx, SocketType.PULL)) {

            // Setup sockets
            socket1.bind("tcp://*:5562");
            socket2.bind("tcp://*:5563");

            // Create poll items (monitor for incoming messages)
            PollItem[] items = {
                new PollItem(socket1, PollEvents.IN),
                new PollItem(socket2, PollEvents.IN)
            };

            // Simulate sending data from another thread
            Thread sender = new Thread(() -> {
                try {
                    Thread.sleep(200);
                    try (Socket pusher1 = new Socket(ctx, SocketType.PUSH);
                         Socket pusher2 = new Socket(ctx, SocketType.PUSH)) {

                        pusher1.connect("tcp://localhost:5562");
                        pusher2.connect("tcp://localhost:5563");
                        Thread.sleep(100);

                        pusher1.send("Message for socket 1");
                        Thread.sleep(50);
                        pusher2.send("Message for socket 2");
                    }
                }
            });

            sender.start();

            // Poll with 5 second timeout
            System.out.println("Polling sockets...");
            int ready = Poller.poll(items, 5000);

            System.out.println("Number of sockets with events: " + ready);

            // Check which sockets are readable
            if (items[0].isReadable()) {
                String data = socket1.recvString();
                System.out.println("Socket 1 received: " + data);
            }

            if (items[1].isReadable()) {
                String data = socket2.recvString();
                System.out.println("Socket 2 received: " + data);
            }

            sender.join();
        }
    }
}
```

### Event Loop with Polling

```java
import io.github.ulalax.zmq.*;

public class EventLoopExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket socket1 = new Socket(ctx, SocketType.PULL);
             Socket socket2 = new Socket(ctx, SocketType.PULL)) {

            socket1.bind("tcp://*:5562");
            socket2.bind("tcp://*:5563");

            PollItem[] items = {
                new PollItem(socket1, PollEvents.IN),
                new PollItem(socket2, PollEvents.IN)
            };

            // Start data generators
            startDataGenerator(ctx, "tcp://localhost:5562", "Socket1");
            startDataGenerator(ctx, "tcp://localhost:5563", "Socket2");

            Thread.sleep(200);

            // Event loop
            int messageCount = 0;
            while (messageCount < 10) {
                // Poll with 1 second timeout
                int ready = Poller.poll(items, 1000);

                if (ready > 0) {
                    for (int i = 0; i < items.length; i++) {
                        if (items[i].isReadable()) {
                            Socket socket = items[i].getSocket();
                            String data = socket.recvString();
                            System.out.println("Received: " + data);
                            messageCount++;
                        }
                    }
                } else {
                    System.out.println("Poll timeout");
                    break;
                }
            }
        }
    }

    private static void startDataGenerator(Context ctx, String endpoint, String name) {
        new Thread(() -> {
            try (Socket pusher = new Socket(ctx, SocketType.PUSH)) {
                pusher.connect(endpoint);
                Thread.sleep(100);

                for (int i = 0; i < 5; i++) {
                    pusher.send(name + " message " + i);
                    Thread.sleep(200);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
```

---

## 8. Multipart Messages

Multipart messages allow sending multiple frames as an atomic unit.

### Basic Multipart Message

```java
import io.github.ulalax.zmq.*;

public class MultipartExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PAIR);
             Socket receiver = new Socket(ctx, SocketType.PAIR)) {

            receiver.bind("inproc://multipart");
            sender.connect("inproc://multipart");
            Thread.sleep(50);

            receiver.setOption(SocketOption.RCVTIMEO, 1000);

            // Send multipart message using Message objects
            try (Message header = new Message("Header");
                 Message body = new Message("Body");
                 Message footer = new Message("Footer")) {

                // Send with SEND_MORE flag for all but the last frame
                sender.send(header, SendFlags.SEND_MORE);
                sender.send(body, SendFlags.SEND_MORE);
                sender.send(footer, SendFlags.NONE);

                System.out.println("Sent multipart message");
            }

            // Receive multipart message
            try (Message recv1 = new Message();
                 Message recv2 = new Message();
                 Message recv3 = new Message()) {

                receiver.recv(recv1, RecvFlags.NONE);
                System.out.println("Frame 1: " + recv1.toString() +
                    " (hasMore: " + receiver.hasMore() + ")");

                receiver.recv(recv2, RecvFlags.NONE);
                System.out.println("Frame 2: " + recv2.toString() +
                    " (hasMore: " + receiver.hasMore() + ")");

                receiver.recv(recv3, RecvFlags.NONE);
                System.out.println("Frame 3: " + recv3.toString() +
                    " (hasMore: " + receiver.hasMore() + ")");
            }
        }
    }
}
```

### Using MultipartMessage Utility

```java
import io.github.ulalax.zmq.*;

public class MultipartMessageExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH);
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            receiver.bind("tcp://*:5564");
            sender.connect("tcp://localhost:5564");
            Thread.sleep(100);

            receiver.setOption(SocketOption.RCVTIMEO, 1000);

            // Create and send multipart message
            MultipartMessage outMsg = new MultipartMessage();
            outMsg.add("Protocol-V1");
            outMsg.add("Command:GET");
            outMsg.add("Resource:/data");
            outMsg.add("Payload:{}");

            sender.send(outMsg);
            System.out.println("Sent multipart message with " +
                outMsg.size() + " frames");

            // Receive multipart message
            MultipartMessage inMsg = receiver.recvMultipart();
            System.out.println("Received " + inMsg.size() + " frames:");

            for (String frame : inMsg.asStrings()) {
                System.out.println("  " + frame);
            }
        }
    }
}
```

### Envelope Pattern

```java
import io.github.ulalax.zmq.*;

public class EnvelopeExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket router = new Socket(ctx, SocketType.ROUTER);
             Socket dealer = new Socket(ctx, SocketType.DEALER)) {

            router.bind("tcp://*:5565");
            dealer.setOption(SocketOption.ROUTING_ID, "CLIENT-1");
            dealer.connect("tcp://localhost:5565");
            Thread.sleep(100);

            router.setOption(SocketOption.RCVTIMEO, 1000);
            dealer.setOption(SocketOption.RCVTIMEO, 1000);

            // Dealer sends envelope message
            try (Message envelope = new Message("ENVELOPE-INFO");
                 Message body = new Message("MESSAGE-BODY")) {

                dealer.send(envelope, SendFlags.SEND_MORE);
                dealer.send(body, SendFlags.NONE);
            }

            // Router receives: [identity][envelope][body]
            try (Message identity = new Message();
                 Message envelope = new Message();
                 Message body = new Message()) {

                router.recv(identity, RecvFlags.NONE);
                System.out.println("Identity: " + identity.toString());

                router.recv(envelope, RecvFlags.NONE);
                System.out.println("Envelope: " + envelope.toString());

                router.recv(body, RecvFlags.NONE);
                System.out.println("Body: " + body.toString());

                // Reply with envelope
                try (Message replyId = new Message();
                     Message replyEnv = new Message("REPLY-ENVELOPE");
                     Message replyBody = new Message("REPLY-BODY")) {

                    replyId.copy(identity);
                    router.send(replyId, SendFlags.SEND_MORE);
                    router.send(replyEnv, SendFlags.SEND_MORE);
                    router.send(replyBody, SendFlags.NONE);
                }
            }

            // Dealer receives reply (without identity)
            MultipartMessage reply = dealer.recvMultipart();
            System.out.println("\nDealer received " + reply.size() + " frames:");
            for (String frame : reply.asStrings()) {
                System.out.println("  " + frame);
            }
        }
    }
}
```

---

## 9. Socket Monitoring

Socket monitoring provides real-time notifications about connection events.

### Basic Socket Monitoring

```java
import io.github.ulalax.zmq.*;

public class SocketMonitorExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.REP);
             Socket client = new Socket(ctx, SocketType.REQ)) {

            server.setOption(SocketOption.LINGER, 0);
            client.setOption(SocketOption.LINGER, 0);

            // Setup monitor for client socket
            String monitorEndpoint = "inproc://client-monitor";
            client.monitor(monitorEndpoint,
                SocketMonitorEvent.CONNECTED
                    .combine(SocketMonitorEvent.DISCONNECTED));

            // Create monitor socket
            try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                monitor.setOption(SocketOption.RCVTIMEO, 2000);
                monitor.connect(monitorEndpoint);

                // Bind server and connect client
                server.bind("tcp://127.0.0.1:5566");
                client.connect("tcp://127.0.0.1:5566");

                // Receive connection event
                byte[] eventFrame = monitor.recvBytes();
                String addressFrame = monitor.recvString();

                SocketMonitorEventData event =
                    SocketMonitorEventData.parse(eventFrame, addressFrame);

                System.out.println("Event: " + event.event());
                System.out.println("Address: " + event.address());
                System.out.println("Value: " + event.value());

                // Close client to trigger disconnect
                client.close();
                Thread.sleep(100);

                // Receive disconnect event
                try {
                    eventFrame = monitor.recvBytes();
                    addressFrame = monitor.recvString();
                    event = SocketMonitorEventData.parse(eventFrame, addressFrame);
                    System.out.println("\nDisconnect event: " + event.event());
                } catch (Exception e) {
                    // May timeout
                }

                // Stop monitoring
                server.stopMonitor();
            }
        }
    }
}
```

### Monitoring All Events

```java
import io.github.ulalax.zmq.*;

public class MonitorAllEventsExample {
    public static void main(String[] args) throws Exception {
        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.REP)) {

            server.setOption(SocketOption.LINGER, 0);

            // Monitor ALL events
            String monitorEndpoint = "inproc://monitor-all";
            server.monitor(monitorEndpoint, SocketMonitorEvent.ALL);

            try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                monitor.setOption(SocketOption.RCVTIMEO, 2000);
                monitor.connect(monitorEndpoint);

                // Bind server
                server.bind("tcp://127.0.0.1:5567");

                // Receive and print events
                while (true) {
                    try {
                        byte[] eventFrame = monitor.recvBytes();
                        String addressFrame = monitor.recvString();

                        SocketMonitorEventData event =
                            SocketMonitorEventData.parse(eventFrame, addressFrame);

                        System.out.println("Event: " + event.event());
                        System.out.println("  Address: " + event.address());
                        System.out.println("  Value: " + event.value());
                        System.out.println();

                        // Stop after receiving listening event
                        if (event.event() == SocketMonitorEvent.LISTENING) {
                            break;
                        }
                    } catch (Exception e) {
                        break;
                    }
                }

                server.stopMonitor();
            }
        }
    }
}
```

---

## 10. CURVE Security

CURVE provides authentication and encryption for ZeroMQ connections.

### Basic CURVE Encryption

```java
import io.github.ulalax.zmq.*;

public class CurveExample {
    public static void main(String[] args) throws Exception {
        // Check if CURVE is available
        if (!Context.has("curve")) {
            System.err.println("CURVE security is not available");
            return;
        }

        // Generate keypairs
        Curve.KeyPair serverKeys = Curve.generateKeypair();
        Curve.KeyPair clientKeys = Curve.generateKeypair();

        System.out.println("Server public key: " + serverKeys.publicKey());
        System.out.println("Client public key: " + clientKeys.publicKey());

        try (Context ctx = new Context();
             Socket server = new Socket(ctx, SocketType.REP);
             Socket client = new Socket(ctx, SocketType.REQ)) {

            // Configure server as CURVE server
            server.setOption(SocketOption.CURVE_SERVER, 1);
            server.setOption(SocketOption.CURVE_SECRETKEY,
                serverKeys.secretKey());
            server.bind("tcp://127.0.0.1:5568");

            // Configure client with CURVE credentials
            client.setOption(SocketOption.CURVE_SERVERKEY,
                serverKeys.publicKey());
            client.setOption(SocketOption.CURVE_PUBLICKEY,
                clientKeys.publicKey());
            client.setOption(SocketOption.CURVE_SECRETKEY,
                clientKeys.secretKey());
            client.connect("tcp://127.0.0.1:5568");

            Thread.sleep(200);

            client.setOption(SocketOption.RCVTIMEO, 2000);
            server.setOption(SocketOption.RCVTIMEO, 2000);

            // Encrypted communication
            client.send("Secure message");
            System.out.println("Client sent encrypted message");

            String received = server.recvString();
            System.out.println("Server received: " + received);

            server.send("Secure reply");
            String reply = client.recvString();
            System.out.println("Client received: " + reply);
        }
    }
}
```

### CURVE with Multiple Clients

```java
import io.github.ulalax.zmq.*;

public class CurveMultipleClientsExample {
    public static void main(String[] args) throws Exception {
        if (!Context.has("curve")) {
            System.err.println("CURVE not available");
            return;
        }

        // Generate keys
        Curve.KeyPair serverKeys = Curve.generateKeypair();
        Curve.KeyPair client1Keys = Curve.generateKeypair();
        Curve.KeyPair client2Keys = Curve.generateKeypair();

        try (Context ctx = new Context()) {
            // Start secure server
            Thread server = new Thread(() -> {
                try (Socket router = new Socket(ctx, SocketType.ROUTER)) {
                    router.setOption(SocketOption.CURVE_SERVER, 1);
                    router.setOption(SocketOption.CURVE_SECRETKEY,
                        serverKeys.secretKey());
                    router.bind("tcp://127.0.0.1:5569");
                    router.setOption(SocketOption.RCVTIMEO, 3000);

                    // Handle 2 requests
                    for (int i = 0; i < 2; i++) {
                        try (Message identity = new Message();
                             Message request = new Message()) {

                            router.recv(identity, RecvFlags.NONE);
                            router.recv(request, RecvFlags.NONE);

                            System.out.println("Server received: " +
                                request.toString());

                            try (Message replyId = new Message();
                                 Message reply = new Message("Secure reply " + i)) {

                                replyId.copy(identity);
                                router.send(replyId, SendFlags.SEND_MORE);
                                router.send(reply, SendFlags.NONE);
                            }
                        }
                    }
                }
            });

            // Start clients
            Thread client1 = createSecureClient(ctx, "Client-1",
                serverKeys.publicKey(), client1Keys);
            Thread client2 = createSecureClient(ctx, "Client-2",
                serverKeys.publicKey(), client2Keys);

            server.start();
            Thread.sleep(200);
            client1.start();
            client2.start();

            server.join(5000);
            client1.join(5000);
            client2.join(5000);
        }
    }

    private static Thread createSecureClient(Context ctx, String name,
            String serverPublicKey, Curve.KeyPair clientKeys) {
        return new Thread(() -> {
            try (Socket dealer = new Socket(ctx, SocketType.DEALER)) {
                dealer.setOption(SocketOption.ROUTING_ID, name);
                dealer.setOption(SocketOption.CURVE_SERVERKEY, serverPublicKey);
                dealer.setOption(SocketOption.CURVE_PUBLICKEY,
                    clientKeys.publicKey());
                dealer.setOption(SocketOption.CURVE_SECRETKEY,
                    clientKeys.secretKey());
                dealer.connect("tcp://127.0.0.1:5569");
                dealer.setOption(SocketOption.RCVTIMEO, 2000);

                dealer.send("Secure request from " + name);
                String reply = dealer.recvString();
                System.out.println(name + " received: " + reply);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
```

### Key Management

```java
import io.github.ulalax.zmq.*;

public class CurveKeyManagementExample {
    public static void main(String[] args) {
        if (!Context.has("curve")) {
            System.err.println("CURVE not available");
            return;
        }

        // Generate keypair
        Curve.KeyPair keys = Curve.generateKeypair();
        System.out.println("Generated keypair:");
        System.out.println("  Public:  " + keys.publicKey());
        System.out.println("  Secret:  (hidden in toString)");
        System.out.println("  KeyPair: " + keys);

        // Validate keys
        boolean publicValid = Curve.isValidKey(keys.publicKey());
        boolean secretValid = Curve.isValidKey(keys.secretKey());
        System.out.println("\nKey validation:");
        System.out.println("  Public key valid: " + publicValid);
        System.out.println("  Secret key valid: " + secretValid);

        // Derive public key from secret key
        String derivedPublic = Curve.derivePublicKey(keys.secretKey());
        System.out.println("\nDerived public key: " + derivedPublic);
        System.out.println("Keys match: " +
            derivedPublic.equals(keys.publicKey()));

        // Invalid key examples
        System.out.println("\nInvalid key tests:");
        System.out.println("  Empty string: " + Curve.isValidKey(""));
        System.out.println("  Too short: " +
            Curve.isValidKey("shortkey"));
        System.out.println("  Invalid chars: " +
            Curve.isValidKey("1234567890".repeat(4)));
    }
}
```

---

## Building and Running Examples

### Prerequisites

- JDK 21 or later
- Enable native access when running

### Gradle Configuration

```kotlin
// build.gradle.kts
tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
```

### Command Line Execution

```bash
# Compile
javac --enable-preview -cp "zmq-1.0.0.jar" Example.java

# Run
java --enable-native-access=ALL-UNNAMED \
     --enable-preview \
     -cp ".:zmq-1.0.0.jar" \
     Example
```

### Maven Configuration

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>3.1.0</version>
            <configuration>
                <mainClass>your.package.Example</mainClass>
                <arguments>
                    <argument>--enable-native-access=ALL-UNNAMED</argument>
                </arguments>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## Best Practices

### Resource Management

Always use try-with-resources to ensure proper cleanup:

```java
try (Context ctx = new Context();
     Socket socket = new Socket(ctx, SocketType.REP);
     Message msg = new Message("data")) {
    // Your code here
} // Automatic cleanup
```

### Error Handling

```java
try {
    String data = socket.recvString();
    // Process data
} catch (ZmqException e) {
    System.err.println("ZMQ Error: " + e.getMessage());
    System.err.println("Error code: " + e.getErrorCode());
}
```

### Timeouts

Always set timeouts to prevent indefinite blocking:

```java
socket.setOption(SocketOption.RCVTIMEO, 5000); // 5 second timeout
socket.setOption(SocketOption.SNDTIMEO, 5000);
```

### Thread Safety

Context and Socket objects are NOT thread-safe. Each thread should have its own sockets:

```java
// Bad: sharing socket between threads
Socket socket = new Socket(ctx, SocketType.REQ);
new Thread(() -> socket.send("data")).start(); // WRONG!

// Good: each thread has its own socket
new Thread(() -> {
    try (Socket socket = new Socket(ctx, SocketType.REQ)) {
        socket.send("data");
    }
}).start();
```

---

## Additional Resources

- [ZeroMQ Guide](https://zguide.zeromq.org/)
- [JVM-ZMQ API Documentation](https://github.com/ulala-x/jvm-zmq)
- [ZeroMQ RFC Specifications](https://rfc.zeromq.org/)
- [libzmq Documentation](https://libzmq.readthedocs.io/)

---

## License

These examples are released under the MIT License, same as the JVM-ZMQ library.
