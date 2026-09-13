package singularity.loading;

import java.util.Optional;

/**
 * A specialised {@link Loader} that manages a more specific subtype {@code E} of the base
 * loadable type {@code L}.
 *
 * <p>While the parent {@code Loader<L>} works with the base interface, {@code ExtendedLoader}
 * adds typed retrieval and creation methods for the more concrete type {@code E}.  This is
 * useful when, for example, a player loader (type {@code L}) needs to also support a richer
 * administrative player wrapper (type {@code E}).</p>
 *
 * @param <L> the base loadable type managed by the parent {@link Loader}
 * @param <E> the extended loadable subtype whose instances this loader specialises in
 */
public abstract class ExtendedLoader<L extends Loadable<L>, E extends L> extends Loader<L> {

    /**
     * Looks up a loaded entity by identifier and attempts to cast it to type {@code E}.
     *
     * <p>Returns an empty {@link Optional} if no entity with the given identifier is loaded
     * or if the loaded entity cannot be cast to {@code E}.</p>
     *
     * @param identifier the unique identifier of the entity to retrieve
     * @return an {@link Optional} containing the typed entity, or empty if absent or wrong type
     */
    public Optional<E> getExtended(String identifier) {
        Optional<L> optional = get(identifier);
        if (optional.isEmpty()) return Optional.empty();
        L loadable = optional.get();

        try {
            return Optional.of((E) loadable);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new instance of the extended type {@code E} for the given identifier without
     * persisting or registering it.
     *
     * <p>Implementations should allocate the object and populate any identifier-derived fields,
     * but should not call {@code save()} or {@code load()} — that is done by
     * {@link #createNewExtended(String)}.</p>
     *
     * @param identifier the unique identifier for the new entity
     * @return a freshly constructed, not-yet-persisted instance of {@code E}
     */
    public abstract E instantiateExtended(String identifier);

    /**
     * Creates, persists, and registers a new extended entity for the given identifier.
     *
     * <p>Calls {@link #instantiateExtended(String)} to build the object, then {@code save()}
     * to write it to the database, fires creation events, and finally registers it with the
     * loaded set via {@link #load(Loadable)}.</p>
     *
     * @param identifier the unique identifier for the new entity
     * @return the newly created and loaded entity of type {@code E}
     */
    public E createNewExtended(String identifier) {
        E created = instantiateExtended(identifier);
        created.save();

        fireCreateEvents(created);

        return (E) load(created);
    }
}
