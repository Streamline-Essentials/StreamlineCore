package net.streamline.api.base.scheduler;

import net.streamline.api.base.Streamline;
import net.streamline.api.base.modules.Module;
import org.jetbrains.annotations.NotNull;

/**
 * This class is provided as an easy way to handle scheduling tasks.
 */
public abstract class StreamlineRunnable implements Runnable {
    private StreamlineTask task;

    /**
     * Returns true if this task has been cancelled.
     *
     * @return true if the task has been cancelled
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized boolean isCancelled() throws IllegalStateException {
        checkScheduled();
        return task.isCancelled();
    }

    /**
     * Attempts to cancel this task.
     *
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized void cancel() throws IllegalStateException {
        Streamline.getScheduler().cancelTask(getTaskId());
    }

    /**
     * Schedules this in the Streamline scheduler to run on next tick.
     *
     * @param module the reference to the module scheduling task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalStateException if this was already scheduled
     * @see StreamlineScheduler#runTask(Module, Runnable)
     */
    @NotNull
    public synchronized StreamlineTask runTask(@NotNull Module module) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Streamline.getScheduler().runTask(module, (Runnable) this));
    }

    /**
     * <b>Asynchronous tasks should never access any API in Streamline. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules this in the Streamline scheduler to run asynchronously.
     *
     * @param module the reference to the module scheduling task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalStateException if this was already scheduled
     * @see StreamlineScheduler#runTaskAsynchronously(Module, Runnable)
     */
    @NotNull
    public synchronized StreamlineTask runTaskAsynchronously(@NotNull Module module) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Streamline.getScheduler().runTaskAsynchronously(module, (Runnable) this));
    }

    /**
     * Schedules this to run after the specified number of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param delay the ticks to wait before running the task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalStateException if this was already scheduled
     * @see StreamlineScheduler#runTaskLater(Module, Runnable, long)
     */
    @NotNull
    public synchronized StreamlineTask runTaskLater(@NotNull Module module, long delay) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Streamline.getScheduler().runTaskLater(module, (Runnable) this, delay));
    }

    /**
     * <b>Asynchronous tasks should never access any API in Streamline. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules this to run asynchronously after the specified number of
     * server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param delay the ticks to wait before running the task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalStateException if this was already scheduled
     * @see StreamlineScheduler#runTaskLaterAsynchronously(Module, Runnable, long)
     */
    @NotNull
    public synchronized StreamlineTask runTaskLaterAsynchronously(@NotNull Module module, long delay) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Streamline.getScheduler().runTaskLaterAsynchronously(module, (Runnable) this, delay));
    }

    /**
     * Schedules this to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalStateException if this was already scheduled
     * @see StreamlineScheduler#runTaskTimer(Module, Runnable, long, long)
     */
    @NotNull
    public synchronized StreamlineTask runTaskTimer(@NotNull Module module, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Streamline.getScheduler().runTaskTimer(module, (Runnable) this, delay, period));
    }

    /**
     * <b>Asynchronous tasks should never access any API in Streamline. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules this to repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param delay the ticks to wait before running the task for the first
     *     time
     * @param period the ticks to wait between runs
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalStateException if this was already scheduled
     * @see StreamlineScheduler#runTaskTimerAsynchronously(Module, Runnable, long,
     *     long)
     */
    @NotNull
    public synchronized StreamlineTask runTaskTimerAsynchronously(@NotNull Module module, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Streamline.getScheduler().runTaskTimerAsynchronously(module, (Runnable) this, delay, period));
    }

    /**
     * Gets the task id for this runnable.
     *
     * @return the task id that this runnable was scheduled as
     * @throws IllegalStateException if task was not scheduled yet
     */
    public synchronized int getTaskId() throws IllegalStateException {
        checkScheduled();
        return task.getTaskId();
    }

    private void checkScheduled() {
        if (task == null) {
            throw new IllegalStateException("Not scheduled yet");
        }
    }

    private void checkNotYetScheduled() {
        if (task != null) {
            throw new IllegalStateException("Already scheduled as " + task.getTaskId());
        }
    }

    @NotNull
    private StreamlineTask setupTask(@NotNull final StreamlineTask task) {
        this.task = task;
        return task;
    }
}
