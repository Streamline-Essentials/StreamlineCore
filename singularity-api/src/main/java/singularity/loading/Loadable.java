package singularity.loading;

import gg.drak.thebase.objects.Identifiable;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface Loadable<L> extends Identifiable {
    boolean isFullyLoaded();

    void setFullyLoaded(boolean fullyLoaded);

    void save(boolean async);

    default void save() {
        save(true);
    }

    L augment(CompletableFuture<Optional<L>> loader, boolean isGet);

    default L augment(CompletableFuture<Optional<L>> loader) {
        return augment(loader, false);
    }

    void unload();

    void load();

    boolean isLoaded();

    default void saveAndUnload(boolean async) {
        save(async);
        unload();
    }

    default void saveAndUnload() {
        saveAndUnload(true);
    }

    default Loadable<L> waitUntilFullyLoaded() {
        while (! isFullyLoaded()) {
            Thread.onSpinWait();
        }

        return this;
    }

    default <T extends Loadable<L>> T waitUntilFullyLoadedTyped() {
        try {
            return (T) waitUntilFullyLoaded();
        } catch (Throwable e) {
            e.printStackTrace();

            return null;
        }
    }

    default void onceFullyLoaded(Consumer<Loadable<L>> consumer) {
        consumer.accept(waitUntilFullyLoaded());
    }

    default <T extends Loadable<L>> void onceFullyLoadedTyped(Consumer<T> consumer) {
        consumer.accept(waitUntilFullyLoadedTyped());
    }

    default Optional<CosmicSender> asSender() {
        return UserUtils.getOrGetSender(getIdentifier());
    }

    default Optional<CosmicSender> asSenderOrCreate() {
        return UserUtils.getOrCreateSender(getIdentifier());
    }

    default Optional<CosmicPlayer> asPlayer() {
        return UserUtils.getOrGetPlayer(getIdentifier());
    }

    default Optional<CosmicPlayer> asPlayerOrCreate() {
        return UserUtils.getOrCreatePlayer(getIdentifier());
    }
}
