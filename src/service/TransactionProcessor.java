package service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages a fixed pool of four threads for processing bank transaction tasks.
 */
public class TransactionProcessor {

    private static final int  POOL_SIZE            = 4;
    private static final long SHUTDOWN_TIMEOUT_SEC = 10;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(POOL_SIZE);

    /**
     * Submits a task for execution on the thread pool.
     *
     * @param task the runnable task to execute; must not be null
     * @throws NullPointerException if task is null
     * @throws IllegalStateException if the executor has already been shut down
     */
    public void submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task must not be null");
        }
        if (executor.isShutdown()) {
            throw new IllegalStateException("TransactionProcessor has been shut down");
        }
        executor.execute(task);
    }

    /**
     * Initiates an orderly shutdown, waiting up to {@value SHUTDOWN_TIMEOUT_SEC} seconds
     * for submitted tasks to complete. If tasks do not finish in time, forces shutdown
     * and waits a further bounded period.
     */
    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    System.err.println("TransactionProcessor: executor did not terminate cleanly.");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
