package io.github.ulalax.zmq;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Helper for multipart messages.
 *
 * <p>Multipart messages allow you to send multiple frames as a single atomic message.
 * All frames are delivered together or not at all.</p>
 *
 * <p>Sending example:</p>
 * <pre>{@code
 * MultipartMessage msg = new MultipartMessage();
 * msg.addString("frame1");
 * msg.addString("frame2");
 * msg.send(socket);
 * }</pre>
 *
 * <p>Receiving example:</p>
 * <pre>{@code
 * MultipartMessage msg = MultipartMessage.recv(socket);
 * for (byte[] frame : msg) {
 *     // process frame
 * }
 * }</pre>
 *
 * @see Socket
 */
public final class MultipartMessage implements Iterable<byte[]> {

    private final List<byte[]> frames = new ArrayList<>();

    /**
     * Creates an empty multipart message.
     */
    public MultipartMessage() {
    }

    /**
     * Adds a byte array frame.
     * @param data The frame data
     */
    public void add(byte[] data) {
        frames.add(data.clone());
    }

    /**
     * Adds a string frame (UTF-8 encoded).
     * @param text The frame text
     */
    public void addString(String text) {
        add(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Gets the number of frames.
     * @return Frame count
     */
    public int size() {
        return frames.size();
    }

    /**
     * Gets a frame by index.
     * @param index The frame index
     * @return Frame data
     */
    public byte[] get(int index) {
        return frames.get(index);
    }

    /**
     * Gets a frame as string.
     * @param index The frame index
     * @return Frame as UTF-8 string
     */
    public String getString(int index) {
        return new String(get(index), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Sends all frames on a socket.
     * @param socket The socket to send on
     */
    public void send(Socket socket) {
        for (int i = 0; i < frames.size(); i++) {
            boolean more = i < frames.size() - 1;
            SendFlags flags = more ? SendFlags.SEND_MORE : SendFlags.NONE;
            socket.send(frames.get(i), flags);
        }
    }

    /**
     * Receives all frames from a socket.
     * @param socket The socket to receive from
     * @return The multipart message
     */
    public static MultipartMessage recv(Socket socket) {
        MultipartMessage msg = new MultipartMessage();
        do {
            byte[] frame = socket.recvBytes();
            msg.add(frame);
        } while (socket.hasMore());
        return msg;
    }

    /**
     * Clears all frames.
     */
    public void clear() {
        frames.clear();
    }

    @Override
    public Iterator<byte[]> iterator() {
        return frames.iterator();
    }
}
