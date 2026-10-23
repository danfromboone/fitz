package dfb.fitz.jdbc;

import dfb.fitz.Validate;
import lombok.Getter;

import java.time.Duration;

/**
 * Immutable configuration for {@link MutableDataSource}, including JDBC connection
 * credentials, connection/validation/idle/max-lifetime timeouts, and HikariCP pool sizing.
 */
@Getter
public class DatabaseConfig {
    /** Default maximum time to wait for a connection from the pool (30 seconds). */
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    /** Default maximum time to wait for a connection validation check (5 seconds). */
    public static final Duration DEFAULT_VALIDATION_TIMEOUT = Duration.ofSeconds(5);
    /** Default maximum time a connection is allowed to sit idle in the pool (10 minutes). */
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofMinutes(10);
    /** Default maximum lifetime of a connection in the pool (30 minutes). */
    public static final Duration DEFAULT_MAX_LIFETIME = Duration.ofMinutes(30);
    /** Default maximum number of connections in the pool (10). */
    public static final int DEFAULT_MAXIMUM_POOL_SIZE = 10;
    /** Default minimum number of idle connections maintained in the pool (10). */
    public static final int DEFAULT_MINIMUM_IDLE = 10;

    private final String url;
    private final String userName;
    private final String password;
    private final String driverClassName;
    private final Duration connectionTimeout;
    private final Duration validationTimeout;
    private final Duration idleTimeout;
    private final Duration maxLifetime;
    private final int maximumPoolSize;
    private final int minimumIdle;

    /**
     * Creates a {@code DatabaseConfig} with the given JDBC URL, username, and password,
     * and default HikariCP timeout and pool settings.
     *
     * @param url      the JDBC URL (must not be {@code null} or blank; trimmed)
     * @param userName the database username (must not be {@code null} or blank; trimmed)
     * @param password the database password (must not be {@code null} or blank; trimmed)
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if any argument is blank
     */
    public DatabaseConfig(String url, String userName, String password) {
        this(
                url,
                userName,
                password,
                null,
                DEFAULT_CONNECTION_TIMEOUT,
                DEFAULT_VALIDATION_TIMEOUT,
                DEFAULT_IDLE_TIMEOUT,
                DEFAULT_MAX_LIFETIME,
                DEFAULT_MAXIMUM_POOL_SIZE,
                DEFAULT_MINIMUM_IDLE
        );
    }

    /**
     * Creates a {@code DatabaseConfig} with the given JDBC URL, username, password, and driver class name,
     * and default HikariCP timeout and pool settings.
     *
     * @param url             the JDBC URL (must not be {@code null} or blank; trimmed)
     * @param userName        the database username (must not be {@code null} or blank; trimmed)
     * @param password        the database password (must not be {@code null} or blank; trimmed)
     * @param driverClassName optional explicit driver class name (or {@code null} to auto-detect from JDBC URL)
     * @throws NullPointerException     if any required argument is {@code null}
     * @throws IllegalArgumentException if any argument is blank
     */
    public DatabaseConfig(String url, String userName, String password, String driverClassName) {
        this(
                url,
                userName,
                password,
                driverClassName,
                DEFAULT_CONNECTION_TIMEOUT,
                DEFAULT_VALIDATION_TIMEOUT,
                DEFAULT_IDLE_TIMEOUT,
                DEFAULT_MAX_LIFETIME,
                DEFAULT_MAXIMUM_POOL_SIZE,
                DEFAULT_MINIMUM_IDLE
        );
    }

    /**
     * Creates a {@code DatabaseConfig} with explicit JDBC credentials, timeouts, and pool sizes.
     *
     * @param url               the JDBC URL (must not be {@code null} or blank; trimmed)
     * @param userName          the database username (must not be {@code null} or blank; trimmed)
     * @param password          the database password (must not be {@code null} or blank; trimmed)
     * @param connectionTimeout maximum wait time for a connection from the pool (must be positive and non-null)
     * @param validationTimeout maximum wait time for connection validation (must be positive and non-null)
     * @param idleTimeout       maximum time a connection may remain idle in the pool (must be positive and non-null)
     * @param maxLifetime       maximum total lifetime of a connection in the pool (must be positive and non-null)
     * @param maximumPoolSize   maximum number of connections in the pool (must be {@code > 0})
     * @param minimumIdle       minimum number of idle connections in the pool (must be in {@code 0..maximumPoolSize})
     * @throws NullPointerException     if any reference argument is {@code null}
     * @throws IllegalArgumentException if any argument violates its constraints
     */
    public DatabaseConfig(
            String url,
            String userName,
            String password,
            Duration connectionTimeout,
            Duration validationTimeout,
            Duration idleTimeout,
            Duration maxLifetime,
            int maximumPoolSize,
            int minimumIdle
    ) {
        this(
                url,
                userName,
                password,
                null,
                connectionTimeout,
                validationTimeout,
                idleTimeout,
                maxLifetime,
                maximumPoolSize,
                minimumIdle
        );
    }

