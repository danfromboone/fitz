package dfb.di.http;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * High-level representation of an incoming HTTP request, wrapping an {@link HttpServletRequest}
 * or constructed directly for testing.
 */
public class HttpRequest {
    private final HttpServletRequest request;
    private final HttpMethod method;
    private final HttpUri httpUri;
    private final HttpHeaders headers;
    private final HttpRequestBody body;

    /**
     * Creates an {@code HttpRequest} backed by a Jakarta {@link HttpServletRequest}.
     *
     * @param request the underlying servlet request (must not be {@code null})
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public HttpRequest(HttpServletRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
        this.method = HttpMethod.from(request);
        this.httpUri = HttpUri.from(request);
        this.headers = HttpHeaders.from(request);
        this.body = HttpRequestBody.from(request);
    }

    /**
     * Creates an {@code HttpRequest} with the given HTTP method and URI string,
     * empty headers, and an empty body.
     *
     * @param method the HTTP method (must not be {@code null})
     * @param uri    the request URI string (must not be {@code null} or blank)
     * @throws NullPointerException     if {@code method} or {@code uri} is {@code null}
     * @throws IllegalArgumentException if {@code uri} is blank or invalid
     */
    public HttpRequest(HttpMethod method, String uri) {
        this(method, new HttpUri(uri), null, null);
    }

    /**
     * Creates an {@code HttpRequest} with explicit components.
     *
     * @param method  the HTTP method (must not be {@code null})
     * @param httpUri the parsed {@link HttpUri} (must not be {@code null})
     * @param headers the request headers; if {@code null}, empty headers are used
     * @param body    the request body; if {@code null}, an empty body is used
     * @throws NullPointerException if {@code method} or {@code httpUri} is {@code null}
     */
    public HttpRequest(HttpMethod method, HttpUri httpUri, HttpHeaders headers, HttpRequestBody body) {
        this.request = null;
        this.method = Objects.requireNonNull(method, "method must not be null");
        this.httpUri = Objects.requireNonNull(httpUri, "httpUri must not be null");
        this.headers = (headers != null) ? headers : new HttpHeaders();
        this.body = (body != null) ? body : new HttpRequestBody("");
    }

    /**
     * Returns the underlying {@link HttpServletRequest}, if this instance was created from one.
     *
     * @return the underlying servlet request, or {@code null} if constructed directly
     */
    public HttpServletRequest getServletRequest() {
        return request;
    }

    /**
     * Returns the parsed {@link HttpUri} for this request.
     *
     * @return the request URI (never {@code null})
     */
    public HttpUri getHttpUri() {
        return httpUri;
    }

    /**
     * Returns the parsed query parameters from the request URI.
     *
     * @return the {@link HttpQueryParams} for this request (never {@code null})
     */
    public HttpQueryParams getQueryParams() {
        return httpUri.getQueryParams();
    }

    /**
     * Returns the HTTP method of this request.
     *
     * @return the {@link HttpMethod} (never {@code null})
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Returns the HTTP headers of this request.
     *
     * @return the {@link HttpHeaders} (never {@code null})
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * Returns the body of this request.
     *
     * @return the {@link HttpRequestBody} (never {@code null})
     */
    public HttpRequestBody getBody() {
        return body;
    }

    /**
     * Returns the servlet context path, or an empty string if not backed by a servlet request.
     *
     * @return the context path
     */
    public String getContextPath() {
        return (request != null) ? request.getContextPath() : "";
    }

    /**
     * Returns the servlet path, or an empty string if not backed by a servlet request.
     *
     * @return the servlet path
     */
    public String getServletPath() {
        return (request != null) ? request.getServletPath() : "";
    }

    /**
     * Returns any extra path information associated with the URL the client sent,
     * or {@code null} if not available.
     *
     * @return the path info, or {@code null}
     */
    public String getPathInfo() {
        return (request != null) ? request.getPathInfo() : null;
    }

    /**
     * Returns the request URI path from the underlying servlet request, or from the {@link HttpUri}
     * if not backed by a servlet request.
     *
     * @return the request URI path
     */
    public String getRequestURI() {
        return (request != null) ? request.getRequestURI() : httpUri.getPath();
    }

    /**
     * Returns the raw query string from the underlying servlet request, or from the {@link HttpUri}
     * if not backed by a servlet request.
     *
     * @return the raw query string, or {@code null} if none exists
     */
    public String getQueryString() {
        return (request != null) ? request.getQueryString() : httpUri.getRawQuery();
    }
}
