package dfb.di.property;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A {@link PropertySource} whose properties may change over time and that allows listeners
 * to be notified when properties are added, modified, or removed.
 */
public interface MutablePropertySource extends PropertySource {

    /**
     * Registers a listener that will be invoked whenever any property changes.
     *
     * @param listener the listener to notify (must not be {@code null})
     */
    void registerListener(PropertyChangeListener listener);

    /**
     * Registers a callback that will be run whenever any property changes.
     *
     * @param listener the callback to run (must not be {@code null})
     */
    default void registerListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        registerListener(event -> listener.run());
    }

    /**
     * Registers a callback that will be run once per change event if at least one changed
     * property key matches the given regular expression.
     *
     * @param keyRegex regular expression matched against changed property keys (must not be {@code null})
     * @param listener the callback to run when a matching property changes (must not be {@code null})
     */
    default void registerListener(String keyRegex, Runnable listener) {
        Objects.requireNonNull(keyRegex, "keyRegex must not be null");
        registerListener(Pattern.compile(keyRegex), listener);
    }

    /**
     * Registers a callback that will be run once per change event if at least one changed
     * property key matches the given {@link Pattern}.
     *
     * @param keyPattern compiled regex pattern matched against changed property keys (must not be {@code null})
     * @param listener   the callback to run when a matching property changes (must not be {@code null})
     */
    default void registerListener(Pattern keyPattern, Runnable listener) {
        Objects.requireNonNull(keyPattern, "keyPattern must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        registerListener(event -> {
            boolean matches = event.changes().stream()
                    .anyMatch(change -> keyPattern.matcher(change.key()).matches());
            if (matches) {
                listener.run();
            }
        });
    }

    /**
     * Registers a listener that will be invoked with only the matching {@link PropertyChange} entries
     * whenever one or more changed property keys match the given regular expression.
     *
     * @param keyRegex regular expression matched against changed property keys (must not be {@code null})
     * @param listener the listener to notify with matching changes (must not be {@code null})
     */
    default void registerListener(String keyRegex, PropertyChangeListener listener) {
        Objects.requireNonNull(keyRegex, "keyRegex must not be null");
        registerListener(Pattern.compile(keyRegex), listener);
    }

    /**
     * Registers a listener that will be invoked with only the matching {@link PropertyChange} entries
     * whenever one or more changed property keys match the given {@link Pattern}.
     *
     * @param keyPattern compiled regex pattern matched against changed property keys (must not be {@code null})
     * @param listener   the listener to notify with matching changes (must not be {@code null})
     */
    default void registerListener(Pattern keyPattern, PropertyChangeListener listener) {
        Objects.requireNonNull(keyPattern, "keyPattern must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        registerListener(event -> {
            List<PropertyChange> matchingChanges = event.changes().stream()
                    .filter(change -> keyPattern.matcher(change.key()).matches())
                    .toList();
            if (!matchingChanges.isEmpty()) {
                listener.onPropertyChange(new PropertyChangeEvent(matchingChanges));
            }
        });
    }
}
