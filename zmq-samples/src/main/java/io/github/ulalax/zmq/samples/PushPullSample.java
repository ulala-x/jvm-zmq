package io.github.ulalax.zmq.samples;

import io.github.ulalax.zmq.Context;
import io.github.ulalax.zmq.Socket;
import io.github.ulalax.zmq.SocketOption;
import io.github.ulalax.zmq.SocketType;
import io.github.ulalax.zmq.core.ZmqException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * PUSH-PULL Pipeline Pattern Sample
 *
 * <p>Demonstrates the Ventilator-Worker-Sink pattern using ZeroMQ PUSH-PULL sockets.
 * This pattern implements a parallel task distribution pipeline where:</p>
 * <ul>
 *   <li>Ventilator (PUSH) - Distributes tasks to workers</li>
 *   <li>Workers (PULL-PUSH) - Process tasks and forward results</li>
 *   <li>Sink (PULL) - Collects results and displays statistics</li>
 * </ul>
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>All components: java PushPullSample all (default)</li>
 *   <li>Ventilator only: java PushPullSample ventilator</li>
 *   <li>Worker only: java PushPullSample worker [worker-id]</li>
 *   <li>Sink only: java PushPullSample sink</li>
 * </ul>
 */
public class PushPullSample {

    // Port configuration
    private static final String VENTILATOR_ADDRESS = "tcp://*:5557";
    private static final String SINK_ADDRESS = "tcp://*:5558";
    private static final String VENTILATOR_CONNECT_ADDRESS = "tcp://localhost:5557";
    private static final String SINK_CONNECT_ADDRESS = "tcp://localhost:5558";

    // Workload configuration
    private static final int TASK_COUNT = 100;
    private static final int WORKER_COUNT = 3;

    public static void main(String[] args) {
        System.out.println("JVM-ZMQ PUSH-PULL Pipeline Pattern Sample");
        System.out.println("==========================================");
        System.out.println("Demonstrating Ventilator-Worker-Sink Pattern");
        System.out.println();

        String mode = args.length > 0 ? args[0].toLowerCase() : "all";

        if (mode.equals("ventilator")) {
            runVentilator();
            return;
        }

        if (mode.equals("worker")) {
            int workerId = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            runWorker(workerId);
            return;
        }

        if (mode.equals("sink")) {
            runSink();
            return;
        }

        if (mode.equals("all")) {
            System.out.println("Starting complete pipeline: 1 Ventilator, " + WORKER_COUNT + " Workers, 1 Sink");
            System.out.println();

            CountDownLatch latch = new CountDownLatch(1);

            // Start Sink first
            Thread sinkThread = new Thread(() -> {
                runSink();
                latch.countDown();
            });
            sinkThread.start();

            // Wait for sink to initialize
            sleep(500);

            // Start Workers
            List<Thread> workerThreads = new ArrayList<>();
            for (int i = 0; i < WORKER_COUNT; i++) {
                int workerId = i + 1;
                Thread workerThread = new Thread(() -> runWorker(workerId));
                workerThread.start();
                workerThreads.add(workerThread);
            }

            // Wait for workers to initialize
            sleep(500);

            // Start Ventilator
            Thread ventilatorThread = new Thread(PushPullSample::runVentilator);
            ventilatorThread.start();

            // Wait for all components to complete
            try {
                ventilatorThread.join();
                for (Thread workerThread : workerThreads) {
                    workerThread.join();
                }
                latch.await();

                System.out.println();
                System.out.println("Pipeline completed successfully!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for completion");
            }
        } else {
            System.out.println("Unknown mode: " + mode);
            System.out.println("Usage: PushPullSample [all|ventilator|worker|sink] [worker-id]");
            System.out.println();
            System.out.println("Modes:");
            System.out.println("  all        - Run complete pipeline (default)");
            System.out.println("  ventilator - Run only the task generator");
            System.out.println("  worker     - Run a single worker (specify worker-id as second argument)");
            System.out.println("  sink       - Run only the result collector");
        }
    }

