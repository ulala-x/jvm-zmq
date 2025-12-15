package io.github.ulalax.zmq;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents ZeroMQ socket monitor event data parsed from monitor event messages.
 *
 * <p>Socket monitor events are delivered as two-frame messages:</p>
 * <ul>
 * <li>Frame 1: 6 bytes (event: uint16 LE + value: int32 LE)</li>
 * <li>Frame 2: endpoint address string</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * try (Socket monitorSocket = new Socket(ctx, SocketType.PAIR)) {
 *     monitorSocket.connect("inproc://monitor");
 *     byte[] eventFrame = monitorSocket.recvBytes();
 *     String address = monitorSocket.recvString();
 *     SocketMonitorEventData eventData = SocketMonitorEventData.parse(eventFrame, address);
 *     System.out.println("Event: " + eventData.event() + " at " + eventData.address());
 * }
 * }</pre>
 *
 * @param event The socket monitor event type
 * @param value The event value (file descriptor or error code)
 * @param address The endpoint address associated with the event
 * @see SocketMonitorEvent
 */
public record SocketMonitorEventData(
    SocketMonitorEvent event,
    int value,
    String address
) {
    /**
     * Creates a new SocketMonitorEventData instance.
     *
     * @param event The socket monitor event type
     * @param value The event value (file descriptor or error code)
     * @param address The endpoint address
     * @throws NullPointerException if event or address is null
     */
    public SocketMonitorEventData {
        if (event == null) {
            throw new NullPointerException("event cannot be null");
        }
        if (address == null) {
            throw new NullPointerException("address cannot be null");
        }
    }

    /**
     * Parses a socket monitor event from the event frame bytes and endpoint address.
     *
     * @param eventFrame The 6-byte event frame containing event (uint16 LE) and value (int32 LE)
     * @param address The endpoint address string from the second frame
     * @return A SocketMonitorEventData instance containing the parsed event data
     * @throws IllegalArgumentException if eventFrame is not exactly 6 bytes
     * @throws NullPointerException if eventFrame or address is null
     */
    public static SocketMonitorEventData parse(byte[] eventFrame, String address) {
        if (eventFrame == null) {
            throw new NullPointerException("eventFrame cannot be null");
        }
        if (address == null) {
            throw new NullPointerException("address cannot be null");
        }
        if (eventFrame.length != 6) {
            throw new IllegalArgumentException(
                "Event frame must be exactly 6 bytes (2 bytes for event + 4 bytes for value), got " + eventFrame.length
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(eventFrame).order(ByteOrder.LITTLE_ENDIAN);

        // Read event as uint16 LE from bytes 0-1
        int eventValue = buffer.getShort() & 0xFFFF;

        // Read value as int32 LE from bytes 2-5
        int value = buffer.getInt();

        SocketMonitorEvent event = SocketMonitorEvent.fromValue(eventValue);
        return new SocketMonitorEventData(event, value, address);
    }
}
