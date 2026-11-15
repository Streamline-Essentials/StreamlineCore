package gg.drak.thebase.async;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

public class ThreadHolder {
    @Getter @Setter
    private static ConcurrentSkipListSet<SyncInstance> loadedSyncInstances = new ConcurrentSkipListSet<>();

    public static void register(SyncInstance syncInstance) {
        unregister(syncInstance);

        getLoadedSyncInstances().add(syncInstance);
    }

    public static void unregister(String identifier) {
        getLoadedSyncInstances().removeIf(syncInstance -> syncInstance.getIdentifier().equals(identifier));
    }

    public static void unregister(SyncInstance syncInstance) {
        unregister(syncInstance.getIdentifier());
    }

    public static Optional<SyncInstance> get(String identifier) {
        return getLoadedSyncInstances().stream().filter(syncInstance -> syncInstance.getIdentifier().equals(identifier)).findFirst();
    }

    public static Optional<SyncInstance> get(SyncInstance syncInstance) {
        return get(syncInstance.getIdentifier());
    }

    public static boolean isRegistered(String identifier) {
        return get(identifier).isPresent();
    }

    public static boolean isRegistered(SyncInstance syncInstance) {
        return isRegistered(syncInstance.getIdentifier());
    }

    public static Optional<SyncInstance> getFirst() {
        return getLoadedSyncInstances().stream().findFirst();
    }

    public static Consumer<SyncTask> convert(Runnable runnable) {
        return task -> runnable.run();
    }

    public static CompletableFuture<Void> with(SyncInstance instance, SyncTask task) {
        long delay = task.getDelay();
        long period = task.getPeriod();

        if (period != -1 && delay != -1) {
            return instance.runSync(task::runThreaded, delay, period);
        } else if (period == -1 && delay != -1) {
            return instance.runSync(task::runThreaded, delay);
        } else {
            return instance.executeSync(task::runThreaded);
        }
    }

    public static CompletableFuture<Void> with(String identifier, SyncTask task) {
        Optional<SyncInstance> optional = get(identifier);
        if (optional.isEmpty()) return CompletableFuture.completedFuture(null);

        return with(optional.get(), task);
    }

    public static CompletableFuture<Void> with(String identifier, Runnable runnable) {
        return with(identifier, new SyncTask(convert(runnable)));
    }

    public static CompletableFuture<Void> with(String identifier, Runnable runnable, long delay) {
        return with(identifier, new SyncTask(convert(runnable), delay));
    }

    public static CompletableFuture<Void> with(String identifier, Runnable runnable, long delay, long period) {
        return with(identifier, new SyncTask(convert(runnable), delay, period));
    }

    public static CompletableFuture<Void> with(SyncTask task) {
        Optional<SyncInstance> optional = getFirst();
        if (optional.isEmpty()) return CompletableFuture.completedFuture(null);

        return with(optional.get(), task);
    }

    public static CompletableFuture<Void> with(Runnable runnable) {
        return with(new SyncTask(convert(runnable)));
    }

    public static CompletableFuture<Void> with(Runnable runnable, long delay) {
        return with(new SyncTask(convert(runnable), delay));
    }

    public static CompletableFuture<Void> with(Runnable runnable, long delay, long period) {
        return with(new SyncTask(convert(runnable), delay, period));
    }
}
