package dbf.demo

import dbf.di.HttpMethod
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
        null      | null
        ""        | null
        "   "     | null
    }

    def "resolves HttpMethod from HttpServletRequest"() {
        given:
        def request = Mock(HttpServletRequest) {
            getMethod() >> "POST"
        }

        expect:
        HttpMethod.from(request) == HttpMethod.POST
        HttpMethod.from((HttpServletRequest) null) == null
    }
}
