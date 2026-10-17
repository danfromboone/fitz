package dfb.di.property;

import java.util.Map;

/**
 * Source of key-value configuration properties.
 */
public interface PropertySource {
    /**
     * Returns a map of all properties available in this source.
     *
     * @return a map of property keys to values
     */
    Map<String, String> getProperties();

    /**
     * Retrieves the property value associated with the given key.
     *
     * @param key the property key to look up
     * @return the property value, or {@code null} if the key is not present
     */
    default String getProperty(String key) {
        Map<String, String> props = getProperties();
        return (props != null) ? props.get(key) : null;
    }
}
