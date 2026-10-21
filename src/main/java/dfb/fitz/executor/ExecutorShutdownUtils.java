package dfb.fitz.executor;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ExecutorShutdownUtils {

    private static final Logger log = Logger.getLogger(ExecutorShutdownUtils.class.getName());
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private ExecutorShutdownUtils() {
        // Utility class
    }

    /**
     * Gracefully shuts down the executor using default timeouts (10s graceful, 10s forced).
     */
    public static void shutdownAndAwaitTermination(ExecutorService executor) {
        shutdownAndAwaitTermination(executor, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT);
    }

    /**
     * 2-Phase graceful shutdown:
     * 1. Rejects new tasks and waits up to gracefulTimeout for running tasks to complete.
     * 2. If timed out, cancels running tasks via interrupts and waits up to forcedTimeout.
     *
     * @param executor        The executor service to stop.
     * @param gracefulTimeout Time to wait for already running tasks to finish normally.
     * @param forcedTimeout   Time to wait for tasks to respond to cancellation/interrupt.
     */
    public static void shutdownAndAwaitTermination(ExecutorService executor,
                                                   Duration gracefulTimeout,
                                                   Duration forcedTimeout) {
        Objects.requireNonNull(executor, "executor cannot be null");
        Objects.requireNonNull(gracefulTimeout, "gracefulTimeout cannot be null");
        Objects.requireNonNull(forcedTimeout, "forcedTimeout cannot be null");

        if (executor.isTerminated()) {
            return;
        }

        // Phase 1: Disable new tasks from being submitted
        executor.shutdown();

        try {
            // Wait for existing tasks to terminate
            if (!executor.awaitTermination(gracefulTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warning("Executor did not terminate in " + gracefulTimeout + "; initiating hard shutdown...");

                // Phase 2: Cancel currently executing tasks
                List<Runnable> droppedTasks = executor.shutdownNow();
                if (!droppedTasks.isEmpty()) {
                    log.warning("Discarded " + droppedTasks.size() + " tasks that were still queued.");
                }

                // Wait a while for tasks to respond to being cancelled
                if (!executor.awaitTermination(forcedTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    log.severe("Executor did not terminate after forced shutdown and interrupt.");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status for upstream callers
            Thread.currentThread().interrupt();
            log.log(Level.WARNING, "Shutdown interrupted, executor forced down.", ie);
        }
    }

    /**
     * Registers a JVM shutdown hook to automatically shut down the executor when the process exits.
     */
    public static Thread registerShutdownHook(ExecutorService executor, String hookThreadName) {
        Thread hook = new Thread(() -> shutdownAndAwaitTermination(executor), hookThreadName);
        Runtime.getRuntime().addShutdownHook(hook);
        return hook;
    }
}