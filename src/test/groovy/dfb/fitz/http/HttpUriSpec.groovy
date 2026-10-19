package dfb.fitz.http

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

import java.util.regex.Pattern

class HttpUriSpec extends Specification {

    def "constructs HttpUri from a single URI string and trims whitespace"() {
        when:
        def httpUri = new HttpUri("  https://example.com/items?name=John%20Doe&tag=a&tag=b  ")

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
        def httpUri = new HttpUri("  https://example.com/search  ", "  q=hello%20world&page=2  ")

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
        new HttpUri("https://example.com/search", "   ").rawUri == "https://example.com/search"
        new HttpUri("https://example.com/search", "?").rawUri == "https://example.com/search"
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

    def "throws NullPointerException or IllegalArgumentException on null or blank URI inputs"() {
        when:
        HttpUri.from(null)

        then:
        thrown(NullPointerException)

        when:
        new HttpUri(null)

        then:
        thrown(NullPointerException)

        when:
        new HttpUri("   ")

        then:
        thrown(IllegalArgumentException)

        when:
        new HttpUri(null, "a=1")

        then:
        thrown(NullPointerException)

        when:
        new HttpUri("  ", "a=1")

        then:
        thrown(IllegalArgumentException)
    }

    def "supports getPath and regex matching against path or full URI"() {
        given:
        def httpUri = new HttpUri("http://localhost:8080/demo/foo?a=1")

        expect:
        httpUri.path == "/demo/foo"
        httpUri.matches("  .*/foo  ")
        httpUri.matches(~/.+\/foo/)
        !httpUri.matches(".*/bar")

        when:
        httpUri.matches((String) null)

        then:
        thrown(NullPointerException)

        when:
        httpUri.matches((Pattern) null)

        then:
        thrown(NullPointerException)

        when:
        httpUri.matches("   ")

        then:
        thrown(IllegalArgumentException)
    }

    def "extracts named path parameters using regex capturing groups"() {
        given:
        def httpUri = new HttpUri("http://localhost:8080/demo/users/42/orders/99?detail=full")

        when:
        def pathParams = httpUri.extractPathParams("  .*/users/(?<userId>[^/]+)/orders/(?<orderId>[^/]+)  ")

        then:
        pathParams == [userId: "42", orderId: "99"]
        httpUri.extractPathParams(".*/non-matching/(?<id>[^/]+)").isEmpty()

        when:
        httpUri.extractPathParams((String) null)

        then:
        thrown(NullPointerException)

        when:
        httpUri.extractPathParams((Pattern) null)

        then:
        thrown(NullPointerException)

        when:
        httpUri.extractPathParams("   ")

        then:
        thrown(IllegalArgumentException)
    }

    def "extracts decoded positional path segments while preserving encoded slashes and plus signs"() {
        given:
        def httpUri = new HttpUri("http://app/some-id/123/John%20Doe/a%2Fb/c+d")

        expect:
        httpUri.pathSegments == ["some-id", "123", "John Doe", "a/b", "c+d"]
        httpUri.getPathSegment(0) == "some-id"
        httpUri.getPathSegment(1) == "123"
        httpUri.getPathSegment(3) == "a/b"
        httpUri.getPathSegment(4) == "c+d"
        httpUri.getPathSegment(-1) == null
        httpUri.getPathSegment(5) == null

        and: "empty or root paths return an empty unmodifiable list"
        new HttpUri("http://app").pathSegments == []
        new HttpUri("http://app/").pathSegments == []

        when:
        httpUri.pathSegments.add("extra")

        then:
        thrown(UnsupportedOperationException)
    }
}
