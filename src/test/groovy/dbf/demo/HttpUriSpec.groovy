package dbf.demo

import dbf.di.HttpUri
import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class HttpUriSpec extends Specification {

    def "constructs HttpUri from a single URI string"() {
        when:
        def httpUri = new HttpUri("https://example.com/items?name=John%20Doe&tag=a&tag=b")

        then:
        httpUri.uri == URI.create("https://example.com/items?name=John%20Doe&tag=a&tag=b")
        httpUri.rawUri == "https://example.com/items?name=John%20Doe&tag=a&tag=b"
        httpUri.rawQuery == "name=John%20Doe&tag=a&tag=b"
        httpUri.query == "name=John Doe&tag=a&tag=b"
        httpUri.queryParams.getFirst("name") == "John Doe"
        httpUri.queryParams.getValues("tag") == ["a", "b"]
        httpUri.toString() == "https://example.com/items?name=John%20Doe&tag=a&tag=b"
    }

    def "constructs HttpUri from separate base URI and query string"() {
        when:
        def httpUri = new HttpUri("https://example.com/search", "q=hello%20world&page=2")

        then:
        httpUri.rawUri == "https://example.com/search?q=hello%20world&page=2"
        httpUri.rawQuery == "q=hello%20world&page=2"
        httpUri.query == "q=hello world&page=2"
        httpUri.queryParams.getFirst("q") == "hello world"
        httpUri.queryParams.getFirst("page") == "2"
    }

    def "handles leading question mark or existing query when combining URI and query"() {
        expect:
        new HttpUri("https://example.com/search", "?a=1").rawUri == "https://example.com/search?a=1"
        new HttpUri("https://example.com/search?a=1", "b=2").rawUri == "https://example.com/search?a=1&b=2"
        new HttpUri("https://example.com/search", null).rawUri == "https://example.com/search"
        new HttpUri("https://example.com/search", "").rawUri == "https://example.com/search"
    }

    def "constructs HttpUri from HttpServletRequest with requestURL and queryString"() {
        given:
        def request = Mock(HttpServletRequest) {
            getRequestURL() >> new StringBuffer("http://localhost:8080/demo/api")
            getQueryString() >> "filter=active%20users&flag"
        }

        when:
        def httpUri = HttpUri.from(request)

        then:
        httpUri.rawUri == "http://localhost:8080/demo/api?filter=active%20users&flag"
        httpUri.rawQuery == "filter=active%20users&flag"
        httpUri.query == "filter=active users&flag"
        httpUri.queryParams.getFirst("filter") == "active users"
        httpUri.queryParams.containsKey("flag")
        httpUri.queryParams.getValues("flag") == []
    }

    def "falls back to getRequestURI when getRequestURL is null"() {
        given:
        def request = Mock(HttpServletRequest) {
            getRequestURL() >> null
            getRequestURI() >> "/demo/path"
            getQueryString() >> null
        }

        when:
        def httpUri = HttpUri.from(request)

        then:
        httpUri.rawUri == "/demo/path"
        httpUri.query == null
        httpUri.rawQuery == null
        httpUri.queryParams.isEmpty()
    }

    def "throws NullPointerException when constructing from null HttpServletRequest"() {
        when:
        HttpUri.from(null)

        then:
        thrown(NullPointerException)
    }

    def "handles null URI string gracefully"() {
        when:
        def httpUri = new HttpUri(null)

        then:
        httpUri.uri == null
        httpUri.path == null
        httpUri.rawUri == null
        httpUri.query == null
        httpUri.rawQuery == null
        httpUri.queryParams.isEmpty()
        !httpUri.matches(".*/foo")
        httpUri.toString() == null
    }

    def "supports getPath and regex matching against path or full URI"() {
        given:
        def httpUri = new HttpUri("http://localhost:8080/demo/foo?a=1")

        expect:
        httpUri.path == "/demo/foo"
        httpUri.matches(".*/foo")
        httpUri.matches(~/.+\/foo/)
        !httpUri.matches(".*/bar")
        !httpUri.matches((String) null)
    }

    def "extracts named path parameters using regex capturing groups"() {
        given:
        def httpUri = new HttpUri("http://localhost:8080/demo/users/42/orders/99?detail=full")

        when:
        def pathParams = httpUri.extractPathParams(".*/users/(?<userId>[^/]+)/orders/(?<orderId>[^/]+)")

        then:
        pathParams == [userId: "42", orderId: "99"]
        httpUri.extractPathParams(".*/non-matching/(?<id>[^/]+)").isEmpty()
        httpUri.extractPathParams((String) null).isEmpty()
    }
}
