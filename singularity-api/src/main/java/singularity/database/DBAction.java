package singularity.database;

import java.sql.ResultSet;
import java.util.function.Consumer;

/**
 * A functional interface for processing a JDBC {@link ResultSet} returned by a
 * database query.
 *
 * <p>Implementations receive the open {@link ResultSet} and are responsible for
 * iterating over rows and extracting the data they need. The result set is closed
 * by the surrounding {@link DBOperator#executeQuery} call after the action returns.</p>
 *
 * <p>This interface extends {@link Consumer}{@code <ResultSet>} and can be used as
 * a lambda or method reference wherever a {@code DBAction} is expected.</p>
 */
public interface DBAction extends Consumer<ResultSet> {
}
