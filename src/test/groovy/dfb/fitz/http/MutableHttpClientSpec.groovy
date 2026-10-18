package dfb.fitz.http

import com.sun.net.httpserver.HttpServer
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Duration

class MutableHttpClientSpec extends Specification {

    HttpServer server
    String baseUrl

    def setup() {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/echo") { exchange ->
            String method = exchange.requestMethod
            String query = exchange.requestURI.rawQuery ?: ""
            String reqHeader = exchange.requestHeaders.getFirst("X-Custom-In") ?: ""
            byte[] reqBytes = exchange.requestBody.readAllBytes()
            String reqBody = new String(reqBytes, StandardCharsets.UTF_8)

            exchange.responseHeaders.add("X-Echo-Method", method)
            exchange.responseHeaders.add("X-Echo-Query", query)
            exchange.responseHeaders.add("X-Echo-Header", reqHeader)
            exchange.responseHeaders.add("Content-Type", "text/plain; charset=UTF-8")

            byte[] respBytes = ("body:" + reqBody).getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, respBytes.length)
            exchange.responseBody.withCloseable { it.write(respBytes) }
        }
        server.createContext("/slow") { exchange ->
            Thread.sleep(300)
            byte[] respBytes = "done".getBytes(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, respBytes.length)
            exchange.responseBody.withCloseable { it.write(respBytes) }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    def cleanup() {
        if (server != null) {
            server.stop(0)
        }
    }

    def "HttpClientConfig validates arguments and builder applies defaults"() {
        when:
        def defaultConfig = new HttpClientConfig()

        then:
        defaultConfig.connectTimeout == Duration.ofSeconds(5)
        defaultConfig.responseTimeout == Duration.ofSeconds(30)
        defaultConfig.connectionRequestTimeout == Duration.ofSeconds(5)
        defaultConfig.timeToLive == Duration.ofMinutes(5)
        defaultConfig.idleTimeout == Duration.ofSeconds(30)
        defaultConfig.maxTotalConnections == 100
        defaultConfig.maxConnectionsPerRoute == 20

        when:
        def customConfig = HttpClientConfig.builder()
                .connectTimeout(Duration.ofSeconds(2))
                .responseTimeout(Duration.ofSeconds(10))
                .connectionRequestTimeout(Duration.ofSeconds(3))
                .timeToLive(Duration.ofMinutes(1))
                .idleTimeout(Duration.ofSeconds(15))
                .maxTotalConnections(50)
                .maxConnectionsPerRoute(10)
                .build()

        then:
        customConfig.connectTimeout == Duration.ofSeconds(2)
        customConfig.responseTimeout == Duration.ofSeconds(10)
        customConfig.connectionRequestTimeout == Duration.ofSeconds(3)
        customConfig.timeToLive == Duration.ofMinutes(1)
        customConfig.idleTimeout == Duration.ofSeconds(15)
        customConfig.maxTotalConnections == 50
        customConfig.maxConnectionsPerRoute == 10

        when:
        HttpClientConfig.builder().maxTotalConnections(0).build()

        then:
        thrown(IllegalArgumentException)

        when:
        HttpClientConfig.builder().connectTimeout(Duration.ZERO).build()

        then:
        thrown(IllegalArgumentException)

        when:
        HttpClientConfig.builder().responseTimeout(null).build()

        then:
        thrown(NullPointerException)
    }

    def "MutableHttpClient executes GET, POST, PUT, and DELETE requests with headers and bodies"() {
        given:
        def client = new MutableHttpClient()

        when: "GET with query params and headers"
        def headers = new HttpHeaders()
        headers.set("X-Custom-In", "hello-header")
        headers.set("X-Valueless", null)
        HttpResponse getResponse = client.get(new HttpUri("${baseUrl}/echo?foo=bar"), headers)

        then:
        getResponse.statusCode == HttpStatus.OK
        getResponse.headers.getFirst("X-Echo-Method") == "GET"
        getResponse.headers.getFirst("X-Echo-Query") == "foo=bar"
        getResponse.headers.getFirst("X-Echo-Header") == "hello-header"
        getResponse.body.asString() == "body:"

        when: "POST with string payload"
        HttpResponse postResponse = client.post(new HttpUri("${baseUrl}/echo"), "post-payload")

        then:
        postResponse.statusCode == HttpStatus.OK
        postResponse.headers.getFirst("X-Echo-Method") == "POST"
        postResponse.body.asString() == "body:post-payload"

        when: "PUT with HttpRequestBody payload"
        HttpResponse putResponse = client.put(new HttpUri("${baseUrl}/echo"), new HttpRequestBody("put-payload"))

        then:
        putResponse.statusCode == HttpStatus.OK
        putResponse.headers.getFirst("X-Echo-Method") == "PUT"
        putResponse.body.asString() == "body:put-payload"

        when: "DELETE request"
        HttpResponse deleteResponse = client.delete(new HttpUri("${baseUrl}/echo"))

        then:
        deleteResponse.statusCode == HttpStatus.OK
        deleteResponse.headers.getFirst("X-Echo-Method") == "DELETE"

        cleanup:
        client.close()
    }

    def "MutableHttpClient supports live reconfiguration of timeouts and pool settings via setConfig"() {
        given: "client initially configured with a tight 50ms response timeout"
        def tightConfig = HttpClientConfig.builder()
                .responseTimeout(Duration.ofMillis(50))
                .build()
        def client = new MutableHttpClient(tightConfig)
        def slowUri = new HttpUri("${baseUrl}/slow")

        when: "calling a slow endpoint (300ms) with a 50ms response timeout"
        client.get(slowUri)

        then: "request times out"
        thrown(UncheckedIOException)

        when: "live-reconfiguring the client to a 2s response timeout"
        def relaxedConfig = HttpClientConfig.builder()
                .responseTimeout(Duration.ofSeconds(2))
                .maxTotalConnections(40)
                .maxConnectionsPerRoute(10)
                .build()
        client.setConfig(relaxedConfig)
        HttpResponse response = client.get(slowUri)

        then: "subsequent request succeeds using updated config"
        client.config.is(relaxedConfig)
        response.statusCode == HttpStatus.OK
        response.body.asString() == "done"

        cleanup:
        client.close()
    }

    def "MutableHttpClient resolves relative request URIs against HttpClientConfig baseUri"() {
        given:
        def config = HttpClientConfig.builder()
                .baseUri(new HttpUri(baseUrl))
                .build()
        def client = new MutableHttpClient(config)

        when: "executing a relative path with leading slash"
        HttpResponse response1 = client.get(new HttpUri("/echo?foo=1"))

        and: "executing a relative path without leading slash"
        HttpResponse response2 = client.post(new HttpUri("echo"), "hello-base")

        then:
        config.baseUri.rawUri == baseUrl
        response1.statusCode == HttpStatus.OK
        response1.headers.getFirst("X-Echo-Query") == "foo=1"
        response2.statusCode == HttpStatus.OK
        response2.body.asString() == "body:hello-base"

        cleanup:
        client.close()
    }
}
