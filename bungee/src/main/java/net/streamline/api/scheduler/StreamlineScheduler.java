package net.streamline.api.scheduler;

import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface StreamlineScheduler {

    /**
     * Schedules a once off task to occur after a delay.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param module Module that owns the task
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing task
     * @return Task id number (-1 if scheduling failed)
     */
    public int scheduleSyncDelayedTask(@NotNull Module module, @NotNull Runnable task, long delay);

    /**
     * @param module Module that owns the task
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing task
     * @return Task id number (-1 if scheduling failed)
     * @deprecated Use {@link StreamlineRunnable#runTaskLater(Module, long)}
     */
    @Deprecated
    public int scheduleSyncDelayedTask(@NotNull Module module, @NotNull StreamlineRunnable task, long delay);

    /**
     * Schedules a once off task to occur as soon as possible.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param module Module that owns the task
     * @param task Task to be executed
     * @return Task id number (-1 if scheduling failed)
     */
    public int scheduleSyncDelayedTask(@NotNull Module module, @NotNull Runnable task);

    /**
     * @param module Module that owns the task
     * @param task Task to be executed
     * @return Task id number (-1 if scheduling failed)
     * @deprecated Use {@link StreamlineRunnable#runTask(Module)}
     */
    @Deprecated
    public int scheduleSyncDelayedTask(@NotNull Module module, @NotNull StreamlineRunnable task);

    /**
     * Schedules a repeating task.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param module Module that owns the task
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing first repeat
     * @param period Period in server ticks of the task
     * @return Task id number (-1 if scheduling failed)
     */
    public int scheduleSyncRepeatingTask(@NotNull Module module, @NotNull Runnable task, long delay, long period);

    /**
     * @param module Module that owns the task
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing first repeat
     * @param period Period in server ticks of the task
     * @return Task id number (-1 if scheduling failed)
     * @deprecated Use {@link StreamlineRunnable#runTaskTimer(Module, long, long)}
     */
    @Deprecated
    public int scheduleSyncRepeatingTask(@NotNull Module module, @NotNull StreamlineRunnable task, long delay, long period);

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules a once off task to occur after a delay. This task will be
     * executed by a thread managed by the scheduler.
     *
     * @param module Module that owns the task
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing task
     * @return Task id number (-1 if scheduling failed)
     * @deprecated This name is misleading, as it does not schedule "a sync"
     *     task, but rather, "an async" task
     */
    @Deprecated
    public int scheduleAsyncDelayedTask(@NotNull Module module, @NotNull Runnable task, long delay);

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules a once off task to occur as soon as possible. This task will
     * be executed by a thread managed by the scheduler.
     *
     * @param module Module that owns the task
     * @param task Task to be executed
     * @return Task id number (-1 if scheduling failed)
     * @deprecated This name is misleading, as it does not schedule "a sync"
     *     task, but rather, "an async" task
     */
    @Deprecated
    public int scheduleAsyncDelayedTask(@NotNull Module module, @NotNull Runnable task);

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Schedules a repeating task. This task will be executed by a thread
     * managed by the scheduler.
     *
     * @param module Module that owns the task
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing first repeat
     * @param period Period in server ticks of the task
     * @return Task id number (-1 if scheduling failed)
     * @deprecated This name is misleading, as it does not schedule "a sync"
     *     task, but rather, "an async" task
     */
    @Deprecated
    public int scheduleAsyncRepeatingTask(@NotNull Module module, @NotNull Runnable task, long delay, long period);

    /**
     * Calls a method on the main thread and returns a Future object. This
     * task will be executed by the main server thread.
     * <ul>
     * <li>Note: The Future.get() methods must NOT be called from the main
     *     thread.
     * <li>Note2: There is at least an average of 10ms latency until the
     *     isDone() method returns true.
     * </ul>
     * @param <T> The callable's return type
     * @param module Module that owns the task
     * @param task Task to be executed
     * @return Future Future object related to the task
     */
    @NotNull
    public <T> Future<T> callSyncMethod(@NotNull Module module, @NotNull Callable<T> task);

    /**
     * Removes task from scheduler.
     *
     * @param taskId Id number of task to be removed
     */
    public void cancelTask(int taskId);

    /**
     * Removes all tasks associated with a particular module from the
     * scheduler.
     *
     * @param module Owner of tasks to be removed
     */
    public void cancelTasks(@NotNull Module module);

    /**
     * Check if the task currently running.
     * <p>
     * A repeating task might not be running currently, but will be running in
     * the future. A task that has finished, and does not repeat, will not be
     * running ever again.
     * <p>
     * Explicitly, a task is running if there exists a thread for it, and that
     * thread is alive.
     *
     * @param taskId The task to check.
     * <p>
     * @return If the task is currently running.
     */
    public boolean isCurrentlyRunning(int taskId);

    /**
     * Check if the task queued to be run later.
     * <p>
     * If a repeating task is currently running, it might not be queued now
     * but could be in the future. A task that is not queued, and not running,
     * will not be queued again.
     *
     * @param taskId The task to check.
     * <p>
     * @return If the task is queued to be run.
     */
    public boolean isQueued(int taskId);

    /**
     * Returns a list of all active workers.
     * <p>
     * This list contains asynch tasks that are being executed by separate
     * threads.
     *
     * @return Active workers
     */
    @NotNull
    public List<StreamlineWorker> getActiveWorkers();

    /**
     * Returns a list of all pending tasks. The ordering of the tasks is not
     * related to their order of execution.
     *
     * @return Active workers
     */
    @NotNull
    public List<StreamlineTask> getPendingTasks();

    /**
     * Returns a task that will run on the next server tick.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    @NotNull
    public StreamlineTask runTask(@NotNull Module module, @NotNull Runnable task) throws IllegalArgumentException;

    /**
     * Returns a task that will run on the next server tick.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    public void runTask(@NotNull Module module, @NotNull Consumer<StreamlineTask> task) throws IllegalArgumentException;

    /**
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     * @deprecated Use {@link StreamlineRunnable#runTask(Module)}
     */
    @Deprecated
    @NotNull
    public StreamlineTask runTask(@NotNull Module module, @NotNull StreamlineRunnable task) throws IllegalArgumentException;

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will run asynchronously.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    @NotNull
    public StreamlineTask runTaskAsynchronously(@NotNull Module module, @NotNull Runnable task) throws IllegalArgumentException;

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will run asynchronously.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    public void runTaskAsynchronously(@NotNull Module module, @NotNull Consumer<StreamlineTask> task) throws IllegalArgumentException;

    /**
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     * @deprecated Use {@link StreamlineRunnable#runTaskAsynchronously(Module)}
     */
    @Deprecated
    @NotNull
    public StreamlineTask runTaskAsynchronously(@NotNull Module module, @NotNull StreamlineRunnable task) throws IllegalArgumentException;

    /**
     * Returns a task that will run after the specified number of server
     * ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    @NotNull
    public StreamlineTask runTaskLater(@NotNull Module module, @NotNull Runnable task, long delay) throws IllegalArgumentException;

    /**
     * Returns a task that will run after the specified number of server
     * ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    public void runTaskLater(@NotNull Module module, @NotNull Consumer<StreamlineTask> task, long delay) throws IllegalArgumentException;

    /**
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     * @deprecated Use {@link StreamlineRunnable#runTaskLater(Module, long)}
     */
    @Deprecated
    @NotNull
    public StreamlineTask runTaskLater(@NotNull Module module, @NotNull StreamlineRunnable task, long delay) throws IllegalArgumentException;

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will run asynchronously after the specified number
     * of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    @NotNull
    public StreamlineTask runTaskLaterAsynchronously(@NotNull Module module, @NotNull Runnable task, long delay) throws IllegalArgumentException;

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will run asynchronously after the specified number
     * of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    public void runTaskLaterAsynchronously(@NotNull Module module, @NotNull Consumer<StreamlineTask> task, long delay) throws IllegalArgumentException;

    /**
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     * @deprecated Use {@link StreamlineRunnable#runTaskLaterAsynchronously(Module, long)}
     */
    @Deprecated
    @NotNull
    public StreamlineTask runTaskLaterAsynchronously(@NotNull Module module, @NotNull StreamlineRunnable task, long delay) throws IllegalArgumentException;

    /**
     * Returns a task that will repeatedly run until cancelled, starting after
     * the specified number of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    @NotNull
    public StreamlineTask runTaskTimer(@NotNull Module module, @NotNull Runnable task, long delay, long period) throws IllegalArgumentException;

    /**
     * Returns a task that will repeatedly run until cancelled, starting after
     * the specified number of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    public void runTaskTimer(@NotNull Module module, @NotNull Consumer<StreamlineTask> task, long delay, long period) throws IllegalArgumentException;

    /**
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     * @deprecated Use {@link StreamlineRunnable#runTaskTimer(Module, long, long)}
     */
    @Deprecated
    @NotNull
    public StreamlineTask runTaskTimer(@NotNull Module module, @NotNull StreamlineRunnable task, long delay, long period) throws IllegalArgumentException;

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task for the first
     *     time
     * @param period the ticks to wait between runs
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    @NotNull
    public StreamlineTask runTaskTimerAsynchronously(@NotNull Module module, @NotNull Runnable task, long delay, long period) throws IllegalArgumentException;

    /**
     * <b>Asynchronous tasks should never access any API in Streamline.</b> <b>Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task for the first
     *     time
     * @param period the ticks to wait between runs
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     */
    public void runTaskTimerAsynchronously(@NotNull Module module, @NotNull Consumer<StreamlineTask> task, long delay, long period) throws IllegalArgumentException;

    /**
     * @param module the reference to the module scheduling task
     * @param task the task to be run
     * @param delay the ticks to wait before running the task for the first
     *     time
     * @param period the ticks to wait between runs
     * @return a StreamlineTask that contains the id number
     * @throws IllegalArgumentException if module is null
     * @throws IllegalArgumentException if task is null
     * @deprecated Use {@link StreamlineRunnable#runTaskTimerAsynchronously(Module, long, long)}
     */
    @Deprecated
    @NotNull
    public StreamlineTask runTaskTimerAsynchronously(@NotNull Module module, @NotNull StreamlineRunnable task, long delay, long period) throws IllegalArgumentException;
}
