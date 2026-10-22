package dfb.fitz.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Functional interface for mapping a single row of a {@link ResultSet} to an object.
 *
 * @param <T> the type of the mapped object
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps the current row of the given {@link ResultSet} to an instance of {@code T}.
     *
     * @param rs the {@link ResultSet} positioned on the row to map
     * @return the mapped object (may be {@code null})
     * @throws SQLException if an error occurs reading the result set
     */
    T map(ResultSet rs) throws SQLException;
}
