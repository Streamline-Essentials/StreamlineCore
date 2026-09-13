package singularity.data.update;

import java.util.function.Consumer;

/**
 * A specialised {@link Consumer} that persists an object of type {@code T} to
 * a data store (such as a database or cache).
 *
 * <p>Implementations are supplied to {@link UpdateType} to define how a specific
 * entity type is written back to persistent storage.</p>
 *
 * @param <T> the type of the object to be persisted
 */
public interface PutterFunction<T> extends Consumer<T> {
}
