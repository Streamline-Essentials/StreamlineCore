package net.streamline.api.scheduler;

import lombok.Getter;
import net.streamline.api.modules.ModuleLike;
import net.streamline.api.modules.ModuleUtils;

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
