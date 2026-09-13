package singularity.loading;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.database.CoreDBOperator;
import singularity.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Generic registry and lifecycle manager for {@link Loadable} entities.
 *
 * <p>{@code Loader} maintains an in-memory set of active loadables, delegates database
 * persistence to a {@link DBKeeper}, and provides convenience methods for creating,
 * retrieving, and unloading entities.  Subclasses specialise the type parameter {@code L}
 * to a concrete loadable domain object (e.g., {@code CosmicSender} or {@code CosmicPlayer})
 * and implement the abstract factory and event-firing hooks.</p>
 *
 * @param <L> the concrete loadable type managed by this loader
 */
@Getter @Setter
public abstract class Loader<L extends Loadable<L>> {

    /**
     * The thread-safe set of all entities that are currently held in memory.
     *
     * <p>Entities are added by {@link #load(Loadable)} and removed by {@link #unload(String)}.
     * Direct mutation should be avoided; prefer the provided lifecycle methods.</p>
     */
    public ConcurrentSkipListSet<L> loaded;

    /**
     * Constructs a {@code Loader} with an empty in-memory entity set.
     */
    public Loader() {
        loaded = new ConcurrentSkipListSet<>();
    }

    /**
     * Returns the {@link DBKeeper} responsible for reading and writing entities of type
     * {@code L} to the backing database.
     *
     * @return the database keeper; never {@code null}
     */
    public abstract DBKeeper<L> getKeeper();

    /**
     * Returns the shared {@link CoreDBOperator} used by all loaders, sourced from the
     * active {@link Singularity} instance.
     *
     * @return the main database operator; never {@code null}
     */
    public static CoreDBOperator getOperator() {
        return Singularity.getMainDatabase();
    }

    /**
     * Asynchronously checks whether a persisted record exists for the given identifier.
     *
     * @param identifier the unique identifier to query
     * @return a future that resolves to {@code true} if a record exists in the database
     */
    public CompletableFuture<Boolean> userExists(String identifier) {
        return getKeeper().exists(identifier);
    }

    /**
     * Returns the singleton console/server-sender entity managed by this loader.
     *
     * <p>The console is special-cased throughout the loader because it does not have a UUID
     * in the conventional sense; it is identified by
     * {@link CosmicSender#getConsoleDiscriminator()}.</p>
     *
     * @return the console entity; never {@code null}
     */
    public abstract L getConsole();

    /**
     * Retrieves the in-memory entity with the given identifier.
     *
     * <p>If the identifier matches the console discriminator, the console entity is returned
     * directly without searching the loaded set.</p>
     *
     * @param identifier the unique identifier to look up; may be {@code null} (returns empty)
     * @return an {@link Optional} containing the entity, or empty if not currently loaded
     */
    public Optional<L> get(String identifier) {
        if (identifier == null) return Optional.empty();
        if (identifier.equals(CosmicSender.getConsoleDiscriminator())) return Optional.of(getConsole());

        return getLoaded().stream().filter(a -> a.getIdentifier().equals(identifier)).findFirst();
    }

    /**
     * Registers an already-constructed entity into the in-memory loaded set.
     *
     * <p>If an entity with the same identifier is already loaded, the existing instance is
     * returned and the provided instance is discarded (idempotent behaviour).</p>
     *
     * @param toLoad the entity to register; must not be {@code null}
     * @return the entity that is now in the loaded set (either {@code toLoad} or the existing one)
     */
    public L load(L toLoad) {
        if (isLoaded(toLoad.getIdentifier())) return get(toLoad.getIdentifier()).get();

        getLoaded().add(toLoad);

        fireLoadEvents(toLoad);

        return toLoad;
    }

    /**
     * Fires the appropriate load lifecycle events after an entity has been added to the
     * in-memory set.
     *
     * @param loaded the entity that was just loaded into memory
     */
    public abstract void fireLoadEvents(L loaded);

    /**
     * Creates a new, unsaved entity for the given identifier without registering it.
     *
     * <p>Implementations should allocate the object and populate identifier-derived fields,
     * but must not call {@code save()} — that is handled by {@link #createNew(String)}.</p>
     *
     * @param identifier the unique identifier for the new entity
     * @return a freshly constructed, not-yet-persisted entity
     */
    public abstract L instantiate(String identifier);

    /**
     * Creates, saves, and registers a new entity for the given identifier.
     *
     * <p>Calls {@link #instantiate(String)} to build the object, persists it via
     * {@code save()}, fires creation events, and registers it with {@link #load(Loadable)}.</p>
     *
     * @param identifier the unique identifier for the new entity
     * @return the newly created and loaded entity
     */
    public L createNew(String identifier) {
        L created = instantiate(identifier);
        created.save();

        fireCreateEvents(created);

        return load(created);
    }

