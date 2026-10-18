package dfb.fitz

import spock.lang.Specification

import java.time.Duration

class ValidateSpec extends Specification {

    def "nonNull returns value when non-null"() {
        expect:
        Validate.nonNull("hello", "value") == "hello"
    }

    def "nonNull throws NullPointerException when null"() {
        when:
        Validate.nonNull(null, "myField")

        then:
        def e = thrown(NullPointerException)
        e.message == "myField must not be null"
    }

    def "trimmedNonBlank trims and returns non-blank string"() {
        expect:
        Validate.trimmedNonBlank(input, "myField") == expected

        where:
        input       | expected
        "hello"     | "hello"
        "  hello  " | "hello"
        "\thello\n" | "hello"
    }

    def "trimmedNonBlank throws NullPointerException when null"() {
        when:
        Validate.trimmedNonBlank(null, "myField")

        then:
        def e = thrown(NullPointerException)
        e.message == "myField must not be null"
    }

    def "trimmedNonBlank throws IllegalArgumentException for empty or blank string"() {
        when:
        Validate.trimmedNonBlank(input, "myField")

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "myField must not be blank"

        where:
        input << ["", "   ", "\t\n"]
    }

    def "trimToEmpty returns trimmed string or empty string for null"() {
        expect:
        Validate.trimToEmpty(input) == expected

        where:
        input       | expected
        null        | ""
        ""          | ""
        "   "       | ""
        "  hello  " | "hello"
    }

    def "inRange returns value when within inclusive bounds"() {
        expect:
        Validate.inRange(val, 100, 599, "statusCode") == val

        where:
        val << [100, 200, 599]
    }

    def "inRange throws IllegalArgumentException when out of bounds"() {
        when:
        Validate.inRange(val, 100, 599, "statusCode")

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "statusCode must be between 100 and 599: $val"

        where:
        val << [99, 600, -1]
    }

    def "positive returns duration when strictly positive"() {
        expect:
        Validate.positive(duration, "pollInterval") == duration

        where:
        duration << [Duration.ofMillis(1), Duration.ofSeconds(10), Duration.ofMinutes(5)]
    }

    def "positive throws NullPointerException when duration is null"() {
        when:
        Validate.positive(null, "pollInterval")

        then:
        def e = thrown(NullPointerException)
        e.message == "pollInterval must not be null"
    }

    def "positive throws IllegalArgumentException when duration is zero or negative"() {
        when:
        Validate.positive(duration, "pollInterval")

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "pollInterval must be positive"

        where:
        duration << [Duration.ZERO, Duration.ofMillis(-1), Duration.ofSeconds(-5)]
    }

    def "positive returns int when strictly positive"() {
        expect:
        Validate.positive(val, "maxTotalConnections") == val

        where:
        val << [1, 20, 100]
    }

    def "positive throws IllegalArgumentException when int is zero or negative"() {
        when:
        Validate.positive(val, "maxTotalConnections")

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "maxTotalConnections must be positive: $val"

        where:
        val << [0, -1, -50]
    }
}
