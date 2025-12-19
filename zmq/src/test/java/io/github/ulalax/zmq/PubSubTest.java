package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PUB-SUB pattern.
 */
@Tag("Socket")
@Tag("PubSub")
class PubSubTest {

    @Nested
    @DisplayName("Basic Pub-Sub")
    class BasicPubSub {

        @Test
        @DisplayName("Should publish and receive messages")
        void should_Publish_And_Receive_Messages() throws Exception {
            // Given: A PUB publisher and SUB subscriber connected
            try (Context ctx = new Context();
                 Socket pub = new Socket(ctx, SocketType.PUB);
                 Socket sub = new Socket(ctx, SocketType.SUB)) {

                pub.bind("inproc://test-pubsub");

                // Subscribe to all messages
                sub.subscribe("");
                sub.connect("inproc://test-pubsub");

                // Give time for subscription to propagate
                Thread.sleep(100);

                // When: Publish a message
                String message = "Hello PubSub";
                pub.send(message);

                // Then: Subscriber should receive the message
                sub.setOption(SocketOption.RCVTIMEO, 1000);
                byte[] received = sub.recvBytes().value();

                assertThat(received).isNotNull();
                assertThat(new String(received, StandardCharsets.UTF_8))
                        .as("Received message")
                        .isEqualTo(message);
            }
        }
    }

    @Nested
    @DisplayName("Topic Filtering")
    class TopicFiltering {

        @Test
        @DisplayName("Should filter messages by topic prefix")
        void should_Filter_Messages_By_Topic_Prefix() throws Exception {
            // Given: A publisher and subscriber with topic filter
            try (Context ctx = new Context();
                 Socket pub = new Socket(ctx, SocketType.PUB);
                 Socket sub = new Socket(ctx, SocketType.SUB)) {

                pub.bind("inproc://test-topics");

                // Subscribe only to "news." prefix
                sub.subscribe("news.");
                sub.connect("inproc://test-topics");

                Thread.sleep(100);

                // Set receive timeout
                sub.setOption(SocketOption.RCVTIMEO, 500);

                // When: Publish a news message
                pub.send("news.sports Hello");

                // Then: Should receive the news message
                byte[] received = sub.recvBytes().value();
                assertThat(received).isNotNull();
                assertThat(new String(received, StandardCharsets.UTF_8))
                        .as("Received news message")
                        .startsWith("news.");

                // When: Publish a weather message (should be filtered out)
                pub.send("weather.today Sunny");

                // Then: Should not receive the weather message (timeout)
                byte[] buffer = new byte[256];
                RecvResult<Integer> bytesReceived = sub.recv(buffer, RecvFlags.DONT_WAIT);
                assertThat(bytesReceived.wouldBlock())
                        .as("Filtered message should not be received")
                        .isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Multiple Subscribers")
    class MultipleSubscribers {

        @Test
        @DisplayName("Should broadcast to multiple subscribers")
        void should_Broadcast_To_Multiple_Subscribers() throws Exception {
            // Given: A publisher and two subscribers
            try (Context ctx = new Context();
                 Socket pub = new Socket(ctx, SocketType.PUB);
                 Socket sub1 = new Socket(ctx, SocketType.SUB);
                 Socket sub2 = new Socket(ctx, SocketType.SUB)) {

                pub.bind("inproc://test-multi-sub");

                // Both subscribers subscribe to all messages
                sub1.subscribe("");
                sub1.connect("inproc://test-multi-sub");

                sub2.subscribe("");
                sub2.connect("inproc://test-multi-sub");

                Thread.sleep(100);

                sub1.setOption(SocketOption.RCVTIMEO, 1000);
                sub2.setOption(SocketOption.RCVTIMEO, 1000);

                // When: Publish a broadcast message
                String message = "Broadcast";
                pub.send(message);

                // Then: Both subscribers should receive the message
                byte[] received1 = sub1.recvBytes().value();
                byte[] received2 = sub2.recvBytes().value();

                assertThat(received1).isNotNull();
                assertThat(received2).isNotNull();

                assertThat(new String(received1, StandardCharsets.UTF_8))
                        .as("First subscriber received message")
                        .isEqualTo(message);
                assertThat(new String(received2, StandardCharsets.UTF_8))
                        .as("Second subscriber received message")
                        .isEqualTo(message);
            }
        }
    }
}
