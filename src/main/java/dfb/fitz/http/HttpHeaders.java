package dfb.fitz.http;

import dfb.fitz.Validate;
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
     * @param initialHeaders initial map of header names to lists of values (must not be {@code null})
     * @throws NullPointerException     if {@code initialHeaders} or any header name is {@code null}
     * @throws IllegalArgumentException if any header name is blank
     */
    public HttpHeaders(Map<String, List<String>> initialHeaders) {
        Validate.nonNull(initialHeaders, "initialHeaders");
        for (Map.Entry<String, List<String>> entry : initialHeaders.entrySet()) {
            String name = Validate.trimmedNonBlank(entry.getKey(), "header name");
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
        Validate.nonNull(request, "request");
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String rawName = headerNames.nextElement();
                String name = Validate.trimToEmpty(rawName);
                if (name.isEmpty()) {
                    continue;
                }
                Enumeration<String> values = request.getHeaders(rawName);
                List<String> valueList = new ArrayList<>();
                if (values != null) {
                    while (values.hasMoreElements()) {
                        String trimmedVal = Validate.trimToEmpty(values.nextElement());
                        if (!trimmedVal.isEmpty()) {
                            valueList.add(trimmedVal);
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
     * Returns all values associated with the given {@link HttpHeaderName}.
     *
     * @param headerName the header name to look up (must not be {@code null})
     * @return an unmodifiable list of values, or an empty list if none exist
     * @throws NullPointerException if {@code headerName} is {@code null}
     */
    public List<String> getValues(HttpHeaderName headerName) {
        Validate.nonNull(headerName, "headerName");
        return getValues(headerName.getName());
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
        List<String> values = headers.get(Validate.trimmedNonBlank(name, "header name"));
        return (values != null) ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    /**
     * Returns the first value associated with the given {@link HttpHeaderName}.
     *
     * @param headerName the header name to look up (must not be {@code null})
     * @return the first header value, or {@code null} if the header is not present or has no values
     * @throws NullPointerException if {@code headerName} is {@code null}
     */
    public String getFirst(HttpHeaderName headerName) {
        Validate.nonNull(headerName, "headerName");
        return getFirst(headerName.getName());
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
        List<String> values = headers.get(Validate.trimmedNonBlank(name, "header name"));
        return (values == null || values.isEmpty()) ? null : values.getFirst();
    }

    /**
     * Replaces any existing values for the given {@link HttpHeaderName} with a single value (or no value if
     * {@code value} is {@code null} or blank).
     *
     * @param headerName the header name to set (must not be {@code null})
     * @param value      the header value to set; may be {@code null} or empty for a header without a value
     * @throws NullPointerException if {@code headerName} is {@code null}
     */
    public void set(HttpHeaderName headerName, String value) {
        Validate.nonNull(headerName, "headerName");
        set(headerName.getName(), value);
    }

    /**
     * Replaces any existing values for the given header name with a single value (or no value if
     * {@code value} is {@code null} or blank).
     *
     * @param name  the header name (must not be {@code null} or blank; trimmed)
     * @param value the header value to set; may be {@code null} or empty for a header without a value
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public void set(String name, String value) {
        String validName = Validate.trimmedNonBlank(name, "header name");
        List<String> values = new ArrayList<>(1);
        String trimmedValue = Validate.trimToEmpty(value);
        if (!trimmedValue.isEmpty()) {
            values.add(trimmedValue);
        }
        headers.put(validName, values);
    }

    /**
     * Appends a value to the list of values for the given {@link HttpHeaderName} (or registers the header
     * with no value if {@code value} is {@code null} or blank).
     *
     * @param headerName the header name to add to (must not be {@code null})
     * @param value      the header value to add; may be {@code null} or empty for a header without a value
     * @throws NullPointerException if {@code headerName} is {@code null}
     */
    public void add(HttpHeaderName headerName, String value) {
        Validate.nonNull(headerName, "headerName");
        add(headerName.getName(), value);
    }

    /**
     * Appends a value to the list of values for the given header name (or registers the header
     * with no value if {@code value} is {@code null} or blank).
     *
     * @param name  the header name (must not be {@code null} or blank; trimmed)
     * @param value the header value to add; may be {@code null} or empty for a header without a value
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public void add(String name, String value) {
        String validName = Validate.trimmedNonBlank(name, "header name");
        List<String> values = headers.computeIfAbsent(validName, k -> new ArrayList<>());
        String trimmedValue = Validate.trimToEmpty(value);
        if (!trimmedValue.isEmpty()) {
            values.add(trimmedValue);
        }
    }

    /**
     * Checks whether the given {@link HttpHeaderName} is present (case-insensitive).
     *
     * @param headerName the header name to check (must not be {@code null})
     * @return {@code true} if present; {@code false} otherwise
     * @throws NullPointerException if {@code headerName} is {@code null}
     */
    public boolean contains(HttpHeaderName headerName) {
        Validate.nonNull(headerName, "headerName");
        return contains(headerName.getName());
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
        return headers.containsKey(Validate.trimmedNonBlank(name, "header name"));
    }

    /**
     * Returns the parsed {@link MimeType} from the {@code Content-Type} header, if present.
     *
     * @return the parsed {@link MimeType}, or {@code null} if no {@code Content-Type} header is set
     * @throws IllegalArgumentException if the {@code Content-Type} header value is malformed
     */
    public MimeType getContentType() {
        String value = getFirst(HttpHeaderName.CONTENT_TYPE);
        return (value != null && !value.isBlank()) ? MimeType.parse(value) : null;
    }

    /**
     * Sets the {@code Content-Type} header from the given {@link MimeType}.
     *
     * @param mimeType the {@link MimeType} to set (must not be {@code null})
     * @throws NullPointerException if {@code mimeType} is {@code null}
     */
    public void setContentType(MimeType mimeType) {
        Validate.nonNull(mimeType, "mimeType");
        set(HttpHeaderName.CONTENT_TYPE, mimeType.toString());
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
