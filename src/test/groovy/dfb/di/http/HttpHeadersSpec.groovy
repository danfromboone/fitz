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

    def "supports headers without values"() {
        given:
        def headers = new HttpHeaders(["X-Initial-Empty": null, "X-Initial-Blank": ["   "]])

        when:
        headers.set("X-Null-Val", null)
        headers.set("X-Empty-Val", "   ")
        headers.add("X-Added-Null", null)

        then:
        headers.contains("X-Initial-Empty")
        headers.getValues("X-Initial-Empty") == []
        headers.getFirst("X-Initial-Empty") == null

        headers.contains("X-Initial-Blank")
        headers.getValues("X-Initial-Blank") == []
        headers.getFirst("X-Initial-Blank") == null

        headers.contains("X-Null-Val")
        headers.getValues("X-Null-Val") == []
        headers.getFirst("X-Null-Val") == null

        headers.contains("X-Empty-Val")
        headers.getValues("X-Empty-Val") == []
        headers.getFirst("X-Empty-Val") == null

        headers.contains("X-Added-Null")
        headers.getValues("X-Added-Null") == []
        headers.getFirst("X-Added-Null") == null
    }

    def "extracts and trims headers from HttpServletRequest including headers with empty values"() {
        given:
        def request = Mock(HttpServletRequest) {
            getHeaderNames() >> Collections.enumeration(["X-Custom", "Authorization", "X-No-Value"])
            getHeaders("X-Custom") >> Collections.enumeration([" val1 ", "val2"])
            getHeaders("Authorization") >> Collections.enumeration([" Bearer token "])
            getHeaders("X-No-Value") >> Collections.enumeration([""])
        }

        when:
        def headers = HttpHeaders.from(request)

        then:
        headers.names.size() == 3
        headers.getValues("x-custom") == ["val1", "val2"]
        headers.getFirst("authorization") == "Bearer token"
        headers.contains("x-no-value")
        headers.getValues("x-no-value") == []
        headers.getFirst("x-no-value") == null
        headers.asMap()["X-CUSTOM"] == ["val1", "val2"]
    }

    def "constructs from initial map and validates header names"() {
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
        headers.add(null, "val")

        then:
        thrown(NullPointerException)
    }
}
