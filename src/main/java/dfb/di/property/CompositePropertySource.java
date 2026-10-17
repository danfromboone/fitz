package dfb.di.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link PropertySource} composed of a stack of underlying {@link PropertySource} instances.
 * <p>
 * Sources are ordered from the bottom of the stack (lowest precedence, e.g., defaults)
 * to the top of the stack (highest precedence, e.g., environment-specific overrides).
 * Properties from sources higher in the stack override properties from sources lower in the stack.
 */
public class CompositePropertySource implements PropertySource {
    private final List<PropertySource> propertySources;

    public CompositePropertySource(PropertySource... propertySources) {
        this(propertySources != null ? Arrays.asList(propertySources) : Collections.emptyList());
    }

    public CompositePropertySource(List<PropertySource> propertySources) {
        this.propertySources = new ArrayList<>();
        if (propertySources != null) {
            for (PropertySource source : propertySources) {
                if (source != null) {
                    this.propertySources.add(source);
                }
            }
        }
    }

    /**
     * Pushes a higher-precedence {@link PropertySource} onto the top of the stack,
     * overriding any matching keys from sources below it.
     */
    public void addPropertySource(PropertySource propertySource) {
        Objects.requireNonNull(propertySource, "propertySource must not be null");
        this.propertySources.add(propertySource);
    }

    public List<PropertySource> getPropertySources() {
        return Collections.unmodifiableList(propertySources);
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> merged = new LinkedHashMap<>();
        for (PropertySource source : propertySources) {
            Map<String, String> sourceProps = source.getProperties();
            if (sourceProps != null) {
                merged.putAll(sourceProps);
            }
        }
        return Collections.unmodifiableMap(merged);
    }

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
}
