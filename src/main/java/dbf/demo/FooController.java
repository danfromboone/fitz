package dbf.demo;

import dbf.di.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class FooController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(FooController.class);
    private static final Pattern FOO_PATTERN = Pattern.compile("/foo");

    @Override
    public boolean canHandle(HttpRequest request) {
        return request != null
                && request.getHttpUri() != null
                && request.getHttpUri().matches(FOO_PATTERN)
                && HttpMethod.GET.equals(request.getMethod());
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        QueryParams queryParams = request.getQueryParams();
        log.info("Handling /foo request with queryParams={}", queryParams.asMap());
        String message = "Request params: " + queryParams.asMap();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain; charset=UTF-8");

        return new HttpResponse(HttpStatus.OK, HttpResponseBody.of(message), headers);
    }
}
