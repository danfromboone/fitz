package dfb.fitz.http;

import dfb.fitz.Closeables;
import dfb.fitz.Validate;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * {@link HttpClient} implementation backed by Apache HttpClient 5 and a
 * {@link PoolingHttpClientConnectionManager} that supports live runtime reconfiguration
 * via {@link #setConfig(HttpClientConfig)}.
 */
public class ApacheHttpClient implements HttpClient {
    private static final TimeValue VALIDATE_AFTER_INACTIVITY = TimeValue.ofSeconds(2);

    private final PoolingHttpClientConnectionManager connectionManager;
    private final CloseableHttpClient client;
    private volatile HttpClientConfig config;

    /**
     * Creates an {@code ApacheHttpClient} with default {@link HttpClientConfig} settings.
     */
    public ApacheHttpClient() {
        this(new HttpClientConfig());
    }

    /**
     * Creates an {@code ApacheHttpClient} initialized with the given {@link HttpClientConfig}.
     *
     * @param config the initial client configuration (must not be {@code null})
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public ApacheHttpClient(HttpClientConfig config) {
        Validate.nonNull(config, "config");
        this.connectionManager = new PoolingHttpClientConnectionManager();
        applyConfigToConnectionManager(config);
        this.config = config;
        this.client = HttpClients.custom()
                .setConnectionManager(this.connectionManager)
                .disableAutomaticRetries()
                .build();
    }

    /**
     * Returns the current {@link HttpClientConfig}.
     *
     * @return the current configuration (never {@code null})
     */
    public HttpClientConfig getConfig() {
        return config;
    }

    /**
     * Atomically updates the active {@link HttpClientConfig} and applies the updated
     * pool sizes, timeouts, and TTL settings to the underlying connection manager in-place,
     * while immediately evicting expired and idle connections.
     *
     * @param newConfig the new configuration (must not be {@code null})
     * @throws NullPointerException if {@code newConfig} is {@code null}
     */
    public void setConfig(HttpClientConfig newConfig) {
        Validate.nonNull(newConfig, "newConfig");
        applyConfigToConnectionManager(newConfig);
        this.config = newConfig;
        this.connectionManager.closeExpired();
        this.connectionManager.closeIdle(TimeValue.of(newConfig.getIdleTimeout()));
    }

    private void applyConfigToConnectionManager(HttpClientConfig cfg) {
        connectionManager.setMaxTotal(cfg.getMaxTotalConnections());
        connectionManager.setDefaultMaxPerRoute(cfg.getMaxConnectionsPerRoute());

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(cfg.getConnectTimeout()))
                .setSocketTimeout(Timeout.of(cfg.getResponseTimeout()))
                .setTimeToLive(TimeValue.of(cfg.getTimeToLive()))
                .setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY)
                .build();
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.of(cfg.getResponseTimeout()))
                .build();
        connectionManager.setDefaultSocketConfig(socketConfig);
    }

    /**
     * Closes expired connections and connections that have been idle longer than
     * the currently configured {@link HttpClientConfig#getIdleTimeout()}.
     */
    public void evictExpiredAndIdleConnections() {
        HttpClientConfig currentConfig = this.config;
        connectionManager.closeExpired();
        connectionManager.closeIdle(TimeValue.of(currentConfig.getIdleTimeout()));
    }

    @Override
    public HttpResponse execute(HttpRequest request) {
        Validate.nonNull(request, "request");
        HttpClientConfig currentConfig = this.config;

        URI targetUri = resolveTargetUri(currentConfig.getBaseUri(), request.getHttpUri());
        HttpUriRequestBase apacheRequest = new HttpUriRequestBase(
                request.getMethod().name(),
                targetUri
        );

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(currentConfig.getConnectionRequestTimeout()))
                .setResponseTimeout(Timeout.of(currentConfig.getResponseTimeout()))
                .build();
        apacheRequest.setConfig(requestConfig);

        copyRequestHeaders(request.getHeaders(), apacheRequest);
        attachRequestBody(request, apacheRequest);

        try {
            return client.execute(apacheRequest, this::toHttpResponse);
        } catch (IOException e) {
            throw new UncheckedIOException("HTTP request failed: " + request.getMethod() + " " + targetUri, e);
        }
    }

    private static URI resolveTargetUri(HttpUri baseUri, HttpUri requestUri) {
        URI rawRequestUri = requestUri.getUri();
        if (rawRequestUri.isAbsolute() || baseUri == null) {
            return rawRequestUri;
        }
        String base = baseUri.getRawUri();
        String rel = requestUri.getRawUri();
        if (rel.startsWith("?")) {
            return URI.create(base + rel);
        }
        if (base.endsWith("/") && rel.startsWith("/")) {
            return URI.create(base + rel.substring(1));
        }
        if (!base.endsWith("/") && !rel.startsWith("/")) {
            return URI.create(base + "/" + rel);
        }
        return URI.create(base + rel);
    }

    private static void copyRequestHeaders(HttpHeaders sourceHeaders, HttpUriRequestBase targetRequest) {
        for (Map.Entry<String, List<String>> entry : sourceHeaders.asMap().entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (values.isEmpty()) {
                targetRequest.addHeader(name, "");
            } else {
                for (String value : values) {
                    targetRequest.addHeader(name, value);
                }
            }
        }
    }

    private static void attachRequestBody(HttpRequest request, HttpUriRequestBase targetRequest) {
        HttpRequestBody body = request.getBody();
        if (body != null && !body.isEmpty()) {
            String contentTypeHeader = request.getHeaders().getFirst("Content-Type");
            ContentType contentType = parseContentType(contentTypeHeader, body.getCharset());
            targetRequest.setEntity(new ByteArrayEntity(body.asBytes(), contentType));
        }
    }

    private static ContentType parseContentType(String headerValue, Charset fallbackCharset) {
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                return ContentType.parse(headerValue);
            } catch (Exception ignored) {
                // Fall back to default binary/text content type
            }
        }
        return ContentType.TEXT_PLAIN.withCharset(fallbackCharset != null ? fallbackCharset : StandardCharsets.UTF_8);
    }

    private HttpResponse toHttpResponse(ClassicHttpResponse classicResponse) throws IOException {
        HttpStatus status = HttpStatus.of(classicResponse.getCode(), classicResponse.getReasonPhrase());

        HttpHeaders responseHeaders = new HttpHeaders();
        for (Header header : classicResponse.getHeaders()) {
            responseHeaders.add(header.getName(), header.getValue());
        }

        HttpEntity entity = classicResponse.getEntity();
        HttpResponseBody responseBody;
        if (entity != null) {
            byte[] bytes = EntityUtils.toByteArray(entity);
            Charset charset = resolveEntityCharset(entity);
            responseBody = (bytes != null && bytes.length > 0)
                    ? new HttpResponseBody(bytes, charset)
                    : HttpResponseBody.empty();
        } else {
            responseBody = HttpResponseBody.empty();
        }

        return new HttpResponse(status, responseBody, responseHeaders);
    }

    private static Charset resolveEntityCharset(HttpEntity entity) {
        try {
            ContentType contentType = ContentType.parse(entity.getContentType());
            if (contentType != null && contentType.getCharset() != null) {
                return contentType.getCharset();
            }
        } catch (Exception ignored) {
            // Fall back to UTF-8
        }
        return StandardCharsets.UTF_8;
    }

    @Override
    public void close() {
        Closeables.closeAllQuietly(client, connectionManager);
    }
}
