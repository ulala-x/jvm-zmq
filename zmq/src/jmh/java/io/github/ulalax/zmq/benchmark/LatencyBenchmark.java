package io.github.ulalax.zmq.benchmark;

import io.github.ulalax.zmq.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * JMH Latency Benchmarks for ZMQ messaging patterns.
 * <p>
 * Measures round-trip latency using REQ-REP and DEALER-ROUTER patterns
 * with various message sizes.
 * <p>
 * JMH automatically provides statistics including: average, min, max, and percentiles.
 * <p>
 * Run with: ./gradlew :zmq:jmh
 * For more detailed statistics: ./gradlew :zmq:jmh -Pjmh.profilers=gc
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class LatencyBenchmark {

    /**
     * State class for REQ-REP pattern benchmarks.
     */
    @State(Scope.Thread)
    public static class ReqRepState {
        Context ctx;
        Socket server;
        Socket client;
        byte[] requestData;
        byte[] responseData;
        byte[] receiveBuffer;
        Thread serverThread;
        volatile boolean stopServer = false;

        @Param({"1", "128", "1024", "65536"})
        int messageSize;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            ctx = new Context();
            server = new Socket(ctx, SocketType.REP);
            client = new Socket(ctx, SocketType.REQ);

            // Configure sockets
            server.setOption(SocketOption.LINGER, 0);
            client.setOption(SocketOption.LINGER, 0);
            client.setOption(SocketOption.RCVTIMEO, 5000);
            server.setOption(SocketOption.RCVTIMEO, 5000);

            // Bind and connect
            server.bind("tcp://127.0.0.1:0");
            String endpoint = server.getOptionString(SocketOption.LAST_ENDPOINT);
            client.connect(endpoint);

            Thread.sleep(100); // Allow connection to establish

            // Prepare message data
            requestData = new byte[messageSize];
            responseData = new byte[messageSize];
            receiveBuffer = new byte[messageSize];
            Arrays.fill(requestData, (byte) 'Q');
            Arrays.fill(responseData, (byte) 'R');

            // Start echo server thread
            startEchoServer();
        }

        private void startEchoServer() {
            stopServer = false;

            serverThread = new Thread(() -> {
                try {
                    byte[] serverBuffer = new byte[messageSize];

                    while (!stopServer) {
                        try {
                            // Receive request
                            int received = server.recv(serverBuffer, RecvFlags.NONE);
                            if (received > 0) {
                                // Send response
                                server.send(responseData, SendFlags.NONE);
                            }
                        } catch (Exception e) {
                            // Socket might be closed or timeout
                            if (stopServer) break;
                        }
                    }
                } catch (Exception e) {
                    if (!stopServer) {
                        e.printStackTrace();
                    }
                }
            }, "LatencyEchoServer");

            serverThread.start();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            stopServer = true;
            if (serverThread != null) {
                serverThread.join(5000);
            }

            if (client != null) client.close();
            if (server != null) server.close();
            if (ctx != null) ctx.close();
        }
    }

    /**
     * State class for DEALER-ROUTER pattern benchmarks.
     */
    @State(Scope.Thread)
    public static class DealerRouterState {
        Context ctx;
        Socket router;
        Socket dealer;
        byte[] messageData;
        byte[] receiveBuffer;
        Thread routerThread;
        volatile boolean stopRouter = false;

        @Param({"1024"})
        int messageSize;

        @Setup(Level.Trial)
        public void setup() throws Exception {
            ctx = new Context();
            router = new Socket(ctx, SocketType.ROUTER);
            dealer = new Socket(ctx, SocketType.DEALER);

            // Configure sockets
            router.setOption(SocketOption.LINGER, 0);
            dealer.setOption(SocketOption.LINGER, 0);
            dealer.setOption(SocketOption.RCVTIMEO, 5000);
            router.setOption(SocketOption.RCVTIMEO, 5000);

            // Bind and connect
            router.bind("tcp://127.0.0.1:0");
            String endpoint = router.getOptionString(SocketOption.LAST_ENDPOINT);
            dealer.connect(endpoint);

            Thread.sleep(100); // Allow connection to establish

            // Prepare message data
            messageData = new byte[messageSize];
            receiveBuffer = new byte[messageSize];
            Arrays.fill(messageData, (byte) 'D');

            // Start router echo server thread
            startRouterEchoServer();
        }

        private void startRouterEchoServer() {
            stopRouter = false;

            routerThread = new Thread(() -> {
                try {
                    byte[] identity = new byte[256];
                    byte[] buffer = new byte[messageSize];

                    while (!stopRouter) {
                        try {
                            // Receive identity frame
                            int idLen = router.recv(identity, RecvFlags.NONE);
                            if (idLen > 0) {
                                // Receive message frame
                                int msgLen = router.recv(buffer, RecvFlags.NONE);

                                // Echo back: identity + message
                                byte[] identityFrame = Arrays.copyOf(identity, idLen);
                                byte[] messageFrame = Arrays.copyOf(buffer, msgLen);
                                router.send(identityFrame, SendFlags.SEND_MORE);
                                router.send(messageFrame, SendFlags.NONE);
                            }
                        } catch (Exception e) {
                            // Socket might be closed or timeout
                            if (stopRouter) break;
                        }
                    }
                } catch (Exception e) {
                    if (!stopRouter) {
                        e.printStackTrace();
                    }
                }
            }, "RouterEchoServer");

            routerThread.start();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @TearDown(Level.Trial)
        public void tearDown() throws Exception {
            stopRouter = true;
            if (routerThread != null) {
                routerThread.join(5000);
            }

            if (dealer != null) dealer.close();
            if (router != null) router.close();
            if (ctx != null) ctx.close();
        }
    }

    /**
     * Benchmark for REQ-REP round-trip latency.
     * <p>
     * Measures the time for a complete request-response cycle.
     * JMH will report average time in microseconds.
     */
    @Benchmark
    public void reqRepLatency(ReqRepState state, Blackhole blackhole) {
        try {
            // Send request
            state.client.send(state.requestData, SendFlags.NONE);

            // Receive response
            int received = state.client.recv(state.receiveBuffer, RecvFlags.NONE);

            blackhole.consume(received);
        } catch (Exception e) {
            throw new RuntimeException("REQ-REP cycle failed", e);
        }
    }

    /**
     * Benchmark for DEALER-ROUTER round-trip latency.
     * <p>
     * Measures the time for a complete request-response cycle using DEALER-ROUTER.
     * JMH will report average time in microseconds.
     */
    @Benchmark
    public void dealerRouterLatency(DealerRouterState state, Blackhole blackhole) {
        try {
            // Send message
            state.dealer.send(state.messageData, SendFlags.NONE);

            // Receive response
            int received = state.dealer.recv(state.receiveBuffer, RecvFlags.NONE);

            blackhole.consume(received);
        } catch (Exception e) {
            throw new RuntimeException("DEALER-ROUTER cycle failed", e);
        }
    }
}
