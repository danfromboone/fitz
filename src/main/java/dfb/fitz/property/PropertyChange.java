package dfb.fitz.property;

import lombok.Getter;

import java.util.Objects;

/**
 * Represents a change to a single property key between two polling snapshots.
 */
@Getter
public class PropertyChange {
    private final String key;
    private final String oldValue;
    private final String newValue;

    /**
     * Creates a new {@code PropertyChange}.
     *
     * @param key      the property key that changed (must not be {@code null})
     * @param oldValue the previous value of the property, or {@code null} if newly added
     * @param newValue the new value of the property, or {@code null} if removed
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public PropertyChange(String key, String oldValue, String newValue) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
