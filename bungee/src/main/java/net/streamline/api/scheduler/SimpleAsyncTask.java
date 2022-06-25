package net.streamline.api.scheduler;

import net.streamline.api.modules.Module;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Level;

class SimpleAsyncTask extends SimpleTask {

    private final LinkedList<StreamlineWorker> workers = new LinkedList<StreamlineWorker>();
    private final Map<Integer, SimpleTask> runners;

    SimpleAsyncTask(final Map<Integer, SimpleTask> runners, final Module module, final Object task, final int id, final long delay) {
        super(module, task, id, delay);
        this.runners = runners;
    }

    @Override
    public boolean isSync() {
        return false;
    }

    @Override
    public void run() {
        final Thread thread = Thread.currentThread();
        synchronized (workers) {
            if (getPeriod() == SimpleTask.CANCEL) {
                // Never continue running after cancelled.
                // Checking this with the lock is important!
                return;
            }
            workers.add(
                    new StreamlineWorker() {
                        @Override
                        public Thread getThread() {
                            return thread;
                        }

                        @Override
                        public int getTaskId() {
                            return SimpleAsyncTask.this.getTaskId();
                        }

                        @Override
                        public @NotNull Module getOwner() {
                            return SimpleAsyncTask.this.getOwner();
                        }
                    });
        }
        Throwable thrown = null;
        try {
            super.run();
        } catch (final Throwable t) {
            thrown = t;
            getOwner().getLogger().log(
                    Level.WARNING,
                    String.format(
                            "Module %s generated an exception while executing task %s",
                            getOwner().getDescription().getFullName(),
                            getTaskId()),
                    thrown);
        } finally {
            // Cleanup is important for any async task, otherwise ghost tasks are everywhere
            synchronized (workers) {
                try {
                    final Iterator<StreamlineWorker> workers = this.workers.iterator();
                    boolean removed = false;
                    while (workers.hasNext()) {
                        if (workers.next().getThread() == thread) {
                            workers.remove();
                            removed = true; // Don't throw exception
                            break;
                        }
                    }
                    if (!removed) {
                        throw new IllegalStateException(
                                String.format(
                                        "Unable to remove worker %s on task %s for %s",
                                        thread.getName(),
                                        getTaskId(),
                                        getOwner().getDescription().getFullName()),
                                thrown); // We don't want to lose the original exception, if any
                    }
                } finally {
                    if (getPeriod() < 0 && workers.isEmpty()) {
                        // At this spot, we know we are the final async task being executed!
                        // Because we have the lock, nothing else is running or will run because delay < 0
                        runners.remove(getTaskId());
                    }
                }
            }
        }
    }

    LinkedList<StreamlineWorker> getWorkers() {
        return workers;
    }

    @Override
    boolean cancel0() {
        synchronized (workers) {
            // Synchronizing here prevents race condition for a completing task
            setPeriod(SimpleTask.CANCEL);
            if (workers.isEmpty()) {
                runners.remove(getTaskId());
            }
        }
        return true;
    }
}
