package dfb.di.property;

import java.util.List;
import java.util.Objects;

public record PropertyChangeEvent(List<PropertyChange> changes) {
    public PropertyChangeEvent {
        Objects.requireNonNull(changes, "changes must not be null");
        changes = List.copyOf(changes);
    }
}
