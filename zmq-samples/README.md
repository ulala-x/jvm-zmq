[![English](https://img.shields.io/badge/lang-en-red.svg)](README.md)
[![한국어](https://img.shields.io/badge/lang-한국어-green.svg)](README.ko.md)

# JVM-ZMQ Samples

This module provides usage examples for the JVM-ZMQ library.

## Sample List

### PushPullSample

A Ventilator-Worker-Sink pipeline sample demonstrating the PUSH-PULL pattern.

**How to Run:**

```bash
# Run the complete pipeline (Ventilator + 3 Workers + Sink, default)
./gradlew :zmq-samples:run --args="all"

# Run Ventilator only
./gradlew :zmq-samples:run --args="ventilator"

# Run Worker only (in separate terminal, worker-id can be specified)
./gradlew :zmq-samples:run --args="worker 1"
./gradlew :zmq-samples:run --args="worker 2"

# Run Sink only
./gradlew :zmq-samples:run --args="sink"
```

**How It Works:**

1. **Ventilator (PUSH socket)**
   - Binds to `tcp://*:5557` to distribute tasks
   - Generates 100 tasks and assigns random workload (1-100ms) to each
   - Sends "START" signal to Sink to notify batch start
   - Tasks are sent in "taskNum:workload" format (e.g., "42:75")
   - Displays progress every 20 tasks

2. **Workers (PULL -> PUSH socket)**
   - Receives tasks from `tcp://localhost:5557`
   - Sends results to `tcp://localhost:5558`
   - Simulates processing by sleeping for workload duration
   - Results are sent in "workerId:taskNum:workload" format
   - Auto-terminates after 3-second timeout

3. **Sink (PULL socket)**
   - Binds to `tcp://*:5558` to collect results
   - Starts collecting results after waiting for "START" signal
   - Waits for 100 results or 5-second timeout
   - Displays task distribution statistics per Worker (task count, percentage, total workload)

**Load Balancing:**
- ZeroMQ's PUSH-PULL pattern automatically performs round-robin load balancing
- Each Worker processes approximately equal number of tasks (about 33%)
- Workers efficiently receive next task after completing previous one

**Sample Output:**

```
JVM-ZMQ PUSH-PULL Pipeline Pattern Sample
==========================================
Demonstrating Ventilator-Worker-Sink Pattern

Starting complete pipeline: 1 Ventilator, 3 Workers, 1 Sink

[Sink] Starting result collector...
[Sink] Bound to tcp://*:5558
[Sink] Waiting for batch start signal...
[Worker-1] Starting...
[Worker-1] Connected and ready for tasks
[Worker-2] Starting...
[Worker-2] Connected and ready for tasks
[Worker-3] Starting...
[Worker-3] Connected and ready for tasks
[Ventilator] Starting task generator...
[Ventilator] Bound to tcp://*:5557
[Ventilator] Distributing 100 tasks...
[Sink] Batch started, collecting results...
[Ventilator] Dispatched 20/100 tasks
[Ventilator] Dispatched 40/100 tasks
[Ventilator] Dispatched 60/100 tasks
[Ventilator] Dispatched 80/100 tasks
[Ventilator] Dispatched 100/100 tasks
[Ventilator] All 100 tasks dispatched
[Ventilator] Total expected workload: 5216ms
[Ventilator] Average per task: 52ms
[Worker-1] Processed 10 tasks (current: task#27, 40ms)
[Worker-2] Processed 10 tasks (current: task#29, 62ms)
[Worker-3] Processed 10 tasks (current: task#28, 12ms)
[Sink] Received 20/100 results
[Sink] Received 40/100 results
...
[Sink] Received 100/100 results

[Sink] ========== Pipeline Statistics ==========
[Sink] Total results received: 100/100
[Sink] Total elapsed time: 1.83s

[Sink] Worker Load Distribution:
[Sink]   Worker-1: 34 tasks (34.0%), 1816ms workload
[Sink]   Worker-2: 33 tasks (33.0%), 1753ms workload
[Sink]   Worker-3: 33 tasks (33.0%), 1647ms workload
[Sink] =============================================
[Sink] Done

Pipeline completed successfully!
```

### PubSubSample

A topic-based message broadcasting sample demonstrating the PUB-SUB pattern.

**How to Run:**

```bash
# Run Publisher and Subscriber simultaneously (default)
./gradlew :zmq-samples:run

# Run Publisher only
./gradlew :zmq-samples:run --args="pub"

# Run Subscriber only (after starting Publisher in separate terminal)
./gradlew :zmq-samples:run --args="sub"

# Run both mode explicitly
./gradlew :zmq-samples:run --args="both"
```

**How It Works:**

1. **Publisher (PUB socket)**
   - Binds to `tcp://*:5556`
   - Broadcasts 10 messages across 3 topics (weather, sports, news)
   - Each message starts with topic name (e.g., "weather Update #1")
   - Sends messages at 500ms intervals

2. **Subscriber (SUB socket)**
   - Connects to `tcp://localhost:5556`
   - Subscribes to "weather" and "news" topics only (sports is filtered out)
   - Sets 2-second receive timeout
   - Receives only subscribed topic messages

**Topic Filtering:**
- Subscriber receives only "weather" and "news" messages
- "sports" messages are automatically filtered and not received
- Topic filtering works by message prefix matching

**Sample Output:**

```
JVM-ZMQ PUB-SUB Sample
=====================

[Publisher] Starting...
[Publisher] Binding to tcp://*:5556
[Subscriber] Starting...
[Subscriber] Subscribed to 'weather' and 'news' topics
[Publisher] Sent: weather Update #1
[Subscriber] Received: weather Update #1
[Publisher] Sent: sports Update #2    <- filtered (not received)
[Publisher] Sent: news Update #3
[Subscriber] Received: news Update #3
[Publisher] Sent: weather Update #4
[Subscriber] Received: weather Update #4
...
[Publisher] Done
[Subscriber] Timeout, no message received
[Subscriber] Done
```

### ReqRepSample

A simple client-server sample demonstrating the REQ-REP pattern.

**How to Run:**

```bash
# Run server and client simultaneously (default)
./gradlew :zmq-samples:run

# Run server only
./gradlew :zmq-samples:run --args="server"

# Run client only (after starting server in separate terminal)
./gradlew :zmq-samples:run --args="client"

# Run both mode explicitly
./gradlew :zmq-samples:run --args="both"
```

**How It Works:**

1. **Server (REP socket)**
   - Binds to `tcp://*:5555`
   - Receives 5 requests and replies to each
   - Waits 100ms before processing each request (simulates processing)

2. **Client (REQ socket)**
   - Connects to `tcp://localhost:5555`
   - Sends 5 "Hello #N" messages
   - Receives "Reply #N" response for each request

**Sample Output:**

```
JVM-ZMQ REQ-REP Sample
=====================

[Server] Starting...
[Server] Listening on tcp://*:5555
[Client] Starting...
[Client] Connected to tcp://localhost:5555
[Client] Sent: Hello #1
[Server] Received: Hello #1
[Server] Sent: Reply #1
[Client] Received: Reply #1
[Client] Sent: Hello #2
[Server] Received: Hello #2
[Server] Sent: Reply #2
[Client] Received: Reply #2
...
[Server] Done
[Client] Done
```

### RouterDealerSample

An asynchronous broker sample demonstrating the ROUTER-DEALER pattern.

**How to Run:**

```bash
# Run class directly
java --enable-native-access=ALL-UNNAMED -cp "zmq-samples/build/libs/zmq-samples-0.1.jar:zmq/build/libs/zmq-0.1.jar:zmq-core/build/libs/zmq-core-0.1.jar" io.github.ulalax.zmq.samples.RouterDealerSample
```

**How It Works:**

1. **Broker (ROUTER socket)**
   - Frontend: Binds to `tcp://*:5555` to receive client requests
   - Backend: Binds to `tcp://*:5556` to communicate with workers
   - Uses Poller to asynchronously handle messages from frontend and backend
   - Queues client requests and routes to available workers
   - Registers workers as available when receiving "READY" message

2. **Client (DEALER socket)**
   - Sets unique client ID via ROUTING_ID option
   - Connects to broker at `tcp://localhost:5555`
   - Each client sends 3 requests sequentially
   - Synchronously waits for response to each request

3. **Worker (DEALER socket)**
   - Sets unique worker ID via ROUTING_ID option
   - Connects to broker's backend at `tcp://localhost:5556`
   - Sends "READY" message on initial connection to signal ready state
   - Simulates 300ms processing on receiving request, then sends response

**Message Envelope Format:**

- Client → Broker: `[client-identity][empty][request]`
- Broker → Worker: `[worker-identity][empty][client-identity][empty][request]`
- Worker → Broker: `[worker-identity][empty][client-identity][empty][reply]`
- Broker → Client: `[client-identity][empty][reply]`

**Load Balancing:**
- Broker manages client request queue and available worker queue
- Assigns new requests only when workers are ready (Lazy Pirate pattern)
- Workers are added back to available queue after processing request for next assignment

**Sample Output:**

```
JVM-ZMQ ROUTER-DEALER Async Broker Sample
==========================================

This sample demonstrates the async broker pattern:
- Broker with ROUTER frontend and ROUTER backend
- Multiple DEALER clients sending requests
- Multiple DEALER workers processing requests

[Broker] Starting...
[Broker] Frontend listening on tcp://*:5555
[Broker] Backend listening on tcp://*:5556
[Broker] Polling started...
[worker-1] Starting...
[worker-2] Starting...
[worker-1] Connected to broker
[worker-2] Connected to broker
[Broker] Worker worker-1 is ready
[Broker] Worker worker-2 is ready
[client-1] Starting...
[client-2] Starting...
[client-1] Connected to broker
[client-2] Connected to broker
[client-1] Sent: Request #1 from client-1
[Broker] Client client-1 -> Request: Request #1 from client-1
[Broker] Routed to Worker worker-1 for Client client-1
[worker-1] Processing request from client-1: Request #1 from client-1
[worker-1] Sent reply to client-1: Processed by worker-1
[Broker] Worker worker-1 -> Client client-1: Processed by worker-1
[client-1] Received: Processed by worker-1
...
[client-1] Done
[client-2] Done

All clients completed. Press Ctrl+C to exit.
```

### RouterToRouterSample

An advanced routing sample demonstrating the ROUTER-to-ROUTER pattern. Implemented with clean code using the MultipartMessage API.

**How to Run:**

```bash
# Run using Gradle task
./gradlew :zmq-samples:runRouterToRouter

# Or run class directly
java --enable-native-access=ALL-UNNAMED -cp "zmq-samples/build/install/zmq-samples/lib/*" io.github.ulalax.zmq.samples.RouterToRouterSample
```

**How It Works:**

This sample includes three Router-to-Router pattern examples:

#### Example 1: Basic Peer-to-Peer

Two ROUTER sockets communicating directly.

- **Peer A**: Binds to `tcp://127.0.0.1:15700`, ROUTING_ID = "PEER_A"
- **Peer B**: Connects to Peer A, ROUTING_ID = "PEER_B"
- Each peer uses the other's ROUTING_ID as first frame to send messages
- Uses MultipartMessage to send in [target-id][message] format
- Bidirectional communication: Peer B → Peer A → Peer B

**Message Format:**
- Send: `[Target Identity][Message Content]`
- Receive: `[Sender Identity][Message Content]`

#### Example 2: Hub and Spoke Pattern

Central hub communicating with multiple spokes.

- **Hub**: Binds to `tcp://127.0.0.1:15701`, ROUTING_ID = "HUB"
- **Spoke1-3**: Connect to Hub, ROUTING_ID = "SPOKE1", "SPOKE2", "SPOKE3"
- Each spoke sends registration message to Hub
- Hub broadcasts messages to all registered spokes
- Topology: Centralized communication (all communication goes through Hub)

**Operation Sequence:**
1. 3 spokes send "REGISTER:SPOKEX" messages to Hub
2. Hub receives all registration messages and adds to peer list
3. Hub broadcasts welcome message to each spoke
4. Each spoke receives and displays their message

#### Example 3: Broker Pattern

Clients exchanging messages through a broker.

- **Broker**: Binds to `tcp://127.0.0.1:15702`, ROUTING_ID = "BROKER"
- **Client1-2**: Connect to Broker, ROUTING_ID = "CLIENT1", "CLIENT2"
- Clients send messages to other clients through broker
- Broker forwards messages including original sender information
- 3-frame message: `[Broker][Target][Message]` → `[Target][Sender][Message]`

**Message Flow:**
1. Client1 → Broker: `[BROKER][CLIENT2][Hello Client2, this is Client1!]`
2. Broker → Client2: `[CLIENT2][CLIENT1][Hello Client2, this is Client1!]`
3. Client2 → Broker: `[BROKER][CLIENT1][Got your message! Reply from Client2.]`
4. Broker → Client1: `[CLIENT1][CLIENT2][Got your message! Reply from Client2.]`

**Key Features:**

- **Explicit Routing ID**: All ROUTER sockets must set explicit ID via `ROUTING_ID` option
- **MultipartMessage API**: Cleanly compose and send frame-based messages
- **Fully Asynchronous**: Flexible network topology implementation with bidirectional async communication
- **Message Routing**: First frame is always used as target/sender ID

**Use Cases:**
- Building P2P networks
- Implementing message brokers and proxies
- Complex network topologies (mesh, hub-spoke, broker, etc.)
- Systems requiring explicit addressing

**Sample Output:**

```
=== JVM-ZMQ Router-to-Router Examples (MultipartMessage API) ===

--- Example 1: Basic Peer-to-Peer (MultipartMessage) ---
Peer B sending message to Peer A...
Peer A received from [PEER_B]: Hello from Peer B!
Peer A replying to Peer B...
Peer B received from [PEER_A]: Hello back from Peer A!

--- Example 2: Hub and Spoke Pattern (MultipartMessage) ---
Spokes sending registration to Hub...
Hub received: [SPOKE1] -> REGISTER:SPOKE1
Hub received: [SPOKE2] -> REGISTER:SPOKE2
Hub received: [SPOKE3] -> REGISTER:SPOKE3

Hub broadcasting to all spokes...
SPOKE1 received from [HUB]: Welcome SPOKE1! You are connected.
SPOKE2 received from [HUB]: Welcome SPOKE2! You are connected.
SPOKE3 received from [HUB]: Welcome SPOKE3! You are connected.

--- Example 3: Broker Pattern (MultipartMessage) ---
Client1 sending message to Client2 via Broker...
Broker received from [CLIENT1]: forward to [CLIENT2] -> Hello Client2, this is Client1!
Client2 received from [CLIENT1] (via BROKER): Hello Client2, this is Client1!

Client2 replying to Client1 via Broker...
Client1 received from [CLIENT2] (via BROKER): Got your message! Reply from Client2.


All examples completed!
```

### ProxySample

A message broker sample demonstrating the XPub-XSub Proxy pattern.

**How to Run:**

```bash
# Run using Gradle task
./gradlew :zmq-samples:runProxy
```

**How It Works:**

1. **Proxy (XSub-XPub sockets)**
   - Frontend (XSUB): Binds to `tcp://*:5559` for publishers to connect
   - Backend (XPUB): Binds to `tcp://*:5560` for subscribers to connect
   - `Proxy.start(frontend, backend)` bidirectionally forwards messages and subscriptions
   - Acts as intermediary between publishers and subscribers

2. **Publishers (PUB sockets)**
   - Publisher-1: Publishes 10 messages with "weather" topic
   - Publisher-2: Publishes 10 messages with "sports" topic
   - Connect to proxy's Frontend (XSUB)
   - Send messages at 800ms intervals

3. **Subscribers (SUB sockets)**
   - Subscriber-1: Subscribes to "weather" topic only
   - Subscriber-2: Subscribes to "sports" topic only
   - Subscriber-3: Subscribes to both "weather" and "sports" topics
   - Connect to proxy's Backend (XPUB)
   - 15-second receive timeout

**Proxy Pattern Advantages:**
- Complete decoupling of publishers and subscribers
- Dynamic addition/removal of publishers/subscribers
- Subscription information automatically forwarded to publishers through proxy
- Simplified network topology (all connections concentrated at proxy)

**Message Flow:**
```
Publishers → Frontend (XSUB) → Proxy → Backend (XPUB) → Subscribers
                                  ↓
                        Subscription info reverse propagation
```

**Sample Output:**

```
JVM-ZMQ XPub-XSub Proxy Pattern Sample
======================================

Architecture:
  Publishers -> XSub (Frontend) -> Proxy -> XPub (Backend) -> Subscribers

This sample demonstrates:
  - XSub socket receiving from multiple publishers
  - XPub socket distributing to multiple subscribers
  - Built-in Proxy forwarding messages and subscriptions
  - Dynamic subscription handling

[Proxy] Starting XPub-XSub proxy...
[Proxy] Frontend XSub bound to tcp://*:5559 (for publishers)
[Proxy] Backend XPub bound to tcp://*:5560 (for subscribers)
[Proxy] Proxy running - forwarding messages and subscriptions...

[Publisher-1] Starting...
[Publisher-1] Connected to proxy frontend (tcp://localhost:5559)
[Publisher-1] Publishing topic: 'weather'

[Publisher-2] Starting...
[Publisher-2] Connected to proxy frontend (tcp://localhost:5559)
[Publisher-2] Publishing topic: 'sports'

[Subscriber-1] Starting...
[Subscriber-1] Connected to proxy backend (tcp://localhost:5560)
[Subscriber-1] Subscribed to topic: 'weather'

[Subscriber-2] Starting...
[Subscriber-2] Connected to proxy backend (tcp://localhost:5560)
[Subscriber-2] Subscribed to topic: 'sports'

[Subscriber-3] Starting...
[Subscriber-3] Connected to proxy backend (tcp://localhost:5560)
[Subscriber-3] Subscribed to topic: 'weather'
[Subscriber-3] Subscribed to topic: 'sports'

[Publisher-1] Sent: weather Update #1 from Publisher-1
[Subscriber-1] Received: weather Update #1 from Publisher-1
[Subscriber-3] Received: weather Update #1 from Publisher-1

[Publisher-2] Sent: sports Update #1 from Publisher-2
[Subscriber-2] Received: sports Update #1 from Publisher-2
[Subscriber-3] Received: sports Update #1 from Publisher-2

...

[Subscriber-1] Received 10 messages. Unsubscribing...
[Subscriber-1] Unsubscribed from topic: 'weather'
[Subscriber-1] Completed

[Subscriber-2] Received 10 messages. Unsubscribing...
[Subscriber-2] Unsubscribed from topic: 'sports'
[Subscriber-2] Completed

[Subscriber-3] Received 15 messages. Unsubscribing...
[Subscriber-3] Unsubscribed from topic: 'weather'
[Subscriber-3] Unsubscribed from topic: 'sports'
[Subscriber-3] Completed

All subscribers completed.
```

### PollerSample

A sample using Poller to simultaneously monitor multiple sockets. Demonstrates non-blocking I/O and timeout handling.

**How to Run:**

```bash
# Run using Gradle task
./gradlew :zmq-samples:runPoller
```

**How It Works:**

1. **Receivers (PULL sockets)**
   - Receiver 1: Binds to `tcp://*:5561`
   - Receiver 2: Binds to `tcp://*:5562`
   - Uses Poller to simultaneously monitor readable events on both sockets

2. **Senders (PUSH sockets)**
   - Sender-1: Sends 10 messages at 300ms intervals
   - Sender-2: Sends 10 messages at 500ms intervals
   - Runs as daemon threads, sending messages in background

3. **Polling Loop**
   - `Poller.poll(pollItems, 1000)`: Waits for socket events with 1-second timeout
   - `pollItem.isReadable()`: Checks readability of each socket
   - Continues polling until 20 messages received

**Poller Advantages:**
- Efficiently monitors multiple sockets simultaneously (leverages select/poll system calls)
- Receives messages without blocking
- Prevents infinite waiting with timeout
- Suitable for implementing event-driven architecture

**Sample Output:**

```
JVM-ZMQ Poller Sample
====================

This sample demonstrates:
  - Polling multiple sockets simultaneously
  - Non-blocking receive with timeout
  - Handling multiple message sources

[Main] Receiver 1 bound to tcp://*:5561
[Main] Receiver 2 bound to tcp://*:5562

[Sender-2] Connected to tcp://localhost:5562
[Sender-1] Connected to tcp://localhost:5561
[Main] Starting to poll both receivers...

[Receiver-1] Message #1 from Sender-1
[Receiver-2] Message #1 from Sender-2
[Receiver-1] Message #2 from Sender-1
[Receiver-2] Message #2 from Sender-2
[Receiver-1] Message #3 from Sender-1
[Receiver-1] Message #4 from Sender-1
[Receiver-2] Message #3 from Sender-2
[Receiver-1] Message #5 from Sender-1
[Receiver-2] Message #4 from Sender-2
[Receiver-1] Message #6 from Sender-1
[Receiver-1] Message #7 from Sender-1
[Receiver-2] Message #5 from Sender-2
[Receiver-1] Message #8 from Sender-1
[Receiver-1] Message #9 from Sender-1
[Receiver-2] Message #6 from Sender-2
[Receiver-1] Message #10 from Sender-1
[Receiver-2] Message #7 from Sender-2
[Sender-1] Done sending
[Receiver-2] Message #8 from Sender-2
[Receiver-2] Message #9 from Sender-2
[Receiver-2] Message #10 from Sender-2

[Main] Received 20 total messages
[Main] Done
[Sender-2] Done sending
```

### SteerableProxySample

A dynamic proxy sample demonstrating the Steerable Proxy pattern. Proxy can be controlled at runtime with PAUSE/RESUME/TERMINATE commands.

**How to Run:**

```bash
# Run using Gradle task
./gradlew :zmq-samples:runSteerableProxy
```

**How It Works:**

1. **Steerable Proxy (XSub-XPub-PAIR sockets)**
   - Frontend (XSUB): Binds to `tcp://*:5564` for publishers to connect
   - Backend (XPUB): Binds to `tcp://*:5565` for subscribers to connect
   - Control (PAIR): Binds to `inproc://proxy-control` to receive control commands
   - `Proxy.startSteerable(frontend, backend, control)` starts controllable proxy
   - Message flow can be controlled by control commands

2. **Controller (PAIR socket)**
   - Connects to proxy's Control socket
   - Sends 3 types of control commands:
     - `PAUSE`: Temporarily pause message flow (messages are queued)
     - `RESUME`: Resume message flow (queued messages are delivered)
     - `TERMINATE`: Safely terminate proxy
   - Timing: 2s flow → PAUSE → 2s wait → RESUME → 2s flow → TERMINATE

3. **Publisher (PUB socket)**
   - Connects to proxy's Frontend (XSUB)
   - Publishes 15 messages with "news" topic
   - Sends messages at 500ms intervals
   - Continues sending during PAUSE, messages are queued in proxy

4. **Subscriber (SUB socket)**
   - Connects to proxy's Backend (XPUB)
   - Subscribes to "news" topic
   - 1-second receive timeout
   - Message reception stops during PAUSE, receives queued messages at once on RESUME

**Steerable Proxy Advantages:**
- Runtime message flow control
- Backpressure mechanism implementation
- Temporarily pause traffic during maintenance windows
- Safe shutdown scenarios (prevent queued message loss)
- Traffic control and flow management

**Control Commands:**
```
PAUSE      - Temporarily pause message flow (queuing)
RESUME     - Resume message flow
TERMINATE  - Terminate proxy
STATISTICS - Request statistics (returns 8 uint64 values)
```

**Message Flow:**
```
Publisher → Frontend (XSUB) → Proxy → Backend (XPUB) → Subscriber
                                ↑
                                |
                            Controller (PAIR)
                            PAUSE/RESUME/TERMINATE
```

**Sample Output:**

```
JVM-ZMQ Steerable Proxy Sample
=============================

This sample demonstrates:
  - Steerable proxy with control socket
  - PAUSE/RESUME/TERMINATE commands
  - Dynamic proxy control at runtime

[Proxy] Starting steerable proxy...
[Proxy] Frontend XSub: tcp://*:5564
[Proxy] Backend XPub:  tcp://*:5565
[Proxy] Control:       inproc://proxy-control

[Publisher] Starting...
[Controller] Starting...
[Subscriber] Starting...
[Controller] Connected to proxy control socket

[Subscriber] Subscribed to 'news'

[Publisher] Sent: news Message #1
[Subscriber] Received: news Message #1
[Publisher] Sent: news Message #2
[Subscriber] Received: news Message #2
[Publisher] Sent: news Message #3
[Subscriber] Received: news Message #3
[Publisher] Sent: news Message #4
[Subscriber] Received: news Message #4

[Controller] >>> Sending PAUSE command
[Controller] Proxy paused - messages will be queued

[Publisher] Sent: news Message #5  <- queued
[Publisher] Sent: news Message #6  <- queued
[Publisher] Sent: news Message #7  <- queued
[Publisher] Sent: news Message #8  <- queued

[Controller] >>> Sending RESUME command
[Controller] Proxy resumed - queued messages will flow

[Subscriber] Received: news Message #5  <- receiving queued messages
[Subscriber] Received: news Message #6
[Subscriber] Received: news Message #7
[Subscriber] Received: news Message #8

[Publisher] Sent: news Message #9
[Publisher] Sent: news Message #10
[Subscriber] Waiting for messages...

[Controller] >>> Sending TERMINATE command
[Controller] Proxy termination requested
[Controller] Done

[Proxy] Terminated
[Subscriber] Received 8 messages total
[Subscriber] Done

[Main] Done
```

### MonitorSample

A real-time event monitoring sample demonstrating the Socket Monitor pattern. Tracks socket lifecycle events (binding, connection, disconnection, etc.) in real-time.

**How to Run:**

```bash
# Run using Gradle task
./gradlew :zmq-samples:runMonitor
```

**How It Works:**

1. **Hub (ROUTER socket)**
   - Binds to `tcp://*:5564`, ROUTING_ID = "HUB"
   - Publishes all socket events to `inproc://hub-monitor` through monitor socket
   - Performs Router-to-Router communication with 2 Spokes

2. **Monitor (PAIR socket)**
   - Connects to `inproc://hub-monitor` to receive Hub's events
   - Continuously polls events in background thread
   - Formats and displays each event with timestamp, icon, and color

3. **Spokes (ROUTER sockets)**
   - Spoke1, Spoke2: Connect to Hub, using "SPOKE1", "SPOKE2" IDs respectively
   - Send registration message to Hub and receive broadcast messages
   - Spoke1 disconnects to generate DISCONNECTED event

**Monitoring Events:**
- **LISTENING** (green ▶): Socket is bound and waiting for connections
- **ACCEPTED** (green ✓): New inbound connection accepted (FD displayed)
- **CONNECTED** (green ↗): Outbound connection successfully established (FD displayed)
- **DISCONNECTED** (yellow ✗): Connection terminated
- **CLOSED** (yellow ⊗): Socket closed
- **BIND_FAILED** (red ✗): Binding failed (error code displayed)
- **ACCEPT_FAILED** (red ✗): Connection acceptance failed (error code displayed)
- **MONITOR_STOPPED** (cyan ■): Monitoring stopped

**Monitor API Usage:**
```java
// Start monitoring - publish all events to inproc endpoint
socket.monitor("inproc://monitor-endpoint", SocketMonitorEvent.ALL);

// Or monitor specific events only
SocketMonitorEvent events = SocketMonitorEvent.CONNECTED
    .combine(SocketMonitorEvent.DISCONNECTED);
socket.monitor("inproc://monitor-endpoint", events);

// Receive events with monitor socket (use PAIR socket)
Socket monitor = new Socket(ctx, SocketType.PAIR);
monitor.connect("inproc://monitor-endpoint");

// Receive event (2-frame message)
byte[] eventFrame = monitor.recvBytes();  // 6 bytes: event(uint16) + value(int32)
String address = monitor.recvString();     // endpoint address

// Parse event
SocketMonitorEventData eventData = SocketMonitorEventData.parse(eventFrame, address);
System.out.println("Event: " + eventData.event());
System.out.println("Address: " + eventData.address());
System.out.println("Value: " + eventData.value());  // FD or error code

// Stop monitoring
socket.stopMonitor();
```

**Use Cases:**
- Connection status debugging and diagnostics
- Network event logging
- Connection troubleshooting and monitoring
- System health checks and alerts

**Sample Output:**

```
=== JVM-ZMQ Socket Monitor Sample ===
Router-to-Router with Real-time Event Monitoring

[Monitor] Attaching to Hub socket (inproc://hub-monitor)
[Monitor] Watching for: All events

[Hub] Binding to tcp://*:5564...
[09:45:04.188] ▶ EVENT: LISTENING
           Address: tcp://0.0.0.0:5564

[Spoke1] Connecting to Hub...
[09:45:04.382] ✓ EVENT: ACCEPTED
           Address: tcp://127.0.0.1:5564
           FD: 18

[Spoke2] Connecting to Hub...
[09:45:04.582] ✓ EVENT: ACCEPTED
           Address: tcp://127.0.0.1:5564
           FD: 20

--- Message Exchange ---
[Hub] Received from [SPOKE1]: Hello from SPOKE1!
[Hub] Received from [SPOKE2]: Hello from SPOKE2!

[Hub] Broadcasting to all spokes...
[SPOKE1] received: Welcome SPOKE1!
[SPOKE2] received: Welcome SPOKE2!

[Spoke1] Disconnecting...
[09:45:04.886] ✗ EVENT: DISCONNECTED
           Address: tcp://127.0.0.1:5564

[Monitor] Stopping...

Sample completed!
```

### RouterBenchmarkSample

A performance benchmark sample for the ROUTER-to-ROUTER pattern. Measures maximum performance using inproc transport and analyzes message processing speed.

**How to Run:**

```bash
# Run using Gradle task
./gradlew :zmq-samples:runRouterBenchmark
```

**How It Works:**

1. **Benchmark Configuration**
   - Transport: inproc (in-process, lowest latency)
   - Message count: 10,000 messages
   - Message size: 64 bytes (random data)
   - Pattern: One-way routing (router1 → router2)

2. **ROUTER Socket Setup**
   - Router1: Binds to `inproc://router-bench` with "router1" ROUTING_ID
   - Router2: Connects to `inproc://router-bench` with "router2" ROUTING_ID
   - Can send messages directly to peer with explicit routing ID setting

3. **Message Format**
   - Send: `[Target Identity (SEND_MORE)][Message Payload]`
   - Receive: `[Sender Identity][Message Payload...]`
   - Router1 sends messages using Router2's ID
   - Router2 receives messages including Router1's ID

4. **Performance Measurement**
   - Total elapsed time (milliseconds)
   - Average time per message (microseconds)
   - Messages per second throughput (msg/s)
   - Precise measurement with System.nanoTime()

**Benchmark Features:**

- **Inproc Transport**: Measures pure ZeroMQ routing performance without network overhead
- **Frame-based Communication**: Multi-frame messages using SendFlags.SEND_MORE
- **Identity Routing**: Explicit peer specification with ROUTING_ID
- **Memory Management**: Proper Message object close() handling

**Sample Output:**

```
=== JVM-ZMQ Router-to-Router Performance Benchmark ===

Configuration:
  Transport:     inproc (in-process)
  Message count: 10,000
  Message size:  64 bytes
  Pattern:       One-way routing (router1 → router2)

Starting benchmark...

  Progress: 10,000/10,000

=== Benchmark Results ===
Total time:       1,234 ms
Average per msg:  123.400 μs/op
Throughput:       8,103 msg/s

Benchmark completed successfully!
```

**Use Cases:**

- Router-to-Router pattern performance measurement
- Performance comparison baseline with other transport protocols (tcp, ipc)
- Throughput analysis by message size
- System maximum processing capacity assessment

### CurveSecuritySample

A secure communication sample using CURVE encryption. Demonstrates server-client encryption and authentication using ZeroMQ's CURVE mechanism.

**How to Run:**

```bash
# Run using Gradle task
./gradlew :zmq-samples:runCurveSecurity
```

**How It Works:**

1. **CURVE Support Check**
   - Checks libzmq's CURVE support with `Context.has("curve")`
   - CURVE requires libsodium library and must be included in libzmq build

2. **Keypair Generation**
   - Server keypair: `Curve.generateKeypair()` - generates server's public/secret keys
   - Client keypair: `Curve.generateKeypair()` - generates client's public/secret keys
   - All keys are represented in Z85 format (40-character ASCII string)

3. **Public Key Derivation**
   - `Curve.derivePublicKey(secretKey)` - computes public key from secret key
   - Verifies that generated and derived public keys match

4. **Server Setup (REP socket)**
   - `CURVE_SERVER=1`: Set to server mode
   - `CURVE_SECRETKEY`: Set server's secret key
   - Binds to `tcp://*:5563` to wait for encrypted connections
   - Authenticates clients with client's public key

5. **Client Setup (REQ socket)**
   - `CURVE_SERVERKEY`: Set server's public key (for server authentication)
   - `CURVE_PUBLICKEY`: Set client's public key
   - `CURVE_SECRETKEY`: Set client's secret key
   - Encrypted connection to server at `tcp://localhost:5563`

6. **Encrypted Communication**
   - 3 message exchanges in REQ-REP pattern
   - All messages are transmitted encrypted with CURVE
   - 5-second receive timeout

**CURVE Security Mechanism:**
- **CurveCP Protocol**: Based on elliptic curve cryptography (Curve25519)
- **Mutual Authentication**: Server and client authenticate each other with public keys
- **Forward Secrecy**: Previous messages remain safe even if session key is exposed
- **Encryption**: All messages automatically encrypted in transit
- **Replay Attack Prevention**: Blocks replay attacks with nonce mechanism

**Key Format:**
- Z85 encoding: 40 printable ASCII characters
- Binary key size: 32 bytes (256 bits)
- Example: `WC}N(M?OtYffU-466X[rWiBTl4XDUja[sgvIzGM&`

**Use Cases:**
- Secure communication over untrusted networks
- Systems requiring client authentication
- Environments requiring message eavesdropping and tampering prevention
- Secure communication between IoT devices and servers

**Sample Output:**

```
JVM-ZMQ CURVE Security Sample
============================

This sample demonstrates:
  - CURVE keypair generation
  - Secure encrypted communication
  - Server and client authentication setup

[Setup] CURVE security is available

[Server] Generated keypair:
  Public:  WC}N(M?OtYffU-466X[rWiBTl4XDUja[sgvIzGM&
  Secret:  O(t0p*O.N!y0:}09SLZ[70^W2BLf@MwCuGV!<7w5

[Client] Generated keypair:
  Public:  H*)T8hlWYMqGwyOwx5dZkjmVPrPzATFZ#=dX>0Y6
  Secret:  ql)h1N#v3)5@t@}[yRp8o2@PmsRZTO4#F5Y5Tz1d

[Client] Derived public key from secret: H*)T8hlWYMqGwyOwx5dZkjmVPrPzATFZ#=dX>0Y6
[Client] Keys match: true

[Server] Starting secure server...
[Server] Bound to tcp://*:5563 with CURVE encryption
[Client] Starting secure client...
[Client] Connected to tcp://localhost:5563 with CURVE encryption

[Client] Sent encrypted: Secure request #1
[Server] Received encrypted: Secure request #1
[Server] Sent encrypted: Secure response #1
[Client] Received encrypted: Secure response #1

[Client] Sent encrypted: Secure request #2
[Server] Received encrypted: Secure request #2
[Server] Sent encrypted: Secure response #2
[Client] Received encrypted: Secure response #2

[Client] Sent encrypted: Secure request #3
[Server] Received encrypted: Secure request #3
[Server] Sent encrypted: Secure response #3
[Client] Received encrypted: Secure response #3

[Server] Done
[Client] Done

[Main] Secure communication completed successfully!
```

## Sample Execution Summary

All samples can be easily run through Gradle tasks:

```bash
# Dedicated task for each sample
./gradlew :zmq-samples:runReqRep           # REQ-REP sample
./gradlew :zmq-samples:runPubSub           # PUB-SUB sample
./gradlew :zmq-samples:runPushPull         # PUSH-PULL sample
./gradlew :zmq-samples:runPair             # PAIR sample
./gradlew :zmq-samples:runRouterDealer     # ROUTER-DEALER sample
./gradlew :zmq-samples:runRouterToRouter   # ROUTER-to-ROUTER sample
./gradlew :zmq-samples:runRouterBenchmark  # ROUTER-to-ROUTER benchmark
./gradlew :zmq-samples:runProxy            # Proxy (XPub-XSub) sample
./gradlew :zmq-samples:runSteerableProxy   # Steerable Proxy sample
./gradlew :zmq-samples:runPoller           # Poller sample
./gradlew :zmq-samples:runMonitor          # Socket Monitor sample
./gradlew :zmq-samples:runCurveSecurity    # CURVE Security sample
```

## Build

```bash
# Build samples
./gradlew :zmq-samples:build

# Run samples
./gradlew :zmq-samples:run
```

## Requirements

- JDK 22 or higher
- libzmq library must be installed on the system
