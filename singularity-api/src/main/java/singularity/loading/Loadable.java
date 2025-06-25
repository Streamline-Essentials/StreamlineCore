package singularity.loading;

import gg.drak.thebase.objects.Identifiable;

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

    void saveAndUnload(boolean async);

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
}
