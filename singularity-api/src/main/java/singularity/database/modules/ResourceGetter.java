package singularity.database.modules;

import java.util.function.Function;

public interface ResourceGetter<T> extends Function<String, T> {
}
