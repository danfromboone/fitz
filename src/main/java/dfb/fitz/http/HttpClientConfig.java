package dfb.fitz.http;

import dfb.fitz.Validate;
import lombok.Getter;

import java.time.Duration;

/**
 * Immutable configuration for {@link MutableHttpClient}, including an optional {@code baseUri},
 * connection timeouts, response timeouts, connection-request timeouts, connection pool sizes,
 * and TTL settings.
 */
@Getter
public class HttpClientConfig {
    /** Default connection establishment timeout (5 seconds). */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    /** Default socket/response read timeout (30 seconds). */
    public static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(30);
    /** Default timeout when requesting a connection from the pool (5 seconds). */
    public static final Duration DEFAULT_CONNECTION_REQUEST_TIMEOUT = Duration.ofSeconds(5);
    /** Default maximum lifetime (TTL) for pooled connections (5 minutes). */
    public static final Duration DEFAULT_TIME_TO_LIVE = Duration.ofMinutes(5);
    /** Default maximum idle time before pooled connections are eligible for eviction (30 seconds). */
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofSeconds(30);
    /** Default maximum total connections across all routes (100). */
    public static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 100;
    /** Default maximum connections per route (20). */
    public static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 20;

    private final HttpUri baseUri;
    private final Duration connectTimeout;
    private final Duration responseTimeout;
    private final Duration connectionRequestTimeout;
    private final Duration timeToLive;
    private final Duration idleTimeout;
    private final int maxTotalConnections;
    private final int maxConnectionsPerRoute;

    /**
     * Creates an {@code HttpClientConfig} with default timeout and pool settings and no {@code baseUri}.
     */
    public HttpClientConfig() {
        this(
                null,
                DEFAULT_CONNECT_TIMEOUT,
                DEFAULT_RESPONSE_TIMEOUT,
                DEFAULT_CONNECTION_REQUEST_TIMEOUT,
                DEFAULT_TIME_TO_LIVE,
                DEFAULT_IDLE_TIMEOUT,
                DEFAULT_MAX_TOTAL_CONNECTIONS,
                DEFAULT_MAX_CONNECTIONS_PER_ROUTE
        );
    }

    /**
     * Creates an {@code HttpClientConfig} with explicit timeout and pool settings and no {@code baseUri}.
     *
     * @param connectTimeout           the TCP connection establishment timeout (must be positive and non-null)
     * @param responseTimeout          the socket/response read timeout (must be positive and non-null)
     * @param connectionRequestTimeout the timeout when borrowing a connection from the pool (must be positive and non-null)
     * @param timeToLive               the maximum total lifetime (TTL) of a pooled connection (must be positive and non-null)
     * @param idleTimeout              the maximum time a connection may remain idle in the pool (must be positive and non-null)
     * @param maxTotalConnections      the maximum total connections in the pool (must be {@code > 0})
     * @param maxConnectionsPerRoute   the maximum connections per route (must be {@code > 0})
     * @throws NullPointerException     if any {@link Duration} argument is {@code null}
     * @throws IllegalArgumentException if any duration or connection count is zero or negative
     */
    public HttpClientConfig(
            Duration connectTimeout,
            Duration responseTimeout,
            Duration connectionRequestTimeout,
            Duration timeToLive,
            Duration idleTimeout,
            int maxTotalConnections,
            int maxConnectionsPerRoute
    ) {
        this(
                null,
                connectTimeout,
                responseTimeout,
                connectionRequestTimeout,
                timeToLive,
                idleTimeout,
                maxTotalConnections,
                maxConnectionsPerRoute
        );
    }

    /**
     * Creates an {@code HttpClientConfig} with an optional {@code baseUri} and explicit timeout and pool settings.
     *
     * @param baseUri                  optional base URI used to resolve relative request URIs; may be {@code null}
     * @param connectTimeout           the TCP connection establishment timeout (must be positive and non-null)
     * @param responseTimeout          the socket/response read timeout (must be positive and non-null)
     * @param connectionRequestTimeout the timeout when borrowing a connection from the pool (must be positive and non-null)
     * @param timeToLive               the maximum total lifetime (TTL) of a pooled connection (must be positive and non-null)
     * @param idleTimeout              the maximum time a connection may remain idle in the pool (must be positive and non-null)
     * @param maxTotalConnections      the maximum total connections in the pool (must be {@code > 0})
     * @param maxConnectionsPerRoute   the maximum connections per route (must be {@code > 0})
     * @throws NullPointerException     if any {@link Duration} argument is {@code null}
     * @throws IllegalArgumentException if any duration or connection count is zero or negative
     */
    public HttpClientConfig(
            HttpUri baseUri,
            Duration connectTimeout,
            Duration responseTimeout,
            Duration connectionRequestTimeout,
            Duration timeToLive,
            Duration idleTimeout,
            int maxTotalConnections,
            int maxConnectionsPerRoute
    ) {
        this.baseUri = baseUri;
        this.connectTimeout = Validate.positive(connectTimeout, "connectTimeout");
        this.responseTimeout = Validate.positive(responseTimeout, "responseTimeout");
        this.connectionRequestTimeout = Validate.positive(connectionRequestTimeout, "connectionRequestTimeout");
        this.timeToLive = Validate.positive(timeToLive, "timeToLive");
        this.idleTimeout = Validate.positive(idleTimeout, "idleTimeout");
        this.maxTotalConnections = Validate.positive(maxTotalConnections, "maxTotalConnections");
        this.maxConnectionsPerRoute = Validate.positive(maxConnectionsPerRoute, "maxConnectionsPerRoute");
    }

    /**
     * Returns a new {@link Builder} initialized with default settings.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link HttpClientConfig}, initialized with default values.
     */
    public static class Builder {
        private HttpUri baseUri;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration responseTimeout = DEFAULT_RESPONSE_TIMEOUT;
        private Duration connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
        private Duration timeToLive = DEFAULT_TIME_TO_LIVE;
        private Duration idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTIONS;
        private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTIONS_PER_ROUTE;

        public Builder baseUri(HttpUri baseUri) {
            this.baseUri = baseUri;
            return this;
        }

        public Builder baseUri(String baseUri) {
            this.baseUri = (baseUri != null) ? new HttpUri(baseUri) : null;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder responseTimeout(Duration responseTimeout) {
            this.responseTimeout = responseTimeout;
            return this;
        }

        public Builder connectionRequestTimeout(Duration connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        public Builder timeToLive(Duration timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        public Builder idleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public Builder maxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder maxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            return this;
        }

        public HttpClientConfig build() {
            return new HttpClientConfig(
                    baseUri,
                    connectTimeout,
                    responseTimeout,
                    connectionRequestTimeout,
                    timeToLive,
                    idleTimeout,
                    maxTotalConnections,
                    maxConnectionsPerRoute
            );
        }
    }
}
