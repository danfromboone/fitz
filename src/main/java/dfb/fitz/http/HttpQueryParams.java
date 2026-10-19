package dfb.fitz.http;

import dfb.fitz.Validate;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Multi-value map of URL query parameters preserving insertion order.
 *
 * <p>Internally stores decoded parameter keys and values. Wire-format factory methods
 * ({@link #fromEncodedQuery(String)} and {@link #from(URI)}) split on {@code '&'}
 * and {@code '='} first and URL-decode each individual key and value, while programmatic
 * mutators ({@link #add} and {@link #set}) accept plain decoded strings directly and
 * {@link #toEncodedQuery()} / {@link #toString()} URL-encode keys and values on output.
 */
public class HttpQueryParams {
    private final Map<String, List<String>> params = new LinkedHashMap<>();

    /**
     * Creates an empty {@code HttpQueryParams} instance.
     */
    public HttpQueryParams() {
    }

    /**
     * Creates an {@code HttpQueryParams} instance initialized with a defensive copy of the given
     * map of decoded parameter keys and values.
     *
     * @param initialParams initial map of decoded parameter keys to lists of decoded values (must not be {@code null})
     * @throws NullPointerException     if {@code initialParams} or any key is {@code null}
     * @throws IllegalArgumentException if any key is blank
     */
    public HttpQueryParams(Map<String, List<String>> initialParams) {
        Validate.nonNull(initialParams, "initialParams");
        for (Map.Entry<String, List<String>> entry : initialParams.entrySet()) {
            String key = Validate.trimmedNonBlank(entry.getKey(), "query parameter key");
            List<String> sourceValues = entry.getValue();
            List<String> copiedValues = new ArrayList<>();
            if (sourceValues != null) {
                for (String value : sourceValues) {
                    String trimmedValue = Validate.trimToEmpty(value);
                    if (!trimmedValue.isEmpty()) {
                        copiedValues.add(trimmedValue);
                    }
                }
            }
            params.put(key, copiedValues);
        }
    }

    /**
     * Parses a raw (URL-encoded) query string (e.g., {@code "a=1&b=hello%20world&a=3"})
     * by splitting on {@code '&'} and {@code '='} first and then URL-decoding each key and value.
     *
     * @param rawQuery the raw URL-encoded query string without a leading {@code '?'} (must not be {@code null})
     * @return a new {@code HttpQueryParams} instance containing the decoded parameters
     * @throws NullPointerException     if {@code rawQuery} is {@code null}
     * @throws IllegalArgumentException if any parameter key is blank or contains invalid percent-encoding
     */
    public static HttpQueryParams fromEncodedQuery(String rawQuery) {
        Validate.nonNull(rawQuery, "rawQuery");
        HttpQueryParams queryParams = new HttpQueryParams();
        queryParams.parseQueryParams(rawQuery.trim());
        return queryParams;
    }

    /**
     * Extracts and parses the raw query component of a {@link URI} into an {@code HttpQueryParams} instance.
     *
     * @param uri the URI from which to extract query parameters (must not be {@code null})
     * @return a new {@code HttpQueryParams} instance containing the decoded parameters
     * @throws NullPointerException     if {@code uri} is {@code null}
     * @throws IllegalArgumentException if any parameter key is blank or contains invalid percent-encoding
     */
    public static HttpQueryParams from(URI uri) {
        Validate.nonNull(uri, "uri");
        return fromEncodedQuery(Validate.trimToEmpty(uri.getRawQuery()));
    }

    private void parseQueryParams(String rawQuery) {
        if (rawQuery.isEmpty()) {
            return;
        }

        for (String pair : rawQuery.split("&")) {
            String trimmedPair = pair.trim();
            if (trimmedPair.isEmpty()) {
                continue;
            }
            int idx = trimmedPair.indexOf('=');
            String rawKey = (idx >= 0) ? trimmedPair.substring(0, idx) : trimmedPair;
            String rawValue = (idx >= 0 && idx + 1 < trimmedPair.length())
                    ? trimmedPair.substring(idx + 1)
                    : null;
            add(decode(rawKey), (rawValue != null) ? decode(rawValue) : null);
        }
    }

    /**
     * Appends a parameter key with no value (e.g., a flag parameter).
     *
     * @param key the parameter name to add (must not be {@code null} or blank; trimmed)
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     */
    public void add(String key) {
        add(key, null);
    }

    /**
     * Appends a value to the list of values for the given parameter key (or registers the key
     * with no value if {@code value} is {@code null} or blank).
     *
     * @param key   the parameter name (must not be {@code null} or blank; trimmed)
     * @param value the parameter value to add; may be {@code null} or empty for a parameter without a value
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     */
    public void add(String key, String value) {
        String validKey = Validate.trimmedNonBlank(key, "query parameter key");
        List<String> values = params.computeIfAbsent(validKey, k -> new ArrayList<>());
        String trimmedValue = Validate.trimToEmpty(value);
        if (!trimmedValue.isEmpty()) {
            values.add(trimmedValue);
        }
    }

    /**
     * Replaces any existing values for the given parameter key with a single value (or no value if
     * {@code value} is {@code null} or blank).
     *
     * @param key   the parameter name (must not be {@code null} or blank; trimmed)
     * @param value the parameter value to set; may be {@code null} or empty for a parameter without a value
     * @throws NullPointerException     if {@code key} is {@code null}
     * @throws IllegalArgumentException if {@code key} is blank
     */
    public void set(String key, String value) {
        String validKey = Validate.trimmedNonBlank(key, "query parameter key");
        List<String> values = new ArrayList<>(1);
        String trimmedValue = Validate.trimToEmpty(value);
        if (!trimmedValue.isEmpty()) {
            values.add(trimmedValue);
        }
        params.put(validKey, values);
    }

    /**
     * Returns an unmodifiable map of query parameter names to their unmodifiable value lists.
     *
     * @return the map of query parameters
     */
    public Map<String, List<String>> asMap() {
        Map<String, List<String>> view = new LinkedHashMap<>(params.size());
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            view.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(view);
    }

    /**
     * Returns an unmodifiable set of all query parameter names.
     *
     * @return the set of parameter keys
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(params.keySet());
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
        List<String> values = params.get(Validate.trimmedNonBlank(key, "key"));
        return (values != null) ? Collections.unmodifiableList(values) : Collections.emptyList();
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

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Returns this query parameters map formatted as a URL-encoded query string
     * (e.g., {@code "name=Alice&tag=java&tag=groovy"}).
     *
     * @return the encoded query string, or an empty string if there are no parameters
     */
    public String toEncodedQuery() {
        if (params.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String encodedKey = encode(entry.getKey());
            List<String> values = entry.getValue();
            if (values.isEmpty()) {
                joiner.add(encodedKey);
            } else {
                for (String value : values) {
                    joiner.add(encodedKey + "=" + encode(value));
                }
            }
        }
        return joiner.toString();
    }

    /**
     * Returns this query parameters map formatted as a URL-encoded query string.
     *
     * @return the encoded query string, or an empty string if there are no parameters
     * @see #toEncodedQuery()
     */
    @Override
    public String toString() {
        return toEncodedQuery();
    }
}
