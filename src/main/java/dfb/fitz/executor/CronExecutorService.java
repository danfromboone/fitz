package dfb.fitz.executor;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CronExecutorService implements ScheduledExecutorService {

    private final ScheduledExecutorService delegate;
    private final CronParser cronParser;

    public CronExecutorService() {
        this(Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            private final AtomicInteger threadId = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "cron-executor-" + threadId.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        }), CronType.UNIX);
    }

    public CronExecutorService(ScheduledExecutorService delegate, CronType cronType) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(cronType);
        this.cronParser = new CronParser(cronDefinition);
    }

    public interface CronScheduledFuture extends ScheduledFuture<Void> {
        /**
         * Dynamically reconfigures the cron expression.
         *
         * @param newCronExpr      New cron string matching the configured CronType (e.g., standard 5-part UNIX).
         * @param resetImmediately If true, cancels the pending execution countdown and reschedules from now.
         */
        void updateCron(String newCronExpr, boolean resetImmediately);
    }

    /**
     * Schedules a task with a dynamic cron expression using the system default timezone.
     */
    public CronScheduledFuture schedule(Runnable command, String cronExpression) {
        return schedule(command, cronExpression, ZoneId.systemDefault());
    }

    /**
     * Schedules a task with a dynamic cron expression in a specific timezone.
     */
    public CronScheduledFuture schedule(Runnable command, String cronExpression, ZoneId zoneId) {
        Objects.requireNonNull(command, "command cannot be null");
        Objects.requireNonNull(cronExpression, "cronExpression cannot be null");
        Objects.requireNonNull(zoneId, "zoneId cannot be null");

        Cron cron = cronParser.parse(cronExpression);
        cron.validate();

        CronTask task = new CronTask(command, cron, zoneId);
        task.init();
        return task;
    }

    private class CronTask implements CronScheduledFuture {
        private final Runnable command;
        private final ZoneId zoneId;
        private final AtomicReference<ExecutionTime> executionTimeRef;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> currentFuture;

        CronTask(Runnable command, Cron initialCron, ZoneId zoneId) {
            this.command = command;
            this.zoneId = zoneId;
            this.executionTimeRef = new AtomicReference<>(ExecutionTime.forCron(initialCron));
        }

        void init() {
            scheduleNext();
        }

        @Override
        public synchronized void updateCron(String newCronExpr, boolean resetImmediately) {
            Cron cron = cronParser.parse(newCronExpr);
            cron.validate();
            executionTimeRef.set(ExecutionTime.forCron(cron));

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

            ZonedDateTime now = ZonedDateTime.now(zoneId);
            ExecutionTime executionTime = executionTimeRef.get();

            ZonedDateTime next = executionTime.nextExecution(now)
                    .orElseThrow(() -> new IllegalStateException("No future execution found for the provided cron."));

            long delayMillis = Math.max(0, Duration.between(now, next).toMillis());
            currentFuture = delegate.schedule(this::runAndReschedule, delayMillis, TimeUnit.MILLISECONDS);
        }

        private void runAndReschedule() {
            try {
                command.run();
            } catch (Throwable t) {
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

        @Override public boolean isCancelled() { return cancelled.get(); }
        @Override public boolean isDone() { return cancelled.get() && (currentFuture == null || currentFuture.isDone()); }
        @Override public Void get() throws InterruptedException, ExecutionException {
            if (currentFuture != null) currentFuture.get();
            return null;
        }
        @Override public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (currentFuture != null) currentFuture.get(timeout, unit);
            return null;
        }
        @Override public long getDelay(TimeUnit unit) {
            ScheduledFuture<?> f = currentFuture;
            return f != null ? f.getDelay(unit) : 0;
        }
        @Override public int compareTo(Delayed o) {
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

    @Override public void shutdown() { delegate.shutdown(); }
    @Override public List<Runnable> shutdownNow() { return delegate.shutdownNow(); }
    @Override public boolean isShutdown() { return delegate.isShutdown(); }
    @Override public boolean isTerminated() { return delegate.isTerminated(); }
    @Override public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
    @Override public <T> Future<T> submit(Callable<T> task) { return delegate.submit(task); }
    @Override public <T> Future<T> submit(Runnable task, T result) { return delegate.submit(task, result); }
    @Override public Future<?> submit(Runnable task) { return delegate.submit(task); }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }
    @Override public void execute(Runnable command) { delegate.execute(command); }
}