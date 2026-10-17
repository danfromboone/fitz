package dbf.demo

import dbf.di.HttpResponseBody
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class HttpResponseBodySpec extends Specification {

    def "constructs from String and byte[] and writes to OutputStream"() {
        given:
        def body = HttpResponseBody.of("Hello World!")
        def out = new ByteArrayOutputStream()

        when:
        body.writeTo(out)

        then:
        !body.isEmpty()
        body.length() == 12
        body.asString() == "Hello World!"
        body.toString() == "Hello World!"
        out.toString(StandardCharsets.UTF_8) == "Hello World!"
        body == HttpResponseBody.of("Hello World!".getBytes(StandardCharsets.UTF_8))
    }

    def "returns singleton empty body for null or empty inputs"() {
        expect:
        HttpResponseBody.empty().isEmpty()
        HttpResponseBody.empty().length() == 0
        HttpResponseBody.of((String) null).is(HttpResponseBody.empty())
        HttpResponseBody.of("").is(HttpResponseBody.empty())
        HttpResponseBody.of((byte[]) null).is(HttpResponseBody.empty())
        HttpResponseBody.of(new byte[0]).is(HttpResponseBody.empty())
    }
}
