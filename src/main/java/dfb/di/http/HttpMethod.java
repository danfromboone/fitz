package dfb.di.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Enumeration of standard HTTP request methods.
 */
public enum HttpMethod {
    /** HTTP GET method. */
    GET,
    /** HTTP HEAD method. */
    HEAD,
    /** HTTP POST method. */
    POST,
    /** HTTP PUT method. */
    PUT,
    /** HTTP PATCH method. */
    PATCH,
    /** HTTP DELETE method. */
    DELETE,
    /** HTTP OPTIONS method. */
    OPTIONS,
    /** HTTP TRACE method. */
    TRACE,
    /** HTTP CONNECT method. */
    CONNECT;

    /**
     * Parses a method string (case-insensitive, trimmed) into an {@code HttpMethod}.
     *
     * @param method the HTTP method name; may be {@code null} or blank
     * @return the matching {@code HttpMethod}, or {@code null} if {@code method} is {@code null} or blank
     * @throws IllegalArgumentException if the method name does not match a known {@code HttpMethod}
     */
    public static HttpMethod from(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        return HttpMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Extracts and parses the {@code HttpMethod} from an {@link HttpServletRequest}.
     *
     * @param request the servlet request; may be {@code null}
     * @return the corresponding {@code HttpMethod}, or {@code null} if the request or its method is {@code null}
     * @throws IllegalArgumentException if the servlet request method does not match a known {@code HttpMethod}
     */
    public static HttpMethod from(HttpServletRequest request) {
        return (request != null) ? from(request.getMethod()) : null;
    }
}
