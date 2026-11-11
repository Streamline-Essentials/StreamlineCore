package gg.drak.thebase.async;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter @Setter
public class SyncTask implements TaskLike<SyncTask> {
    private long id;

    private long delay;
    private long period;

    private Consumer<SyncTask> consumer;

    public SyncTask(long id, Consumer<SyncTask> consumer, long delay, long period) {
        this.id = id;
        this.consumer = consumer;
        this.delay = delay;
        this.period = period;
    }

    public SyncTask(Consumer<SyncTask> consumer, long delay, long period) {
        this(AsyncUtils.getNextTaskId(), consumer, delay, period);
    }

    public SyncTask(long id, Consumer<SyncTask> consumer, long delay) {
        this(id, consumer, delay, -1);
    }

    public SyncTask(Consumer<SyncTask> consumer, long delay) {
        this(AsyncUtils.getNextTaskId(), consumer, delay);
    }

    public SyncTask(long id, Consumer<SyncTask> consumer) {
        this(id, consumer, -1, -1);
    }

    public SyncTask(Consumer<SyncTask> consumer) {
        this(AsyncUtils.getNextTaskId(), consumer);
    }

    public CompletableFuture<Void> runThreaded() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getConsumer().accept(this);
        future.complete(null);

        return future;
    }
}
