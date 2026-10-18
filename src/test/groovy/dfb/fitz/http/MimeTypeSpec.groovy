package dfb.fitz.http

import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MimeTypeSpec extends Specification {

    def "constructs and normalizes MimeType type, subtype, and optional charset"() {
        when:
        def mime = new MimeType("  Application ", " JSON  ", StandardCharsets.UTF_8)

        then:
        mime.type == "application"
        mime.subtype == "json"
        mime.charset == StandardCharsets.UTF_8
        mime.baseType == "application/json"
        mime.toString() == "application/json; charset=UTF-8"
        mime == MimeType.APPLICATION_JSON_UTF8
        mime.withCharset(null) == MimeType.APPLICATION_JSON
    }

    def "parse extracts type, subtype, and optional charset parameter"() {
        expect:
        MimeType parsed = MimeType.parse(input)
        parsed.type == expectedType
        parsed.subtype == expectedSubtype
        parsed.charset == expectedCharset

        where:
        input                                      | expectedType  | expectedSubtype | expectedCharset
        "application/json"                         | "application" | "json"          | null
        "  TEXT/PLAIN ; charset=UTF-8  "           | "text"        | "plain"         | StandardCharsets.UTF_8
        "text/html; boundary=abc; charset=\"utf-8\"" | "text"        | "html"          | StandardCharsets.UTF_8
        "*/*"                                      | "*"           | "*"             | null
    }

    def "rejects null, blank, or malformed MimeType inputs"() {
        when:
        MimeType.parse(invalid)

        then:
        thrown(expectedException)

        where:
        invalid                        | expectedException
        null                           | NullPointerException
        ""                             | IllegalArgumentException
        "   "                          | IllegalArgumentException
        "application"                  | IllegalArgumentException
        "/json"                        | IllegalArgumentException
        "application/"                 | IllegalArgumentException
        "application/json/extra"       | IllegalArgumentException
        "*/json"                       | IllegalArgumentException
        "text/plain; charset=NOT_REAL" | IllegalArgumentException
    }

    def "matches supports exact and wildcard comparisons ignoring charset"() {
        expect:
        MimeType.APPLICATION_JSON.matches(MimeType.APPLICATION_JSON_UTF8)
        MimeType.of("text", "*").matches(MimeType.TEXT_PLAIN)
        MimeType.of("text", "*").matches(MimeType.TEXT_HTML_UTF8)
        !MimeType.of("text", "*").matches(MimeType.APPLICATION_JSON)
        MimeType.ALL.matches(MimeType.APPLICATION_JSON)
        !MimeType.APPLICATION_JSON.matches(null)
    }

    def "HttpHeaders getContentType and setContentType round-trip MimeType"() {
        given:
        def headers = new HttpHeaders()

        expect:
        headers.contentType == null

        when:
        headers.setContentType(MimeType.APPLICATION_JSON_UTF8)

        then:
        headers.getFirst("Content-Type") == "application/json; charset=UTF-8"
        headers.contentType == MimeType.APPLICATION_JSON_UTF8
    }
}
