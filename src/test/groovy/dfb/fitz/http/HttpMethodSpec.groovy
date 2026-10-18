package dfb.fitz.http

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class HttpMethodSpec extends Specification {

    def "resolves HttpMethod from string case-insensitively"() {
        expect:
        HttpMethod.from(input) == expected

        where:
        input     | expected
        "GET"     | HttpMethod.GET
        "get"     | HttpMethod.GET
        " post "  | HttpMethod.POST
        "Put"     | HttpMethod.PUT
        "DELETE"  | HttpMethod.DELETE
        "PATCH"   | HttpMethod.PATCH
        "HEAD"    | HttpMethod.HEAD
        "OPTIONS" | HttpMethod.OPTIONS
        "TRACE"   | HttpMethod.TRACE
        "CONNECT" | HttpMethod.CONNECT
    }

    def "throws NullPointerException when method string is null"() {
        when:
        HttpMethod.from((String) null)

        then:
        thrown(NullPointerException)
    }

    def "throws IllegalArgumentException when method string is blank or unknown"() {
        when:
        HttpMethod.from(input)

        then:
        thrown(IllegalArgumentException)

        where:
        input << ["", "   ", "INVALID"]
    }

    def "resolves HttpMethod from HttpServletRequest"() {
        given:
        def request = Mock(HttpServletRequest) {
            getMethod() >> "POST"
        }

        expect:
        HttpMethod.from(request) == HttpMethod.POST
    }

    def "throws NullPointerException when HttpServletRequest is null"() {
        when:
        HttpMethod.from((HttpServletRequest) null)

        then:
        thrown(NullPointerException)
    }
}
