package net.streamline.api.scheduler;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.modules.ModuleUtils;

import java.util.Date;

@Getter
public abstract class BaseRunnable implements Runnable {
    @Setter
    private Date startedAt;
    @Setter
    private long currentTickCount;
    @Setter
    private long period;
    @Setter
    private int index;

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

    public boolean isCancelled() {
        return ! ModuleUtils.getMainScheduler().currentRunnables.containsKey(this.index);
    }
}
