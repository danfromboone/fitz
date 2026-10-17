package dbf.di;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable multi-value map of parsed URL query parameters preserving insertion order.
 */
public class QueryParams {
    private final Map<String, List<String>> params;

    /**
     * Creates a {@code QueryParams} instance by parsing a decoded query string
     * (e.g., {@code "a=1&b=2&a=3"}).
     *
     * @param decodedQuery the query string without a leading {@code '?'}; may be {@code null} or empty
     */
    public QueryParams(String decodedQuery) {
        this.params = parseQueryParams(decodedQuery);
    }

    /**
     * Creates a {@code QueryParams} instance from the decoded query component of a {@link URI}.
     *
     * @param uri the URI from which to extract query parameters; may be {@code null}
     */
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

    /**
     * Returns an unmodifiable map of query parameter names to their unmodifiable value lists.
     *
     * @return the map of query parameters
     */
    public Map<String, List<String>> asMap() {
        return params;
    }

    /**
     * Returns an unmodifiable set of all query parameter names.
     *
     * @return the set of parameter keys
     */
    public Set<String> getKeys() {
        return params.keySet();
    }

    /**
     * Returns an unmodifiable list of values associated with the given parameter key.
     *
     * @param key the parameter name to look up
     * @return the list of values for {@code key}, or an empty list if not present
     */
    public List<String> getValues(String key) {
        return params.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Returns the first value associated with the given parameter key.
     *
     * @param key the parameter name to look up
     * @return the first value for {@code key}, or {@code null} if the key is absent or has no values
     */
    public String getFirst(String key) {
        List<String> values = params.get(key);
        return (values == null || values.isEmpty()) ? null : values.getFirst();
    }

    /**
     * Checks whether the given query parameter key is present (even if it has no associated value).
     *
     * @param key the parameter name to check
     * @return {@code true} if present; {@code false} otherwise
     */
    public boolean containsKey(String key) {
        return params.containsKey(key);
    }

    /**
     * Checks whether there are no query parameters.
     *
     * @return {@code true} if empty; {@code false} otherwise
     */
    public boolean isEmpty() {
        return params.isEmpty();
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
