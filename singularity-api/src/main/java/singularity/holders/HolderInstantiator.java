package singularity.holders;

import java.util.function.Supplier;

/**
 * A factory interface for creating instances of {@link CosmicHolder} implementations.
 *
 * <p>Extends {@link Supplier} so that the single abstract method {@code get()} can be
 * provided as a lambda or method reference.  The supplier may return {@code null} to signal
 * that the holder could not be instantiated (e.g., because the required plugin is absent),
 * in which case {@link HolderInit} will mark itself as disabled.</p>
 *
 * @param <T> the specific {@link CosmicHolder} type to instantiate
 */
public interface HolderInstantiator<T extends CosmicHolder> extends Supplier<T> {
}
