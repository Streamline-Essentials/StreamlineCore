package gg.drak.thebase.async;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter @Setter
public class AsyncTask implements TaskLike<AsyncTask> {
    /**
     * Interval between ticks, in milliseconds: one server tick.
     */
    public static final long TICK_MILLIS = 50L;

    /**
     * Shared ticker for every queued task. A single daemon thread keeps tick bookkeeping
     * single-threaded per task; the task bodies themselves run through {@link AsyncUtils#executeAsync}.
     * Created on first use and discarded by {@link #shutdownTicker()}, so a plugin reload does not
     * leave a thread holding the old class loader.
     */
    private static ScheduledExecutorService ticker;

    private long id;

    private Consumer<AsyncTask> consumer;
    private long delay;
    private long period;
    private long currentDelay;

    private long ticksLived;

    private int timesRan;

    /**
     * The scheduled tick loop for this task, or null while it is stopped.
     */
    private ScheduledFuture<?> timer;

    public AsyncTask(long id, Consumer<AsyncTask> consumer, long delay, long period) {
        this.id = id;

        this.consumer = consumer;
        this.delay = delay;
        this.period = period;
        this.currentDelay = delay;

        this.ticksLived = 0;
        this.timesRan = 0;
    }

    private static synchronized ScheduledExecutorService getTicker() {
        if (ticker == null || ticker.isShutdown()) {
            ticker = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "BOU-AsyncTask-Ticker");
                thread.setDaemon(true);
                return thread;
            });
        }
        return ticker;
    }

    /**
     * Stops the shared ticker thread. Tasks started afterwards create a new one.
     */
    public static synchronized void shutdownTicker() {
        if (ticker == null) return;

        ticker.shutdownNow();
        ticker = null;
    }

    /**
     * Schedules this task's tick loop on the shared ticker.
     *
     * @return the scheduled tick loop
     */
    public ScheduledFuture<?> createTimer() {
        return getTicker().scheduleAtFixedRate(() -> {
            try {
                tick();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }, TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
    }

    public synchronized boolean isRunning() {
        return timer != null && ! timer.isDone();
    }

    public synchronized void start() {
        if (isRunning()) return;

        timer = createTimer();
    }

    public synchronized void stop() {
        if (timer == null) return;

        timer.cancel(false);
        timer = null;
    }

    public void reset() {
        ticksLived = 0;
        timesRan = 0;
    }

    public void restart() {
        stop();
        reset();
        start();
    }

    public AsyncTask(Consumer<AsyncTask> consumer, long delay, long period) {
        this(AsyncUtils.getNextTaskId(), consumer, delay, period);
    }

    public AsyncTask(long id, Consumer<AsyncTask> consumer, long delay) {
        this(id, consumer, delay, -1);
    }

    public AsyncTask(Consumer<AsyncTask> consumer, long delay) {
        this(AsyncUtils.getNextTaskId(), consumer, delay);
    }

    public AsyncTask(long id, Consumer<AsyncTask> consumer) {
        this(id, consumer, 0, -1);
    }

    public AsyncTask(Consumer<AsyncTask> consumer) {
        this(AsyncUtils.getNextTaskId(), consumer);
    }

    public long getWarpedTicks() {
        if (timesRan == 0) {
            return ticksLived;
        }

        return ticksLived - (period * timesRan);
    }

    public long getNeededTicks() {
        if (delay > 0 && timesRan == 0) {
            return delay;
        }

        if (period > 0) {
            return period;
        }

        return -1;
    }

    public CompletableFuture<Void> executeAsync() {
        return AsyncUtils.executeAsync(this::execute);
    }

    public CompletableFuture<Void> tick(boolean runAsync) {
        ticksLived ++;
        if (currentDelay > 0) {
            currentDelay --;
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = doExecute(runAsync);

        if (isCompleted()) {
            remove();
        }

        return future;
    }

    public CompletableFuture<Void> doExecute(boolean runAsync) {
        timesRan ++;
        currentDelay = period;

        if (runAsync) {
            return executeAsync();
        } else {
            execute();

            return CompletableFuture.completedFuture(null);
        }
    }

    public long queue() {
        start();
        return AsyncUtils.queueTask(this);
    }

    public void remove() {
        stop();
        AsyncUtils.removeTask(this);
    }

    public CompletableFuture<Void> tick() {
        return tick(true);
    }

    public void execute() {
        consumer.accept(this);
    }

    public boolean completedAtLeast(int times) {
        return timesRan >= times;
    }

    public boolean completedAtLeastOnce() {
        return completedAtLeast(1);
    }

    public boolean isCompleted() {
        if (isRepeatable()) return false;

        return completedAtLeastOnce();
    }

    public boolean isRepeatable() {
        return period > -1;
    }
}
