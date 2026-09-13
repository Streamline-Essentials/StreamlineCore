package singularity.scheduler;

/**
 * A {@link BaseRunnable} that executes its work exactly once after an initial delay
 * and then cancels itself automatically.
 *
 * <p>The delay is expressed in ticks and forwarded to the parent {@link BaseRunnable}
 * as a delay with a period of {@code 0}. When the scheduler fires {@link #run()},
 * it delegates to {@link #runDelayed()} and immediately calls {@link #cancel()} so
 * the task is never repeated.</p>
 *
 * <p>Subclasses must implement {@link #runDelayed()} with the logic to execute after
 * the delay elapses.</p>
 */
public abstract class BaseDelayedRunnable extends BaseRunnable {

    /**
     * Creates a delayed, one-shot runnable that fires after {@code delay} ticks.
     *
     * @param delay the number of ticks to wait before executing {@link #runDelayed()}
     */
    public BaseDelayedRunnable(long delay) {
        super(delay, 0);
    }

    /**
     * Invoked by the scheduler when the delay has elapsed. Calls {@link #runDelayed()}
     * and then cancels the task so it does not repeat.
     */
    @Override
    public void run() {
        runDelayed();

        this.cancel();
    }

    /**
     * Contains the logic to execute once after the configured delay.
     * Implementations must not call {@link #cancel()} themselves; cancellation is
     * handled automatically by {@link #run()}.
     */
    public abstract void runDelayed();
}
