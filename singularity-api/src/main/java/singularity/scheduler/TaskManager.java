package singularity.scheduler;

import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

import javax.swing.*;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Global scheduler that drives all {@link BaseRunnable} tasks via a
 * {@link javax.swing.Timer} that fires every 50 ms (one Minecraft tick).
 *
 * <p>Tasks are stored in a thread-safe {@link ConcurrentSkipListMap} keyed by their
 * unique index.  {@link #init()} must be called once during server startup to start
 * the underlying timer, and {@link #stop()} should be called on shutdown.
 */
public class TaskManager {

    /**
     * All currently active runnables, keyed by their unique task index.
     * The skip-list map ensures stable iteration order and safe concurrent modification.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<Integer, BaseRunnable> currentRunnables = new ConcurrentSkipListMap<>();

    /**
     * The Swing timer that fires the tick loop every 50 ms.
     * Set during {@link #init()} and stopped during {@link #stop()}.
     */
    @Getter @Setter
    private static Timer timer;

    /**
     * Registers a {@link BaseRunnable} so it will be ticked on each timer interval.
     *
     * @param runnable the runnable to register; must not be {@code null}
     */
    public static void start(BaseRunnable runnable) {
        currentRunnables.put(runnable.getIndex(), runnable);
    }

    /**
     * Cancels a runnable by its {@link BaseRunnable} reference.
     *
     * @param runnable the runnable to cancel; must not be {@code null}
     */
    public static void cancel(BaseRunnable runnable) {
        cancel(runnable.getIndex());
    }

    /**
     * Returns the next available task index, which is the current number of
     * registered runnables.
     *
     * @return the next index to assign to a new task
     */
    public static int getNextIndex() {
        return currentRunnables.size();
    }

    /**
     * Advances every registered runnable by one tick.  Any {@link Throwable}
     * thrown by an individual task is caught and logged so that one failing task
     * cannot interrupt the rest of the tick loop.
     */
    public static void tick() {
        for (BaseRunnable runnable : currentRunnables.values()) {
            try {
                runnable.tick();
            } catch (Throwable e) {
                MessageUtils.logDebug("Error while ticking runnable: " + runnable, e);
            }
        }
    }

    /**
     * Stops the underlying timer and clears all registered runnables.
     * Should be called during server shutdown.
     */
    public static void stop() {
        timer.stop();
        currentRunnables.clear();
    }

    /**
     * Checks whether the task with the given index has been cancelled (i.e., is absent
     * from the registry).
     *
     * @param index the task index to query
     * @return {@code true} if no task with this index is currently registered
     */
    public static boolean isCancelled(int index) {
        return ! currentRunnables.containsKey(index);
    }

    /**
     * Removes the task with the given index from the registry, preventing it from
     * receiving further ticks.
     *
     * @param index the unique index of the task to cancel
     */
    public static void cancel(int index) {
        currentRunnables.remove(index);
    }

    /**
     * Returns the currently registered runnable with the given index, or
     * {@code null} if no such task exists.
     *
     * @param index the unique task index to look up
     * @return the matching {@link BaseRunnable}, or {@code null}
     */
    public static BaseRunnable getRunnable(int index) {
        return currentRunnables.get(index);
    }

    /**
     * Initialises the task manager by creating and starting the 50 ms Swing timer.
     * Must be called once during server startup before any {@link BaseRunnable} is constructed.
     */
    public static void init() {
        timer = new Timer(50, e -> tick());
        timer.start();

        MessageUtils.logInfo("&cTaskManager &fis now initialized!");
    }
}
