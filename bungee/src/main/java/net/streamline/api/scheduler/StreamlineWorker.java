package net.streamline.api.scheduler;

import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a worker thread for the scheduler. This gives information about
 * the Thread object for the task, owner of the task and the taskId.
 * <p>
 * Workers are used to execute async tasks.
 */
public interface StreamlineWorker {

    /**
     * Returns the taskId for the task being executed by this worker.
     *
     * @return Task id number
     */
    public int getTaskId();

    /**
     * Returns the Module that owns this task.
     *
     * @return The Module that owns the task
     */
    @NotNull
    public Module getOwner();

    /**
     * Returns the thread for the worker.
     *
     * @return The Thread object for the worker
     */
    @NotNull
    public Thread getThread();

}
