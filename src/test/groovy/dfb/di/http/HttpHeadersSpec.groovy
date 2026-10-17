package dfb.di.http

import dfb.di.http.HttpHeaders
import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class HttpHeadersSpec extends Specification {

    def "supports case-insensitive header lookup, set, and add"() {
        given:
        def headers = new HttpHeaders()

        when:
        headers.add("Accept", "text/html")
        headers.add("accept", "application/xhtml+xml")
        headers.set("Content-Type", "application/json")

        then:
        !headers.isEmpty()
        headers.contains("ACCEPT")
        headers.contains("content-type")
        headers.getValues("Accept") == ["text/html", "application/xhtml+xml"]
        headers.getFirst("ACCEPT") == "text/html"
        headers.getFirst("content-type") == "application/json"

        when:
        headers.set("Accept", "text/plain")

        then:
        headers.getValues("accept") == ["text/plain"]
    }

    def "extracts headers from HttpServletRequest"() {
        given:
        def request = Mock(HttpServletRequest) {
            getHeaderNames() >> Collections.enumeration(["X-Custom", "Authorization"])
            getHeaders("X-Custom") >> Collections.enumeration(["val1", "val2"])
            getHeaders("Authorization") >> Collections.enumeration(["Bearer token"])
        }

        when:
        def headers = HttpHeaders.from(request)

        then:
        headers.names.size() == 2
        headers.getValues("x-custom") == ["val1", "val2"]
        headers.getFirst("authorization") == "Bearer token"
        headers.asMap()["X-CUSTOM"] == ["val1", "val2"]
    }

    def "constructs from initial map and handles null inputs safely"() {
        when:
        def headers = new HttpHeaders([Host: ["localhost:8080"]])

        then:
        headers.getFirst("host") == "localhost:8080"
        headers.getValues(null) == []
        headers.getFirst(null) == null
        !headers.contains(null)
        new HttpHeaders((Map) null).isEmpty()
        new HttpHeaders((HttpServletRequest) null).isEmpty()
    }
}