    /**
     * Fires the appropriate creation lifecycle events after a new entity has been saved and
     * is about to be loaded.
     *
     * @param created the entity that was just created and saved
     */
    public abstract void fireCreateEvents(L created);

    /**
     * Returns a future that resolves to the entity for the given identifier, creating one
     * if no persisted record is found.
     *
     * <p>The entire operation (DB lookup and optional creation) runs on an async thread.</p>
     *
     * @param identifier the unique identifier of the entity to retrieve or create
     * @return a future containing the loaded or newly created entity
     */
    public CompletableFuture<L> getOrCreateAsync(String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<L> optional = getKeeper().load(identifier).join();
            if (optional.isPresent()) {
                return load(optional.get());
            } else {
                return createNew(identifier);
            }
        });
    }

    /**
     * Returns whether an entity with the given identifier is currently held in memory.
     *
     * @param identifier the unique identifier to check
     * @return {@code true} if the entity is loaded in memory
     */
    public boolean isLoaded(String identifier) {
        return get(identifier).isPresent();
    }

    /**
     * Returns the in-memory entity for the given identifier, triggering an async load or
     * creation if it is not already present.
     *
     * <p>Because the load is asynchronous, this method may return an empty {@link Optional}
     * on the first call even when the entity will eventually be available.  Callers that
     * require a guaranteed result should use {@link #getOrCreateAsync(String)} instead.</p>
     *
     * @param identifier the unique identifier of the entity to retrieve
     * @return an {@link Optional} with the entity if already in memory, or empty otherwise
     */
    public Optional<L> getOrLoad(String identifier) {
        CompletableFuture.runAsync(() -> {
            if (isLoaded(identifier)) return;

            L created = getOrCreateAsync(identifier).join();
            load(created);
        });

        return get(identifier);
    }

    /**
     * Returns the loaded entity matching the given instance's identifier, or creates a new
     * one if absent.
     *
     * @param sender an entity whose identifier is used for the lookup; must not be {@code null}
     * @return the matching entity from the loaded set, or a newly created one
     */
    public L getOrCreate(L sender) {
        return getOrCreate(sender.getIdentifier());
    }

    /**
     * Returns the entity for the given identifier from the in-memory set, or creates and
     * registers a new one if it is not yet present.
     *
     * <p>If the identifier matches the console discriminator, the console entity is returned
     * directly.  Otherwise, an async database load is started and the new entity is augmented
     * once the load completes.</p>
     *
     * @param identifier the unique identifier of the entity to retrieve or create
     * @return the existing or newly created entity; never {@code null}
     */
    public L getOrCreate(String identifier) {
        Optional<L> optional = getOrLoad(identifier);
        if (optional.isPresent()) return optional.get();

        if (identifier.equals(CosmicSender.getConsoleDiscriminator())) {
            return getConsole();
        }

        CompletableFuture<Optional<L>> loader = load(identifier);

        L toGet = createNew(identifier);

        load(toGet);

        return toGet.augment(loader);
    }

    /**
     * Asynchronously loads the entity for the given UUID from the backing database.
     *
     * <p>Unlike {@link #load(Loadable)}, this method performs a database fetch rather than
     * registering an already-constructed object.</p>
     *
     * @param uuid the UUID string identifying the entity to fetch
     * @return a future that resolves to an {@link Optional} containing the entity if found
     */
    public CompletableFuture<Optional<L>> load(String uuid) {
        return getKeeper().load(uuid);
    }

    /**
     * Saves and removes the entity with the given identifier from the in-memory loaded set.
     *
     * <p>If no entity with the given identifier is currently loaded, this method is a no-op.</p>
     *
     * @param identifier the unique identifier of the entity to unload
     */
    public void unload(String identifier) {
        Optional<L> optional = get(identifier);
        if (optional.isEmpty()) return;
        L loadable = optional.get();

        loadable.save();
        getLoaded().remove(loadable);
    }

    /**
     * Saves and removes the given entity from the in-memory loaded set.
     *
     * <p>Delegates to {@link #unload(String)} using the entity's identifier.</p>
     *
     * @param loadable the entity to unload; must not be {@code null}
     */
    public void unload(L loadable) {
        unload(loadable.getIdentifier());
    }

    /**
     * Returns whether the given entity instance is currently held in the loaded set,
     * determined by its identifier.
     *
     * @param loadable the entity to check; must not be {@code null}
     * @return {@code true} if the entity's identifier is found in the loaded set
     */
    public boolean isLoaded(L loadable) {
        return isLoaded(loadable.getIdentifier());
    }
}
