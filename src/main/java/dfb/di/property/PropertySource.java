package dfb.di.property;

import java.util.Map;

public interface PropertySource {
    Map<String, String> getProperties();

    default String getProperty(String key) {
        Map<String, String> props = getProperties();
        return (props != null) ? props.get(key) : null;
    }
}
