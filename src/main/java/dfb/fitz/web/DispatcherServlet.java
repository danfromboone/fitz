package dfb.fitz.web;

import dfb.fitz.http.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Front-controller servlet that dispatches incoming HTTP requests to the first matching
 * {@link Controller} in its configured controller list.
 * <p>
 * If no controller matches a request, a {@code 404 Not Found} status is returned.
 * If an unhandled exception occurs during dispatch or handling, a {@code 500 Internal Server Error}
 * plain-text response is written.
 */
public class DispatcherServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(DispatcherServlet.class);

    /**
     * Unmodifiable list of controllers registered to handle incoming requests.
     */
    private final List<Controller> controllers;

    /**
     * Creates a new {@code DispatcherServlet} with the given collection of controllers.
     *
     * @param controllers the controllers to evaluate in iteration order; if {@code null}, an empty list is used
     */
    public
    DispatcherServlet(Collection<Controller> controllers) {
        this.controllers = (controllers != null)
                ? List.copyOf(controllers)
                : Collections.emptyList();
    }

    /**
     * Logs servlet initialization along with the number of registered controllers.
     */
    @Override
    public void init() {
        log.info("Initialized DispatcherServlet with {} controller(s)", this.controllers.size());
    }

    /**
     * Returns an unmodifiable list of controllers registered with this servlet.
     *
     * @return the registered {@link Controller} instances
     */
    public List<Controller> getControllers() {
        return controllers;
    }

    /**
     * Dispatches any incoming HTTP request regardless of HTTP method to the matching controller.
     *
     * @param request  the incoming servlet request
     * @param response the outgoing servlet response
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        dispatch(request, response);
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            HttpRequest httpRequest = new HttpRequest(request);
            log.debug("Incoming request: {} {}", httpRequest.getMethod(), httpRequest.getHttpUri());

            for (Controller controller : controllers) {
                if (controller != null && controller.canHandle(httpRequest)) {
                    log.debug("Dispatching {} {} to {}",
                            httpRequest.getMethod(),
                            httpRequest.getHttpUri().getPath(),
                            controller.getClass().getSimpleName());
                    HttpResponse httpResponse = controller.handle(httpRequest);
                    writeResponse(httpResponse, response);
                    return;
                }
            }

            log.warn("No controller found for {} {}", httpRequest.getMethod(), httpRequest.getHttpUri());
            response.setStatus(HttpStatus.NOT_FOUND.getCode());
        } catch (Exception e) {
            log.error("Unhandled exception while processing request", e);
            HttpHeaders errorHeaders = new HttpHeaders();
            errorHeaders.setContentType(MimeType.TEXT_PLAIN_UTF8);
            HttpResponse errorResponse = new HttpResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    HttpResponseBody.of(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()),
                    errorHeaders
            );
            writeResponse(errorResponse, response);
        }
    }

    private void writeResponse(HttpResponse httpResponse, HttpServletResponse servletResponse) throws IOException {
        if (httpResponse == null) {
            servletResponse.setStatus(HttpStatus.NO_CONTENT.getCode());
            return;
        }

        servletResponse.setStatus(httpResponse.getStatusCode().getCode());

        for (Map.Entry<String, List<String>> entry : httpResponse.getHeaders().asMap().entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (values.isEmpty()) {
                servletResponse.addHeader(name, "");
            } else {
                for (String value : values) {
                    servletResponse.addHeader(name, value);
                }
            }
        }

        HttpResponseBody body = httpResponse.getBody();
        if (!body.isEmpty()) {
            body.writeTo(servletResponse.getOutputStream());
        }
    }
}