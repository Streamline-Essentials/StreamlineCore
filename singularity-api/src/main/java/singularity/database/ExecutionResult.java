package singularity.database;

/**
 * Represents the outcome of a single SQL statement execution performed by
 * {@link DBOperator#executeSingle(String, java.util.function.Consumer)}.
 */
public enum ExecutionResult {

    /**
     * The statement could not be executed due to an exception (e.g. a connection
     * failure or SQL syntax error). The statement had no effect.
     */
    ERROR,

    /**
     * The statement executed successfully and returned a result set (i.e.
     * {@link java.sql.PreparedStatement#execute()} returned {@code true}).
     */
    YES,

    /**
     * The statement executed successfully but did not return a result set (i.e.
     * {@link java.sql.PreparedStatement#execute()} returned {@code false}), which is
     * the normal outcome for DML statements such as {@code INSERT} or {@code UPDATE}.
     */
    NO,
    ;
}
