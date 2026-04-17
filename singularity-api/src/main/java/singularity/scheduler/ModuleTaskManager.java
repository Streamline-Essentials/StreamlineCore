package singularity.scheduler;

import singularity.modules.ModuleLike;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks all active {@link ModuleRunnable} tasks organised by their owning
 * {@link ModuleLike} module.
 *
 * <p>This manager is the per-module counterpart to the global {@link TaskManager}.
 * It allows all tasks belonging to a specific module to be cancelled at once
 * (e.g., during module disable) and provides a unified tick-forward entry point
 * that drives every registered task each server tick.
 */
public class ModuleTaskManager {

    /**
     * The live registry of module tasks, keyed first by module and then by the
     * task's unique index.  Using a {@link ConcurrentHashMap} allows concurrent
     * reads during the tick loop while tasks register or cancel themselves.
     */
    public ConcurrentHashMap<ModuleLike, TreeMap<Integer, ModuleRunnable>> currentRunnables = new ConcurrentHashMap<>();

    /**
     * Registers a {@link ModuleRunnable} with this manager so it will be ticked
     * on every call to {@link #tick()}.
     *
     * @param moduleRunnable the runnable to register; must not be {@code null}
     */
    public void start(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.getModule());
        if (map == null) map = new TreeMap<>();

        map.put(moduleRunnable.getIndex(), moduleRunnable);

        currentRunnables.put(moduleRunnable.getModule(), map);
    }

    /**
     * Removes a {@link ModuleRunnable} from this manager so it will no longer
     * be ticked.  The runnable is not cancelled at the global level; callers
     * that need both levels removed should use {@link ModuleRunnable#cancel()}.
     *
     * @param moduleRunnable the runnable to deregister; must not be {@code null}
     */
    public void cancel(ModuleRunnable moduleRunnable) {
        TreeMap<Integer, ModuleRunnable> map = currentRunnables.get(moduleRunnable.getModule());
        if (map == null) map = new TreeMap<>();

        map.remove(moduleRunnable.getIndex());

        currentRunnables.put(moduleRunnable.getModule(), map);
    }

    /**
     * Cancels every task that is currently registered under the given module.
     * This is typically called during module shutdown to clean up all pending tasks.
     *
     * @param module the module whose tasks should all be cancelled; must not be {@code null}
     */
    public void cancelAll(ModuleLike module) {
        for (ModuleRunnable runnable : currentRunnables.get(module).values()) {
            runnable.cancel();
        }
    }

    /**
     * Advances every registered module runnable by one tick.
     * This method is called by the global tick loop once per server tick.
     */
    public void tick() {
        for (ModuleLike module : currentRunnables.keySet()) {
            for (ModuleRunnable runnable : currentRunnables.get(module).values()) {
                runnable.tick();
            }
        }
    }
}
