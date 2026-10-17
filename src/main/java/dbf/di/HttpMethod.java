package dbf.di;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

public enum HttpMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS,
    TRACE,
    CONNECT;

    public static HttpMethod from(String method) {
        if (method == null || method.isBlank()) {
            return null;
        }
        return HttpMethod.valueOf(method.trim().toUpperCase(Locale.ROOT));
    }

    public static HttpMethod from(HttpServletRequest request) {
        return (request != null) ? from(request.getMethod()) : null;
    }
}
