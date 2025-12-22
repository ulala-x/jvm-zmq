package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.*;
import io.github.ulalax.zmq.core.ZmqException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Socket Monitor Sample
 *
 * <p>Demonstrates real-time socket event monitoring using ZeroMQ's monitoring capabilities.
 * This sample shows how to track connection events, disconnections, and other socket lifecycle events.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Monitor socket lifecycle events (LISTENING, ACCEPTED, DISCONNECTED, etc.)</li>
 *   <li>Receive events through PAIR socket pattern via inproc:// transport</li>
 *   <li>Real-time event visualization with timestamps and formatted output</li>
 *   <li>Hub-and-Spoke pattern with Router-to-Router monitoring</li>
 * </ul>
 *
 * <p>Monitoring events include:</p>
 * <ul>
 *   <li>LISTENING: Socket is ready to accept connections</li>
 *   <li>ACCEPTED: New connection established</li>
 *   <li>CONNECTED: Outbound connection successful</li>
 *   <li>DISCONNECTED: Connection closed</li>
 *   <li>MONITOR_STOPPED: Monitoring has been stopped</li>
 * </ul>
 */
public class MonitorSample {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void main(String[] args) {
        System.out.println("=== JVM-ZMQ Socket Monitor Sample ===");
        System.out.println("Router-to-Router with Real-time Event Monitoring");
        System.out.println();

        // Run the Hub-and-Spoke example with monitoring
        hubAndSpokeWithMonitoring();

        System.out.println();
        System.out.println("Sample completed!");
    }

