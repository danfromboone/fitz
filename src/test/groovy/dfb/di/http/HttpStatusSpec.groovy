package dfb.di.http

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

    def "reuses the exact same predefined instance when looked up via HttpStatus.of"() {
        expect:
        HttpStatus.of(200).is(HttpStatus.OK)
        HttpStatus.of(200, "OK").is(HttpStatus.OK)
        HttpStatus.of(200, "").is(HttpStatus.OK)
        HttpStatus.of(200, null).is(HttpStatus.OK)
        HttpStatus.of(404).is(HttpStatus.NOT_FOUND)
        HttpStatus.of(500).is(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    def "supports custom status codes and custom reason phrases"() {
        when:
        def custom1 = HttpStatus.of(299, "Custom Warning")
        def custom2 = HttpStatus.of(299, "Custom Warning")
        def customReasonForStandardCode = HttpStatus.of(200, "Custom OK")
        def codeOnly = HttpStatus.of(499)

        then:
        custom1 == custom2
        custom1.hashCode() == custom2.hashCode()
        custom1.toString() == "299 Custom Warning"

        !customReasonForStandardCode.is(HttpStatus.OK)
        customReasonForStandardCode == HttpStatus.OK
        customReasonForStandardCode.reasonPhrase == "Custom OK"

        codeOnly.code == 499
        codeOnly.reasonPhrase == ""
        codeOnly.toString() == "499"
    }
}
