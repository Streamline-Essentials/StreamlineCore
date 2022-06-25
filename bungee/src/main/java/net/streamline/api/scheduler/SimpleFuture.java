package net.streamline.api.scheduler;

import net.streamline.api.modules.Module;

import java.util.concurrent.*;

class SimpleFuture<T> extends SimpleTask implements Future<T> {

    private final Callable<T> callable;
    private T value;
    private Exception exception = null;

    SimpleFuture(final Callable<T> callable, final Module module, final int id) {
        super(module, null, id, SimpleTask.NO_REPEATING);
        this.callable = callable;
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (getPeriod() != SimpleTask.NO_REPEATING) {
            return false;
        }
        setPeriod(SimpleTask.CANCEL);
        return true;
    }

    @Override
    public boolean isDone() {
        final long period = this.getPeriod();
        return period != SimpleTask.NO_REPEATING && period != SimpleTask.PROCESS_FOR_FUTURE;
    }

    @Override
    public T get() throws CancellationException, InterruptedException, ExecutionException {
        try {
            return get(0, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            throw new Error(e);
        }
    }

    @Override
    public synchronized T get(long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        timeout = unit.toMillis(timeout);
        long period = this.getPeriod();
        long timestamp = timeout > 0 ? System.currentTimeMillis() : 0L;
        while (true) {
            if (period == SimpleTask.NO_REPEATING || period == SimpleTask.PROCESS_FOR_FUTURE) {
                this.wait(timeout);
                period = this.getPeriod();
                if (period == SimpleTask.NO_REPEATING || period == SimpleTask.PROCESS_FOR_FUTURE) {
                    if (timeout == 0L) {
                        continue;
                    }
                    timeout += timestamp - (timestamp = System.currentTimeMillis());
                    if (timeout > 0) {
                        continue;
                    }
                    throw new TimeoutException();
                }
            }
            if (period == SimpleTask.CANCEL) {
                throw new CancellationException();
            }
            if (period == SimpleTask.DONE_FOR_FUTURE) {
                if (exception == null) {
                    return value;
                }
                throw new ExecutionException(exception);
            }
            throw new IllegalStateException("Expected " + SimpleTask.NO_REPEATING + " to " + SimpleTask.DONE_FOR_FUTURE + ", got " + period);
        }
    }

    @Override
    public void run() {
        synchronized (this) {
            if (getPeriod() == SimpleTask.CANCEL) {
                return;
            }
            setPeriod(SimpleTask.PROCESS_FOR_FUTURE);
        }
        try {
            value = callable.call();
        } catch (final Exception e) {
            exception = e;
        } finally {
            synchronized (this) {
                setPeriod(SimpleTask.DONE_FOR_FUTURE);
                this.notifyAll();
            }
        }
    }

    @Override
    synchronized boolean cancel0() {
        if (getPeriod() != SimpleTask.NO_REPEATING) {
            return false;
        }
        setPeriod(SimpleTask.CANCEL);
        notifyAll();
        return true;
    }
}
