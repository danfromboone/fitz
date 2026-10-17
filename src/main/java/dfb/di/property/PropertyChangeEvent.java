package dfb.di.property;

import java.util.List;
import java.util.Objects;

/**
 * Event emitted by a {@link PollingPropertySource} containing one or more {@link PropertyChange} entries
 * detected during a poll cycle.
 *
 * @param changes an unmodifiable list of property changes (never {@code null})
 */
public record PropertyChangeEvent(List<PropertyChange> changes) {
    /**
     * Validates and defensively copies the list of property changes.
     *
     * @throws NullPointerException if {@code changes} or any element is {@code null}
     */
    public PropertyChangeEvent {
        Objects.requireNonNull(changes, "changes must not be null");
        changes = List.copyOf(changes);
    }
}
