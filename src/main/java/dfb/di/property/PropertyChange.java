package dfb.di.property;

import java.util.Objects;

/**
 * Represents a change to a single property key between two polling snapshots.
 *
 * @param key      the property key that changed (never {@code null})
 * @param oldValue the previous value of the property, or {@code null} if the property was newly added
 * @param newValue the new value of the property, or {@code null} if the property was removed
 */
public record PropertyChange(String key, String oldValue, String newValue) {
    /**
     * Validates that {@code key} is non-null.
     *
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public PropertyChange {
        Objects.requireNonNull(key, "key must not be null");
    }
}
