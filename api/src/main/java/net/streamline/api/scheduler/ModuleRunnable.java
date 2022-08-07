package net.streamline.api.scheduler;

import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.modules.StreamlineModule;

public abstract class ModuleRunnable extends BaseRunnable {
    public StreamlineModule module;

    public ModuleRunnable(StreamlineModule module, long delay, long period) {
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
