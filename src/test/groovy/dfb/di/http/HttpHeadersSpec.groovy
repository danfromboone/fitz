package dfb.di.http

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class HttpHeadersSpec extends Specification {

    def "supports case-insensitive header lookup, set, and add with trimming"() {
        given:
        def headers = new HttpHeaders()

        when:
        headers.add("  Accept  ", "  text/html  ")
        headers.add("accept", "application/xhtml+xml")
        headers.set("  Content-Type  ", "  application/json  ")

        then:
        !headers.isEmpty()
        headers.contains("ACCEPT")
        headers.contains("  content-type  ")
        headers.getValues("Accept") == ["text/html", "application/xhtml+xml"]
        headers.getFirst("  ACCEPT  ") == "text/html"
        headers.getFirst("content-type") == "application/json"

        when:
        headers.set("Accept", "text/plain")

        then:
        headers.getValues("accept") == ["text/plain"]
    }

    def "extracts and trims headers from HttpServletRequest"() {
        given:
        def request = Mock(HttpServletRequest) {
            getHeaderNames() >> Collections.enumeration(["X-Custom", "Authorization"])
            getHeaders("X-Custom") >> Collections.enumeration([" val1 ", "val2"])
            getHeaders("Authorization") >> Collections.enumeration([" Bearer token "])
        }

        when:
        def headers = HttpHeaders.from(request)

        then:
        headers.names.size() == 2
        headers.getValues("x-custom") == ["val1", "val2"]
        headers.getFirst("authorization") == "Bearer token"
        headers.asMap()["X-CUSTOM"] == ["val1", "val2"]
    }

    def "constructs from initial map and validates inputs"() {
        given:
        def headers = new HttpHeaders(["  Host  ": ["  localhost:8080  "]])

        expect:
        headers.getFirst("host") == "localhost:8080"

        when:
        new HttpHeaders((Map) null)

        then:
        thrown(NullPointerException)

        when:
        new HttpHeaders((HttpServletRequest) null)

        then:
        thrown(NullPointerException)

        when:
        headers.getValues(null)

        then:
        thrown(NullPointerException)

        when:
        headers.getValues("   ")

        then:
        thrown(IllegalArgumentException)

        when:
        headers.getFirst(null)

        then:
        thrown(NullPointerException)

        when:
        headers.contains("   ")

        then:
        thrown(IllegalArgumentException)

        when:
        headers.set("   ", "val")

        then:
        thrown(IllegalArgumentException)

        when:
        headers.set("X-Key", null)

        then:
        thrown(NullPointerException)

        when:
        headers.add("X-Key", null)

        then:
        thrown(NullPointerException)
    }
}
