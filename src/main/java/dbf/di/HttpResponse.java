package dbf.di;

public class HttpResponse {
    private HttpStatus statusCode;
    private HttpResponseBody body;
    private final HttpHeaders headers;

    public HttpResponse() {
        this(HttpStatus.OK, HttpResponseBody.empty(), new HttpHeaders());
    }

    public HttpResponse(HttpStatus statusCode) {
        this(statusCode, HttpResponseBody.empty(), new HttpHeaders());
    }

    public HttpResponse(HttpStatus statusCode, HttpResponseBody body) {
        this(statusCode, body, new HttpHeaders());
    }

    public HttpResponse(HttpStatus statusCode, String body) {
        this(statusCode, HttpResponseBody.of(body), new HttpHeaders());
    }

    public HttpResponse(HttpStatus statusCode, HttpResponseBody body, HttpHeaders headers) {
        this.statusCode = statusCode;
        this.body = (body != null) ? body : HttpResponseBody.empty();
        this.headers = (headers != null) ? headers : new HttpHeaders();
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    public HttpResponseBody getBody() {
        return body;
    }

    public void setBody(HttpResponseBody body) {
        this.body = (body != null) ? body : HttpResponseBody.empty();
    }

    public void setBody(String body) {
        this.body = HttpResponseBody.of(body);
    }

    public HttpHeaders getHeaders() {
        return headers;
    }
}
