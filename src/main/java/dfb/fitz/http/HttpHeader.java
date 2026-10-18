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
 * Predefined header instances are cached and reused (case-insensitively) when looked up
 * via {@link #of(String)} or {@link #from(String)} to reduce memory allocation, while arbitrary
 * custom header names are instantiated on demand.
 */
@Getter
public final class HttpHeader {
    private static final Map<String, HttpHeader> PREDEFINED = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    // Content & Negotiation
    public static final HttpHeader ACCEPT = register("Accept");
    public static final HttpHeader ACCEPT_CHARSET = register("Accept-Charset");
    public static final HttpHeader ACCEPT_ENCODING = register("Accept-Encoding");
    public static final HttpHeader ACCEPT_LANGUAGE = register("Accept-Language");
    public static final HttpHeader CONTENT_DISPOSITION = register("Content-Disposition");
    public static final HttpHeader CONTENT_ENCODING = register("Content-Encoding");
    public static final HttpHeader CONTENT_LANGUAGE = register("Content-Language");
    public static final HttpHeader CONTENT_LENGTH = register("Content-Length");
    public static final HttpHeader CONTENT_LOCATION = register("Content-Location");
    public static final HttpHeader CONTENT_RANGE = register("Content-Range");
    public static final HttpHeader CONTENT_TYPE = register("Content-Type");

    // Authentication, Cookies & Security
    public static final HttpHeader AUTHORIZATION = register("Authorization");
    public static final HttpHeader COOKIE = register("Cookie");
    public static final HttpHeader PROXY_AUTHENTICATE = register("Proxy-Authenticate");
    public static final HttpHeader PROXY_AUTHORIZATION = register("Proxy-Authorization");
    public static final HttpHeader SET_COOKIE = register("Set-Cookie");
    public static final HttpHeader WWW_AUTHENTICATE = register("WWW-Authenticate");
    public static final HttpHeader STRICT_TRANSPORT_SECURITY = register("Strict-Transport-Security");
    public static final HttpHeader CONTENT_SECURITY_POLICY = register("Content-Security-Policy");
    public static final HttpHeader X_CONTENT_TYPE_OPTIONS = register("X-Content-Type-Options");
    public static final HttpHeader X_FRAME_OPTIONS = register("X-Frame-Options");

    // Caching & Conditionals
    public static final HttpHeader AGE = register("Age");
    public static final HttpHeader CACHE_CONTROL = register("Cache-Control");
    public static final HttpHeader ETAG = register("ETag");
    public static final HttpHeader EXPIRES = register("Expires");
    public static final HttpHeader IF_MATCH = register("If-Match");
    public static final HttpHeader IF_MODIFIED_SINCE = register("If-Modified-Since");
    public static final HttpHeader IF_NONE_MATCH = register("If-None-Match");
    public static final HttpHeader IF_RANGE = register("If-Range");
    public static final HttpHeader IF_UNMODIFIED_SINCE = register("If-Unmodified-Since");
    public static final HttpHeader LAST_MODIFIED = register("Last-Modified");
    public static final HttpHeader PRAGMA = register("Pragma");
    public static final HttpHeader VARY = register("Vary");

    // Routing, Redirects & Connection
    public static final HttpHeader ALLOW = register("Allow");
    public static final HttpHeader CONNECTION = register("Connection");
    public static final HttpHeader DATE = register("Date");
    public static final HttpHeader HOST = register("Host");
    public static final HttpHeader LOCATION = register("Location");
    public static final HttpHeader ORIGIN = register("Origin");
    public static final HttpHeader REFERER = register("Referer");
    public static final HttpHeader RETRY_AFTER = register("Retry-After");
    public static final HttpHeader SERVER = register("Server");
    public static final HttpHeader TRANSFER_ENCODING = register("Transfer-Encoding");
    public static final HttpHeader UPGRADE = register("Upgrade");
    public static final HttpHeader USER_AGENT = register("User-Agent");
    public static final HttpHeader VIA = register("Via");

    // CORS
    public static final HttpHeader ACCESS_CONTROL_ALLOW_CREDENTIALS = register("Access-Control-Allow-Credentials");
    public static final HttpHeader ACCESS_CONTROL_ALLOW_HEADERS = register("Access-Control-Allow-Headers");
    public static final HttpHeader ACCESS_CONTROL_ALLOW_METHODS = register("Access-Control-Allow-Methods");
    public static final HttpHeader ACCESS_CONTROL_ALLOW_ORIGIN = register("Access-Control-Allow-Origin");
    public static final HttpHeader ACCESS_CONTROL_EXPOSE_HEADERS = register("Access-Control-Expose-Headers");
    public static final HttpHeader ACCESS_CONTROL_MAX_AGE = register("Access-Control-Max-Age");
    public static final HttpHeader ACCESS_CONTROL_REQUEST_HEADERS = register("Access-Control-Request-Headers");
    public static final HttpHeader ACCESS_CONTROL_REQUEST_METHOD = register("Access-Control-Request-Method");

    // Proxies & Tracing
    public static final HttpHeader FORWARDED = register("Forwarded");
    public static final HttpHeader X_FORWARDED_FOR = register("X-Forwarded-For");
    public static final HttpHeader X_FORWARDED_HOST = register("X-Forwarded-Host");
    public static final HttpHeader X_FORWARDED_PROTO = register("X-Forwarded-Proto");
    public static final HttpHeader X_REQUEST_ID = register("X-Request-Id");
    public static final HttpHeader X_CORRELATION_ID = register("X-Correlation-Id");

    private final String headerName;

    private HttpHeader(String headerName) {
        this.headerName = headerName;
    }

    private static HttpHeader register(String headerName) {
        HttpHeader header = new HttpHeader(headerName);
        PREDEFINED.put(headerName, header);
        return header;
    }

    /**
     * Returns the cached predefined {@code HttpHeader} instance if {@code name} matches a known
     * standard header (case-insensitively), or creates a new {@code HttpHeader} with the trimmed name.
     *
     * @param name the header name string (must not be {@code null} or blank)
     * @return the cached predefined {@code HttpHeader} or a new instance for custom header names
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static HttpHeader of(String name) {
        String trimmed = Validate.trimmedNonBlank(name, "header name");
        HttpHeader predefined = PREDEFINED.get(trimmed);
        if (predefined != null) {
            return predefined;
        }
        return new HttpHeader(trimmed);
    }

    /**
     * Alias for {@link #of(String)}.
     *
     * @param name the header name string (must not be {@code null} or blank)
     * @return the cached predefined {@code HttpHeader} or a new instance for custom header names
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static HttpHeader from(String name) {
        return of(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpHeader that)) return false;
        return headerName.equalsIgnoreCase(that.headerName);
    }

    @Override
    public int hashCode() {
        return headerName.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return headerName;
    }
}
