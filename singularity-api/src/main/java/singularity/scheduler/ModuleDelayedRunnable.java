package singularity.scheduler;

import singularity.modules.ModuleLike;

/**
 * A one-shot {@link ModuleRunnable} that executes once after a configurable
 * delay and then cancels itself automatically.
 *
 * <p>Subclasses implement the actual deferred logic in {@link #runDelayed()}.
 */
public abstract class ModuleDelayedRunnable extends ModuleRunnable {

    /**
     * Creates a new delayed runnable that is associated with the given module and will
     * execute exactly once after {@code delay} ticks.
     *
     * @param module the module that owns this task
     * @param delay  the number of ticks to wait before executing
     */
    public ModuleDelayedRunnable(ModuleLike module, long delay) {
        super(module, delay, 0);
    }

    /**
     * Invokes {@link #runDelayed()} and immediately cancels this task so it
     * does not repeat.
     */
    @Override
    public void run() {
        runDelayed();

        this.cancel();
    }

    /**
     * Performs the work that should be executed once after the configured delay.
     * Implementations must not call {@link #cancel()} themselves — cancellation
     * is handled automatically by {@link #run()}.
     */
    public abstract void runDelayed();
}
