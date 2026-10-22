package dfb.fitz.jdbc;

import dfb.fitz.Validate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Lightweight, pure-Java JDBC utility for executing common database operations:
 * reads ({@code query}, {@code queryOne}, {@code queryFirst}),
 * writes ({@code update}, {@code batchUpdate}), and DDL ({@code execute}).
 * <p>
 * Methods accepting a {@link Connection} <strong>never close the connection</strong>,
 * allowing callers or transaction managers to control connection lifecycles.
 * Methods accepting a {@link DataSource} obtain a connection and safely close it
 * upon completion (including on errors) via try-with-resources.
 * Statements and result sets created internally are always closed via try-with-resources.
 * Any underlying {@link SQLException} is wrapped in an unchecked {@link WrappedSqlException}.
 */
public final class Jdbc {

    private Jdbc() {
    }

    // =========================================================================
    // DDL & Arbitrary Execution (execute)
    // =========================================================================

    /**
     * Executes an arbitrary SQL statement (typically DDL such as {@code CREATE}, {@code ALTER},
     * {@code DROP}, or administrative commands).
     *
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL statement to execute
     * @param params     optional query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static boolean execute(Connection connection, String sql, Object... params) throws WrappedSqlException {
        Validate.nonNull(connection, "connection");
        Validate.nonNull(sql, "sql");

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.execute();
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    /**
     * Executes an arbitrary SQL statement with a list of query parameters.
     *
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL statement to execute
     * @param params     the list of query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static boolean execute(Connection connection, String sql, List<?> params) throws WrappedSqlException {
        return execute(connection, sql, params != null ? params.toArray() : (Object[]) null);
    }

    /**
     * Obtains a connection from {@code dataSource}, executes an arbitrary SQL statement, and safely closes the connection.
     *
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL statement to execute
     * @param params     optional query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static boolean execute(DataSource dataSource, String sql, Object... params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> execute(conn, sql, params));
    }

    /**
     * Obtains a connection from {@code dataSource}, executes an arbitrary SQL statement with a list of query parameters, and safely closes the connection.
     *
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL statement to execute
     * @param params     the list of query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static boolean execute(DataSource dataSource, String sql, List<?> params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> execute(conn, sql, params));
    }

    // =========================================================================
    // Writes & Updates (update & batchUpdate)
    // =========================================================================

    /**
     * Executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement.
     *
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL statement to execute
     * @param params     optional query parameters to bind
     * @return the number of rows affected
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int update(Connection connection, String sql, Object... params) throws WrappedSqlException {
        Validate.nonNull(connection, "connection");
        Validate.nonNull(sql, "sql");

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    /**
     * Executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement with a list of parameters.
     *
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL statement to execute
     * @param params     the list of query parameters to bind
     * @return the number of rows affected
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int update(Connection connection, String sql, List<?> params) throws WrappedSqlException {
        return update(connection, sql, params != null ? params.toArray() : (Object[]) null);
    }

    /**
     * Obtains a connection from {@code dataSource}, executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement, and safely closes the connection.
     *
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL statement to execute
     * @param params     optional query parameters to bind
     * @return the number of rows affected
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int update(DataSource dataSource, String sql, Object... params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> update(conn, sql, params));
    }

    /**
     * Obtains a connection from {@code dataSource}, executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement with a list of parameters, and safely closes the connection.
     *
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL statement to execute
     * @param params     the list of query parameters to bind
     * @return the number of rows affected
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int update(DataSource dataSource, String sql, List<?> params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> update(conn, sql, params));
    }

    /**
     * Executes a parameterized batch update against a single prepared statement.
     * Each element of {@code batchParams} can be an {@code Object[]}, a {@link List}, or an {@link Iterable}
     * representing the parameters for one batch row.
     *
     * @param connection  the JDBC connection (not closed by this method)
     * @param sql         the parameterized SQL statement to execute
     * @param batchParams an iterable of parameter arrays or collections (one entry per batch row)
     * @return an array of update counts containing one element for each command in the batch
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int[] batchUpdate(Connection connection, String sql, Iterable<?> batchParams) throws WrappedSqlException {
        Validate.nonNull(connection, "connection");
        Validate.nonNull(sql, "sql");
        Validate.nonNull(batchParams, "batchParams");

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Object rowParams : batchParams) {
                bindRowParameters(stmt, rowParams);
                stmt.addBatch();
            }
            return stmt.executeBatch();
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    /**
     * Executes a batch of static SQL statements (e.g. DDL scripts or non-parameterized updates).
     *
     * @param connection    the JDBC connection (not closed by this method)
     * @param sqlStatements the SQL statements to execute
     * @return an array of update counts containing one element for each command in the batch
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int[] batchUpdate(Connection connection, Iterable<String> sqlStatements) throws WrappedSqlException {
        Validate.nonNull(connection, "connection");
        Validate.nonNull(sqlStatements, "sqlStatements");

        try (Statement stmt = connection.createStatement()) {
            for (String sql : sqlStatements) {
                if (sql != null && !sql.isBlank()) {
                    stmt.addBatch(sql);
                }
            }
            return stmt.executeBatch();
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a parameterized batch update, and safely closes the connection.
     *
     * @param dataSource  the DataSource to obtain a connection from (safely closed afterward)
     * @param sql         the parameterized SQL statement to execute
     * @param batchParams an iterable of parameter arrays or collections (one entry per batch row)
     * @return an array of update counts containing one element for each command in the batch
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int[] batchUpdate(DataSource dataSource, String sql, Iterable<?> batchParams) throws WrappedSqlException {
        return withConnection(dataSource, conn -> batchUpdate(conn, sql, batchParams));
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a batch of static SQL statements, and safely closes the connection.
     *
     * @param dataSource    the DataSource to obtain a connection from (safely closed afterward)
     * @param sqlStatements the SQL statements to execute
     * @return an array of update counts containing one element for each command in the batch
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int[] batchUpdate(DataSource dataSource, Iterable<String> sqlStatements) throws WrappedSqlException {
        return withConnection(dataSource, conn -> batchUpdate(conn, sqlStatements));
    }

    // =========================================================================
    // Reads & Queries (query, queryOne, queryFirst)
    // =========================================================================

    /**
     * Executes a query and maps each row of the resulting {@link ResultSet} using the provided {@link RowMapper}.
     *
     * @param <T>        the mapped result type
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply to each row
     * @param params     optional query parameters to bind
     * @return an unmodifiable list of mapped results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> List<T> query(
            Connection connection,
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        Validate.nonNull(connection, "connection");
        Validate.nonNull(sql, "sql");
        Validate.nonNull(mapper, "mapper");

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                return Collections.unmodifiableList(results);
            }
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    /**
     * Executes a query with a list of parameters and maps each row of the resulting {@link ResultSet}.
     *
     * @param <T>        the mapped result type
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply to each row
     * @param params     the list of query parameters to bind
     * @return an unmodifiable list of mapped results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> List<T> query(
            Connection connection,
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return query(connection, sql, mapper, params != null ? params.toArray() : (Object[]) null);
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a query, maps rows using {@link RowMapper}, and safely closes the connection.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply to each row
     * @param params     optional query parameters to bind
     * @return an unmodifiable list of mapped results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> List<T> query(
            DataSource dataSource,
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        return withConnection(dataSource, conn -> query(conn, sql, mapper, params));
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a query with a list of parameters, maps rows, and safely closes the connection.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply to each row
     * @param params     the list of query parameters to bind
     * @return an unmodifiable list of mapped results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> List<T> query(
            DataSource dataSource,
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return withConnection(dataSource, conn -> query(conn, sql, mapper, params));
    }

    /**
     * Executes a query expecting at most one row, and maps that row using the given {@link RowMapper}.
     *
     * @param <T>        the mapped result type
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     optional query parameters to bind
     * @return an {@link Optional} containing the mapped object if exactly one row was found, or {@link Optional#empty()}
     * @throws IllegalStateException if more than one row is returned by the query
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryOne(
            Connection connection,
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        Validate.nonNull(connection, "connection");
        Validate.nonNull(sql, "sql");
        Validate.nonNull(mapper, "mapper");

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                T result = mapper.map(rs);
                if (rs.next()) {
                    throw new IllegalStateException("Expected at most one row, but query returned multiple rows: " + sql);
                }
                return Optional.ofNullable(result);
            }
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    /**
     * Executes a query expecting at most one row with a list of parameters.
     *
     * @param <T>        the mapped result type
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     the list of query parameters to bind
     * @return an {@link Optional} containing the mapped object if exactly one row was found, or {@link Optional#empty()}
     * @throws IllegalStateException if more than one row is returned by the query
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryOne(
            Connection connection,
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return queryOne(connection, sql, mapper, params != null ? params.toArray() : (Object[]) null);
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a query expecting at most one row, and safely closes the connection.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     optional query parameters to bind
     * @return an {@link Optional} containing the mapped object if exactly one row was found, or {@link Optional#empty()}
     * @throws IllegalStateException if more than one row is returned by the query
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryOne(
            DataSource dataSource,
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        return withConnection(dataSource, conn -> queryOne(conn, sql, mapper, params));
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a query expecting at most one row with a list of parameters, and safely closes the connection.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     the list of query parameters to bind
     * @return an {@link Optional} containing the mapped object if exactly one row was found, or {@link Optional#empty()}
     * @throws IllegalStateException if more than one row is returned by the query
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryOne(
            DataSource dataSource,
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return withConnection(dataSource, conn -> queryOne(conn, sql, mapper, params));
    }

    /**
     * Executes a query and returns the first row mapped by the given {@link RowMapper}, ignoring any subsequent rows.
     *
     * @param <T>        the mapped result type
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     optional query parameters to bind
     * @return an {@link Optional} containing the first mapped object, or {@link Optional#empty()} if no rows returned
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> Optional<T> queryFirst(
            Connection connection,
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        Validate.nonNull(connection, "connection");
        Validate.nonNull(sql, "sql");
        Validate.nonNull(mapper, "mapper");

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(mapper.map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    /**
     * Executes a query with a list of parameters and returns the first row mapped by the given {@link RowMapper}.
     *
     * @param <T>        the mapped result type
     * @param connection the JDBC connection (not closed by this method)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     the list of query parameters to bind
     * @return an {@link Optional} containing the first mapped object, or {@link Optional#empty()} if no rows returned
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> Optional<T> queryFirst(
            Connection connection,
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return queryFirst(connection, sql, mapper, params != null ? params.toArray() : (Object[]) null);
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a query returning the first row, and safely closes the connection.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     optional query parameters to bind
     * @return an {@link Optional} containing the first mapped object, or {@link Optional#empty()} if no rows returned
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> Optional<T> queryFirst(
            DataSource dataSource,
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        return withConnection(dataSource, conn -> queryFirst(conn, sql, mapper, params));
    }

    /**
     * Obtains a connection from {@code dataSource}, executes a query with a list of parameters returning the first row, and safely closes the connection.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (safely closed afterward)
     * @param sql        the SQL query to execute
     * @param mapper     the row mapper to apply
     * @param params     the list of query parameters to bind
     * @return an {@link Optional} containing the first mapped object, or {@link Optional#empty()} if no rows returned
     * @throws WrappedSqlException if a database access error occurs
     */
    public static <T> Optional<T> queryFirst(
            DataSource dataSource,
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return withConnection(dataSource, conn -> queryFirst(conn, sql, mapper, params));
    }

