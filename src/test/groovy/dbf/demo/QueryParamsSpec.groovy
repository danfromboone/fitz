package dbf.demo

import dbf.di.QueryParams
import spock.lang.Specification

class QueryParamsSpec extends Specification {

    def "parses single and multiple values from query string"() {
        when:
        def queryParams = new QueryParams("name=Alice&tag=java&tag=groovy")

        then:
        !queryParams.isEmpty()
        queryParams.keys == ["name", "tag"] as Set
        queryParams.getFirst("name") == "Alice"
        queryParams.getValues("name") == ["Alice"]
        queryParams.getFirst("tag") == "java"
        queryParams.getValues("tag") == ["java", "groovy"]
        queryParams.asMap() == [name: ["Alice"], tag: ["java", "groovy"]]
    }

    def "handles keys without values as empty lists and null first value"() {
        when:
        def queryParams = new QueryParams("flag&empty=&key=val")

        then:
        queryParams.containsKey("flag")
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
        def queryParams = new QueryParams(uri)

        then:
        queryParams.getFirst("greeting") == "Hello World!"
        queryParams.getFirst("symbol") == "✓"
        queryParams.containsKey("noVal")
        queryParams.getValues("noVal") == []
    }

    def "returns empty collection views for null or empty query"() {
        expect:
        new QueryParams((String) null).isEmpty()
        new QueryParams("").isEmpty()
        new QueryParams((URI) null).isEmpty()
        new QueryParams(URI.create("https://example.com/no-query")).isEmpty()

        and:
        def emptyParams = new QueryParams("")
        emptyParams.asMap().isEmpty()
        emptyParams.keys.isEmpty()
        emptyParams.getValues("missing") == []
        emptyParams.getFirst("missing") == null
        !emptyParams.containsKey("missing")
    }

    def "returned map and value lists are unmodifiable"() {
        given:
        def queryParams = new QueryParams("a=1")

        when:
        queryParams.asMap().put("b", ["2"])

        then:
        thrown(UnsupportedOperationException)

        when:
        queryParams.getValues("a").add("2")

        then:
        thrown(UnsupportedOperationException)
    }
}
