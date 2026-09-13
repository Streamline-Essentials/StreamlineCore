package host.plas.timers;

import host.plas.StreamlineMessaging;
import host.plas.database.MyLoader;
import host.plas.savables.SavableChatter;
import singularity.scheduler.ModuleRunnable;

public class ChatterSaver extends ModuleRunnable {
    public ChatterSaver() {
        super(StreamlineMessaging.getInstance(), 0L, 1200L);
    }

    @Override
    public void run() {
        MyLoader.getInstance().getLoaded().forEach(SavableChatter::save);
    }
}
