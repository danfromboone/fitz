package dbf.di;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

public class HttpRequest {
    private final HttpServletRequest request;
    private final HttpMethod method;
    private final HttpUri httpUri;
    private final HttpHeaders headers;
    private final HttpRequestBody body;

    public HttpRequest(HttpServletRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
        this.method = HttpMethod.from(request);
        this.httpUri = HttpUri.from(request);
        this.headers = HttpHeaders.from(request);
        this.body = HttpRequestBody.from(request);
    }

    public HttpRequest(HttpMethod method, String uri) {
        this(method, new HttpUri(uri), new HttpHeaders(), new HttpRequestBody(""));
    }

    public HttpRequest(HttpMethod method, HttpUri httpUri, HttpHeaders headers, HttpRequestBody body) {
        this.request = null;
        this.method = method;
        this.httpUri = Objects.requireNonNull(httpUri, "httpUri must not be null");
        this.headers = (headers != null) ? headers : new HttpHeaders();
        this.body = (body != null) ? body : new HttpRequestBody("");
    }

    public HttpServletRequest getServletRequest() {
        return request;
    }

    public HttpUri getHttpUri() {
        return httpUri;
    }

    public QueryParams getQueryParams() {
        return httpUri.getQueryParams();
    }

    public HttpMethod getMethod() {
        return method;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public HttpRequestBody getBody() {
        return body;
    }

    public String getContextPath() {
        return (request != null) ? request.getContextPath() : "";
    }

    public String getServletPath() {
        return (request != null) ? request.getServletPath() : "";
    }

    public String getPathInfo() {
        return (request != null) ? request.getPathInfo() : null;
    }

    public String getRequestURI() {
        return (request != null) ? request.getRequestURI() : httpUri.getPath();
    }

    public String getQueryString() {
        return (request != null) ? request.getQueryString() : httpUri.getRawQuery();
    }
}
