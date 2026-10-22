package dfb.fitz.jdbc;

/**
 * Functional interface for transactional logic that computes and returns a result of type {@code T}.
 *
 * @param <T> the result type
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * Executes logic within the active transaction scope.
     *
     * @return the computed result (may be {@code null})
     * @throws Exception if an error occurs
     */
    T execute() throws Exception;
}
