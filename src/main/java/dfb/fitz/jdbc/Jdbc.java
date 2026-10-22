package dfb.fitz.jdbc;

import dfb.fitz.Closeables;
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
 * writes ({@code update}, {@code batchUpdate}), DDL ({@code execute}), and scoped transactions.
 * <p>
 * Supported usage modes:
 * <ul>
 *   <li><strong>Connection-based:</strong> Methods accepting a {@link Connection} <em>never</em> close
 *       the connection, allowing callers or transaction managers to control connection lifecycles.</li>
 *   <li><strong>DataSource-based:</strong> Methods accepting a {@link DataSource} automatically participate
 *       in any active scoped transaction for that DataSource; if no transaction is active, they obtain a
 *       connection and safely close it upon completion.</li>
 *   <li><strong>Scoped Ambient:</strong> Within an {@code inTransaction} block, methods without a
 *       Connection or DataSource parameter automatically bind to the active scoped transaction.</li>
 * </ul>
 * Any underlying {@link SQLException} is wrapped in an unchecked {@link WrappedSqlException}.
 * <p>
 * Transactions are strictly bound to the thread that initiated them. Child threads or other threads
 * are prohibited from joining or accessing the transaction and will throw an {@link IllegalStateException}.
 * If another transaction is attempted inside an existing transaction on the same DataSource, it joins
 * the outer transaction.
 */
public final class Jdbc {

    private static final ScopedValue<TransactionContext> CURRENT_TRANSACTION = ScopedValue.newInstance();
    private static final InheritableThreadLocal<TransactionContext> INHERITABLE_TRANSACTION = new InheritableThreadLocal<>();

    private Jdbc() {
    }

    private static TransactionContext getActiveContext() {
        if (CURRENT_TRANSACTION.isBound()) {
            TransactionContext tx = CURRENT_TRANSACTION.get();
            if (tx != null) {
                tx.checkThreadAccess();
                return tx;
            }
        }
        TransactionContext tx = INHERITABLE_TRANSACTION.get();
        if (tx != null) {
            tx.checkThreadAccess();
            return tx;
        }
        return null;
    }

    // =========================================================================
    // Scoped Transactions
    // =========================================================================

    /**
     * Executes the given action within a scoped database transaction on the specified {@link DataSource}.
     * If an error occurs, the transaction is automatically rolled back; otherwise, it is committed.
     * <p>
     * If another transaction is attempted inside this transaction on the same DataSource, it joins
     * the outer transaction. Child threads or other threads must not join this transaction.
     *
     * @param dataSource the DataSource on which to run the transaction
     * @param action     the transactional action to execute
     * @throws IllegalStateException if a child thread or other thread attempts to join
     * @throws WrappedSqlException   if a database error occurs during the transaction
     */
    public static void inTransaction(DataSource dataSource, TransactionAction action) throws WrappedSqlException {
        Validate.nonNull(action, "action");
        inTransaction(dataSource, () -> {
            action.execute();
            return null;
        });
    }

