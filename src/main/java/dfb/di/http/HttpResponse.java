package dfb.di.http;

/**
 * Represents an outgoing HTTP response consisting of an {@link HttpStatus},
 * an {@link HttpResponseBody}, and {@link HttpHeaders}.
 */
public class HttpResponse {
    private HttpStatus statusCode;
    private HttpResponseBody body;
    private final HttpHeaders headers;

    /**
     * Creates a default {@code 200 OK} response with an empty body and empty headers.
     */
    public HttpResponse() {
        this(HttpStatus.OK, HttpResponseBody.empty(), new HttpHeaders());
    }

    /**
     * Creates an {@code HttpResponse} with the given status code, an empty body, and empty headers.
     *
     * @param statusCode the HTTP status code
     */
    public HttpResponse(HttpStatus statusCode) {
        this(statusCode, HttpResponseBody.empty(), new HttpHeaders());
    }

    /**
     * Creates an {@code HttpResponse} with the given status code and body, and empty headers.
     *
     * @param statusCode the HTTP status code
     * @param body       the response body; if {@code null}, an empty body is used
     */
    public HttpResponse(HttpStatus statusCode, HttpResponseBody body) {
        this(statusCode, body, new HttpHeaders());
    }

    /**
     * Creates an {@code HttpResponse} with the given status code and UTF-8 string body, and empty headers.
     *
     * @param statusCode the HTTP status code
     * @param body       the response body string; if {@code null} or empty, an empty body is used
     */
    public HttpResponse(HttpStatus statusCode, String body) {
        this(statusCode, HttpResponseBody.of(body), new HttpHeaders());
    }

    /**
     * Creates an {@code HttpResponse} with the given status code, body, and headers.
     *
     * @param statusCode the HTTP status code
     * @param body       the response body; if {@code null}, an empty body is used
     * @param headers    the response headers; if {@code null}, empty headers are used
     */
    public HttpResponse(HttpStatus statusCode, HttpResponseBody body, HttpHeaders headers) {
        this.statusCode = statusCode;
        this.body = (body != null) ? body : HttpResponseBody.empty();
        this.headers = (headers != null) ? headers : new HttpHeaders();
    }

    /**
     * Returns the HTTP status of this response.
     *
     * @return the {@link HttpStatus}
     */
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    /**
     * Sets the HTTP status of this response.
     *
     * @param statusCode the new {@link HttpStatus}
     */
    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns the body of this response.
     *
     * @return the {@link HttpResponseBody}
     */
    public HttpResponseBody getBody() {
        return body;
    }

    /**
     * Sets the body of this response.
     *
     * @param body the new {@link HttpResponseBody}; if {@code null}, an empty body is set
     */
    public void setBody(HttpResponseBody body) {
        this.body = (body != null) ? body : HttpResponseBody.empty();
    }

    /**
     * Sets the body of this response from a string (encoded as UTF-8).
     *
     * @param body the response body string; if {@code null} or empty, an empty body is set
     */
    public void setBody(String body) {
        this.body = HttpResponseBody.of(body);
    }

    /**
     * Returns the mutable {@link HttpHeaders} associated with this response.
     *
     * @return the response headers
     */
    public HttpHeaders getHeaders() {
        return headers;
    }
}
