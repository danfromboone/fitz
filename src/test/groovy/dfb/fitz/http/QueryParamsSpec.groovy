package dfb.fitz.http

import spock.lang.Specification

class QueryParamsSpec extends Specification {

    def "parses single and multiple values from query string and trims keys and values"() {
        when:
        def queryParams = HttpQueryParams.fromEncodedQuery("  name = Alice & tag = java & tag = groovy  ")

        then:
        !queryParams.isEmpty()
        queryParams.keys == ["name", "tag"] as Set
        queryParams.getFirst("name") == "Alice"
        queryParams.getFirst("  name  ") == "Alice"
        queryParams.getValues("name") == ["Alice"]
        queryParams.getFirst("tag") == "java"
        queryParams.getValues("tag") == ["java", "groovy"]
        queryParams.asMap() == [name: ["Alice"], tag: ["java", "groovy"]]
    }

    def "handles keys without values as empty lists and null first value"() {
        when:
        def queryParams = HttpQueryParams.fromEncodedQuery("flag&empty=&key=val")

        then:
        queryParams.containsKey("flag")
        queryParams.containsKey("  flag  ")
        queryParams.getValues("flag") == []
        queryParams.getFirst("flag") == null

        queryParams.containsKey("empty")
        queryParams.getValues("empty") == []
        queryParams.getFirst("empty") == null

        queryParams.getValues("key") == ["val"]
        queryParams.getFirst("key") == "val"
    }

    def "decodes percent-encoded parameters when constructed from URI"() {
        given:
        def uri = URI.create("https://example.com/test?greeting=Hello%20World%21&symbol=%E2%9C%93&noVal")

        when:
        def queryParams = HttpQueryParams.from(uri)

        then:
        queryParams.getFirst("greeting") == "Hello World!"
        queryParams.getFirst("symbol") == "✓"
        queryParams.containsKey("noVal")
        queryParams.getValues("noVal") == []
    }

    def "returns empty collection views for empty query or URI without query"() {
        expect:
        HttpQueryParams.fromEncodedQuery("").isEmpty()
        HttpQueryParams.fromEncodedQuery("   ").isEmpty()
        HttpQueryParams.from(URI.create("https://example.com/no-query")).isEmpty()

        and:
        def emptyParams = HttpQueryParams.fromEncodedQuery("")
        emptyParams.asMap().isEmpty()
        emptyParams.keys.isEmpty()
        emptyParams.getValues("missing") == []
        emptyParams.getFirst("missing") == null
        !emptyParams.containsKey("missing")
    }

    def "throws NullPointerException or IllegalArgumentException on invalid inputs"() {
        given:
        def queryParams = HttpQueryParams.fromEncodedQuery("a=1")

        when:
        HttpQueryParams.fromEncodedQuery(null)

        then:
        thrown(NullPointerException)

        when:
        HttpQueryParams.from(null)

        then:
        thrown(NullPointerException)

        when:
        HttpQueryParams.fromEncodedQuery("=valueWithoutKey")

        then:
        thrown(IllegalArgumentException)

        when:
        queryParams.getValues(null)

        then:
        thrown(NullPointerException)

        when:
        queryParams.getValues("   ")

        then:
        thrown(IllegalArgumentException)

        when:
        queryParams.getFirst(null)

        then:
        thrown(NullPointerException)

        when:
        queryParams.getFirst("")

        then:
        thrown(IllegalArgumentException)

        when:
        queryParams.containsKey(null)

        then:
        thrown(NullPointerException)

        when:
        queryParams.containsKey(" ")

        then:
        thrown(IllegalArgumentException)
    }

    def "returned map and value lists are unmodifiable"() {
        given:
        def queryParams = HttpQueryParams.fromEncodedQuery("a=1")

        when:
        queryParams.asMap().put("b", ["2"])

        then:
        thrown(UnsupportedOperationException)

        when:
        queryParams.getValues("a").add("2")

        then:
        thrown(UnsupportedOperationException)
    }