    /**
     * Executes the given callback within a scoped database transaction on the specified {@link DataSource}
     * and returns the result. If an error occurs, the transaction is automatically rolled back; otherwise,
     * it is committed.
     * <p>
     * If another transaction is attempted inside this transaction on the same DataSource, it joins
     * the outer transaction. Child threads or other threads must not join this transaction.
     *
     * @param <T>        the return type
     * @param dataSource the DataSource on which to run the transaction
     * @param callback   the transactional callback to execute
     * @return the result returned by the callback
     * @throws IllegalStateException if a child thread or other thread attempts to join
     * @throws WrappedSqlException   if a database error occurs during the transaction
     */
    public static <T> T inTransaction(DataSource dataSource, TransactionCallback<T> callback) throws WrappedSqlException {
        Validate.nonNull(dataSource, "dataSource");
        Validate.nonNull(callback, "callback");

        // Check if a transaction is already active (throws IllegalStateException if accessed by child thread)
        TransactionContext current = getActiveContext();
        if (current != null && current.matches(dataSource)) {
            // Join outer transaction
            try {
                return callback.execute();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Connection connection;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new WrappedSqlException(e);
        }

        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            Closeables.closeQuietly(connection);
            throw new WrappedSqlException(e);
        }

        TransactionContext context = new TransactionContext(dataSource, connection);
        TransactionContext previousTl = INHERITABLE_TRANSACTION.get();
        INHERITABLE_TRANSACTION.set(context);

        try {
            T result = ScopedValue.where(CURRENT_TRANSACTION, context).call(callback::execute);
            if (context.isRollbackOnly()) {
                connection.rollback();
                throw new WrappedSqlException(new SQLException("Transaction completed but was marked as rollback-only"));
            }
            connection.commit();
            return result;
        } catch (Throwable t) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                t.addSuppressed(rollbackEx);
            }
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(t);
        } finally {
            if (previousTl != null) {
                INHERITABLE_TRANSACTION.set(previousTl);
            } else {
                INHERITABLE_TRANSACTION.remove();
            }
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException ignored) {
            }
            Closeables.closeQuietly(connection);
        }
    }

    /**
     * Alias for {@link #inTransaction(DataSource, TransactionAction)}.
     */
    public static void runInTransaction(DataSource dataSource, TransactionAction action) throws WrappedSqlException {
        inTransaction(dataSource, action);
    }

    /**
     * Alias for {@link #inTransaction(DataSource, TransactionCallback)}.
     */
    public static <T> T callInTransaction(DataSource dataSource, TransactionCallback<T> callback) throws WrappedSqlException {
        return inTransaction(dataSource, callback);
    }

    /**
     * Alias for {@link #inTransaction(DataSource, TransactionAction)}.
     */
    public static void transaction(DataSource dataSource, TransactionAction action) throws WrappedSqlException {
        inTransaction(dataSource, action);
    }

    /**
     * Alias for {@link #inTransaction(DataSource, TransactionCallback)}.
     */
    public static <T> T transaction(DataSource dataSource, TransactionCallback<T> callback) throws WrappedSqlException {
        return inTransaction(dataSource, callback);
    }

    /**
     * Checks whether a transaction is currently active in the current scope.
     *
     * @return {@code true} if a transaction is active; {@code false} otherwise
     * @throws IllegalStateException if called from a child thread attempting to access a transaction
     */
    public static boolean isTransactionActive() {
        return getActiveContext() != null;
    }

    /**
     * Checks whether a transaction is currently active on the specified {@link DataSource}.
     *
     * @param dataSource the DataSource to check
     * @return {@code true} if an active transaction matches {@code dataSource}; {@code false} otherwise
     * @throws IllegalStateException if called from a child thread attempting to access a transaction
     */
    public static boolean isTransactionActive(DataSource dataSource) {
        TransactionContext tx = getActiveContext();
        return tx != null && tx.matches(dataSource);
    }

    /**
     * Returns the active {@link Connection} in the current transaction scope, if any.
     *
     * @return an {@link Optional} containing the transactional connection, or {@link Optional#empty()}
     * @throws IllegalStateException if called from a child thread attempting to access a transaction
     */
    public static Optional<Connection> currentConnection() {
        TransactionContext tx = getActiveContext();
        if (tx != null) {
            return Optional.of(tx.getConnection());
        }
        return Optional.empty();
    }

    /**
     * Marks the current active transaction as rollback-only.
     *
     * @throws IllegalStateException if no transaction is active or if accessed by a child thread
     */
    public static void markRollbackOnly() {
        TransactionContext tx = getActiveContext();
        if (tx == null) {
            throw new IllegalStateException("No active transaction found to mark as rollback-only");
        }
        tx.setRollbackOnly();
    }

    private static Connection requireTransactionalConnection() {
        return currentConnection().orElseThrow(() -> new IllegalStateException(
                "No active transaction found in current scope. " +
                "Either execute within a transaction scope, or provide a Connection or DataSource."
        ));
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
     * Executes an arbitrary SQL statement using the active scoped transaction, or opens and closes a connection via {@code dataSource}.
     *
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
     * @param sql        the SQL statement to execute
     * @param params     optional query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static boolean execute(DataSource dataSource, String sql, Object... params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> execute(conn, sql, params));
    }

    /**
     * Executes an arbitrary SQL statement with a list of query parameters using the active scoped transaction or {@code dataSource}.
     *
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
     * @param sql        the SQL statement to execute
     * @param params     the list of query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws WrappedSqlException if a database access error occurs
     */
    public static boolean execute(DataSource dataSource, String sql, List<?> params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> execute(conn, sql, params));
    }

    /**
     * Executes an arbitrary SQL statement using the ambient scoped transaction.
     *
     * @param sql    the SQL statement to execute
     * @param params optional query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static boolean execute(String sql, Object... params) throws WrappedSqlException {
        return execute(requireTransactionalConnection(), sql, params);
    }

    /**
     * Executes an arbitrary SQL statement with a list of parameters using the ambient scoped transaction.
     *
     * @param sql    the SQL statement to execute
     * @param params the list of query parameters to bind
     * @return {@code true} if the first result is a {@link ResultSet}; {@code false} if it is an update count or there are no results
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static boolean execute(String sql, List<?> params) throws WrappedSqlException {
        return execute(requireTransactionalConnection(), sql, params);
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
     * Executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement using the active scoped transaction or {@code dataSource}.
     *
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
     * @param sql        the SQL statement to execute
     * @param params     optional query parameters to bind
     * @return the number of rows affected
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int update(DataSource dataSource, String sql, Object... params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> update(conn, sql, params));
    }

    /**
     * Executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement with a list of parameters using the active scoped transaction or {@code dataSource}.
     *
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
     * @param sql        the SQL statement to execute
     * @param params     the list of query parameters to bind
     * @return the number of rows affected
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int update(DataSource dataSource, String sql, List<?> params) throws WrappedSqlException {
        return withConnection(dataSource, conn -> update(conn, sql, params));
    }

    /**
     * Executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement using the ambient scoped transaction.
     *
     * @param sql    the SQL statement to execute
     * @param params optional query parameters to bind
     * @return the number of rows affected
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static int update(String sql, Object... params) throws WrappedSqlException {
        return update(requireTransactionalConnection(), sql, params);
    }

    /**
     * Executes an {@code INSERT}, {@code UPDATE}, or {@code DELETE} statement with a list of parameters using the ambient scoped transaction.
     *
     * @param sql    the SQL statement to execute
     * @param params the list of query parameters to bind
     * @return the number of rows affected
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static int update(String sql, List<?> params) throws WrappedSqlException {
        return update(requireTransactionalConnection(), sql, params);
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
     * Executes a parameterized batch update using the active scoped transaction or {@code dataSource}.
     *
     * @param dataSource  the DataSource to obtain a connection from (or participate in if in a transaction)
     * @param sql         the parameterized SQL statement to execute
     * @param batchParams an iterable of parameter arrays or collections (one entry per batch row)
     * @return an array of update counts containing one element for each command in the batch
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int[] batchUpdate(DataSource dataSource, String sql, Iterable<?> batchParams) throws WrappedSqlException {
        return withConnection(dataSource, conn -> batchUpdate(conn, sql, batchParams));
    }

    /**
     * Executes a batch of static SQL statements using the active scoped transaction or {@code dataSource}.
     *
     * @param dataSource    the DataSource to obtain a connection from (or participate in if in a transaction)
     * @param sqlStatements the SQL statements to execute
     * @return an array of update counts containing one element for each command in the batch
     * @throws WrappedSqlException if a database access error occurs
     */
    public static int[] batchUpdate(DataSource dataSource, Iterable<String> sqlStatements) throws WrappedSqlException {
        return withConnection(dataSource, conn -> batchUpdate(conn, sqlStatements));
    }

    /**
     * Executes a parameterized batch update using the ambient scoped transaction.
     *
     * @param sql         the parameterized SQL statement to execute
     * @param batchParams an iterable of parameter arrays or collections (one entry per batch row)
     * @return an array of update counts containing one element for each command in the batch
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static int[] batchUpdate(String sql, Iterable<?> batchParams) throws WrappedSqlException {
        return batchUpdate(requireTransactionalConnection(), sql, batchParams);
    }

    /**
     * Executes a batch of static SQL statements using the ambient scoped transaction.
     *
     * @param sqlStatements the SQL statements to execute
     * @return an array of update counts containing one element for each command in the batch
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static int[] batchUpdate(Iterable<String> sqlStatements) throws WrappedSqlException {
        return batchUpdate(requireTransactionalConnection(), sqlStatements);
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
     * Executes a query using the active scoped transaction or {@code dataSource}, mapping rows with {@link RowMapper}.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
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
     * Executes a query with a list of parameters using the active scoped transaction or {@code dataSource}.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
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
     * Executes a query using the ambient scoped transaction, mapping rows with {@link RowMapper}.
     *
     * @param <T>    the mapped result type
     * @param sql    the SQL query to execute
     * @param mapper the row mapper to apply to each row
     * @param params optional query parameters to bind
     * @return an unmodifiable list of mapped results
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> List<T> query(
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        return query(requireTransactionalConnection(), sql, mapper, params);
    }

    /**
     * Executes a query with a list of parameters using the ambient scoped transaction.
     *
     * @param <T>    the mapped result type
     * @param sql    the SQL query to execute
     * @param mapper the row mapper to apply to each row
     * @param params the list of query parameters to bind
     * @return an unmodifiable list of mapped results
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> List<T> query(
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return query(requireTransactionalConnection(), sql, mapper, params);
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
     * Executes a query expecting at most one row using the active scoped transaction or {@code dataSource}.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
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
     * Executes a query expecting at most one row with a list of parameters using the active scoped transaction or {@code dataSource}.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
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
     * Executes a query expecting at most one row using the ambient scoped transaction.
     *
     * @param <T>    the mapped result type
     * @param sql    the SQL query to execute
     * @param mapper the row mapper to apply
     * @param params optional query parameters to bind
     * @return an {@link Optional} containing the mapped object if exactly one row was found, or {@link Optional#empty()}
     * @throws IllegalStateException if no transaction is active or if more than one row is returned
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryOne(
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        return queryOne(requireTransactionalConnection(), sql, mapper, params);
    }

    /**
     * Executes a query expecting at most one row with a list of parameters using the ambient scoped transaction.
     *
     * @param <T>    the mapped result type
     * @param sql    the SQL query to execute
     * @param mapper the row mapper to apply
     * @param params the list of query parameters to bind
     * @return an {@link Optional} containing the mapped object if exactly one row was found, or {@link Optional#empty()}
     * @throws IllegalStateException if no transaction is active or if more than one row is returned
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryOne(
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return queryOne(requireTransactionalConnection(), sql, mapper, params);
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
     * Executes a query returning the first row using the active scoped transaction or {@code dataSource}.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
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
     * Executes a query with a list of parameters returning the first row using the active scoped transaction or {@code dataSource}.
     *
     * @param <T>        the mapped result type
     * @param dataSource the DataSource to obtain a connection from (or participate in if in a transaction)
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

    /**
     * Executes a query returning the first row using the ambient scoped transaction.
     *
     * @param <T>    the mapped result type
     * @param sql    the SQL query to execute
     * @param mapper the row mapper to apply
     * @param params optional query parameters to bind
     * @return an {@link Optional} containing the first mapped object, or {@link Optional#empty()} if no rows returned
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryFirst(
            String sql,
            RowMapper<T> mapper,
            Object... params
    ) throws WrappedSqlException {
        return queryFirst(requireTransactionalConnection(), sql, mapper, params);
    }

    /**
     * Executes a query with a list of parameters returning the first row using the ambient scoped transaction.
     *
     * @param <T>    the mapped result type
     * @param sql    the SQL query to execute
     * @param mapper the row mapper to apply
     * @param params the list of query parameters to bind
     * @return an {@link Optional} containing the first mapped object, or {@link Optional#empty()} if no rows returned
     * @throws IllegalStateException if no transaction is active in current scope
     * @throws WrappedSqlException   if a database access error occurs
     */
    public static <T> Optional<T> queryFirst(
            String sql,
            RowMapper<T> mapper,
            List<?> params
    ) throws WrappedSqlException {
        return queryFirst(requireTransactionalConnection(), sql, mapper, params);
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

        TransactionContext tx = getActiveContext();
        if (tx != null && tx.matches(dataSource)) {
            return action.run(tx.getConnection());
        }

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
