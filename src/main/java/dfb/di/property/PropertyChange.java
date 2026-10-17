package dfb.di.property;

import java.util.Objects;

public record PropertyChange(String key, String oldValue, String newValue) {
    public PropertyChange {
        Objects.requireNonNull(key, "key must not be null");
    }
}
