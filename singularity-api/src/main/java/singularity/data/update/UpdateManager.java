package singularity.data.update;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.database.servers.UpdateInfo;
import singularity.utils.MessageUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

public class UpdateManager {
    @Getter @Setter
    private static ConcurrentSkipListSet<UpdateType<?>> loadedUpdateTypes = new ConcurrentSkipListSet<>();

    public static void load(UpdateType<?> updateType) {
        if (isLoaded(updateType.getIdentifier())) unload(updateType);

        loadedUpdateTypes.add(updateType);
    }

    public static void unload(String identifier) {
        MessageUtils.logInfo("Unloading update type: " + identifier);

        loadedUpdateTypes.removeIf(updateType -> updateType.getIdentifier().equals(identifier));
    }

    public static void unload(UpdateType<?> updateType) {
        unload(updateType.getIdentifier());
    }

    public static Optional<UpdateType<?>> get(String identifier) {
        return loadedUpdateTypes.stream().filter(updateType -> updateType.getIdentifier().equals(identifier)).findFirst();
    }

    public static boolean isLoaded(String identifier) {
        return get(identifier).isPresent();
    }

    public static boolean isLoaded(UpdateType<?> updateType) {
        return isLoaded(updateType.getIdentifier());
    }

    public static void checkAndPull(String updateType, String identifier) {
        get(updateType).ifPresent(type -> type.checkAndPut(identifier));
    }

    // This method is meant to be run asynchronously.
    public static Optional<UpdateInfo> getLastUpdate(String updateType, String identifier) {
        return get(updateType)
                .map(type -> Singularity.getMainDatabase().checkUpdate(type, identifier))
                .map(CompletableFuture::join)
                .filter(Optional::isPresent).map(Optional::get);
    }

    public static void update(String updateType, String identifier) {
        get(updateType).ifPresent(type -> type.update(identifier));
    }
}
