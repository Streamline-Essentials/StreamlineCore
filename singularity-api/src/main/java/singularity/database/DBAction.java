package singularity.database;

import java.sql.ResultSet;
import java.util.function.Consumer;

public interface DBAction extends Consumer<ResultSet> {
}