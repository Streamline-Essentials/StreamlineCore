package net.streamline.api.scheduler;

import net.streamline.api.modules.Module;
import net.streamline.base.Streamline;

import java.util.function.Consumer;

class SimpleTask implements StreamlineTask, Runnable {

    private volatile SimpleTask next = null;
    public static final int ERROR = 0;
    public static final int NO_REPEATING = -1;
    public static final int CANCEL = -2;
    public static final int PROCESS_FOR_FUTURE = -3;
    public static final int DONE_FOR_FUTURE = -4;
    /**
     * -1 means no repeating <br>
     * -2 means cancel <br>
     * -3 means processing for Future <br>
     * -4 means done for Future <br>
     * Never 0 <br>
     * >0 means number of ticks to wait between each execution
     */
    private volatile long period;
    private long nextRun;
    private final Runnable rTask;
    private final Consumer<StreamlineTask> cTask;
    private final Module module;
    private final int id;
    private final long createdAt = System.nanoTime();

    SimpleTask() {
        this(null, null, SimpleTask.NO_REPEATING, SimpleTask.NO_REPEATING);
    }

    SimpleTask(final Object task) {
        this(null, task, SimpleTask.NO_REPEATING, SimpleTask.NO_REPEATING);
    }

    SimpleTask(final Module module, final Object task, final int id, final long period) {
        this.module = module;
        if (task instanceof Runnable) {
            this.rTask = (Runnable) task;
            this.cTask = null;
        } else if (task instanceof Consumer) {
            this.cTask = (Consumer<StreamlineTask>) task;
            this.rTask = null;
        } else if (task == null) {
            // Head or Future task
            this.rTask = null;
            this.cTask = null;
        } else {
            throw new AssertionError("Illegal task class " + task);
        }
        this.id = id;
        this.period = period;
    }

    @Override
    public final int getTaskId() {
        return id;
    }

    @Override
    public final Module getOwner() {
        return module;
    }

    @Override
    public boolean isSync() {
        return true;
    }

    @Override
    public void run() {
        if (rTask != null) {
            rTask.run();
        } else {
            cTask.accept(this);
        }
    }

    long getCreatedAt() {
        return createdAt;
    }

    long getPeriod() {
        return period;
    }

    void setPeriod(long period) {
        this.period = period;
    }

    long getNextRun() {
        return nextRun;
    }

    void setNextRun(long nextRun) {
        this.nextRun = nextRun;
    }

    SimpleTask getNext() {
        return next;
    }

    void setNext(SimpleTask next) {
        this.next = next;
    }

    Class<?> getTaskClass() {
        return (rTask != null) ? rTask.getClass() : ((cTask != null) ? cTask.getClass() : null);
    }

    @Override
    public boolean isCancelled() {
        return (period == SimpleTask.CANCEL);
    }

    @Override
    public void cancel() {
        Streamline.getInstance().getScheduler().cancelTask(id);
    }

    /**
     * This method properly sets the status to cancelled, synchronizing when required.
     *
     * @return false if it is a craft future task that has already begun execution, true otherwise
     */
    boolean cancel0() {
        setPeriod(SimpleTask.CANCEL);
        return true;
    }
}