    /**
     * Creates a {@code DatabaseConfig} with explicit JDBC credentials, driver class name, timeouts, and pool sizes.
     *
     * @param url               the JDBC URL (must not be {@code null} or blank; trimmed)
     * @param userName          the database username (must not be {@code null} or blank; trimmed)
     * @param password          the database password (must not be {@code null} or blank; trimmed)
     * @param driverClassName   optional explicit driver class name (or {@code null} to auto-detect from JDBC URL)
     * @param connectionTimeout maximum wait time for a connection from the pool (must be positive and non-null)
     * @param validationTimeout maximum wait time for connection validation (must be positive and non-null)
     * @param idleTimeout       maximum time a connection may remain idle in the pool (must be positive and non-null)
     * @param maxLifetime       maximum total lifetime of a connection in the pool (must be positive and non-null)
     * @param maximumPoolSize   maximum number of connections in the pool (must be {@code > 0})
     * @param minimumIdle       minimum number of idle connections in the pool (must be in {@code 0..maximumPoolSize})
     * @throws NullPointerException     if any reference argument is {@code null}
     * @throws IllegalArgumentException if any argument violates its constraints
     */
    public DatabaseConfig(
            String url,
            String userName,
            String password,
            String driverClassName,
            Duration connectionTimeout,
            Duration validationTimeout,
            Duration idleTimeout,
            Duration maxLifetime,
            int maximumPoolSize,
            int minimumIdle
    ) {
        this.url = Validate.trimmedNonBlank(url, "url");
        this.userName = Validate.trimmedNonBlank(userName, "userName");
        this.password = Validate.trimmedNonBlank(password, "password");
        this.driverClassName = (driverClassName != null && !driverClassName.isBlank())
                ? driverClassName.trim()
                : autoDetectDriverClassName(this.url);
        this.connectionTimeout = Validate.positive(connectionTimeout, "connectionTimeout");
        this.validationTimeout = Validate.positive(validationTimeout, "validationTimeout");
        this.idleTimeout = Validate.positive(idleTimeout, "idleTimeout");
        this.maxLifetime = Validate.positive(maxLifetime, "maxLifetime");
        this.maximumPoolSize = Validate.positive(maximumPoolSize, "maximumPoolSize");
        this.minimumIdle = Validate.inRange(minimumIdle, 0, maximumPoolSize, "minimumIdle");
    }

    /**
     * Attempts to auto-detect a well-known JDBC driver class name based on the JDBC URL scheme prefix.
     *
     * @param url the JDBC URL
     * @return the fully qualified driver class name, or {@code null} if unknown
     */
    public static String autoDetectDriverClassName(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim().toLowerCase();
        if (trimmed.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        } else if (trimmed.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        } else if (trimmed.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else if (trimmed.startsWith("jdbc:mariadb:")) {
            return "org.mariadb.jdbc.Driver";
        } else if (trimmed.startsWith("jdbc:oracle:")) {
            return "oracle.jdbc.OracleDriver";
        } else if (trimmed.startsWith("jdbc:sqlserver:")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        } else if (trimmed.startsWith("jdbc:sqlite:")) {
            return "org.sqlite.JDBC";
        } else if (trimmed.startsWith("jdbc:derby:")) {
            return "org.apache.derby.jdbc.EmbeddedDriver";
        }
        return null;
    }

    /**
     * Returns a new {@link Builder} initialized with default timeout and pool settings.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link DatabaseConfig}, initialized with default timeout and pool values.
     */
    public static class Builder {
        private String url;
        private String userName;
        private String password;
        private String driverClassName;
        private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private Duration validationTimeout = DEFAULT_VALIDATION_TIMEOUT;
        private Duration idleTimeout = DEFAULT_IDLE_TIMEOUT;
        private Duration maxLifetime = DEFAULT_MAX_LIFETIME;
        private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;
        private Integer minimumIdle;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder driverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
            return this;
        }

        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder validationTimeout(Duration validationTimeout) {
            this.validationTimeout = validationTimeout;
            return this;
        }

        public Builder idleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        public Builder maxLifetime(Duration maxLifetime) {
            this.maxLifetime = maxLifetime;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder minimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        public DatabaseConfig build() {
            int resolvedMinIdle = (minimumIdle != null)
                    ? minimumIdle
                    : Math.min(DEFAULT_MINIMUM_IDLE, Math.max(1, maximumPoolSize));
            return new DatabaseConfig(
                    url,
                    userName,
                    password,
                    driverClassName,
                    connectionTimeout,
                    validationTimeout,
                    idleTimeout,
                    maxLifetime,
                    maximumPoolSize,
                    resolvedMinIdle
            );
        }
    }
}
