package net.streamline.api.scheduler;

import net.streamline.api.modules.StreamlineModule;
import net.streamline.base.Streamline;

public abstract class ModuleRunnable extends BaseRunnable {
    public StreamlineModule module;

    public ModuleRunnable(StreamlineModule module, long delay, long period) {
        super(delay, period);
        this.module = module;
        Streamline.getModuleScheduler().start(this);
    }

    @Override
    public void cancel() {
        super.cancel();
        Streamline.getModuleScheduler().cancel(this);
    }
}
