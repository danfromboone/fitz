package dfb.fitz.property;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * Event emitted by a {@link MutablePropertySource} containing one or more {@link PropertyChange} entries.
 */
@Getter
public class PropertyChangeEvent {
    private final List<PropertyChange> changes;

    /**
     * Creates a new {@code PropertyChangeEvent} with an unmodifiable copy of the given changes.
     *
     * @param changes the list of property changes (must not be {@code null} or contain {@code null} elements)
     * @throws NullPointerException if {@code changes} or any element is {@code null}
     */
    public PropertyChangeEvent(List<PropertyChange> changes) {
        Objects.requireNonNull(changes, "changes must not be null");
        this.changes = List.copyOf(changes);
    }
}
