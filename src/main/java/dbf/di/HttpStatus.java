package dbf.di;

import java.util.Objects;

/**
 * Represents an HTTP response status code and optional reason phrase, along with
 * constants for common standard HTTP statuses.
 */
public class HttpStatus {
    // 1xx Informational
    /** {@code 100 Continue}. */
    public static final HttpStatus CONTINUE = new HttpStatus(100, "Continue");
    /** {@code 101 Switching Protocols}. */
    public static final HttpStatus SWITCHING_PROTOCOLS = new HttpStatus(101, "Switching Protocols");

    // 2xx Success
    /** {@code 200 OK}. */
    public static final HttpStatus OK = new HttpStatus(200, "OK");
    /** {@code 201 Created}. */
    public static final HttpStatus CREATED = new HttpStatus(201, "Created");
    /** {@code 202 Accepted}. */
    public static final HttpStatus ACCEPTED = new HttpStatus(202, "Accepted");
    /** {@code 204 No Content}. */
    public static final HttpStatus NO_CONTENT = new HttpStatus(204, "No Content");

    // 3xx Redirection
    /** {@code 301 Moved Permanently}. */
    public static final HttpStatus MOVED_PERMANENTLY = new HttpStatus(301, "Moved Permanently");
    /** {@code 302 Found}. */
    public static final HttpStatus FOUND = new HttpStatus(302, "Found");
    /** {@code 303 See Other}. */
    public static final HttpStatus SEE_OTHER = new HttpStatus(303, "See Other");
    /** {@code 304 Not Modified}. */
    public static final HttpStatus NOT_MODIFIED = new HttpStatus(304, "Not Modified");
    /** {@code 307 Temporary Redirect}. */
    public static final HttpStatus TEMPORARY_REDIRECT = new HttpStatus(307, "Temporary Redirect");
    /** {@code 308 Permanent Redirect}. */
    public static final HttpStatus PERMANENT_REDIRECT = new HttpStatus(308, "Permanent Redirect");

    // 4xx Client Error
    /** {@code 400 Bad Request}. */
    public static final HttpStatus BAD_REQUEST = new HttpStatus(400, "Bad Request");
    /** {@code 401 Unauthorized}. */
    public static final HttpStatus UNAUTHORIZED = new HttpStatus(401, "Unauthorized");
    /** {@code 403 Forbidden}. */
    public static final HttpStatus FORBIDDEN = new HttpStatus(403, "Forbidden");
    /** {@code 404 Not Found}. */
    public static final HttpStatus NOT_FOUND = new HttpStatus(404, "Not Found");
    /** {@code 405 Method Not Allowed}. */
    public static final HttpStatus METHOD_NOT_ALLOWED = new HttpStatus(405, "Method Not Allowed");
    /** {@code 409 Conflict}. */
    public static final HttpStatus CONFLICT = new HttpStatus(409, "Conflict");
    /** {@code 415 Unsupported Media Type}. */
    public static final HttpStatus UNSUPPORTED_MEDIA_TYPE = new HttpStatus(415, "Unsupported Media Type");
    /** {@code 422 Unprocessable Content}. */
    public static final HttpStatus UNPROCESSABLE_CONTENT = new HttpStatus(422, "Unprocessable Content");
    /** {@code 429 Too Many Requests}. */
    public static final HttpStatus TOO_MANY_REQUESTS = new HttpStatus(429, "Too Many Requests");

    // 5xx Server Error
    /** {@code 500 Internal Server Error}. */
    public static final HttpStatus INTERNAL_SERVER_ERROR = new HttpStatus(500, "Internal Server Error");
    /** {@code 501 Not Implemented}. */
    public static final HttpStatus NOT_IMPLEMENTED = new HttpStatus(501, "Not Implemented");
    /** {@code 502 Bad Gateway}. */
    public static final HttpStatus BAD_GATEWAY = new HttpStatus(502, "Bad Gateway");
    /** {@code 503 Service Unavailable}. */
    public static final HttpStatus SERVICE_UNAVAILABLE = new HttpStatus(503, "Service Unavailable");
    /** {@code 504 Gateway Timeout}. */
    public static final HttpStatus GATEWAY_TIMEOUT = new HttpStatus(504, "Gateway Timeout");

    private final int code;
    private final String reasonPhrase;

    /**
     * Creates an {@code HttpStatus} with the given numeric status code and an empty reason phrase.
     *
     * @param code the numeric HTTP status code
     */
    public HttpStatus(int code) {
        this(code, "");
    }

    /**
     * Creates an {@code HttpStatus} with the given numeric status code and reason phrase.
     *
     * @param code         the numeric HTTP status code
     * @param reasonPhrase the human-readable reason phrase
     */
    public HttpStatus(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
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
        return code == that.code && Objects.equals(reasonPhrase, that.reasonPhrase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, reasonPhrase);
    }

    @Override
    public String toString() {
        return (reasonPhrase != null && !reasonPhrase.isEmpty())
                ? code + " " + reasonPhrase
                : String.valueOf(code);
    }
}
