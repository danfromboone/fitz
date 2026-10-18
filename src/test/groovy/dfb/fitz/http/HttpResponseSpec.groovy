package dfb.fitz.http

import spock.lang.Specification

class HttpResponseSpec extends Specification {

    def "defaults to 200 OK with empty body and empty headers"() {
        when:
        def response = new HttpResponse()

        then:
        response.statusCode == HttpStatus.OK
        response.body.isEmpty()
        response.headers.isEmpty()
    }

    def "supports setting statusCode, body, and headers and allows null body and headers"() {
        given:
        def headers = new HttpHeaders()
        headers.set("X-Test", "123")

        when:
        def response = new HttpResponse(HttpStatus.CREATED, HttpResponseBody.of("created"), headers)

        then:
        response.statusCode == HttpStatus.CREATED
        response.body.asString() == "created"
        response.headers.getFirst("x-test") == "123"

        when:
        response.statusCode = HttpStatus.ACCEPTED
        response.setBody("updated")

        then:
        response.statusCode == HttpStatus.ACCEPTED
        response.body.asString() == "updated"

        when:
        def nullBodyAndHeaders = new HttpResponse(HttpStatus.NO_CONTENT, (HttpResponseBody) null, null)

        then:
        nullBodyAndHeaders.statusCode == HttpStatus.NO_CONTENT
        nullBodyAndHeaders.body.isEmpty()
        nullBodyAndHeaders.headers.isEmpty()
    }

    def "throws NullPointerException when statusCode is null"() {
        when:
        new HttpResponse(null)

        then:
        thrown(NullPointerException)

        when:
        new HttpResponse().setStatusCode(null)

        then:
        thrown(NullPointerException)
    }
}
