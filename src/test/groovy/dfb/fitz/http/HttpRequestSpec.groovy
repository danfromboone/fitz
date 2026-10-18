package dfb.fitz.http

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class HttpRequestSpec extends Specification {

    def "encapsulates HttpServletRequest attributes, URI, query params, headers, method, and body"() {
        given:
        def servletRequest = Mock(HttpServletRequest) {
            getMethod() >> "POST"
            getContextPath() >> "/demo"
            getServletPath() >> "/foo"
            getPathInfo() >> "/extra"
            getRequestURI() >> "/demo/foo/extra"
            getRequestURL() >> new StringBuffer("http://localhost:8080/demo/foo/extra")
            getQueryString() >> "key=val%201&flag"
            getHeaderNames() >> Collections.enumeration(["Content-Type"])
            getHeaders("Content-Type") >> Collections.enumeration(["text/plain"])
            getReader() >> new BufferedReader(new StringReader("request-content"))
        }

        when:
        def httpRequest = new HttpRequest(servletRequest)

        then:
        httpRequest.servletRequest.is(servletRequest)
        httpRequest.method == HttpMethod.POST
        httpRequest.contextPath == "/demo"
        httpRequest.servletPath == "/foo"
        httpRequest.pathInfo == "/extra"
        httpRequest.requestURI == "/demo/foo/extra"
        httpRequest.queryString == "key=val%201&flag"
        httpRequest.httpUri.rawUri == "http://localhost:8080/demo/foo/extra?key=val%201&flag"
        httpRequest.queryParams.getFirst("key") == "val 1"
        httpRequest.queryParams.containsKey("flag")
        httpRequest.headers.getFirst("content-type") == "text/plain"
        httpRequest.body.asString() == "request-content"
    }

    def "constructs directly from domain objects and allows null headers and body"() {
        given:
        def headers = new HttpHeaders([Accept: ["application/json"]])
        def body = new HttpRequestBody('{"hello":"world"}')

        when:
        def httpRequest = new HttpRequest(
                HttpMethod.PUT,
                new HttpUri("http://localhost:8080/demo/items/42?notify=true"),
                headers,
                body
        )
        def withNullHeadersAndBody = new HttpRequest(
                HttpMethod.GET,
                new HttpUri("http://localhost:8080/demo/items/42"),
                null,
                null
        )

        then:
        httpRequest.servletRequest == null
        httpRequest.method == HttpMethod.PUT
        httpRequest.requestURI == "/demo/items/42"
        httpRequest.queryString == "notify=true"
        httpRequest.contextPath == ""
        httpRequest.servletPath == ""
        httpRequest.pathInfo == null
        httpRequest.headers.getFirst("accept") == "application/json"
        httpRequest.body.asString() == '{"hello":"world"}'

        withNullHeadersAndBody.headers.isEmpty()
        withNullHeadersAndBody.body.isEmpty()
    }

    def "throws NullPointerException when constructed with null HttpServletRequest, HttpMethod, or HttpUri"() {
        when:
        new HttpRequest((HttpServletRequest) null)

        then:
        thrown(NullPointerException)

        when:
        new HttpRequest(null, "http://localhost:8080/foo")

        then:
        thrown(NullPointerException)

        when:
        new HttpRequest(HttpMethod.GET, (HttpUri) null, null, null)

        then:
        thrown(NullPointerException)
    }
}
