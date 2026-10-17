package dfb.di.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Objects;

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
     * @param method the HTTP method name (must not be {@code null} or blank)
     * @return the matching {@code HttpMethod}
     * @throws NullPointerException     if {@code method} is {@code null}
     * @throws IllegalArgumentException if the method name is blank or does not match a known {@code HttpMethod}
     */
    public static HttpMethod from(String method) {
        Objects.requireNonNull(method, "method must not be null");
        String trimmed = method.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        return HttpMethod.valueOf(trimmed.toUpperCase(Locale.ROOT));
    }

    /**
     * Extracts and parses the {@code HttpMethod} from an {@link HttpServletRequest}.
     *
     * @param request the servlet request (must not be {@code null})
     * @return the corresponding {@code HttpMethod}
     * @throws NullPointerException     if {@code request} or its method is {@code null}
     * @throws IllegalArgumentException if the servlet request method is blank or does not match a known {@code HttpMethod}
     */
    public static HttpMethod from(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return from(request.getMethod());
    }
}