    def "toEncodedQuery and toString output URL-encoded query string representation"() {
        expect:
        HttpQueryParams.fromEncodedQuery("").toEncodedQuery() == ""
        HttpQueryParams.fromEncodedQuery("").toString() == ""
        HttpQueryParams.fromEncodedQuery("  name = Alice & tag = java & tag = groovy  ").toEncodedQuery() == "name=Alice&tag=java&tag=groovy"
        HttpQueryParams.fromEncodedQuery("flag&empty=&key=val").toEncodedQuery() == "flag&empty&key=val"
        HttpQueryParams.from(URI.create("https://example.com/test?greeting=Hello%20World%21&symbol=%E2%9C%93&noVal")).toEncodedQuery() == "greeting=Hello%20World%21&symbol=%E2%9C%93&noVal"
    }

    def "supports adding and setting query parameters"() {
        given:
        def queryParams = new HttpQueryParams()

        when:
        queryParams.add("  page ", " 1 ")
        queryParams.add("tag", "java")
        queryParams.add("tag", "groovy")
        queryParams.add("flag")

        then:
        queryParams.getFirst("page") == "1"
        queryParams.getValues("tag") == ["java", "groovy"]
        queryParams.containsKey("flag")
        queryParams.getValues("flag") == []
        queryParams.toEncodedQuery() == "page=1&tag=java&tag=groovy&flag"

        when:
        queryParams.set("tag", "spock")

        then:
        queryParams.getValues("tag") == ["spock"]
        queryParams.toEncodedQuery() == "page=1&tag=spock&flag"
    }

    def "supports Map constructor with defensive copy"() {
        given:
        def source = [a: [" 1 ", " 2 "], flag: []]
        def queryParams = new HttpQueryParams(source)

        when:
        source.put("b", ["3"])

        then:
        !queryParams.containsKey("b")
        queryParams.getValues("a") == ["1", "2"]
        queryParams.toEncodedQuery() == "a=1&a=2&flag"
    }

    def "splits on raw delimiters before decoding and guards against double-encoding or double-decoding"() {
        given: "a URI and raw query string containing percent-encoded '&', '=', '+', and '%'"
        def rawQuery = "filter=a%26b%3Dc&math=1%20%2B%201%20%3D%202&discount=50%25%20off"
        def fromString = HttpQueryParams.fromEncodedQuery(rawQuery)
        def fromUri = HttpQueryParams.from(URI.create("https://example.com/search?" + rawQuery))

        expect: "internal storage holds decoded plain strings without corrupting embedded '&' or '='"
        fromString.getFirst("filter") == "a&b=c"
        fromString.getFirst("math") == "1 + 1 = 2"
        fromString.getFirst("discount") == "50% off"
        fromUri.asMap() == fromString.asMap()

        and: "toEncodedQuery() and toString() encode once and round-tripping does not double-encode"
        fromString.toEncodedQuery() == rawQuery
        fromString.toString() == rawQuery
        HttpQueryParams.fromEncodedQuery(fromString.toEncodedQuery()).asMap() == fromString.asMap()
        HttpQueryParams.fromEncodedQuery(fromString.toEncodedQuery()).toEncodedQuery() == rawQuery

        when: "adding plain strings containing '%', '+', '&', and '=' programmatically"
        def programmatic = new HttpQueryParams()
        programmatic.add("filter", "a&b=c")
        programmatic.add("math", "1 + 1 = 2")
        programmatic.add("discount", "50% off")

        then: "programmatic values are stored as-is (never double-decoded) and encoded once on output"
        programmatic.getFirst("filter") == "a&b=c"
        programmatic.getFirst("math") == "1 + 1 = 2"
        programmatic.getFirst("discount") == "50% off"
        programmatic.toEncodedQuery() == rawQuery
        programmatic.toString() == rawQuery
    }
}
