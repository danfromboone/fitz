package dfb.fitz.http

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class HttpRequestBodySpec extends Specification {

    def "lazily reads and caches body from HttpServletRequest reader so it can be read multiple times"() {
        given:
        def request = Mock(HttpServletRequest)
        def body = HttpRequestBody.from(request)

        when:
        def firstRead = body.asString()
        def secondRead = body.asString()

        then:
        1 * request.getCharacterEncoding() >> "  UTF-8  "
        1 * request.getInputStream() >> null
        1 * request.getReader() >> new BufferedReader(new StringReader("hello request body"))
        firstRead == "hello request body"
        secondRead == "hello request body"
        body.length() == "hello request body".length()
        !body.isEmpty()
    }

    def "reads body from ServletInputStream when available"() {
        given:
        def bytes = "stream payload".getBytes(StandardCharsets.UTF_8)
        def bais = new ByteArrayInputStream(bytes)
        def servletInputStream = new ServletInputStream() {
            @Override boolean isFinished() { bais.available() == 0 }
            @Override boolean isReady() { true }
            @Override void setReadListener(ReadListener readListener) {}
            @Override int read() throws IOException { bais.read() }
        }
        def request = Mock(HttpServletRequest) {
            getInputStream() >> servletInputStream
        }

        when:
        def body = new HttpRequestBody(request)

        then:
        body.asString() == "stream payload"
        body.asBytes() == bytes
    }

    def "constructs directly from String or byte array and allows null content for empty body"() {
        when:
        def fromString = new HttpRequestBody("direct text")
        def fromBytes = new HttpRequestBody("direct text".getBytes(StandardCharsets.UTF_8))
        def emptyBody = new HttpRequestBody((String) null)

        then:
        fromString == fromBytes
        fromString.hashCode() == fromBytes.hashCode()
        fromString.toString() == "direct text"
        emptyBody.isEmpty()
        emptyBody.length() == 0
    }

    def "throws NullPointerException when request or charset is null"() {
        when:
        new HttpRequestBody((HttpServletRequest) null)

        then:
        thrown(NullPointerException)

        when:
        new HttpRequestBody("text", (Charset) null)

        then:
        thrown(NullPointerException)

        when:
        new HttpRequestBody(new byte[0], (Charset) null)

        then:
        thrown(NullPointerException)

        when:
        new HttpRequestBody("text").asString(null)

        then:
        thrown(NullPointerException)
    }
}
