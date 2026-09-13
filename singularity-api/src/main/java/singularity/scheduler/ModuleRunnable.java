package singularity.scheduler;

import lombok.Getter;
import singularity.modules.ModuleLike;
import singularity.modules.ModuleUtils;

/**
 * A {@link BaseRunnable} that is bound to a specific {@link ModuleLike} module.
 *
 * <p>On construction the task registers itself with both the global {@link TaskManager}
 * (via the parent constructor) and the per-module {@link ModuleTaskManager}.  Cancelling
 * also removes the task from both registries so that module-level shutdown can cleanly
 * stop all module-owned tasks.
 */
@Getter
public abstract class ModuleRunnable extends BaseRunnable {

    /** The module that owns this runnable. */
    private final ModuleLike module;

    /**
     * Creates a repeating module-bound runnable.
     *
     * @param module the module that owns this task; must not be {@code null}
     * @param delay  the initial delay in ticks before the first execution
     * @param period the number of ticks between subsequent executions
     */
    public ModuleRunnable(ModuleLike module, long delay, long period) {
        super(delay, period);
        this.module = module;
        ModuleUtils.getModuleScheduler().start(this);
    }

    /**
     * Cancels this task from both the global {@link TaskManager} and the
     * per-module {@link ModuleTaskManager}.
     */
    @Override
    public void cancel() {
        super.cancel();
        ModuleUtils.getModuleScheduler().cancel(this);
    }
}
