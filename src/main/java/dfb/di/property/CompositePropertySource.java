package dfb.di.property;

import dfb.di.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link MutablePropertySource} composed of a stack of underlying {@link PropertySource} instances.
 * <p>
 * Sources are ordered from the bottom of the stack (lowest precedence, e.g., defaults)
 * to the top of the stack (highest precedence, e.g., environment-specific overrides).
 * Properties from sources higher in the stack override properties from sources lower in the stack.
 * <p>
 * Any child source that implements {@link MutablePropertySource} (such as an independently polled
 * {@link PollingPropertySource}) is automatically observed; when a child source changes, this
 * composite source computes the new effective merged properties and notifies its own listeners
 * only if the effective merged values have changed.
 */
public class CompositePropertySource implements MutablePropertySource, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(CompositePropertySource.class);

    private final List<PropertySource> propertySources = new CopyOnWriteArrayList<>();
    private final List<PropertyChangeListener> listeners = new CopyOnWriteArrayList<>();
    private volatile Map<String, String> lastSnapshot = Collections.emptyMap();

    /**
     * Creates a {@code CompositePropertySource} from an array of property sources,
     * ordered from lowest precedence (first) to highest precedence (last).
     *
     * @param propertySources the property sources in ascending order of precedence; may be {@code null}
     */
    public CompositePropertySource(PropertySource... propertySources) {
        this(propertySources != null ? Arrays.asList(propertySources) : Collections.emptyList());
    }

    /**
     * Creates a {@code CompositePropertySource} from a list of property sources,
     * ordered from lowest precedence (first) to highest precedence (last).
     *
     * @param propertySources the property sources in ascending order of precedence; may be {@code null}
     */
    public CompositePropertySource(List<PropertySource> propertySources) {
        if (propertySources != null) {
            for (PropertySource source : propertySources) {
                if (source != null) {
                    this.propertySources.add(source);
                    subscribeIfMutable(source);
                }
            }
        }
        this.lastSnapshot = mergeProperties();
    }

    /**
     * Pushes a higher-precedence {@link PropertySource} onto the top of the stack,
     * overriding any matching keys from sources below it.
     *
     * @param propertySource the property source to add (must not be {@code null})
     * @throws NullPointerException if {@code propertySource} is {@code null}
     */
    public synchronized void addPropertySource(PropertySource propertySource) {
        Objects.requireNonNull(propertySource, "propertySource must not be null");
        this.propertySources.add(propertySource);
        subscribeIfMutable(propertySource);
        onChildPropertySourceChanged();
    }

    private void subscribeIfMutable(PropertySource source) {
        if (source instanceof MutablePropertySource mutableSource) {
            mutableSource.registerListener(event -> onChildPropertySourceChanged());
        }
    }

    private synchronized void onChildPropertySourceChanged() {
        Map<String, String> latest;
        try {
            latest = mergeProperties();
        } catch (Exception e) {
            log.warn("Failed to recompute merged properties after child change; keeping previous snapshot", e);
            return;
        }

        Map<String, String> previous = this.lastSnapshot;
        List<PropertyChange> changes = diff(previous, latest);
        if (changes.isEmpty()) {
            return;
        }

        this.lastSnapshot = latest;
        log.debug("CompositePropertySource detected {} effective property change(s): {}", changes.size(), changes);

        PropertyChangeEvent event = new PropertyChangeEvent(changes);
        for (PropertyChangeListener listener : listeners) {
            try {
                listener.onPropertyChange(event);
            } catch (Exception e) {
                log.error("Error notifying PropertyChangeListener in CompositePropertySource", e);
            }
        }
    }

    /**
     * Registers a listener to be notified whenever the effective merged properties change.
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
     * Unregisters a previously registered property change listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns an unmodifiable list of the underlying property sources in ascending order of precedence.
     *
     * @return the list of {@link PropertySource} instances
     */
    public List<PropertySource> getPropertySources() {
        return Collections.unmodifiableList(propertySources);
    }

    /**
     * Returns an unmodifiable merged map of all properties across all configured sources,
     * where values from higher-precedence sources override those from lower-precedence sources.
     *
     * @return the merged properties map
     */
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> merged = mergeProperties();
        this.lastSnapshot = merged;
        return merged;
    }

    private Map<String, String> mergeProperties() {
        Map<String, String> merged = new LinkedHashMap<>();
        for (PropertySource source : propertySources) {
            Map<String, String> sourceProps = source.getProperties();
            if (sourceProps != null) {
                merged.putAll(sourceProps);
            }
        }
        return Collections.unmodifiableMap(merged);
    }

    /**
     * Looks up the property value for the given key starting from the highest-precedence source
     * down to the lowest-precedence source.
     *
     * @param key the property key to look up
     * @return the resolved property value, or {@code null} if not found in any source
     */
    @Override
    public String getProperty(String key) {
        for (int i = propertySources.size() - 1; i >= 0; i--) {
            String value = propertySources.get(i).getProperty(key);
            if (value != null) {
                return value;
            }
        }
        return null;
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
     * Closes any underlying {@link PropertySource} instances that implement {@link AutoCloseable}.
     */
    @Override
    public void close() {
        Closeables.closeAllQuietly(propertySources);
    }
}
