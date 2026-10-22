package dfb.fitz.jdbc;

/**
 * Functional interface for transactional logic that performs an action without returning a result.
 */
@FunctionalInterface
public interface TransactionAction {

    /**
     * Executes logic within the active transaction scope.
     *
     * @throws Exception if an error occurs
     */
    void execute() throws Exception;
}
