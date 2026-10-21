package dfb.fitz.task;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DelayExecutorService implements ScheduledExecutorService {

    private final ScheduledExecutorService delegate;

    public DelayExecutorService() {
        this(Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private final AtomicInteger threadId = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "dynamic-scheduler-" + threadId.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        }));
    }

    public DelayExecutorService(ScheduledExecutorService delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    /**
     * Interface returned for dynamically controllable periodic tasks.
     */
    public interface DynamicScheduledFuture extends ScheduledFuture<Void> {
        /**
         * Updates the dynamic delay.
         *
         * @param newDelay          The new duration.
         * @param newUnit           The time unit.
         * @param resetImmediately  If true, cancels any active countdown and starts
         *                          the new delay from now. If false, takes effect
         *                          after the current cycle finishes.
         */
        void updateDelay(long newDelay, TimeUnit newUnit, boolean resetImmediately);
    }

    public record DelayConfig(long delay, TimeUnit unit) {}

    /**
     * Schedules a task whose delay period can be changed dynamically at runtime.
     */
    public DynamicScheduledFuture scheduleWithDynamicDelay(Runnable command,
                                                           long initialDelay,
                                                           TimeUnit unit) {
        Objects.requireNonNull(command, "command cannot be null");
        Objects.requireNonNull(unit, "unit cannot be null");

        DynamicTask task = new DynamicTask(command, initialDelay, unit);
        task.init();
        return task;
    }

    private class DynamicTask implements DynamicScheduledFuture {
        private final Runnable command;
        private final AtomicReference<DelayConfig> delayConfig;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> currentFuture;

        DynamicTask(Runnable command, long delay, TimeUnit unit) {
            this.command = command;
            this.delayConfig = new AtomicReference<>(new DelayConfig(delay, unit));
        }

        void init() {
            scheduleNext();
        }

        @Override
        public synchronized void updateDelay(long newDelay, TimeUnit newUnit, boolean resetImmediately) {
            delayConfig.set(new DelayConfig(newDelay, newUnit));

            if (!cancelled.get() && resetImmediately) {
                if (currentFuture != null && !currentFuture.isDone()) {
                    currentFuture.cancel(false);
                }
                scheduleNext();
            }
        }

        private synchronized void scheduleNext() {
            if (cancelled.get() || delegate.isShutdown()) {
                return;
            }
            DelayConfig cfg = delayConfig.get();
            currentFuture = delegate.schedule(this::runAndReschedule, cfg.delay(), cfg.unit());
        }

        private void runAndReschedule() {
            try {
                command.run();
            } catch (Throwable t) {
                // Trap uncaught exceptions to keep the periodic loop alive
                t.printStackTrace();
            } finally {
                scheduleNext();
            }
        }

        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            cancelled.set(true);
            return currentFuture != null && currentFuture.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return cancelled.get() && (currentFuture == null || currentFuture.isDone());
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            if (currentFuture != null) currentFuture.get();
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (currentFuture != null) currentFuture.get(timeout, unit);
            return null;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            ScheduledFuture<?> f = currentFuture;
            return f != null ? f.getDelay(unit) : 0;
        }

        @Override
        public int compareTo(Delayed o) {
            ScheduledFuture<?> f = currentFuture;
            return f != null ? f.compareTo(o) : 0;
        }
    }

    // --- Standard ScheduledExecutorService Delegations ---

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return delegate.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return delegate.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }
}