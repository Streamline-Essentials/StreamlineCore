package gg.drak.thebase.async;

import java.util.concurrent.CompletableFuture;

public interface WithSync {
    void sync(Runnable runnable);

    void sync(Runnable runnable, long delay);

    void sync(Runnable runnable, long delay, long period);

    default Runnable wrapRunnable(Runnable runnable, CompletableFuture<Void> completableFuture) {
        return () -> {
            runnable.run();
            completableFuture.complete(null);
        };
    }

    default CompletableFuture<Void> executeSync(Runnable runnable) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        sync(wrapRunnable(runnable, completableFuture));
        return completableFuture;
    }

    default CompletableFuture<Void> runSync(Runnable runnable, long delay) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        sync(wrapRunnable(runnable, completableFuture), delay);
        return completableFuture;
    }

    default CompletableFuture<Void> runSync(Runnable runnable, long delay, long period) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        sync(wrapRunnable(runnable, completableFuture), delay, period);
        return completableFuture;
    }

    boolean isSync();
}
