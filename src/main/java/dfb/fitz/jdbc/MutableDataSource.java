package dfb.fitz.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import dfb.fitz.Closeables;
import dfb.fitz.Validate;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * A live-reconfigurable {@link DataSource} that delegates to a {@link HikariDataSource}.
 * <p>
 * The underlying {@link HikariDataSource} is initialized lazily on first connection access.
 * When {@link #setDatabaseConfig(DatabaseConfig)} is called at runtime:
 * <ul>
 *   <li>If the JDBC URL has not changed, credentials, timeouts, and pool sizes are updated
 *       in-place via HikariCP's runtime configuration MXBean, and existing connections are
 *       soft-evicted when credentials change.</li>
 *   <li>If the JDBC URL has changed, the previous {@link HikariDataSource} is closed and replaced.</li>
 * </ul>
 */
public class MutableDataSource implements DataSource, AutoCloseable {

    private volatile DatabaseConfig databaseConfig;
    private volatile HikariDataSource hikariDataSource;

    /**
     * Creates a {@code MutableDataSource} with the given initial {@link DatabaseConfig}.
     *
     * @param databaseConfig the initial database configuration (must not be {@code null})
     * @throws NullPointerException if {@code databaseConfig} is {@code null}
     */
    public MutableDataSource(DatabaseConfig databaseConfig) {
        this.databaseConfig = Validate.nonNull(databaseConfig, "databaseConfig");
    }

    /**
     * Returns the current {@link DatabaseConfig}.
     *
     * @return the active database configuration (never {@code null})
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    /**
     * Atomically updates the active {@link DatabaseConfig} and reconfigures the underlying
     * {@link HikariDataSource} if one has already been initialized.
     *
     * @param newConfig the updated database configuration (must not be {@code null})
     * @throws NullPointerException if {@code newConfig} is {@code null}
     */
    public synchronized void setDatabaseConfig(DatabaseConfig newConfig) {
        Validate.nonNull(newConfig, "databaseConfig");
        DatabaseConfig previousConfig = this.databaseConfig;
        this.databaseConfig = newConfig;

        HikariDataSource currentDelegate = this.hikariDataSource;
        if (currentDelegate == null || currentDelegate.isClosed()) {
            this.hikariDataSource = null;
            return;
        }

        if (previousConfig != null && previousConfig.getUrl().equals(newConfig.getUrl())) {
            applyLiveConfigUpdate(currentDelegate, previousConfig, newConfig);
        } else {
            this.hikariDataSource = null;
            Closeables.closeQuietly(currentDelegate);
        }
    }

    private static void applyLiveConfigUpdate(
            HikariDataSource delegate,
            DatabaseConfig previousConfig,
            DatabaseConfig newConfig
    ) {
        HikariConfigMXBean configMxBean = delegate.getHikariConfigMXBean();
        if (configMxBean != null) {
            configMxBean.setConnectionTimeout(newConfig.getConnectionTimeout().toMillis());
            configMxBean.setValidationTimeout(newConfig.getValidationTimeout().toMillis());
            configMxBean.setIdleTimeout(newConfig.getIdleTimeout().toMillis());
            configMxBean.setMaxLifetime(newConfig.getMaxLifetime().toMillis());
            configMxBean.setMaximumPoolSize(newConfig.getMaximumPoolSize());
            configMxBean.setMinimumIdle(newConfig.getMinimumIdle());
            configMxBean.setUsername(newConfig.getUserName());
            configMxBean.setPassword(newConfig.getPassword());
        } else {
            delegate.setConnectionTimeout(newConfig.getConnectionTimeout().toMillis());
            delegate.setValidationTimeout(newConfig.getValidationTimeout().toMillis());
            delegate.setIdleTimeout(newConfig.getIdleTimeout().toMillis());
            delegate.setMaxLifetime(newConfig.getMaxLifetime().toMillis());
            delegate.setMaximumPoolSize(newConfig.getMaximumPoolSize());
            delegate.setMinimumIdle(newConfig.getMinimumIdle());
            delegate.setUsername(newConfig.getUserName());
            delegate.setPassword(newConfig.getPassword());
        }

        boolean credentialsChanged = !Objects.equals(previousConfig.getUserName(), newConfig.getUserName())
                || !Objects.equals(previousConfig.getPassword(), newConfig.getPassword());
        if (credentialsChanged) {
            HikariPoolMXBean poolMxBean = delegate.getHikariPoolMXBean();
            if (poolMxBean != null) {
                poolMxBean.softEvictConnections();
            }
        }
    }

    /**
     * Returns the lazily initialized underlying {@link HikariDataSource}, creating it if needed.
     *
     * @return the active {@link HikariDataSource}
     */
    public HikariDataSource getHikariDataSource() {
        HikariDataSource delegate = this.hikariDataSource;
        if (delegate == null || delegate.isClosed()) {
            synchronized (this) {
                delegate = this.hikariDataSource;
                if (delegate == null || delegate.isClosed()) {
                    delegate = createHikariDataSource(this.databaseConfig);
                    this.hikariDataSource = delegate;
                }
            }
        }
        return delegate;
    }

    private static HikariDataSource createHikariDataSource(DatabaseConfig cfg) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(cfg.getUrl());
        hikariConfig.setUsername(cfg.getUserName());
        hikariConfig.setPassword(cfg.getPassword());
        hikariConfig.setConnectionTimeout(cfg.getConnectionTimeout().toMillis());
        hikariConfig.setValidationTimeout(cfg.getValidationTimeout().toMillis());
        hikariConfig.setIdleTimeout(cfg.getIdleTimeout().toMillis());
        hikariConfig.setMaxLifetime(cfg.getMaxLifetime().toMillis());
        hikariConfig.setMaximumPoolSize(cfg.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(cfg.getMinimumIdle());
        return new HikariDataSource(hikariConfig);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getHikariDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getHikariDataSource().getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getHikariDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        getHikariDataSource().setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        getHikariDataSource().setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getHikariDataSource().getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getHikariDataSource().getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return getHikariDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || getHikariDataSource().isWrapperFor(iface);
    }

    @Override
    public synchronized void close() {
        HikariDataSource delegate = this.hikariDataSource;
        this.hikariDataSource = null;
        Closeables.closeQuietly(delegate);
    }
}
