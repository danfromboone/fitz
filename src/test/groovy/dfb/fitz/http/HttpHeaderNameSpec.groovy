package dfb.fitz.http

import spock.lang.Specification

class HttpHeaderNameSpec extends Specification {

    def "HttpHeaderName reuses cached predefined instance when header name matches known header case-insensitively"() {
        expect:
        HttpHeaderName.CONTENT_TYPE.name == "Content-Type"
        HttpHeaderName.CONTENT_TYPE.toString() == "Content-Type"
        HttpHeaderName.of("Content-Type").is(HttpHeaderName.CONTENT_TYPE)
        HttpHeaderName.of("  content-type ").is(HttpHeaderName.CONTENT_TYPE)
        HttpHeaderName.from("X-FORWARDED-FOR").is(HttpHeaderName.X_FORWARDED_FOR)
        HttpHeaderName.of("Authorization").is(HttpHeaderName.AUTHORIZATION)
    }

    def "HttpHeaderName creates a new instance when header name is not one of the predefined headers"() {
        when:
        HttpHeaderName custom1 = HttpHeaderName.of("  X-Custom-Header  ")
        HttpHeaderName custom2 = HttpHeaderName.of("x-custom-header")

        then:
        custom1.name == "X-Custom-Header"
        custom1.toString() == "X-Custom-Header"
        !custom1.is(custom2)
        custom1 == custom2
        custom1.hashCode() == custom2.hashCode()
    }

    def "HttpHeaderName.of rejects null or blank header names"() {
        when:
        HttpHeaderName.of(input)

        then:
        thrown(expectedException)

        where:
        input | expectedException
        null  | NullPointerException
        ""    | IllegalArgumentException
        "   " | IllegalArgumentException
    }

    def "HttpHeaders supports HttpHeaderName overloads for both predefined and custom headers"() {
        given:
        def headers = new HttpHeaders()
        def customHeader = HttpHeaderName.of("X-Trace-Token")

        when:
        headers.set(HttpHeaderName.AUTHORIZATION, "Bearer token123")
        headers.add(HttpHeaderName.ACCEPT, MimeType.APPLICATION_JSON.toString())
        headers.add(HttpHeaderName.ACCEPT, MimeType.TEXT_PLAIN.toString())
        headers.set(customHeader, "abc-999")

        then:
        headers.contains(HttpHeaderName.AUTHORIZATION)
        headers.getFirst(HttpHeaderName.AUTHORIZATION) == "Bearer token123"
        headers.getValues(HttpHeaderName.ACCEPT) == ["application/json", "text/plain"]
        headers.contains(customHeader)
        headers.getFirst(customHeader) == "abc-999"
        !headers.contains(HttpHeaderName.USER_AGENT)
    }
}
