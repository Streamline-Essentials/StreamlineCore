package singularity.database.modules;

import java.util.function.Function;

/**
 * A functional interface that constructs or retrieves a resource of type {@code T}
 * from a string identifier.
 *
 * <p>Implementations are supplied to {@link DBKeeper} as a fallback factory: when
 * the database returns no result for a given identifier, the keeper calls
 * {@link #apply(Object)} to obtain a default or freshly constructed instance.</p>
 *
 * <p>This interface extends {@link Function}{@code <String, T>} and can be used as a
 * lambda or method reference wherever a {@code ResourceGetter} is expected.</p>
 *
 * @param <T> the type of resource produced by this getter
 */
public interface ResourceGetter<T> extends Function<String, T> {
}
