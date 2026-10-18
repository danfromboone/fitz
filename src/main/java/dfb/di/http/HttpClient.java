package dfb.di.http;

import java.io.UncheckedIOException;

/**
 * High-level outbound HTTP client interface operating on {@link HttpRequest} and {@link HttpResponse}.
 */
public interface HttpClient extends AutoCloseable {

    /**
     * Executes the given {@link HttpRequest} and returns the resulting {@link HttpResponse}.
     *
     * @param request the outbound HTTP request to execute (must not be {@code null})
     * @return the HTTP response (never {@code null})
     * @throws NullPointerException if {@code request} is {@code null}
     * @throws UncheckedIOException if an I/O or timeout error occurs during execution
     */
    HttpResponse execute(HttpRequest request);

    /**
     * Executes an HTTP {@code GET} request to the specified URI.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @return the HTTP response
     */
    default HttpResponse get(HttpUri httpUri) {
        return get(httpUri, null);
    }

    /**
     * Executes an HTTP {@code GET} request to the specified URI with custom headers.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param headers the request headers; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse get(HttpUri httpUri, HttpHeaders headers) {
        return execute(new HttpRequest(HttpMethod.GET, httpUri, headers, null));
    }

    /**
     * Executes an HTTP {@code POST} request to the specified URI with a string body.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param body    the request body string; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse post(HttpUri httpUri, String body) {
        return post(httpUri, null, (body != null) ? new HttpRequestBody(body) : null);
    }

    /**
     * Executes an HTTP {@code POST} request to the specified URI with a request body.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param body    the request body; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse post(HttpUri httpUri, HttpRequestBody body) {
        return post(httpUri, null, body);
    }

    /**
     * Executes an HTTP {@code POST} request to the specified URI with custom headers and a request body.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param headers the request headers; may be {@code null}
     * @param body    the request body; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse post(HttpUri httpUri, HttpHeaders headers, HttpRequestBody body) {
        return execute(new HttpRequest(HttpMethod.POST, httpUri, headers, body));
    }

    /**
     * Executes an HTTP {@code PUT} request to the specified URI with a string body.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param body    the request body string; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse put(HttpUri httpUri, String body) {
        return put(httpUri, null, (body != null) ? new HttpRequestBody(body) : null);
    }

    /**
     * Executes an HTTP {@code PUT} request to the specified URI with a request body.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param body    the request body; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse put(HttpUri httpUri, HttpRequestBody body) {
        return put(httpUri, null, body);
    }

    /**
     * Executes an HTTP {@code PUT} request to the specified URI with custom headers and a request body.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param headers the request headers; may be {@code null}
     * @param body    the request body; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse put(HttpUri httpUri, HttpHeaders headers, HttpRequestBody body) {
        return execute(new HttpRequest(HttpMethod.PUT, httpUri, headers, body));
    }

    /**
     * Executes an HTTP {@code DELETE} request to the specified URI.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @return the HTTP response
     */
    default HttpResponse delete(HttpUri httpUri) {
        return delete(httpUri, null);
    }

    /**
     * Executes an HTTP {@code DELETE} request to the specified URI with custom headers.
     *
     * @param httpUri the target {@link HttpUri} (must not be {@code null})
     * @param headers the request headers; may be {@code null}
     * @return the HTTP response
     */
    default HttpResponse delete(HttpUri httpUri, HttpHeaders headers) {
        return execute(new HttpRequest(HttpMethod.DELETE, httpUri, headers, null));
    }

    @Override
    default void close() {
    }
}
