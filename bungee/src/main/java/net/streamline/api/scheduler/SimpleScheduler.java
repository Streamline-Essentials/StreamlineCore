package net.streamline.api.scheduler;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.streamline.api.modules.IllegalModuleAccessException;
import net.streamline.api.modules.Module;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.logging.Level;

/**
 * The fundamental concepts for this implementation:
 * <li>Main thread owns {@link #head} and {@link #currentTick}, but it may be read from any thread</li>
 * <li>Main thread exclusively controls {@link #temp} and {@link #pending}.
 *     They are never to be accessed outside of the main thread; alternatives exist to prevent locking.</li>
 * <li>{@link #head} to {@link #tail} act as a linked list/queue, with 1 consumer and infinite producers.
 *     Adding to the tail is atomic and very efficient; utility method is {@link #handle(SimpleTask, long)} or {@link #addTask(SimpleTask)}. </li>
 * <li>Changing the period on a task is delicate.
 *     Any future task needs to notify waiting threads.
 *     Async tasks must be synchronized to make sure that any thread that's finishing will remove itself from {@link #runners}.
 *     Another utility method is provided for this, {@link #cancelTask(int)}</li>
 * <li>{@link #runners} provides a moderately up-to-date view of active tasks.
 *     If the linked head to tail set is read, all remaining tasks that were active at the time execution started will be located in runners.</li>
 * <li>Async tasks are responsible for removing themselves from runners</li>
 * <li>Sync tasks are only to be removed from runners on the main thread when coupled with a removal from pending and temp.</li>
 * <li>Most of the design in this scheduler relies on queuing special tasks to perform any data changes on the main thread.
 *     When executed from inside a synchronous method, the scheduler will be updated before next execution by virtue of the frequent {@link #parsePending()} calls.</li>
 */
public class SimpleScheduler implements StreamlineScheduler {

    /**
     * The start ID for the counter.
     */
    private static final int START_ID = 1;
    /**
     * Increment the {@link #ids} field and reset it to the {@link #START_ID} if it reaches {@link Integer#MAX_VALUE}
     */
    private static final IntUnaryOperator INCREMENT_IDS = previous -> {
        // We reached the end, go back to the start!
        if (previous == Integer.MAX_VALUE) {
            return START_ID;
        }
        return previous + 1;
    };
    /**
     * Counter for IDs. Order doesn't matter, only uniqueness.
     */
    private final AtomicInteger ids = new AtomicInteger(START_ID);
    /**
     * Current head of linked-list. This reference is always stale, {@link SimpleTask#getNext()} is the live reference.
     */
    private volatile SimpleTask head = new SimpleTask();
    /**
     * Tail of a linked-list. AtomicReference only matters when adding to queue
     */
    private final AtomicReference<SimpleTask> tail = new AtomicReference<SimpleTask>(head);
    /**
     * Main thread logic only
     */
    private final PriorityQueue<SimpleTask> pending = new PriorityQueue<SimpleTask>(10,
            new Comparator<SimpleTask>() {
                @Override
                public int compare(final SimpleTask o1, final SimpleTask o2) {
                    int value = Long.compare(o1.getNextRun(), o2.getNextRun());

                    // If the tasks should run on the same tick they should be run FIFO
                    return value != 0 ? value : Long.compare(o1.getCreatedAt(), o2.getCreatedAt());
                }
            });
    /**
     * Main thread logic only
     */
    private final List<SimpleTask> temp = new ArrayList<SimpleTask>();
    /**
     * These are tasks that are currently active. It's provided for 'viewing' the current state.
     */
    private final ConcurrentHashMap<Integer, SimpleTask> runners = new ConcurrentHashMap<Integer, SimpleTask>();
    /**
     * The sync task that is currently running on the main thread.
     */
    private volatile SimpleTask currentTask = null;
    private volatile int currentTick = -1;
    private final Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Simple Scheduler Thread - %d").build());
    private SimpleAsyncDebugger debugHead = new SimpleAsyncDebugger(-1, null, null) {
        @Override
        StringBuilder debugTo(StringBuilder string) {
            return string;
        }
    };
    private SimpleAsyncDebugger debugTail = debugHead;
    private static final int RECENT_TICKS;

    static {
        RECENT_TICKS = 30;
    }

    @Override
    public int scheduleSyncDelayedTask(final Module module, final Runnable task) {
        return this.scheduleSyncDelayedTask(module, task, 0L);
    }

    @Override
    public StreamlineTask runTask(Module module, Runnable runnable) {
        return runTaskLater(module, runnable, 0L);
    }

    @Override
    public void runTask(Module module, Consumer<StreamlineTask> task) throws IllegalArgumentException {
        runTaskLater(module, task, 0L);
    }

    @Deprecated
    @Override
    public int scheduleAsyncDelayedTask(final Module module, final Runnable task) {
        return this.scheduleAsyncDelayedTask(module, task, 0L);
    }

