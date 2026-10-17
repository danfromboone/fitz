package dbf.demo

import dbf.di.HttpHeaders
import dbf.di.HttpResponse
import dbf.di.HttpResponseBody
import dbf.di.HttpStatus
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

    def "supports setting statusCode, body, and headers"() {
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
    }
}
