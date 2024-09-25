package singularity.scheduler;

import singularity.modules.ModuleLike;

public abstract class ModuleDelayedRunnable extends ModuleRunnable {
    public ModuleDelayedRunnable(ModuleLike module, long delay) {
        super(module, delay, 0);
    }

    @Override
    public void run() {
        runDelayed();

        this.cancel();
    }

    public abstract void runDelayed();
}
