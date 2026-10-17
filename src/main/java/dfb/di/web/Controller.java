package dfb.di.web;

import dfb.di.http.HttpRequest;
import dfb.di.http.HttpResponse;

/**
 * Defines a web request handler capable of inspecting an incoming {@link HttpRequest}
 * and producing an {@link HttpResponse}.
 */
public interface Controller {

    /**
     * Determines whether this controller can handle the given HTTP request.
     *
     * @param request the incoming HTTP request
     * @return {@code true} if this controller should handle the request; {@code false} otherwise
     */
    boolean canHandle(HttpRequest request);

    /**
     * Processes the given HTTP request and returns an HTTP response.
     *
     * @param request the incoming HTTP request
     * @return the HTTP response to write back to the client, or {@code null} for {@code 204 No Content}
     */
    HttpResponse handle(HttpRequest request);
}

