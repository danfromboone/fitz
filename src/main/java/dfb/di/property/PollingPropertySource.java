package dfb.di.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A decorating {@link PropertySource} that periodically polls a delegate {@link PropertySource}
 * for changes, caches the latest snapshot, and notifies registered {@link PropertyChangeListener}s
 * with a {@link PropertyChangeEvent} whenever properties are added, modified, or removed.
 */
public class PollingPropertySource implements MutablePropertySource, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PollingPropertySource.class);

    private final PropertySource delegate;
    private final ScheduledExecutorService executor;
    private final boolean ownsExecutor;
    private final ScheduledFuture<?> pollTask;
    private final List<PropertyChangeListener> listeners = new CopyOnWriteArrayList<>();

    private volatile Map<String, String> cachedProperties;

    /**
     * Creates a {@code PollingPropertySource} using a dedicated daemon single-thread scheduled executor.
     *
     * @param delegate     the underlying property source to poll (must not be {@code null})
     * @param pollInterval the delay between poll executions (must be positive and non-null)
     * @throws NullPointerException     if {@code delegate} or {@code pollInterval} is {@code null}
     * @throws IllegalArgumentException if {@code pollInterval} is zero or negative
     */
    public PollingPropertySource(PropertySource delegate, Duration pollInterval) {
        this(
                delegate,
                pollInterval,
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "polling-property-source");
                    t.setDaemon(true);
                    return t;
                }),
                true
        );
    }

    /**
     * Creates a {@code PollingPropertySource} using a caller-supplied {@link ScheduledExecutorService}.
     * <p>
     * When {@link #close()} is called, the scheduled polling task is cancelled, but the caller-supplied
     * executor is not shut down.
     *
     * @param delegate     the underlying property source to poll (must not be {@code null})
     * @param pollInterval the delay between poll executions (must be positive and non-null)
     * @param executor     the scheduled executor service to schedule polling tasks on (must not be {@code null})
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code pollInterval} is zero or negative
     */
    public PollingPropertySource(PropertySource delegate, Duration pollInterval, ScheduledExecutorService executor) {
        this(delegate, pollInterval, Objects.requireNonNull(executor, "executor must not be null"), false);
    }

    private PollingPropertySource(
            PropertySource delegate,
            Duration pollInterval,
            ScheduledExecutorService executor,
            boolean ownsExecutor
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(pollInterval, "pollInterval must not be null");
        if (pollInterval.isZero() || pollInterval.isNegative()) {
            throw new IllegalArgumentException("pollInterval must be positive");
        }
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
        this.cachedProperties = snapshot(delegate.getProperties());

        long intervalMillis = Math.max(1L, pollInterval.toMillis());
        this.pollTask = this.executor.scheduleWithFixedDelay(this::poll, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Registers a listener to be notified when property changes are detected during polling.
     *
     * @param listener the listener to register (must not be {@code null})
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    @Override
    public void registerListener(PropertyChangeListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    /**
     * Registers a listener to be notified when property changes are detected during polling.
     *
     * @param listener the listener to register (must not be {@code null})
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addListener(PropertyChangeListener listener) {
        registerListener(listener);
    }

    /**
     * Unregisters a previously registered property change listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Immediately polls the delegate {@link PropertySource}, updates the cached snapshot if any
     * properties have changed, and notifies all registered {@link PropertyChangeListener}s.
     * <p>
     * If the delegate throws an exception during polling, a warning is logged and the previous
     * cached snapshot is retained.
     */
    public synchronized void poll() {
        Map<String, String> latest;
        try {
            latest = snapshot(delegate.getProperties());
        } catch (Exception e) {
            log.warn("Failed to poll property source; keeping previous properties snapshot", e);
            return;
        }

        Map<String, String> previous = this.cachedProperties;
        List<PropertyChange> changes = diff(previous, latest);
        if (changes.isEmpty()) {
            return;
        }

        this.cachedProperties = latest;
        log.debug("Detected {} property change(s): {}", changes.size(), changes);

        PropertyChangeEvent event = new PropertyChangeEvent(changes);
        for (PropertyChangeListener listener : listeners) {
            try {
                listener.onPropertyChange(event);
            } catch (Exception e) {
                log.error("Error notifying PropertyChangeListener", e);
            }
        }
    }

    private static Map<String, String> snapshot(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static List<PropertyChange> diff(Map<String, String> previous, Map<String, String> latest) {
        Set<String> allKeys = new LinkedHashSet<>(previous.keySet());
        allKeys.addAll(latest.keySet());

        List<PropertyChange> changes = new ArrayList<>();
        for (String key : allKeys) {
            String oldValue = previous.get(key);
            String newValue = latest.get(key);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new PropertyChange(key, oldValue, newValue));
            }
        }
        return changes;
    }

    /**
     * Returns the most recently cached unmodifiable snapshot of properties.
     *
     * @return the cached properties map
     */
    @Override
    public Map<String, String> getProperties() {
        return cachedProperties;
    }

    /**
     * Cancels the scheduled polling task and shuts down the internal executor if owned by this instance.
     */
    @Override
    public void close() {
        pollTask.cancel(false);
        if (ownsExecutor) {
            executor.shutdown();
        }
    }
}