    /**
     * Runs the Ventilator component that distributes tasks to workers.
     */
    private static void runVentilator() {
        System.out.println("[Ventilator] Starting task generator...");

        try (Context ctx = new Context();
             Socket sender = new Socket(ctx, SocketType.PUSH)) {

            sender.setOption(SocketOption.LINGER, 0);
            sender.bind(VENTILATOR_ADDRESS);
            System.out.println("[Ventilator] Bound to " + VENTILATOR_ADDRESS);

            // Give workers time to connect
            sleep(1000);

            System.out.println("[Ventilator] Distributing " + TASK_COUNT + " tasks...");

            // Signal start of batch to sink
            try (Socket signalSocket = new Socket(ctx, SocketType.PUSH)) {
                signalSocket.setOption(SocketOption.LINGER, 0);
                signalSocket.connect(SINK_CONNECT_ADDRESS);
                signalSocket.send("START");
            }

            // Distribute tasks
            int totalWorkload = 0;
            Random random = new Random();

            for (int taskNum = 0; taskNum < TASK_COUNT; taskNum++) {
                // Generate random workload (1-100 milliseconds)
                int workload = random.nextInt(100) + 1;
                totalWorkload += workload;

                String message = taskNum + ":" + workload;
                sender.send(message);

                if ((taskNum + 1) % 20 == 0) {
                    System.out.println("[Ventilator] Dispatched " + (taskNum + 1) + "/" + TASK_COUNT + " tasks");
                }
            }

            System.out.println("[Ventilator] All " + TASK_COUNT + " tasks dispatched");
            System.out.println("[Ventilator] Total expected workload: " + totalWorkload + "ms");
            System.out.println("[Ventilator] Average per task: " + (totalWorkload / TASK_COUNT) + "ms");

        } catch (Exception e) {
            System.err.println("[Ventilator] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs a Worker component that processes tasks.
     *
     * @param workerId The unique identifier for this worker
     */
    private static void runWorker(int workerId) {
        System.out.println("[Worker-" + workerId + "] Starting...");

        try (Context ctx = new Context();
             Socket receiver = new Socket(ctx, SocketType.PULL);
             Socket sender = new Socket(ctx, SocketType.PUSH)) {

            // Socket to receive tasks from ventilator
            receiver.setOption(SocketOption.LINGER, 0);
            receiver.connect(VENTILATOR_CONNECT_ADDRESS);

            // Socket to send results to sink
            sender.setOption(SocketOption.LINGER, 0);
            sender.connect(SINK_CONNECT_ADDRESS);

            System.out.println("[Worker-" + workerId + "] Connected and ready for tasks");

            int tasksProcessed = 0;
            int totalWorkload = 0;

            // Set receive timeout to detect when work is done
            receiver.setOption(SocketOption.RCVTIMEO, 3000);

            while (true) {
                try {
                    // Receive task from ventilator
                    String message = receiver.recvString().value();
                    String[] parts = message.split(":");
                    int taskNum = Integer.parseInt(parts[0]);
                    int workload = Integer.parseInt(parts[1]);

                    // Simulate processing time
                    sleep(workload);

                    tasksProcessed++;
                    totalWorkload += workload;

                    // Send result to sink
                    String result = workerId + ":" + taskNum + ":" + workload;
                    sender.send(result);

                    if (tasksProcessed % 10 == 0) {
                        System.out.println("[Worker-" + workerId + "] Processed " + tasksProcessed +
                                " tasks (current: task#" + taskNum + ", " + workload + "ms)");
                    }
                } catch (ZmqException e) {
                    if (e.isAgain()) {
                        // Timeout - no more tasks available
                        System.out.println("[Worker-" + workerId + "] No more tasks available (timeout)");
                        break;
                    }
                    throw e;
                }
            }

            System.out.println("[Worker-" + workerId + "] Completed " + tasksProcessed +
                    " tasks, total workload: " + totalWorkload + "ms");

        } catch (Exception e) {
            System.err.println("[Worker-" + workerId + "] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs the Sink component that collects results and displays statistics.
     */
    private static void runSink() {
        System.out.println("[Sink] Starting result collector...");

        try (Context ctx = new Context();
             Socket receiver = new Socket(ctx, SocketType.PULL)) {

            receiver.setOption(SocketOption.LINGER, 0);
            receiver.bind(SINK_ADDRESS);
            System.out.println("[Sink] Bound to " + SINK_ADDRESS);

            // Wait for start signal
            System.out.println("[Sink] Waiting for batch start signal...");
            String startSignal = receiver.recvString().value();
            if (!startSignal.equals("START")) {
                System.out.println("[Sink] Unexpected signal: " + startSignal);
                return;
            }

            System.out.println("[Sink] Batch started, collecting results...");
            long startTime = System.currentTimeMillis();

            // Process results
            int resultsReceived = 0;
            Map<Integer, Integer> workerStats = new HashMap<>();
            Map<Integer, Integer> workerWorkload = new HashMap<>();

            // Set timeout to detect completion
            receiver.setOption(SocketOption.RCVTIMEO, 5000);

            while (resultsReceived < TASK_COUNT) {
                try {
                    String result = receiver.recvString().value();
                    String[] parts = result.split(":");
                    int workerId = Integer.parseInt(parts[0]);
                    int taskNum = Integer.parseInt(parts[1]);
                    int workload = Integer.parseInt(parts[2]);

                    resultsReceived++;

                    // Update statistics
                    workerStats.put(workerId, workerStats.getOrDefault(workerId, 0) + 1);
                    workerWorkload.put(workerId, workerWorkload.getOrDefault(workerId, 0) + workload);

                    if (resultsReceived % 20 == 0) {
                        System.out.println("[Sink] Received " + resultsReceived + "/" + TASK_COUNT + " results");
                    }
                } catch (ZmqException e) {
                    if (e.isAgain()) {
                        // Timeout
                        System.out.println("[Sink] Timeout waiting for results. Received " +
                                resultsReceived + "/" + TASK_COUNT);
                        break;
                    }
                    throw e;
                }
            }

            long endTime = System.currentTimeMillis();
            long elapsed = endTime - startTime;

            // Display final statistics
            System.out.println();
            System.out.println("[Sink] ========== Pipeline Statistics ==========");
            System.out.println("[Sink] Total results received: " + resultsReceived + "/" + TASK_COUNT);
            System.out.println("[Sink] Total elapsed time: " + String.format("%.2f", elapsed / 1000.0) + "s");
            System.out.println();
            System.out.println("[Sink] Worker Load Distribution:");

            final int finalResultsReceived = resultsReceived;
            workerStats.keySet().stream()
                    .sorted()
                    .forEach(workerId -> {
                        int taskCount = workerStats.get(workerId);
                        int totalWorkload = workerWorkload.get(workerId);
                        double percentage = (taskCount / (double) finalResultsReceived) * 100;

                        System.out.println("[Sink]   Worker-" + workerId + ": " + taskCount +
                                " tasks (" + String.format("%.1f", percentage) + "%), " +
                                totalWorkload + "ms workload");
                    });

            System.out.println("[Sink] =============================================");
            System.out.println("[Sink] Done");

        } catch (Exception e) {
            System.err.println("[Sink] Error: " + e.getMessage());
            e.printStackTrace();
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
            System.err.println("Sleep interrupted");
        }
    }
}
