package dfb.demo;

import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

// this is a demo class to prove out some concepts.
// it doesn't do anything.
// don't expect it to.
@Getter
@Setter
public class MutableDataSource implements DataSource {

    private volatile DatabaseConfig databaseConfig;

    public MutableDataSource(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void setDatabaseConfig(DatabaseConfig databaseConfig){
        this.databaseConfig = databaseConfig;
    }


    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
