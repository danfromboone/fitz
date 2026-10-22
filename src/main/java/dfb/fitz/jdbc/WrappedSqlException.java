package dfb.fitz.jdbc;

import java.sql.SQLException;
import java.util.Objects;

/**
 * Unchecked runtime exception wrapping a {@link SQLException} to avoid checked exception
 * boilerplate in consumers.
 */
public class WrappedSqlException extends RuntimeException {

    /**
     * Constructs a {@code WrappedSqlException} with the specified cause.
     *
     * @param cause the underlying {@link SQLException} (must not be {@code null})
     * @throws NullPointerException if {@code cause} is {@code null}
     */
    public WrappedSqlException(SQLException cause) {
        super(Objects.requireNonNull(cause, "cause must not be null"));
    }

    /**
     * Constructs a {@code WrappedSqlException} with a custom message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying {@link SQLException} (must not be {@code null})
     * @throws NullPointerException if {@code cause} is {@code null}
     */
    public WrappedSqlException(String message, SQLException cause) {
        super(message, Objects.requireNonNull(cause, "cause must not be null"));
    }

    /**
     * Returns the underlying {@link SQLException} cause.
     *
     * @return the {@link SQLException} that caused this exception
     */
    @Override
    public synchronized SQLException getCause() {
        Throwable cause = super.getCause();
        return (cause instanceof SQLException sqlException) ? sqlException : null;
    }
}
