package dfb.fitz.jdbc;

import dfb.fitz.Validate;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Package-private contextual state representing an active scoped transaction.
 */
final class TransactionContext {

    private final DataSource dataSource;
    private final Connection connection;
    private final Thread ownerThread;
    private boolean rollbackOnly;

    TransactionContext(DataSource dataSource, Connection connection) {
        this.dataSource = Validate.nonNull(dataSource, "dataSource");
        this.connection = Validate.nonNull(connection, "connection");
        this.ownerThread = Thread.currentThread();
    }

    DataSource getDataSource() {
        return dataSource;
    }

    Connection getConnection() {
        return connection;
    }

    Thread getOwnerThread() {
        return ownerThread;
    }

    boolean isOwner() {
        return Thread.currentThread() == ownerThread;
    }

    /**
     * Enforces that child threads or other threads must not join or access the transaction.
     *
     * @throws IllegalStateException if accessed from a thread other than the owner thread
     */
    void checkThreadAccess() {
        if (!isOwner()) {
            throw new IllegalStateException(
                    "Child threads (or other threads) must not join or access an existing transaction " +
                    "bound to thread [" + ownerThread.getName() + "] (attempted from thread [" +
                    Thread.currentThread().getName() + "])."
            );
        }
    }

    boolean matches(DataSource other) {
        return other != null && (this.dataSource == other || this.dataSource.equals(other));
    }

    void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    boolean isRollbackOnly() {
        return rollbackOnly;
    }
}
