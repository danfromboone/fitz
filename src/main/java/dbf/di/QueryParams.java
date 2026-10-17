package dbf.di;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryParams {
    private final Map<String, List<String>> params;

    public QueryParams(String decodedQuery) {
        this.params = parseQueryParams(decodedQuery);
    }

    public QueryParams(URI uri) {
        this(uri != null ? uri.getQuery() : null);
    }

    private static Map<String, List<String>> parseQueryParams(String query) {
        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> parsed = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String key = (idx >= 0) ? pair.substring(0, idx) : pair;
            List<String> values = parsed.computeIfAbsent(key, k -> new ArrayList<>());
            if (idx >= 0 && idx + 1 < pair.length()) {
                values.add(pair.substring(idx + 1));
            }
        }
        parsed.replaceAll((k, v) -> Collections.unmodifiableList(v));
        return Collections.unmodifiableMap(parsed);
    }

    public Map<String, List<String>> asMap() {
        return params;
    }

    public Set<String> getKeys() {
        return params.keySet();
    }

    public List<String> getValues(String key) {
        return params.getOrDefault(key, Collections.emptyList());
    }

    public String getFirst(String key) {
        List<String> values = params.get(key);
        return (values == null || values.isEmpty()) ? null : values.getFirst();
    }

    public boolean containsKey(String key) {
        return params.containsKey(key);
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
