package dbf.demo

import dbf.di.HttpStatus
import spock.lang.Specification

class HttpStatusSpec extends Specification {

    def "provides standard static final status constants"() {
        expect:
        HttpStatus.OK.code == 200
        HttpStatus.OK.reasonPhrase == "OK"
        HttpStatus.OK.toString() == "200 OK"

        HttpStatus.NOT_FOUND.code == 404
        HttpStatus.NOT_FOUND.reasonPhrase == "Not Found"

        HttpStatus.INTERNAL_SERVER_ERROR.code == 500
        HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase == "Internal Server Error"
    }

    def "supports custom status codes and value equality"() {
        when:
        def custom1 = new HttpStatus(299, "Custom Warning")
        def custom2 = new HttpStatus(299, "Custom Warning")
        def codeOnly = new HttpStatus(499)

        then:
        custom1 == custom2
        custom1.hashCode() == custom2.hashCode()
        custom1.toString() == "299 Custom Warning"
        codeOnly.code == 499
        codeOnly.reasonPhrase == ""
        codeOnly.toString() == "499"
    }
}
