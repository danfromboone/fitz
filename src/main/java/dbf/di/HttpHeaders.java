package dbf.di;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
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
     * @param initialHeaders initial map of header names to lists of values; may be {@code null}
     */
    public HttpHeaders(Map<String, List<String>> initialHeaders) {
        if (initialHeaders != null) {
            for (Map.Entry<String, List<String>> entry : initialHeaders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    headers.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
        }
    }

    /**
     * Creates an {@code HttpHeaders} instance populated from the headers of an {@link HttpServletRequest}.
     *
     * @param request the servlet request to extract headers from; may be {@code null}
     */
    public HttpHeaders(HttpServletRequest request) {
        if (request != null) {
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    Enumeration<String> values = request.getHeaders(name);
                    List<String> valueList = (values != null)
                            ? new ArrayList<>(Collections.list(values))
                            : new ArrayList<>();
                    headers.put(name, valueList);
                }
            }
        }
    }

    /**
     * Factory method that creates an {@code HttpHeaders} instance from an {@link HttpServletRequest}.
     *
     * @param request the servlet request to extract headers from; may be {@code null}
     * @return a new {@code HttpHeaders} instance
     */
    public static HttpHeaders from(HttpServletRequest request) {
        return new HttpHeaders(request);
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
     * @param name the header name to look up
     * @return an unmodifiable list of values, or an empty list if none exist
     */
    public List<String> getValues(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        List<String> values = headers.get(name);
        return (values != null) ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    /**
     * Returns the first value associated with the given header name (case-insensitive).
     *
     * @param name the header name to look up
     * @return the first header value, or {@code null} if the header is not present or has no values
     */
    public String getFirst(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name);
        return (values == null || values.isEmpty()) ? null : values.getFirst();
    }

    /**
     * Replaces any existing values for the given header name with a single value.
     *
     * @param name  the header name (ignored if {@code null})
     * @param value the header value to set (if {@code null}, sets an empty value list)
     */
    public void set(String name, String value) {
        if (name != null) {
            List<String> values = new ArrayList<>();
            if (value != null) {
                values.add(value);
            }
            headers.put(name, values);
        }
    }

    /**
     * Appends a value to the list of values for the given header name.
     *
     * @param name  the header name (ignored if {@code null})
     * @param value the header value to add (ignored if {@code null})
     */
    public void add(String name, String value) {
        if (name != null && value != null) {
            headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
    }

    /**
     * Checks whether a header with the given name exists (case-insensitive).
     *
     * @param name the header name to check
     * @return {@code true} if present; {@code false} otherwise
     */
    public boolean contains(String name) {
        return name != null && headers.containsKey(name);
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
