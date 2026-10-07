package host.plas.collections.data;

import host.plas.collections.StreamlineCollections;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Threads for work that reads vanilla statistics. Each read waits for the server thread, so doing
 * them one after another on Streamline's single task thread would hold up every other module's
 * tasks for a tick per read; here a few players' reads wait at once, off that thread.
 */
public final class StatReader {
    private StatReader() {
    }

    private static final int THREADS = 4;
    private static volatile ExecutorService executor;

    public static void start() {
        stop();
        AtomicInteger count = new AtomicInteger();
        executor = Executors.newFixedThreadPool(THREADS, runnable -> {
            Thread thread = new Thread(runnable, "StreamlineCollections-Stats-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Lets queued reads finish, waiting up to five seconds. */
    public static void stop() {
        ExecutorService current = executor;
        executor = null;
        if (current == null) return;
        current.shutdown();
        try {
            current.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Runs {@code task} on a reader thread, or right here once the module is stopping. */
    public static CompletableFuture<Void> submit(String what, Runnable task) {
        ExecutorService current = executor;
        Runnable guarded = () -> {
            try {
                task.run();
            } catch (Throwable t) {
                StreamlineCollections.getInstance().logWarning("Could not " + what + ": " + t.getMessage());
            }
        };
        if (current == null || current.isShutdown()) {
            guarded.run();
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(guarded, current);
    }
}
