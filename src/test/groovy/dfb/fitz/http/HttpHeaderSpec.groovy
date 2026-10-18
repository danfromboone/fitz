package dfb.fitz.http

import spock.lang.Specification

class HttpHeaderSpec extends Specification {

    def "HttpHeader reuses cached predefined instance when header name matches known header case-insensitively"() {
        expect:
        HttpHeader.CONTENT_TYPE.headerName == "Content-Type"
        HttpHeader.CONTENT_TYPE.toString() == "Content-Type"
        HttpHeader.of("Content-Type").is(HttpHeader.CONTENT_TYPE)
        HttpHeader.of("  content-type ").is(HttpHeader.CONTENT_TYPE)
        HttpHeader.from("X-FORWARDED-FOR").is(HttpHeader.X_FORWARDED_FOR)
        HttpHeader.of("Authorization").is(HttpHeader.AUTHORIZATION)
    }

    def "HttpHeader creates a new instance when header name is not one of the predefined headers"() {
        when:
        HttpHeader custom1 = HttpHeader.of("  X-Custom-Header  ")
        HttpHeader custom2 = HttpHeader.of("x-custom-header")

        then:
        custom1.headerName == "X-Custom-Header"
        custom1.toString() == "X-Custom-Header"
        !custom1.is(custom2)
        custom1 == custom2
        custom1.hashCode() == custom2.hashCode()
    }

    def "HttpHeader.of rejects null or blank header names"() {
        when:
        HttpHeader.of(input)

        then:
        thrown(expectedException)

        where:
        input | expectedException
        null  | NullPointerException
        ""    | IllegalArgumentException
        "   " | IllegalArgumentException
    }

    def "HttpHeaders supports HttpHeader overloads for both predefined and custom headers"() {
        given:
        def headers = new HttpHeaders()
        def customHeader = HttpHeader.of("X-Trace-Token")

        when:
        headers.set(HttpHeader.AUTHORIZATION, "Bearer token123")
        headers.add(HttpHeader.ACCEPT, MimeType.APPLICATION_JSON.toString())
        headers.add(HttpHeader.ACCEPT, MimeType.TEXT_PLAIN.toString())
        headers.set(customHeader, "abc-999")

        then:
        headers.contains(HttpHeader.AUTHORIZATION)
        headers.getFirst(HttpHeader.AUTHORIZATION) == "Bearer token123"
        headers.getValues(HttpHeader.ACCEPT) == ["application/json", "text/plain"]
        headers.contains(customHeader)
        headers.getFirst(customHeader) == "abc-999"
        !headers.contains(HttpHeader.USER_AGENT)
    }
}
