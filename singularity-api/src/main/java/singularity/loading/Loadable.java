package singularity.loading;

import gg.drak.thebase.objects.Identifiable;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Represents a persistable, identifiable entity that can be loaded from and saved to a
 * backing database asynchronously.
 *
 * <p>Implementations include {@link CosmicSender} and {@link CosmicPlayer}.  The interface
 * provides a full lifecycle: loading, tracking completion, saving, and unloading.  Async
 * augmentation via {@link #augment(CompletableFuture, boolean)} allows an object to be
 * placed in memory immediately while its database load completes in the background.</p>
 *
 * @param <L> the concrete loadable type (self-referential bound for fluent return types)
 */
public interface Loadable<L> extends Identifiable {

    /**
     * Returns whether this entity has finished its asynchronous load sequence and all
     * fields are populated with database values.
     *
     * @return {@code true} if fully loaded; {@code false} if the load is still in progress
     */
    boolean isFullyLoaded();

    /**
     * Sets the fully-loaded flag, typically called by the async loader once the database
     * future completes.
     *
     * @param fullyLoaded {@code true} to mark loading as complete
     */
    void setFullyLoaded(boolean fullyLoaded);

    /**
     * Persists this entity's current state to the backing database.
     *
     * @param async {@code true} to perform the save on an async thread; {@code false} to block
     */
    void save(boolean async);

    /**
     * Persists this entity's current state asynchronously (equivalent to {@code save(true)}).
     */
    default void save() {
        save(true);
    }

    /**
     * Augments this entity with data from the given asynchronous loader future.
     *
     * <p>When {@code isGet} is {@code true}, the augmentation is treated as a retrieval
     * (read-only) rather than a creation, which may influence merge behaviour in some
     * implementations.</p>
     *
     * @param loader a future that resolves to an {@link Optional} containing the loaded entity
     * @param isGet  {@code true} if the augmentation originates from a get (not a create) path
     * @return this entity after the augmentation has been applied
     */
    L augment(CompletableFuture<Optional<L>> loader, boolean isGet);

    /**
     * Augments this entity with data from the given asynchronous loader future, treating
     * the operation as a non-get (creation) path.
     *
     * @param loader a future that resolves to an {@link Optional} containing the loaded entity
     * @return this entity after the augmentation has been applied
     */
    default L augment(CompletableFuture<Optional<L>> loader) {
        return augment(loader, false);
    }

    /**
     * Removes this entity from the active in-memory set without saving.
     *
     * <p>Call {@link #saveAndUnload()} to persist before removal.</p>
     */
    void unload();

    /**
     * Loads (or re-loads) this entity's state from the backing database.
     *
     * <p>This is typically called by the {@link Loader} infrastructure; direct invocation
     * is discouraged unless rebuilding state explicitly.</p>
     */
    void load();

    /**
     * Returns whether this entity is currently registered in the active loaded set.
     *
     * @return {@code true} if the entity is in memory
     */
    boolean isLoaded();

    /**
     * Saves this entity and then removes it from the active in-memory set.
     *
     * @param async {@code true} to perform the save on an async thread before unloading
     */
    default void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    /**
     * Saves this entity asynchronously and then removes it from the active in-memory set.
     */
    default void saveAndUnload() {
        saveAndUnload(true);
    }

    /**
     * Blocks the calling thread with a spin-wait until {@link #isFullyLoaded()} returns
     * {@code true}.
     *
     * <p>Prefer callbacks via {@link #onceFullyLoaded(Consumer)} over this method on
     * performance-sensitive threads.</p>
     *
     * @return this instance, once fully loaded
     */
    default Loadable<L> waitUntilFullyLoaded() {
        while (! isFullyLoaded()) {
            Thread.onSpinWait();
        }

        return this;
    }

    /**
     * Blocks the calling thread until fully loaded and returns this instance cast to
     * {@code T}.
     *
     * @param <T> the expected concrete subtype of {@code Loadable<L>}
     * @return this instance cast to {@code T}, or {@code null} if the cast fails
     */
    default <T extends Loadable<L>> T waitUntilFullyLoadedTyped() {
        try {
            return (T) waitUntilFullyLoaded();
        } catch (Throwable e) {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Invokes the given consumer on this entity once it is fully loaded, blocking the
     * calling thread until loading completes.
     *
     * @param consumer the action to run with this loadable once it is ready
     */
    default void onceFullyLoaded(Consumer<Loadable<L>> consumer) {
        consumer.accept(waitUntilFullyLoaded());
    }

    /**
     * Invokes the given consumer on this entity (cast to {@code T}) once it is fully loaded,
     * blocking the calling thread until loading completes.
     *
     * @param <T>      the expected concrete subtype of {@code Loadable<L>}
     * @param consumer the action to run with the typed loadable once it is ready
     */
    default <T extends Loadable<L>> void onceFullyLoadedTyped(Consumer<T> consumer) {
        consumer.accept(waitUntilFullyLoadedTyped());
    }

    /**
     * Attempts to resolve this loadable as a {@link CosmicSender} by its identifier,
     * returning the already-loaded instance if available.
     *
     * @return an {@link Optional} containing the sender, or empty if not loaded
     */
    default Optional<CosmicSender> asSender() {
        return UserUtils.getOrGetSender(getIdentifier());
    }

    /**
     * Attempts to resolve this loadable as a {@link CosmicSender} by its identifier,
     * creating a new one if no loaded instance exists.
     *
     * @return an {@link Optional} containing the sender (existing or newly created)
     */
    default Optional<CosmicSender> asSenderOrCreate() {
        return UserUtils.getOrCreateSender(getIdentifier());
    }

    /**
     * Attempts to resolve this loadable as a {@link CosmicPlayer} by its identifier,
     * returning the already-loaded instance if available.
     *
     * @return an {@link Optional} containing the player, or empty if not loaded
     */
    default Optional<CosmicPlayer> asPlayer() {
        return UserUtils.getOrGetPlayer(getIdentifier());
    }

    /**
     * Attempts to resolve this loadable as a {@link CosmicPlayer} by its identifier,
     * creating a new one if no loaded instance exists.
     *
     * @return an {@link Optional} containing the player (existing or newly created)
     */
    default Optional<CosmicPlayer> asPlayerOrCreate() {
        return UserUtils.getOrCreatePlayer(getIdentifier());
    }
}
