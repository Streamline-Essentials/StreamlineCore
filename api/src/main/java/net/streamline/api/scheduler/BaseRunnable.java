package net.streamline.api.scheduler;

import net.streamline.api.SLAPI;
import net.streamline.api.modules.ModuleUtils;

import java.util.Date;

public abstract class BaseRunnable implements Runnable {
    public Date startedAt;
    public long currentTickCount;
    public long period;
    public int index;

    /**
     * Constructor for all Streamline API-ed Runnables.
     *
     * @param delay the delay of the task (in ticks)
     * @param period the period of the task (in ticks)
     */
    public BaseRunnable(long delay, long period) {
        this.startedAt = new Date();
        this.currentTickCount = delay * -1;
        this.period = period;
        this.index = ModuleUtils.getMainScheduler().getNextIndex();

        ModuleUtils.getMainScheduler().start(this);
    }

    public void tick() {
        if (this.currentTickCount >= this.period) {
            this.currentTickCount = 0;
            this.run();
        }

        this.currentTickCount ++;
    }

    public void cancel() {
        ModuleUtils.getMainScheduler().cancel(this);
    }
}
