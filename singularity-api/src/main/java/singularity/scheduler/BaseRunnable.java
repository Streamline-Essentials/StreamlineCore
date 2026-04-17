package singularity.scheduler;

import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

import java.util.Date;

/**
 * Abstract base class for all Streamline scheduler tasks.
 *
 * <p>A {@code BaseRunnable} tracks its own tick count and invokes {@link #run()} once
 * every {@code period} ticks, after an initial {@code delay}-tick delay.  Each instance
 * is automatically registered with {@link TaskManager} on construction and can be
 * cancelled at any time via {@link #cancel()}.
 */
@Setter
@Getter
public abstract class BaseRunnable implements Runnable {
    /** The timestamp at which this runnable was created and registered. */
    private Date startedAt;

    /**
     * The running tick counter.  Starts at {@code -delay} so that the first
     * execution occurs exactly {@code delay} ticks after construction.
     */
    private long currentTickCount;

    /** The number of ticks between consecutive executions of {@link #run()}. */
    private long period;

    /** The unique index assigned by {@link TaskManager} used to identify this task. */
    private int index;

    /**
     * Constructor for all Streamline API-ed Runnables.
     *
     * @param delay the delay of the task (in ticks)
     * @param period the period of the task (in ticks)
     */
    public BaseRunnable(long delay, long period) {
        this.startedAt = new Date();
        this.currentTickCount = delay * -1;
        this.period = period;
        this.index = TaskManager.getNextIndex();

        TaskManager.start(this);
    }

    /**
     * Advances the tick counter by one and executes {@link #run()} when the counter
     * reaches the configured {@code period}.  Any {@link Throwable} thrown by
     * {@link #run()} is caught and logged at debug level so that a single bad tick
     * does not break the scheduler loop.
     */
    public void tick() {
        if (this.currentTickCount >= this.period) {
            this.currentTickCount = 0;
            try {
                this.run();
            } catch (Throwable e) {
                MessageUtils.logDebug("Error while ticking runnable: " + this, e);
            }
        }

        this.currentTickCount ++;
    }

    /**
     * Cancels this runnable by removing it from the {@link TaskManager}.
     * After this call, {@link #isCancelled()} returns {@code true} and
     * the task will no longer be ticked.
     */
    public void cancel() {
        TaskManager.cancel(this);
    }

    /**
     * Checks whether this runnable has been cancelled.
     *
     * @return {@code true} if this task is no longer registered with {@link TaskManager}
     */
    public boolean isCancelled() {
        return ! TaskManager.getCurrentRunnables().containsKey(this.index);
    }
}
