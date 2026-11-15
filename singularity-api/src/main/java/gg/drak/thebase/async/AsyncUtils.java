package gg.drak.thebase.async;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncUtils {
    @Getter @Setter
    private static AtomicLong currentTaskId = new AtomicLong(0);

    public static void init() {
    }

    /**
     * Removes every queued task and stops the shared tick thread.
     */
    public static void shutdown() {
        getQueuedTasks().forEach(AsyncTask::remove);
        AsyncTask.shutdownTicker();
    }

    @Getter @Setter
    private static ConcurrentSkipListSet<AsyncTask> queuedTasks = new ConcurrentSkipListSet<>();

    public static long queueTask(AsyncTask task) {
        queuedTasks.add(task);

        return task.getId();
    }

    public static void removeTask(long taskId) {
        queuedTasks.removeIf(task -> task.getId() == taskId);
    }

    public static void removeTask(AsyncTask task) {
        removeTask(task.getId());
    }

    public static Optional<AsyncTask> getTask(long taskId) {
        return queuedTasks.stream().filter(task -> task.getId() == taskId).findFirst();
    }

    public static long getNextTaskId() {
        return getCurrentTaskId().getAndIncrement();
    }

    public static void tickTasks() {
        getQueuedTasks().forEach(AsyncTask::tick);
    }

    public static <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

    public static long runAsync(Runnable runnable, long delay) {
        return new AsyncTask((asyncTask) -> runnable.run(), delay).queue();
    }

    public static long runAsync(Consumer<AsyncTask> consumer, long delay) {
        return new AsyncTask(consumer, delay).queue();
    }

    public static long runAsync(Runnable runnable, long delay, long period) {
        return new AsyncTask((asyncTask) -> runnable.run(), delay, period).queue();
    }

    public static long runAsync(Consumer<AsyncTask> consumer, long delay, long period) {
        return new AsyncTask(consumer, delay, period).queue();
    }

    public static CompletableFuture<Void> runSync(Runnable runnable, long delay) {
        return ThreadHolder.with(runnable, delay);
    }

    public static CompletableFuture<Void> runSync(Runnable runnable, long delay, long period) {
        return ThreadHolder.with(runnable, delay, period);
    }

    public static long convertToMilliseconds(long ticks) {
        return ticks * getMillisInATick();
    }

    public static long convertToTicks(long milliseconds) {
        return milliseconds / getMillisInATick();
    }

    public static long getMillisInATick() {
        return 1000L / 20; // 20 ticks per second
    }

    public static CompletableFuture<Void> executeAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable);
    }

    public static CompletableFuture<Void> executeSync(Runnable runnable) {
        return ThreadHolder.with(runnable);
    }

    public static void executeThreaded(Runnable runnable) {
        CompletableFuture.runAsync(runnable).join();
    }

    public static void cancelTask(long taskId) {
        getTask(taskId).ifPresent(AsyncTask::remove);
    }
}
