package singularity.scheduler;

import lombok.Getter;
import singularity.modules.ModuleLike;
import singularity.modules.ModuleUtils;

@Getter
public abstract class ModuleRunnable extends BaseRunnable {
    private final ModuleLike module;

    public ModuleRunnable(ModuleLike module, long delay, long period) {
        super(delay, period);
        this.module = module;
        ModuleUtils.getModuleScheduler().start(this);
    }

    @Override
    public void cancel() {
        super.cancel();
        ModuleUtils.getModuleScheduler().cancel(this);
    }
}
