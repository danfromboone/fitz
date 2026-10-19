package dfb.fitz.http;

import dfb.fitz.Validate;
import lombok.Getter;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents an HTTP request or response header name, along with constants for common
 * standard HTTP headers.
 * <p>
 * Predefined header name instances are cached and reused (case-insensitively) when looked up
 * via {@link #of(String)} or {@link #from(String)} to reduce memory allocation, while arbitrary
 * custom header names are instantiated on demand.
 */
@Getter
public final class HttpHeaderName {
    private static final Map<String, HttpHeaderName> PREDEFINED = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    // Content & Negotiation
    public static final HttpHeaderName ACCEPT = register("Accept");
    public static final HttpHeaderName ACCEPT_CHARSET = register("Accept-Charset");
    public static final HttpHeaderName ACCEPT_ENCODING = register("Accept-Encoding");
    public static final HttpHeaderName ACCEPT_LANGUAGE = register("Accept-Language");
    public static final HttpHeaderName CONTENT_DISPOSITION = register("Content-Disposition");
    public static final HttpHeaderName CONTENT_ENCODING = register("Content-Encoding");
    public static final HttpHeaderName CONTENT_LANGUAGE = register("Content-Language");
    public static final HttpHeaderName CONTENT_LENGTH = register("Content-Length");
    public static final HttpHeaderName CONTENT_LOCATION = register("Content-Location");
    public static final HttpHeaderName CONTENT_RANGE = register("Content-Range");
    public static final HttpHeaderName CONTENT_TYPE = register("Content-Type");

    // Authentication, Cookies & Security
    public static final HttpHeaderName AUTHORIZATION = register("Authorization");
    public static final HttpHeaderName COOKIE = register("Cookie");
    public static final HttpHeaderName PROXY_AUTHENTICATE = register("Proxy-Authenticate");
    public static final HttpHeaderName PROXY_AUTHORIZATION = register("Proxy-Authorization");
    public static final HttpHeaderName SET_COOKIE = register("Set-Cookie");
    public static final HttpHeaderName WWW_AUTHENTICATE = register("WWW-Authenticate");
    public static final HttpHeaderName STRICT_TRANSPORT_SECURITY = register("Strict-Transport-Security");
    public static final HttpHeaderName CONTENT_SECURITY_POLICY = register("Content-Security-Policy");
    public static final HttpHeaderName X_CONTENT_TYPE_OPTIONS = register("X-Content-Type-Options");
    public static final HttpHeaderName X_FRAME_OPTIONS = register("X-Frame-Options");

    // Caching & Conditionals
    public static final HttpHeaderName AGE = register("Age");
    public static final HttpHeaderName CACHE_CONTROL = register("Cache-Control");
    public static final HttpHeaderName ETAG = register("ETag");
    public static final HttpHeaderName EXPIRES = register("Expires");
    public static final HttpHeaderName IF_MATCH = register("If-Match");
    public static final HttpHeaderName IF_MODIFIED_SINCE = register("If-Modified-Since");
    public static final HttpHeaderName IF_NONE_MATCH = register("If-None-Match");
    public static final HttpHeaderName IF_RANGE = register("If-Range");
    public static final HttpHeaderName IF_UNMODIFIED_SINCE = register("If-Unmodified-Since");
    public static final HttpHeaderName LAST_MODIFIED = register("Last-Modified");
    public static final HttpHeaderName PRAGMA = register("Pragma");
    public static final HttpHeaderName VARY = register("Vary");

    // Routing, Redirects & Connection
    public static final HttpHeaderName ALLOW = register("Allow");
    public static final HttpHeaderName CONNECTION = register("Connection");
    public static final HttpHeaderName DATE = register("Date");
    public static final HttpHeaderName HOST = register("Host");
    public static final HttpHeaderName LOCATION = register("Location");
    public static final HttpHeaderName ORIGIN = register("Origin");
    public static final HttpHeaderName REFERER = register("Referer");
    public static final HttpHeaderName RETRY_AFTER = register("Retry-After");
    public static final HttpHeaderName SERVER = register("Server");
    public static final HttpHeaderName TRANSFER_ENCODING = register("Transfer-Encoding");
    public static final HttpHeaderName UPGRADE = register("Upgrade");
    public static final HttpHeaderName USER_AGENT = register("User-Agent");
    public static final HttpHeaderName VIA = register("Via");

    // CORS
    public static final HttpHeaderName ACCESS_CONTROL_ALLOW_CREDENTIALS = register("Access-Control-Allow-Credentials");
    public static final HttpHeaderName ACCESS_CONTROL_ALLOW_HEADERS = register("Access-Control-Allow-Headers");
    public static final HttpHeaderName ACCESS_CONTROL_ALLOW_METHODS = register("Access-Control-Allow-Methods");
    public static final HttpHeaderName ACCESS_CONTROL_ALLOW_ORIGIN = register("Access-Control-Allow-Origin");
    public static final HttpHeaderName ACCESS_CONTROL_EXPOSE_HEADERS = register("Access-Control-Expose-Headers");
    public static final HttpHeaderName ACCESS_CONTROL_MAX_AGE = register("Access-Control-Max-Age");
    public static final HttpHeaderName ACCESS_CONTROL_REQUEST_HEADERS = register("Access-Control-Request-Headers");
    public static final HttpHeaderName ACCESS_CONTROL_REQUEST_METHOD = register("Access-Control-Request-Method");

    // Proxies & Tracing
    public static final HttpHeaderName FORWARDED = register("Forwarded");
    public static final HttpHeaderName X_FORWARDED_FOR = register("X-Forwarded-For");
    public static final HttpHeaderName X_FORWARDED_HOST = register("X-Forwarded-Host");
    public static final HttpHeaderName X_FORWARDED_PROTO = register("X-Forwarded-Proto");
    public static final HttpHeaderName X_REQUEST_ID = register("X-Request-Id");
    public static final HttpHeaderName X_CORRELATION_ID = register("X-Correlation-Id");

    private final String name;

    private HttpHeaderName(String name) {
        this.name = name;
    }

    private static HttpHeaderName register(String name) {
        HttpHeaderName headerName = new HttpHeaderName(name);
        PREDEFINED.put(name, headerName);
        return headerName;
    }

    /**
     * Returns the cached predefined {@code HttpHeaderName} instance if {@code name} matches a known
     * standard header (case-insensitively), or creates a new {@code HttpHeaderName} with the trimmed name.
     *
     * @param name the header name string (must not be {@code null} or blank)
     * @return the cached predefined {@code HttpHeaderName} or a new instance for custom header names
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static HttpHeaderName of(String name) {
        String trimmed = Validate.trimmedNonBlank(name, "header name");
        HttpHeaderName predefined = PREDEFINED.get(trimmed);
        if (predefined != null) {
            return predefined;
        }
        return new HttpHeaderName(trimmed);
    }

    /**
     * Alias for {@link #of(String)}.
     *
     * @param name the header name string (must not be {@code null} or blank)
     * @return the cached predefined {@code HttpHeaderName} or a new instance for custom header names
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static HttpHeaderName from(String name) {
        return of(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpHeaderName that)) return false;
        return name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
