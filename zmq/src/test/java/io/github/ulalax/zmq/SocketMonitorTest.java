package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Socket Monitor functionality.
 * Socket Monitor provides real-time event monitoring for socket lifecycle events
 * including connection, binding, disconnection, and other state changes.
 */
@Tag("Feature")
@Tag("Monitor")
class SocketMonitorTest {

    /**
     * Tests for connection-related monitor events (Connected, Accepted).
     */
    @Nested
    @DisplayName("Connection Events")
    class ConnectionEvents {

        @Test
        @DisplayName("Should receive Connected event when client connects to server")
        void should_Receive_Connected_Event_When_Client_Connects() throws Exception {
            // Given: A REQ-REP socket pair with monitor configured for Connected events
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.setOption(SocketOption.LINGER, 0);
                client.setOption(SocketOption.LINGER, 0);

                String monitorEndpoint = "inproc://monitor-connected";
                client.monitor(monitorEndpoint, SocketMonitorEvent.CONNECTED);

                try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                    monitor.setOption(SocketOption.RCVTIMEO, 2000);
                    monitor.connect(monitorEndpoint);

                    server.bind("tcp://127.0.0.1:15750");

                    // When: Client connects to the server
                    client.connect("tcp://127.0.0.1:15750");

                    // Then: Monitor should receive a Connected event with the correct address
                    byte[] eventFrame = monitor.recvBytes().value();
                    String addressFrame = monitor.recvString().value();

                    SocketMonitorEventData eventData = SocketMonitorEventData.parse(eventFrame, addressFrame);
                    assertThat(eventData.event())
                            .as("Event type should be Connected")
                            .isEqualTo(SocketMonitorEvent.CONNECTED);
                    assertThat(eventData.address())
                            .as("Address should match connected endpoint")
                            .isEqualTo("tcp://127.0.0.1:15750");
                }
            }
        }

        @Test
        @DisplayName("Should receive Accepted event when server accepts client connection")
        void should_Receive_Accepted_Event_When_Server_Accepts_Connection() throws Exception {
            // Given: A REQ-REP socket pair with monitor configured for Listening and Accepted events
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.setOption(SocketOption.LINGER, 0);
                client.setOption(SocketOption.LINGER, 0);

                String monitorEndpoint = "inproc://monitor-accepted";
                server.monitor(monitorEndpoint, SocketMonitorEvent.LISTENING.combine(SocketMonitorEvent.ACCEPTED));

                try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                    monitor.setOption(SocketOption.RCVTIMEO, 2000);
                    monitor.connect(monitorEndpoint);

                    server.bind("tcp://127.0.0.1:15752");

                    // Receive Listening event first
                    byte[] listeningEventFrame = monitor.recvBytes().value();
                    String listeningAddressFrame = monitor.recvString().value();
                    SocketMonitorEventData listeningEvent = SocketMonitorEventData.parse(listeningEventFrame, listeningAddressFrame);
                    assertThat(listeningEvent.event())
                            .as("First event should be Listening")
                            .isEqualTo(SocketMonitorEvent.LISTENING);

                    // When: Client connects to the server
                    client.connect("tcp://127.0.0.1:15752");
                    Thread.sleep(100); // Allow connection to establish

                    // Then: Monitor should receive an Accepted event
                    byte[] acceptedEventFrame = monitor.recvBytes().value();
                    String acceptedAddressFrame = monitor.recvString().value();

                    SocketMonitorEventData eventData = SocketMonitorEventData.parse(acceptedEventFrame, acceptedAddressFrame);
                    assertThat(eventData.event())
                            .as("Event type should be Accepted")
                            .isEqualTo(SocketMonitorEvent.ACCEPTED);
                    assertThat(eventData.address())
                            .as("Address should start with tcp://127.0.0.1")
                            .startsWith("tcp://127.0.0.1");
                }
            }
        }
    }

    /**
     * Tests for binding-related monitor events (Listening).
     */
    @Nested
    @DisplayName("Binding Events")
    class BindingEvents {

        @Test
        @DisplayName("Should receive Listening event when socket starts listening")
        void should_Receive_Listening_Event_When_Socket_Binds() throws Exception {
            // Given: A REP socket with monitor configured for Listening events
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP)) {

                server.setOption(SocketOption.LINGER, 0);

                String monitorEndpoint = "inproc://monitor-listening";
                server.monitor(monitorEndpoint, SocketMonitorEvent.LISTENING);

                try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                    monitor.setOption(SocketOption.RCVTIMEO, 2000);
                    monitor.connect(monitorEndpoint);

                    // When: Server binds to an address
                    server.bind("tcp://127.0.0.1:15751");

                    // Then: Monitor should receive a Listening event with the correct address
                    byte[] eventFrame = monitor.recvBytes().value();
                    String addressFrame = monitor.recvString().value();

                    SocketMonitorEventData eventData = SocketMonitorEventData.parse(eventFrame, addressFrame);
                    assertThat(eventData.event())
                            .as("Event type should be Listening")
                            .isEqualTo(SocketMonitorEvent.LISTENING);
                    assertThat(eventData.address())
                            .as("Address should match bound endpoint")
                            .isEqualTo("tcp://127.0.0.1:15751");
                }
            }
        }
    }

    /**
     * Tests for disconnection-related monitor events (Disconnected).
     */
    @Nested
    @DisplayName("Disconnection Events")
    class DisconnectionEvents {

        @Test
        @DisplayName("Should receive Disconnected event when client disconnects")
        void should_Receive_Disconnected_Event_When_Client_Disconnects() throws Exception {
            // Given: A REP socket with monitor configured for multiple events including Disconnected
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP)) {

                server.setOption(SocketOption.LINGER, 0);

                String monitorEndpoint = "inproc://monitor-disconnected";
                server.monitor(monitorEndpoint,
                        SocketMonitorEvent.LISTENING
                                .combine(SocketMonitorEvent.ACCEPTED)
                                .combine(SocketMonitorEvent.DISCONNECTED));

                try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                    monitor.setOption(SocketOption.RCVTIMEO, 2000);
                    monitor.connect(monitorEndpoint);

                    server.bind("tcp://127.0.0.1:15755");

                    // Receive Listening event
                    byte[] listeningEventFrame = monitor.recvBytes().value();
                    String listeningAddressFrame = monitor.recvString().value();
                    SocketMonitorEventData listeningEvent = SocketMonitorEventData.parse(listeningEventFrame, listeningAddressFrame);
                    assertThat(listeningEvent.event())
                            .as("First event should be Listening")
                            .isEqualTo(SocketMonitorEvent.LISTENING);

                    // When: Client connects and then disconnects
                    try (Socket client = new Socket(ctx, SocketType.REQ)) {
                        client.setOption(SocketOption.LINGER, 0);
                        client.connect("tcp://127.0.0.1:15755");
                        Thread.sleep(100); // Allow connection to establish

                        // Receive Accepted event
                        byte[] acceptedEventFrame = monitor.recvBytes().value();
                        String acceptedAddressFrame = monitor.recvString().value();
                        SocketMonitorEventData acceptedEvent = SocketMonitorEventData.parse(acceptedEventFrame, acceptedAddressFrame);
                        assertThat(acceptedEvent.event())
                                .as("Second event should be Accepted")
                                .isEqualTo(SocketMonitorEvent.ACCEPTED);

                        // Client will be disposed here, causing disconnect
                    }

                    Thread.sleep(100); // Allow disconnect to propagate

                    // Then: Monitor should receive a Disconnected event
                    // May receive multiple events (e.g., HANDSHAKE_FAILED, CLOSED), look for DISCONNECTED
                    SocketMonitorEventData eventData = null;
                    for (int i = 0; i < 5; i++) {
                        try {
                            byte[] eventFrame = monitor.recvBytes().value();
                            String addressFrame = monitor.recvString().value();
                            SocketMonitorEventData data = SocketMonitorEventData.parse(eventFrame, addressFrame);

                            if (data.event() == SocketMonitorEvent.DISCONNECTED) {
                                eventData = data;
                                break;
                            }
                        } catch (Exception e) {
                            // No more events
                            break;
                        }
                    }

                    assertThat(eventData)
                            .as("Should receive a Disconnected event")
                            .isNotNull();
                    assertThat(eventData.address())
                            .as("Address should start with tcp://127.0.0.1")
                            .startsWith("tcp://127.0.0.1");
                }
            }
        }
    }

    /**
     * Tests for event filtering functionality in Socket Monitor.
     */
    @Nested
    @DisplayName("Event Filtering")
    class EventFiltering {

        @Test
        @DisplayName("Should only receive events matching the configured filter")
        void should_Only_Receive_Filtered_Events() throws Exception {
            // Given: A monitor configured to only receive Connected events
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP);
                 Socket client = new Socket(ctx, SocketType.REQ)) {

                server.setOption(SocketOption.LINGER, 0);
                client.setOption(SocketOption.LINGER, 0);

                String monitorEndpoint = "inproc://monitor-filtered";
                client.monitor(monitorEndpoint, SocketMonitorEvent.CONNECTED);

                try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                    monitor.setOption(SocketOption.RCVTIMEO, 2000);
                    monitor.connect(monitorEndpoint);

                    server.bind("tcp://127.0.0.1:15753");

                    // When: Client connects and then disconnects
                    client.connect("tcp://127.0.0.1:15753");

                    // Then: Should receive Connected event
                    byte[] eventFrame = monitor.recvBytes().value();
                    String addressFrame = monitor.recvString().value();

                    SocketMonitorEventData eventData = SocketMonitorEventData.parse(eventFrame, addressFrame);
                    assertThat(eventData.event())
                            .as("Should receive Connected event")
                            .isEqualTo(SocketMonitorEvent.CONNECTED);

                    // When: Client is disposed (triggering disconnect)
                    client.close();

                    // Then: Should NOT receive Disconnected event because it's filtered out
                    // With Result API, the recv will return empty result (would block) on timeout
                    RecvResult<byte[]> result = monitor.recvBytes();

                    assertThat(result.wouldBlock())
                            .as("No Disconnected event should be received due to filtering")
                            .isTrue();
                }
            }
        }
    }

    /**
     * Tests for monitor lifecycle management (start/stop).
     */
    @Nested
    @DisplayName("Monitor Lifecycle")
    class MonitorLifecycle {

        @Test
        @DisplayName("Should receive MonitorStopped event when monitoring is stopped")
        void should_Receive_MonitorStopped_Event_When_Monitoring_Stops() throws Exception {
            // Given: A socket with active monitoring configured for all events
            try (Context ctx = new Context();
                 Socket server = new Socket(ctx, SocketType.REP)) {

                server.setOption(SocketOption.LINGER, 0);

                String monitorEndpoint = "inproc://monitor-stop";
                server.monitor(monitorEndpoint, SocketMonitorEvent.ALL);

                try (Socket monitor = new Socket(ctx, SocketType.PAIR)) {
                    monitor.setOption(SocketOption.RCVTIMEO, 2000);
                    monitor.connect(monitorEndpoint);

                    server.bind("tcp://127.0.0.1:15754");

                    // Receive initial Listening event
                    byte[] listeningEventFrame = monitor.recvBytes().value();
                    String listeningAddressFrame = monitor.recvString().value();
                    SocketMonitorEventData listeningEvent = SocketMonitorEventData.parse(listeningEventFrame, listeningAddressFrame);
                    assertThat(listeningEvent.event())
                            .as("First event should be Listening")
                            .isEqualTo(SocketMonitorEvent.LISTENING);

                    // When: Monitoring is stopped by calling stopMonitor
                    server.stopMonitor();

                    // Then: Monitor should receive a MonitorStopped event
                    byte[] stoppedEventFrame = monitor.recvBytes().value();
                    String stoppedAddressFrame = monitor.recvString().value();

                    SocketMonitorEventData eventData = SocketMonitorEventData.parse(stoppedEventFrame, stoppedAddressFrame);
                    assertThat(eventData.event())
                            .as("Event type should be MonitorStopped")
                            .isEqualTo(SocketMonitorEvent.MONITOR_STOPPED);
                }
            }
        }
    }
}
