package dfb.di.http;

import dfb.di.Validate;

/**
 * Represents an HTTP response status code and optional reason phrase, along with
 * constants for common standard HTTP statuses.
 * <p>
 * Predefined status instances are cached and reused when looked up via {@link #of(int)}
 * or {@link #of(int, String)}.
 */
public final class HttpStatus {
    private static final int MIN_CODE = 100;
    private static final int MAX_CODE = 599;
    private static final HttpStatus[] PREDEFINED = new HttpStatus[MAX_CODE + 1];

    // 1xx Informational
    /** {@code 100 Continue}. */
    public static final HttpStatus CONTINUE = register(100, "Continue");
    /** {@code 101 Switching Protocols}. */
    public static final HttpStatus SWITCHING_PROTOCOLS = register(101, "Switching Protocols");

    // 2xx Success
    /** {@code 200 OK}. */
    public static final HttpStatus OK = register(200, "OK");
    /** {@code 201 Created}. */
    public static final HttpStatus CREATED = register(201, "Created");
    /** {@code 202 Accepted}. */
    public static final HttpStatus ACCEPTED = register(202, "Accepted");
    /** {@code 204 No Content}. */
    public static final HttpStatus NO_CONTENT = register(204, "No Content");

    // 3xx Redirection
    /** {@code 301 Moved Permanently}. */
    public static final HttpStatus MOVED_PERMANENTLY = register(301, "Moved Permanently");
    /** {@code 302 Found}. */
    public static final HttpStatus FOUND = register(302, "Found");
    /** {@code 303 See Other}. */
    public static final HttpStatus SEE_OTHER = register(303, "See Other");
    /** {@code 304 Not Modified}. */
    public static final HttpStatus NOT_MODIFIED = register(304, "Not Modified");
    /** {@code 307 Temporary Redirect}. */
    public static final HttpStatus TEMPORARY_REDIRECT = register(307, "Temporary Redirect");
    /** {@code 308 Permanent Redirect}. */
    public static final HttpStatus PERMANENT_REDIRECT = register(308, "Permanent Redirect");

    // 4xx Client Error
    /** {@code 400 Bad Request}. */
    public static final HttpStatus BAD_REQUEST = register(400, "Bad Request");
    /** {@code 401 Unauthorized}. */
    public static final HttpStatus UNAUTHORIZED = register(401, "Unauthorized");
    /** {@code 403 Forbidden}. */
    public static final HttpStatus FORBIDDEN = register(403, "Forbidden");
    /** {@code 404 Not Found}. */
    public static final HttpStatus NOT_FOUND = register(404, "Not Found");
    /** {@code 405 Method Not Allowed}. */
    public static final HttpStatus METHOD_NOT_ALLOWED = register(405, "Method Not Allowed");
    /** {@code 409 Conflict}. */
    public static final HttpStatus CONFLICT = register(409, "Conflict");
    /** {@code 415 Unsupported Media Type}. */
    public static final HttpStatus UNSUPPORTED_MEDIA_TYPE = register(415, "Unsupported Media Type");
    /** {@code 422 Unprocessable Content}. */
    public static final HttpStatus UNPROCESSABLE_CONTENT = register(422, "Unprocessable Content");
    /** {@code 429 Too Many Requests}. */
    public static final HttpStatus TOO_MANY_REQUESTS = register(429, "Too Many Requests");

    // 5xx Server Error
    /** {@code 500 Internal Server Error}. */
    public static final HttpStatus INTERNAL_SERVER_ERROR = register(500, "Internal Server Error");
    /** {@code 501 Not Implemented}. */
    public static final HttpStatus NOT_IMPLEMENTED = register(501, "Not Implemented");
    /** {@code 502 Bad Gateway}. */
    public static final HttpStatus BAD_GATEWAY = register(502, "Bad Gateway");
    /** {@code 503 Service Unavailable}. */
    public static final HttpStatus SERVICE_UNAVAILABLE = register(503, "Service Unavailable");
    /** {@code 504 Gateway Timeout}. */
    public static final HttpStatus GATEWAY_TIMEOUT = register(504, "Gateway Timeout");

    private final int code;
    private final String reasonPhrase;

    private HttpStatus(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    private static HttpStatus register(int code, String reasonPhrase) {
        HttpStatus status = new HttpStatus(code, reasonPhrase);
        PREDEFINED[code] = status;
        return status;
    }

    /**
     * Returns the predefined {@code HttpStatus} instance for the given numeric status code if one
     * exists, or creates a new {@code HttpStatus} with an empty reason phrase for non-standard codes.
     *
     * @param code the numeric HTTP status code (must be between 100 and 599)
     * @return the cached predefined {@code HttpStatus} or a new instance for custom codes
     * @throws IllegalArgumentException if {@code code} is outside the valid range {@code 100..599}
     */
    public static HttpStatus of(int code) {
        Validate.inRange(code, MIN_CODE, MAX_CODE, "HTTP status code");
        HttpStatus predefined = PREDEFINED[code];
        if (predefined != null) {
            return predefined;
        }
        return new HttpStatus(code, "");
    }

    /**
     * Returns the predefined {@code HttpStatus} instance if one exists with the given numeric status
     * code and a matching (or unspecified) reason phrase; otherwise creates a new {@code HttpStatus}
     * with the given code and trimmed reason phrase.
     *
     * @param code         the numeric HTTP status code (must be between 100 and 599)
     * @param reasonPhrase the human-readable reason phrase (trimmed; {@code null} treated as empty)
     * @return the cached predefined {@code HttpStatus} or a new instance
     * @throws IllegalArgumentException if {@code code} is outside the valid range {@code 100..599}
     */
    public static HttpStatus of(int code, String reasonPhrase) {
        Validate.inRange(code, MIN_CODE, MAX_CODE, "HTTP status code");
        String trimmedReason = Validate.trimToEmpty(reasonPhrase);
        HttpStatus predefined = PREDEFINED[code];
        if (predefined != null
                && (trimmedReason.isEmpty() || predefined.reasonPhrase.equals(trimmedReason))) {
            return predefined;
        }
        return new HttpStatus(code, trimmedReason);
    }

    /**
     * Returns the numeric HTTP status code (e.g., {@code 200}, {@code 404}).
     *
     * @return the numeric status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the human-readable reason phrase for this status (e.g., {@code "OK"}, {@code "Not Found"}).
     *
     * @return the reason phrase
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpStatus that)) return false;
        return code == that.code;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(code);
    }

    @Override
    public String toString() {
        return !reasonPhrase.isEmpty()
                ? code + " " + reasonPhrase
                : String.valueOf(code);
    }
}
