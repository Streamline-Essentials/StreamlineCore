package net.streamline.api.scheduler;

import net.streamline.base.Streamline;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public abstract class BaseRunnable implements Runnable {
    public Date startedAt;
    public long currentTickCount;
    public long period;
    public int index;

    /**
     * Constructor for all Streamline API-ed Runnables.
     *
     * @param delay the delay of the task (in milliseconds)
     * @param period the period of the task (in milliseconds)
     */
    public BaseRunnable(long delay, long period) {
        this.startedAt = new Date();
        this.currentTickCount = - delay;
        this.period = period;
        this.index = Streamline.getMainScheduler().getNextIndex();

        Streamline.getMainScheduler().start(this);
    }

    public void tick() {
        if (this.currentTickCount >= this.period) {
            this.currentTickCount = 0;
            this.run();
        }

        this.currentTickCount ++;
    }

    public void cancel() {
        Streamline.getMainScheduler().cancel(this);
    }
}