    /**
     * Hub-and-Spoke pattern with real-time socket monitoring.
     * Demonstrates monitoring a Router (Hub) socket while multiple Router (Spoke) sockets connect to it.
     */
    private static void hubAndSpokeWithMonitoring() {
        try (Context ctx = new Context();
             Socket hub = new Socket(ctx, SocketType.ROUTER);
             Socket spoke1 = new Socket(ctx, SocketType.ROUTER);
             Socket spoke2 = new Socket(ctx, SocketType.ROUTER)) {

            // Configure all sockets
            for (Socket socket : new Socket[]{hub, spoke1, spoke2}) {
                socket.setOption(SocketOption.LINGER, 0);
                socket.setOption(SocketOption.RCVTIMEO, 1000);
            }

            // Set explicit identities for Router-to-Router communication
            hub.setOption(SocketOption.ROUTING_ID, "HUB");
            spoke1.setOption(SocketOption.ROUTING_ID, "SPOKE1");
            spoke2.setOption(SocketOption.ROUTING_ID, "SPOKE2");

            // Create monitor socket to receive events from the Hub
            try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                String monitorEndpoint = "inproc://hub-monitor";

                System.out.println("[Monitor] Attaching to Hub socket (" + monitorEndpoint + ")");
                System.out.println("[Monitor] Watching for: All events");
                System.out.println();

                // Start monitoring the Hub socket - all events will be sent to the monitor socket
                hub.monitor(monitorEndpoint, SocketMonitorEvent.ALL);

                // Connect monitor socket to receive events
                monitor.connect(monitorEndpoint);
                monitor.setOption(SocketOption.RCVTIMEO, 100);

                // Start monitor thread
                AtomicBoolean monitorRunning = new AtomicBoolean(true);
                Thread monitorThread = new Thread(() -> monitorEventsThread(monitor, monitorRunning));
                monitorThread.setName("MonitorThread");
                monitorThread.setDaemon(true);
                monitorThread.start();

                // Give monitor time to initialize
                sleep(100);

                // Hub binds - this will generate a LISTENING event
                System.out.println("[Hub] Binding to tcp://*:5564...");
                hub.bind("tcp://*:5564");
                sleep(200); // Allow monitor to process LISTENING event

                // Spokes connect - each will generate an ACCEPTED event on the Hub
                System.out.println("[Spoke1] Connecting to Hub...");
                spoke1.connect("tcp://127.0.0.1:5564");
                sleep(200); // Allow monitor to process ACCEPTED event

                System.out.println("[Spoke2] Connecting to Hub...");
                spoke2.connect("tcp://127.0.0.1:5564");
                sleep(200); // Allow monitor to process ACCEPTED event

                // Spokes send registration messages to Hub
                System.out.println();
                System.out.println("--- Message Exchange ---");
                sendMultipart(spoke1, "HUB", "Hello from SPOKE1!");
                sendMultipart(spoke2, "HUB", "Hello from SPOKE2!");

                sleep(100);

                // Hub receives messages
                List<String> registeredSpokes = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    Message spokeIdMsg = new Message();
                    hub.recv(spokeIdMsg, RecvFlags.NONE);
                    byte[] spokeIdBytes = spokeIdMsg.toByteArray();
                    if (!hub.hasMore()) {
                        System.err.println("Error: Expected message frame");
                        continue;
                    }
                    String message = hub.recvString();
                    String spokeId = new String(spokeIdBytes, StandardCharsets.UTF_8);
                    registeredSpokes.add(spokeId);
                    System.out.println("[Hub] Received from [" + spokeId + "]: " + message);
                }

                // Hub broadcasts to all spokes
                System.out.println();
                System.out.println("[Hub] Broadcasting to all spokes...");
                for (String spokeId : registeredSpokes) {
                    sendMultipart(hub, spokeId, "Welcome " + spokeId + "!");
                }

                // Spokes receive broadcasts
                for (Socket spoke : new Socket[]{spoke1, spoke2}) {
                    Message fromMsg = new Message();
                    spoke.recv(fromMsg, RecvFlags.NONE);
                    if (!spoke.hasMore()) {
                        System.err.println("Error: Expected message frame");
                        continue;
                    }
                    String msg = spoke.recvString();
                    String spokeName = (spoke == spoke1) ? "SPOKE1" : "SPOKE2";
                    System.out.println("[" + spokeName + "] received: " + msg);
                }

                // Disconnect spoke1 - this will generate a DISCONNECTED event
                System.out.println();
                System.out.println("[Spoke1] Disconnecting...");
                spoke1.close();
                sleep(300); // Allow monitor to process DISCONNECTED event

                // Stop monitoring - this will generate a MONITOR_STOPPED event
                System.out.println();
                System.out.println("[Monitor] Stopping...");
                monitorRunning.set(false);
                sleep(200);

                // Wait for monitor thread to finish
                try {
                    monitorThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        } catch (Exception e) {
            System.err.println("Error in hubAndSpokeWithMonitoring: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Monitor thread that continuously receives and displays socket events.
     *
     * @param monitor The monitor socket to receive events from
     * @param running Atomic boolean flag to control thread execution
     */
    private static void monitorEventsThread(Socket monitor, AtomicBoolean running) {
        try {
            while (running.get()) {
                try {
                    // Receive monitor event (two-frame message)
                    // Frame 1: 6 bytes (event type: uint16 LE + value: int32 LE)
                    // Frame 2: endpoint address string
                    Message eventMsg = new Message();
                    monitor.recv(eventMsg, RecvFlags.NONE);
                    byte[] eventFrame = eventMsg.toByteArray();
                    if (!monitor.hasMore()) {
                        // Invalid monitor message format
                        continue;
                    }

                    String address = monitor.recvString();

                    // Parse the event data
                    SocketMonitorEventData eventData = SocketMonitorEventData.parse(eventFrame, address);

                    // Display the event with formatted output
                    printMonitorEvent(eventData);

                } catch (ZmqException ex) {
                    if (ex.isAgain()) {
                        // Timeout - continue polling
                        sleep(10);
                    } else {
                        // Other error - might be socket closed
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            // Socket was closed or monitoring stopped - exit gracefully
        }
    }

    /**
     * Formats and prints a monitor event with timestamp and event-specific styling.
     *
     * @param eventData The parsed monitor event data
     */
    private static void printMonitorEvent(SocketMonitorEventData eventData) {
        // Filter out unknown events
        // We only want to display the events defined in SocketMonitorEvent enum
        SocketMonitorEvent event = eventData.event();
        if (event == null || event == SocketMonitorEvent.ALL) {
            return; // Skip unknown events
        }

        // Check if this is a known event
        boolean isDefined = false;
        for (SocketMonitorEvent knownEvent : SocketMonitorEvent.values()) {
            if (knownEvent == event && knownEvent != SocketMonitorEvent.ALL) {
                isDefined = true;
                break;
            }
        }
        if (!isDefined) {
            return; // Skip unknown events
        }

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String eventName = event.name();
        String icon = getEventIcon(event);
        ConsoleColor color = getEventColor(event);

        // Set color and print event
        setConsoleColor(color);
        System.out.println("[" + timestamp + "] " + icon + " EVENT: " + eventName);
        System.out.println("           Address: " + eventData.address());

        if (isErrorEvent(event)) {
            System.out.println("           Error Code: " + eventData.value());
        } else if (event == SocketMonitorEvent.CONNECTED || event == SocketMonitorEvent.ACCEPTED) {
            System.out.println("           FD: " + eventData.value());
        }

        resetConsoleColor();
        System.out.println();
    }

    /**
     * Returns a visual icon for different event types.
     *
     * @param eventType The socket monitor event type
     * @return A string icon representing the event
     */
    private static String getEventIcon(SocketMonitorEvent eventType) {
        return switch (eventType) {
            case LISTENING -> "▶";
            case ACCEPTED -> "✓";
            case CONNECTED -> "↗";
            case DISCONNECTED -> "✗";
            case CLOSED -> "⊗";
            case CONNECT_DELAYED -> "⏸";
            case CONNECT_RETRIED -> "↻";
            case BIND_FAILED -> "✗";
            case ACCEPT_FAILED -> "✗";
            case CLOSE_FAILED -> "✗";
            case MONITOR_STOPPED -> "■";
            default -> "•";
        };
    }

    /**
     * Returns a console color for different event types.
     *
     * @param eventType The socket monitor event type
     * @return A ConsoleColor enum value
     */
    private static ConsoleColor getEventColor(SocketMonitorEvent eventType) {
        return switch (eventType) {
            case LISTENING, ACCEPTED, CONNECTED -> ConsoleColor.GREEN;
            case DISCONNECTED, CLOSED -> ConsoleColor.YELLOW;
            case CONNECT_DELAYED, CONNECT_RETRIED -> ConsoleColor.DARK_YELLOW;
            case BIND_FAILED, ACCEPT_FAILED, CLOSE_FAILED -> ConsoleColor.RED;
            case MONITOR_STOPPED -> ConsoleColor.CYAN;
            default -> ConsoleColor.WHITE;
        };
    }

    /**
     * Checks if the event is an error event.
     *
     * @param eventType The socket monitor event type
     * @return true if the event represents an error condition
     */
    private static boolean isErrorEvent(SocketMonitorEvent eventType) {
        return eventType == SocketMonitorEvent.BIND_FAILED
            || eventType == SocketMonitorEvent.ACCEPT_FAILED
            || eventType == SocketMonitorEvent.CLOSE_FAILED;
    }

    /**
     * Helper method to send a multipart message.
     *
     * @param socket The socket to send on
     * @param frames The string frames to send
     */
    private static void sendMultipart(Socket socket, String... frames) {
        for (int i = 0; i < frames.length; i++) {
            SendFlags flags = (i < frames.length - 1) ? SendFlags.SEND_MORE : SendFlags.NONE;
            socket.send(frames[i], flags);
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
        }
    }

    /**
     * Sets console color using ANSI escape codes.
     *
     * @param color The color to set
     */
    private static void setConsoleColor(ConsoleColor color) {
        System.out.print(color.getAnsiCode());
    }

    /**
     * Resets console color to default.
     */
    private static void resetConsoleColor() {
        System.out.print(ConsoleColor.RESET.getAnsiCode());
    }

    /**
     * Console color enumeration with ANSI escape codes.
     */
    private enum ConsoleColor {
        GREEN("\u001B[32m"),
        YELLOW("\u001B[33m"),
        DARK_YELLOW("\u001B[33m"),  // Same as YELLOW in ANSI
        RED("\u001B[31m"),
        CYAN("\u001B[36m"),
        WHITE("\u001B[37m"),
        RESET("\u001B[0m");

        private final String ansiCode;

        ConsoleColor(String ansiCode) {
            this.ansiCode = ansiCode;
        }

        public String getAnsiCode() {
            return ansiCode;
        }
    }
}