    // =========================================================================
    // DataSource & Connection Lifecycle Helper
    // =========================================================================

    @FunctionalInterface
    private interface ConnectionAction<R> {
        R run(Connection connection) throws WrappedSqlException;
    }

    private static <R> R withConnection(DataSource dataSource, ConnectionAction<R> action) throws WrappedSqlException {
        Validate.nonNull(dataSource, "dataSource");
        try (Connection connection = dataSource.getConnection()) {
            return action.run(connection);
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }
    }

    // =========================================================================
    // Parameter Binding Helpers
    // =========================================================================

    private static void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params == null || params.length == 0) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            int paramIndex = i + 1;
            if (param == null) {
                stmt.setNull(paramIndex, Types.NULL);
            } else {
                stmt.setObject(paramIndex, param);
            }
        }
    }

    private static void bindRowParameters(PreparedStatement stmt, Object rowParams) throws SQLException {
        if (rowParams == null) {
            return;
        }
        if (rowParams instanceof Object[] array) {
            setParameters(stmt, array);
        } else if (rowParams instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                Object param = list.get(i);
                int paramIndex = i + 1;
                if (param == null) {
                    stmt.setNull(paramIndex, Types.NULL);
                } else {
                    stmt.setObject(paramIndex, param);
                }
            }
        } else if (rowParams instanceof Iterable<?> iterable) {
            int i = 1;
            for (Object param : iterable) {
                if (param == null) {
                    stmt.setNull(i++, Types.NULL);
                } else {
                    stmt.setObject(i++, param);
                }
            }
        } else {
            stmt.setObject(1, rowParams);
        }
    }
}
