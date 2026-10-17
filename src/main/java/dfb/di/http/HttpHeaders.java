package dfb.di.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Case-insensitive multi-value map representing HTTP request or response headers.
 */
public class HttpHeaders {
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /**
     * Creates an empty {@code HttpHeaders} instance.
     */
    public HttpHeaders() {
    }

    /**
     * Creates an {@code HttpHeaders} instance initialized with a defensive copy of the given header map.
     *
     * @param initialHeaders initial map of header names to lists of values (must not be {@code null})
     * @throws NullPointerException     if {@code initialHeaders}, any header name, value list, or value is {@code null}
     * @throws IllegalArgumentException if any header name is blank
     */
    public HttpHeaders(Map<String, List<String>> initialHeaders) {
        Objects.requireNonNull(initialHeaders, "initialHeaders must not be null");
        for (Map.Entry<String, List<String>> entry : initialHeaders.entrySet()) {
            String name = validateName(entry.getKey());
            List<String> sourceValues = Objects.requireNonNull(entry.getValue(), "header value list must not be null");
            List<String> copiedValues = new ArrayList<>(sourceValues.size());
            for (String value : sourceValues) {
                copiedValues.add(validateValue(value));
            }
            headers.put(name, copiedValues);
        }
    }

    /**
     * Creates an {@code HttpHeaders} instance populated from the headers of an {@link HttpServletRequest}.
     *
     * @param request the servlet request to extract headers from (must not be {@code null})
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public HttpHeaders(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String rawName = headerNames.nextElement();
                if (rawName == null || rawName.isBlank()) {
                    continue;
                }
                String name = rawName.trim();
                Enumeration<String> values = request.getHeaders(rawName);
                List<String> valueList = new ArrayList<>();
                if (values != null) {
                    while (values.hasMoreElements()) {
                        String val = values.nextElement();
                        if (val != null) {
                            valueList.add(val.trim());
                        }
                    }
                }
                headers.put(name, valueList);
            }
        }
    }

    /**
     * Factory method that creates an {@code HttpHeaders} instance from an {@link HttpServletRequest}.
     *
     * @param request the servlet request to extract headers from (must not be {@code null})
     * @return a new {@code HttpHeaders} instance
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public static HttpHeaders from(HttpServletRequest request) {
        return new HttpHeaders(request);
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "header name must not be null");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("header name must not be blank");
        }
        return trimmed;
    }

    private static String validateValue(String value) {
        Objects.requireNonNull(value, "header value must not be null");
        return value.trim();
    }

    /**
     * Returns an unmodifiable case-insensitive map view of all headers and their value lists.
     *
     * @return an unmodifiable map of header names to unmodifiable lists of values
     */
    public Map<String, List<String>> asMap() {
        Map<String, List<String>> view = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            view.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(view);
    }

    /**
     * Returns an unmodifiable set of all header names present.
     *
     * @return the set of header names
     */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    /**
     * Returns all values associated with the given header name (case-insensitive).
     *
     * @param name the header name to look up (must not be {@code null} or blank)
     * @return an unmodifiable list of values, or an empty list if none exist
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public List<String> getValues(String name) {
        List<String> values = headers.get(validateName(name));
        return (values != null) ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    /**
     * Returns the first value associated with the given header name (case-insensitive).
     *
     * @param name the header name to look up (must not be {@code null} or blank)
     * @return the first header value, or {@code null} if the header is not present or has no values
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public String getFirst(String name) {
        List<String> values = headers.get(validateName(name));
        return (values == null || values.isEmpty()) ? null : values.getFirst();
    }

    /**
     * Replaces any existing values for the given header name with a single value.
     *
     * @param name  the header name (must not be {@code null} or blank; trimmed)
     * @param value the header value to set (must not be {@code null}; trimmed)
     * @throws NullPointerException     if {@code name} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public void set(String name, String value) {
        String validName = validateName(name);
        String validValue = validateValue(value);
        List<String> values = new ArrayList<>(1);
        values.add(validValue);
        headers.put(validName, values);
    }

    /**
     * Appends a value to the list of values for the given header name.
     *
     * @param name  the header name (must not be {@code null} or blank; trimmed)
     * @param value the header value to add (must not be {@code null}; trimmed)
     * @throws NullPointerException     if {@code name} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public void add(String name, String value) {
        String validName = validateName(name);
        String validValue = validateValue(value);
        headers.computeIfAbsent(validName, k -> new ArrayList<>()).add(validValue);
    }

    /**
     * Checks whether a header with the given name exists (case-insensitive).
     *
     * @param name the header name to check (must not be {@code null} or blank)
     * @return {@code true} if present; {@code false} otherwise
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public boolean contains(String name) {
        return headers.containsKey(validateName(name));
    }

    /**
     * Checks whether this header collection contains no headers.
     *
     * @return {@code true} if empty; {@code false} otherwise
     */
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public String toString() {
        return headers.toString();
    }
}
