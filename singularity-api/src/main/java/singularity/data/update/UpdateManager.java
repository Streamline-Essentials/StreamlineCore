package singularity.data.update;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.database.servers.UpdateInfo;
import singularity.utils.MessageUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Static registry and coordinator for all active {@link UpdateType} instances.
 *
 * <p>{@code UpdateManager} maintains a thread-safe set of loaded update types and
 * provides convenience methods for registering, deregistering, querying, and
 * triggering cross-server data synchronisation updates.</p>
 */
public class UpdateManager {

    /**
     * The set of currently registered {@link UpdateType} instances, keyed by their
     * identifiers. Thread-safe via {@link ConcurrentSkipListSet}.
     */
    @Getter @Setter
    private static ConcurrentSkipListSet<UpdateType<?>> loadedUpdateTypes = new ConcurrentSkipListSet<>();

    /**
     * Registers an {@link UpdateType} with the manager.
     *
     * <p>If an update type with the same identifier is already registered it is
     * first unloaded before the new instance is added, ensuring no duplicates.</p>
     *
     * @param updateType the update type to register; must not be {@code null}
     */
    public static void load(UpdateType<?> updateType) {
        if (isLoaded(updateType.getIdentifier())) unload(updateType);

        loadedUpdateTypes.add(updateType);
    }

    /**
     * Removes the update type identified by the given string from the registry
     * and logs the unload operation.
     *
     * @param identifier the unique identifier of the update type to remove
     */
    public static void unload(String identifier) {
        MessageUtils.logInfo("Unloading update type: " + identifier);

        loadedUpdateTypes.removeIf(updateType -> updateType.getIdentifier().equals(identifier));
    }

    /**
     * Removes the given {@link UpdateType} from the registry by its identifier.
     *
     * @param updateType the update type instance to remove; must not be {@code null}
     */
    public static void unload(UpdateType<?> updateType) {
        unload(updateType.getIdentifier());
    }

    /**
     * Looks up a registered {@link UpdateType} by its identifier.
     *
     * @param identifier the unique identifier of the update type to find
     * @return an {@link Optional} containing the matching update type, or empty if
     *         none is registered under that identifier
     */
    public static Optional<UpdateType<?>> get(String identifier) {
        return loadedUpdateTypes.stream().filter(updateType -> updateType.getIdentifier().equals(identifier)).findFirst();
    }

    /**
     * Returns whether an update type with the given identifier is currently registered.
     *
     * @param identifier the unique identifier to check
     * @return {@code true} if a matching update type is registered; {@code false} otherwise
     */
    public static boolean isLoaded(String identifier) {
        return get(identifier).isPresent();
    }

    /**
     * Returns whether the given {@link UpdateType} instance is currently registered
     * (matched by identifier).
     *
     * @param updateType the update type whose registration status to check
     * @return {@code true} if an update type with the same identifier is registered
     */
    public static boolean isLoaded(UpdateType<?> updateType) {
        return isLoaded(updateType.getIdentifier());
    }

    /**
     * Checks for a pending cross-server update for the specified resource and, if one
     * exists, pulls the updated data and applies it via the registered
     * {@link UpdateType}.
     *
     * @param updateType the identifier of the {@link UpdateType} to consult
     * @param identifier the resource identifier (e.g. a player UUID) to check
     */
    public static void checkAndPull(String updateType, String identifier) {
        get(updateType).ifPresent(type -> type.checkAndPut(identifier));
    }

    /**
     * Retrieves the most recent {@link UpdateInfo} for the specified resource from the
     * main database.
     *
     * <p><strong>Note:</strong> This method blocks the calling thread while waiting
     * for the database query to complete and should be invoked asynchronously.</p>
     *
     * @param updateType the identifier of the {@link UpdateType} to query
     * @param identifier the resource identifier to look up
     * @return an {@link Optional} containing the latest {@link UpdateInfo}, or empty if
     *         no record exists or the update type is not registered
     */
    // This method is meant to be run asynchronously.
    public static Optional<UpdateInfo> getLastUpdate(String updateType, String identifier) {
        return get(updateType)
                .map(type -> Singularity.getMainDatabase().checkUpdate(type, identifier))
                .map(CompletableFuture::join)
                .filter(Optional::isPresent).map(Optional::get);
    }

    /**
     * Posts a new update record for the specified resource under the given update type,
     * signalling to other servers that this server has changed the resource.
     *
     * @param updateType the identifier of the {@link UpdateType} to post an update for
     * @param identifier the resource identifier that has been updated
     */
    public static void update(String updateType, String identifier) {
        get(updateType).ifPresent(type -> type.update(identifier));
    }
}