    @Override
    public StreamlineTask runTaskAsynchronously(Module module, Runnable runnable) {
        return runTaskLaterAsynchronously(module, runnable, 0L);
    }

    @Override
    public void runTaskAsynchronously(Module module, Consumer<StreamlineTask> task) throws IllegalArgumentException {
        runTaskLaterAsynchronously(module, task, 0L);
    }

    @Override
    public int scheduleSyncDelayedTask(final Module module, final Runnable task, final long delay) {
        return this.scheduleSyncRepeatingTask(module, task, delay, SimpleTask.NO_REPEATING);
    }

    @Override
    public StreamlineTask runTaskLater(Module module, Runnable runnable, long delay) {
        return runTaskTimer(module, runnable, delay, SimpleTask.NO_REPEATING);
    }

    @Override
    public void runTaskLater(Module module, Consumer<StreamlineTask> task, long delay) throws IllegalArgumentException {
        runTaskTimer(module, task, delay, SimpleTask.NO_REPEATING);
    }

    @Deprecated
    @Override
    public int scheduleAsyncDelayedTask(final Module module, final Runnable task, final long delay) {
        return this.scheduleAsyncRepeatingTask(module, task, delay, SimpleTask.NO_REPEATING);
    }

    @Override
    public StreamlineTask runTaskLaterAsynchronously(Module module, Runnable runnable, long delay) {
        return runTaskTimerAsynchronously(module, runnable, delay, SimpleTask.NO_REPEATING);
    }

    @Override
    public void runTaskLaterAsynchronously(Module module, Consumer<StreamlineTask> task, long delay) throws IllegalArgumentException {
        runTaskTimerAsynchronously(module, task, delay, SimpleTask.NO_REPEATING);
    }

    @Override
    public void runTaskTimerAsynchronously(Module module, Consumer<StreamlineTask> task, long delay, long period) throws IllegalArgumentException {
        runTaskTimerAsynchronously(module, (Object) task, delay, SimpleTask.NO_REPEATING);
    }

    @Override
    public int scheduleSyncRepeatingTask(final Module module, final Runnable runnable, long delay, long period) {
        return runTaskTimer(module, runnable, delay, period).getTaskId();
    }

    @Override
    public StreamlineTask runTaskTimer(Module module, Runnable runnable, long delay, long period) {
        return runTaskTimer(module, (Object) runnable, delay, period);
    }

    @Override
    public void runTaskTimer(Module module, Consumer<StreamlineTask> task, long delay, long period) throws IllegalArgumentException {
        runTaskTimer(module, (Object) task, delay, period);
    }

    public StreamlineTask runTaskTimer(Module module, Object runnable, long delay, long period) {
        validate(module, runnable);
        if (delay < 0L) {
            delay = 0;
        }
        if (period == SimpleTask.ERROR) {
            period = 1L;
        } else if (period < SimpleTask.NO_REPEATING) {
            period = SimpleTask.NO_REPEATING;
        }
        return handle(new SimpleTask(module, runnable, nextId(), period), delay);
    }

    @Deprecated
    @Override
    public int scheduleAsyncRepeatingTask(final Module module, final Runnable runnable, long delay, long period) {
        return runTaskTimerAsynchronously(module, runnable, delay, period).getTaskId();
    }

    @Override
    public StreamlineTask runTaskTimerAsynchronously(Module module, Runnable runnable, long delay, long period) {
        return runTaskTimerAsynchronously(module, (Object) runnable, delay, period);
    }

    public StreamlineTask runTaskTimerAsynchronously(Module module, Object runnable, long delay, long period) {
        validate(module, runnable);
        if (delay < 0L) {
            delay = 0;
        }
        if (period == SimpleTask.ERROR) {
            period = 1L;
        } else if (period < SimpleTask.NO_REPEATING) {
            period = SimpleTask.NO_REPEATING;
        }
        return handle(new SimpleAsyncTask(runners, module, runnable, nextId(), period), delay);
    }

    @Override
    public <T> Future<T> callSyncMethod(final Module module, final Callable<T> task) {
        validate(module, task);
        final SimpleFuture<T> future = new SimpleFuture<T>(task, module, nextId());
        handle(future, 0L);
        return future;
    }

