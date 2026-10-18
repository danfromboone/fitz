package dfb.fitz.http;

import dfb.fitz.Validate;
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
public class HttpQueryParams {
    private final Map<String, List<String>> params;

    /**
     * Creates an {@code HttpQueryParams} instance by parsing a decoded query string
     * (e.g., {@code "a=1&b=2&a=3"}).
     *
     * @param decodedQuery the query string without a leading {@code '?'} (must not be {@code null})
     * @throws NullPointerException if {@code decodedQuery} is {@code null}
     */
    public HttpQueryParams(String decodedQuery) {
        Validate.nonNull(decodedQuery, "decodedQuery");
        this.params = parseQueryParams(decodedQuery.trim());
    }

    /**
     * Creates an {@code HttpQueryParams} instance from the decoded query component of a {@link URI}.
     *
     * @param uri the URI from which to extract query parameters (must not be {@code null})
     * @throws NullPointerException if {@code uri} is {@code null}
     */
    public HttpQueryParams(URI uri) {
        Validate.nonNull(uri, "uri");
        this.params = parseQueryParams(Validate.trimToEmpty(uri.getQuery()));
    }

    private static Map<String, List<String>> parseQueryParams(String query) {
        if (query.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> parsed = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String trimmedPair = pair.trim();
            if (trimmedPair.isEmpty()) {
                continue;
            }
            int idx = trimmedPair.indexOf('=');
            String rawKey = (idx >= 0) ? trimmedPair.substring(0, idx) : trimmedPair;
            String key = Validate.trimmedNonBlank(rawKey, "query parameter key");
            List<String> values = parsed.computeIfAbsent(key, k -> new ArrayList<>());
            if (idx >= 0 && idx + 1 < trimmedPair.length()) {
                String value = trimmedPair.substring(idx + 1).trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
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
     * @param key the parameter name to look up (must not be {@code null} or blank)
     * @return the list of values for {@code key}, or an empty list if not present
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     */
    public List<String> getValues(String key) {
        return params.getOrDefault(Validate.trimmedNonBlank(key, "key"), Collections.emptyList());
    }

    /**
     * Returns the first value associated with the given parameter key.
     *
     * @param key the parameter name to look up (must not be {@code null} or blank)
     * @return the first value for {@code key}, or {@code null} if the key is absent or has no values
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     */
    public String getFirst(String key) {
        List<String> values = params.get(Validate.trimmedNonBlank(key, "key"));
        return (values == null || values.isEmpty()) ? null : values.getFirst();
    }

    /**
     * Checks whether the given query parameter key is present (even if it has no associated value).
     *
     * @param key the parameter name to check (must not be {@code null} or blank)
     * @return {@code true} if present; {@code false} otherwise
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     */
    public boolean containsKey(String key) {
        return params.containsKey(Validate.trimmedNonBlank(key, "key"));
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
