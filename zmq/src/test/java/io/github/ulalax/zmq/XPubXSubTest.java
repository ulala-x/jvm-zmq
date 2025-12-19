package io.github.ulalax.zmq;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for XPub and XSub socket types.
 * XPUB/XSUB is an extended publish-subscribe pattern that provides access to subscription messages.
 * XPUB exposes subscription/unsubscription messages, and XSUB allows manual subscription control.
 */
@Tag("Socket")
@Tag("XPubXSub")
class XPubXSubTest {

    /**
     * Tests for subscription and unsubscription message handling.
     */
    @Nested
    @DisplayName("Subscription Messages")
    class SubscriptionMessages {

        @Test
        @DisplayName("XPUB socket should receive subscription messages from subscribers")
        void should_Receive_Subscription_Messages() throws Exception {
            // Given: An XPUB socket bound and a SUB socket connected
            try (Context ctx = new Context();
                 Socket xpub = new Socket(ctx, SocketType.XPUB);
                 Socket sub = new Socket(ctx, SocketType.SUB)) {

                xpub.setOption(SocketOption.LINGER, 0);
                sub.setOption(SocketOption.LINGER, 0);
                xpub.setOption(SocketOption.RCVTIMEO, 1000);

                xpub.bind("tcp://127.0.0.1:15630");
                sub.connect("tcp://127.0.0.1:15630");

                Thread.sleep(200);

                // When: Subscriber subscribes to a topic
                sub.subscribe("news");
                Thread.sleep(200);

                // Then: XPUB receives subscription message with 0x01 indicator
                byte[] subMsg = xpub.recvBytes().value();
                assertThat(subMsg)
                        .as("Subscription message")
                        .isNotNull();
                assertThat(subMsg[0])
                        .as("Subscribe indicator")
                        .isEqualTo((byte) 0x01);

                String topic = new String(subMsg, 1, subMsg.length - 1, StandardCharsets.UTF_8);
                assertThat(topic)
                        .as("Subscription topic")
                        .isEqualTo("news");
            }
        }

        @Test
        @DisplayName("XPUB socket should receive unsubscription messages from subscribers")
        void should_Receive_Unsubscription_Messages() throws Exception {
            // Given: An XPUB socket with an active subscription
            try (Context ctx = new Context();
                 Socket xpub = new Socket(ctx, SocketType.XPUB);
                 Socket sub = new Socket(ctx, SocketType.SUB)) {

                xpub.setOption(SocketOption.LINGER, 0);
                sub.setOption(SocketOption.LINGER, 0);
                xpub.setOption(SocketOption.RCVTIMEO, 1000);

                xpub.bind("tcp://127.0.0.1:15633");
                sub.connect("tcp://127.0.0.1:15633");

                Thread.sleep(200);

                sub.subscribe("topic");
                Thread.sleep(200);
                byte[] subMsg = xpub.recvBytes().value();
                assertThat(subMsg[0])
                        .as("Subscribe indicator")
                        .isEqualTo((byte) 0x01);

                // When: Subscriber unsubscribes from the topic
                sub.unsubscribe("topic");
                Thread.sleep(200);

                // Then: XPUB receives unsubscription message with 0x00 indicator
                byte[] unsubMsg = xpub.recvBytes().value();
                assertThat(unsubMsg[0])
                        .as("Unsubscribe indicator")
                        .isEqualTo((byte) 0x00);

                String topic = new String(unsubMsg, 1, unsubMsg.length - 1, StandardCharsets.UTF_8);
                assertThat(topic)
                        .as("Unsubscription topic")
                        .isEqualTo("topic");
            }
        }

        @Test
        @DisplayName("XPUB socket should send messages to subscribed subscribers")
        void should_Send_Messages_To_Subscribers() throws Exception {
            // Given: An XPUB socket connected to a subscribed SUB socket
            try (Context ctx = new Context();
                 Socket xpub = new Socket(ctx, SocketType.XPUB);
                 Socket sub = new Socket(ctx, SocketType.SUB)) {

                xpub.setOption(SocketOption.LINGER, 0);
                sub.setOption(SocketOption.LINGER, 0);
                xpub.setOption(SocketOption.RCVTIMEO, 500);
                sub.setOption(SocketOption.RCVTIMEO, 1000);

                xpub.bind("tcp://127.0.0.1:15631");
                sub.connect("tcp://127.0.0.1:15631");

                Thread.sleep(200);

                sub.subscribe("");
                Thread.sleep(200);

                // Drain subscription message
                try {
                    xpub.recvBytes();
                } catch (Exception e) {
                    // Ignore if timeout
                }

                // When: XPUB sends a message
                xpub.send("Hello from XPub");

                // Then: Subscriber receives the message
                String msg = sub.recvString().value();
                assertThat(msg)
                        .as("Received message")
                        .isEqualTo("Hello from XPub");
            }
        }
    }

    /**
     * Tests for manual subscription control using XSUB socket.
     */
    @Nested
    @DisplayName("Manual Subscription")
    class ManualSubscription {

        @Test
        @DisplayName("XSUB socket should receive messages after manual subscription")
        void should_Receive_After_Manual_Subscription() throws Exception {
            // Given: An XSUB socket connected to a PUB socket
            try (Context ctx = new Context();
                 Socket pub = new Socket(ctx, SocketType.PUB);
                 Socket xsub = new Socket(ctx, SocketType.XSUB)) {

                pub.setOption(SocketOption.LINGER, 0);
                xsub.setOption(SocketOption.LINGER, 0);
                xsub.setOption(SocketOption.RCVTIMEO, 1000);

                pub.bind("tcp://127.0.0.1:15632");
                xsub.connect("tcp://127.0.0.1:15632");

                Thread.sleep(200);

                // When: XSUB sends a manual subscription message (0x01 for subscribe to all)
                byte[] subscribeMsg = new byte[]{0x01};
                xsub.send(subscribeMsg);

                Thread.sleep(300);

                pub.send("Message from Pub");

                Thread.sleep(100);

                // Then: XSUB receives the published message
                String msg = xsub.recvString().value();
                assertThat(msg)
                        .as("Received message")
                        .isEqualTo("Message from Pub");
            }
        }
    }
}