    @Override
    public void cancelTask(final int taskId) {
        if (taskId <= 0) {
            return;
        }
        SimpleTask task = runners.get(taskId);
        if (task != null) {
            task.cancel0();
        }
        task = new SimpleTask(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!check(SimpleScheduler.this.temp)) {
                            check(SimpleScheduler.this.pending);
                        }
                    }
                    private boolean check(final Iterable<SimpleTask> collection) {
                        final Iterator<SimpleTask> tasks = collection.iterator();
                        while (tasks.hasNext()) {
                            final SimpleTask task = tasks.next();
                            if (task.getTaskId() == taskId) {
                                task.cancel0();
                                tasks.remove();
                                if (task.isSync()) {
                                    runners.remove(taskId);
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                });
        handle(task, 0L);
        for (SimpleTask taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext()) {
            if (taskPending == task) {
                return;
            }
            if (taskPending.getTaskId() == taskId) {
                taskPending.cancel0();
            }
        }
    }

    @Override
    public void cancelTasks(final Module module) {
        Validate.notNull(module, "Cannot cancel tasks of null module");
        final SimpleTask task = new SimpleTask(
                new Runnable() {
                    @Override
                    public void run() {
                        check(SimpleScheduler.this.pending);
                        check(SimpleScheduler.this.temp);
                    }
                    void check(final Iterable<SimpleTask> collection) {
                        final Iterator<SimpleTask> tasks = collection.iterator();
                        while (tasks.hasNext()) {
                            final SimpleTask task = tasks.next();
                            if (task.getOwner().equals(module)) {
                                task.cancel0();
                                tasks.remove();
                                if (task.isSync()) {
                                    runners.remove(task.getTaskId());
                                }
                            }
                        }
                    }
                });
        handle(task, 0L);
        for (SimpleTask taskPending = head.getNext(); taskPending != null; taskPending = taskPending.getNext()) {
            if (taskPending == task) {
                break;
            }
            if (taskPending.getTaskId() != -1 && taskPending.getOwner().equals(module)) {
                taskPending.cancel0();
            }
        }
        for (SimpleTask runner : runners.values()) {
            if (runner.getOwner().equals(module)) {
                runner.cancel0();
            }
        }
    }

    @Override
    public boolean isCurrentlyRunning(final int taskId) {
        final SimpleTask task = runners.get(taskId);
        if (task == null) {
            return false;
        }
        if (task.isSync()) {
            return (task == currentTask);
        }
        final SimpleAsyncTask asyncTask = (SimpleAsyncTask) task;
        synchronized (asyncTask.getWorkers()) {
            return !asyncTask.getWorkers().isEmpty();
        }
    }

    @Override
    public boolean isQueued(final int taskId) {
        if (taskId <= 0) {
            return false;
        }
        for (SimpleTask task = head.getNext(); task != null; task = task.getNext()) {
            if (task.getTaskId() == taskId) {
                return task.getPeriod() >= SimpleTask.NO_REPEATING; // The task will run
            }
        }
        SimpleTask task = runners.get(taskId);
        return task != null && task.getPeriod() >= SimpleTask.NO_REPEATING;
    }

    @Override
    public List<StreamlineWorker> getActiveWorkers() {
        final ArrayList<StreamlineWorker> workers = new ArrayList<StreamlineWorker>();
        for (final SimpleTask taskObj : runners.values()) {
            // Iterator will be a best-effort (may fail to grab very new values) if called from an async thread
            if (taskObj.isSync()) {
                continue;
            }
            final SimpleAsyncTask task = (SimpleAsyncTask) taskObj;
            synchronized (task.getWorkers()) {
                // This will never have an issue with stale threads; it's state-safe
                workers.addAll(task.getWorkers());
            }
        }
        return workers;
    }

    @Override
    public List<StreamlineTask> getPendingTasks() {
        final ArrayList<SimpleTask> truePending = new ArrayList<SimpleTask>();
        for (SimpleTask task = head.getNext(); task != null; task = task.getNext()) {
            if (task.getTaskId() != -1) {
                // -1 is special code
                truePending.add(task);
            }
        }

        final ArrayList<StreamlineTask> pending = new ArrayList<StreamlineTask>();
        for (SimpleTask task : runners.values()) {
            if (task.getPeriod() >= SimpleTask.NO_REPEATING) {
                pending.add(task);
            }
        }

        for (final SimpleTask task : truePending) {
            if (task.getPeriod() >= SimpleTask.NO_REPEATING && !pending.contains(task)) {
                pending.add(task);
            }
        }
        return pending;
    }

    /**
     * This method is designed to never block or wait for locks; an immediate execution of all current tasks.
     */
    public void mainThreadHeartbeat(final int currentTick) {
        this.currentTick = currentTick;
        final List<SimpleTask> temp = this.temp;
        parsePending();
        while (isReady(currentTick)) {
            final SimpleTask task = pending.remove();
            if (task.getPeriod() < SimpleTask.NO_REPEATING) {
                if (task.isSync()) {
                    runners.remove(task.getTaskId(), task);
                }
                parsePending();
                continue;
            }
            if (task.isSync()) {
                currentTask = task;
                try {
                    task.run();
                } catch (final Throwable throwable) {
                    task.getOwner().getLogger().log(
                            Level.WARNING,
                            String.format(
                                    "Task #%s for %s generated an exception",
                                    task.getTaskId(),
                                    task.getOwner().getDescription().getFullName()),
                            throwable);
                } finally {
                    currentTask = null;
                }
                parsePending();
            } else {
                debugTail = debugTail.setNext(new SimpleAsyncDebugger(currentTick + RECENT_TICKS, task.getOwner(), task.getTaskClass()));
                executor.execute(task);
                // We don't need to parse pending
                // (async tasks must live with race-conditions if they attempt to cancel between these few lines of code)
            }
            final long period = task.getPeriod(); // State consistency
            if (period > 0) {
                task.setNextRun(currentTick + period);
                temp.add(task);
            } else if (task.isSync()) {
                runners.remove(task.getTaskId());
            }
        }
        pending.addAll(temp);
        temp.clear();
        debugHead = debugHead.getNextHead(currentTick);
    }

    private void addTask(final SimpleTask task) {
        final AtomicReference<SimpleTask> tail = this.tail;
        SimpleTask tailTask = tail.get();
        while (!tail.compareAndSet(tailTask, task)) {
            tailTask = tail.get();
        }
        tailTask.setNext(task);
    }

    private SimpleTask handle(final SimpleTask task, final long delay) {
        task.setNextRun(currentTick + delay);
        addTask(task);
        return task;
    }

    private static void validate(final Module module, final Object task) {
        Validate.notNull(module, "Module cannot be null");
        Validate.notNull(task, "Task cannot be null");
        Validate.isTrue(task instanceof Runnable || task instanceof Consumer || task instanceof Callable, "Task must be Runnable, Consumer, or Callable");
        if (!module.isEnabled()) {
            throw new IllegalModuleAccessException("Module attempted to register task while disabled");
        }
    }

    private int nextId() {
        Validate.isTrue(runners.size() < Integer.MAX_VALUE, "There are already " + Integer.MAX_VALUE + " tasks scheduled! Cannot schedule more.");
        int id;
        do {
            id = ids.updateAndGet(INCREMENT_IDS);
        } while (runners.containsKey(id)); // Avoid generating duplicate IDs
        return id;
    }

    private void parsePending() {
        SimpleTask head = this.head;
        SimpleTask task = head.getNext();
        SimpleTask lastTask = head;
        for (; task != null; task = (lastTask = task).getNext()) {
            if (task.getTaskId() == -1) {
                task.run();
            } else if (task.getPeriod() >= SimpleTask.NO_REPEATING) {
                pending.add(task);
                runners.put(task.getTaskId(), task);
            }
        }
        // We split this because of the way things are ordered for all of the async calls in SimpleScheduler
        // (it prevents race-conditions)
        for (task = head; task != lastTask; task = head) {
            head = task.getNext();
            task.setNext(null);
        }
        this.head = lastTask;
    }

    private boolean isReady(final int currentTick) {
        return !pending.isEmpty() && pending.peek().getNextRun() <= currentTick;
    }

    @Override
    public String toString() {
        int debugTick = currentTick;
        StringBuilder string = new StringBuilder("Recent tasks from ").append(debugTick - RECENT_TICKS).append('-').append(debugTick).append('{');
        debugHead.debugTo(string);
        return string.append('}').toString();
    }

    @Deprecated
    @Override
    public int scheduleSyncDelayedTask(Module module, StreamlineRunnable task, long delay) {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTaskLater(Module, long)");
    }

    @Deprecated
    @Override
    public int scheduleSyncDelayedTask(Module module, StreamlineRunnable task) {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTask(Module)");
    }

    @Deprecated
    @Override
    public int scheduleSyncRepeatingTask(Module module, StreamlineRunnable task, long delay, long period) {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTaskTimer(Module, long, long)");
    }

    @Deprecated
    @Override
    public StreamlineTask runTask(Module module, StreamlineRunnable task) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTask(Module)");
    }

    @Deprecated
    @Override
    public StreamlineTask runTaskAsynchronously(Module module, StreamlineRunnable task) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTaskAsynchronously(Module)");
    }

    @Deprecated
    @Override
    public StreamlineTask runTaskLater(Module module, StreamlineRunnable task, long delay) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTaskLater(Module, long)");
    }

    @Deprecated
    @Override
    public StreamlineTask runTaskLaterAsynchronously(Module module, StreamlineRunnable task, long delay) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTaskLaterAsynchronously(Module, long)");
    }

    @Deprecated
    @Override
    public StreamlineTask runTaskTimer(Module module, StreamlineRunnable task, long delay, long period) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTaskTimer(Module, long, long)");
    }

    @Deprecated
    @Override
    public StreamlineTask runTaskTimerAsynchronously(Module module, StreamlineRunnable task, long delay, long period) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Use StreamlineRunnable#runTaskTimerAsynchronously(Module, long, long)");
    }
}