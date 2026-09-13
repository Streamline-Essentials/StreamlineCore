package singularity.data.update;

import java.util.function.Function;

/**
 * A specialised {@link Function} that loads an object of type {@code T} from
 * persistent storage given a string identifier (typically a UUID or name).
 *
 * <p>Implementations are supplied to {@link UpdateType} to define how a specific
 * entity type is fetched from the database or another data source.</p>
 *
 * @param <T> the type of the object to be loaded
 */
public interface PullFunction<T> extends Function<String, T> {
}
