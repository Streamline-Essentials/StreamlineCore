package gg.drak.thebase.async;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import gg.drak.thebase.objects.handling.IEventable;

import java.util.concurrent.CompletableFuture;

@Getter @Setter
public class SyncInstance implements Comparable<SyncInstance> {
    private IEventable eventable;
    private WithSync withSync;

    public SyncInstance(IEventable eventable, WithSync withSync) {
        this.eventable = eventable;
        this.withSync = withSync;
    }

    public String getIdentifier() {
        return getEventable().getIdentifier();
    }

    public CompletableFuture<Void> runSync(Runnable runnable, long delay) {
        return getWithSync().runSync(runnable, delay);
    }

    public CompletableFuture<Void> runSync(Runnable runnable, long delay, long period) {
        return getWithSync().runSync(runnable, delay, period);
    }

    public CompletableFuture<Void> executeSync(Runnable runnable) {
        return getWithSync().executeSync(runnable);
    }

    public boolean isSync() {
        return getWithSync().isSync();
    }

    @Override
    public int compareTo(@NotNull SyncInstance o) {
        return getIdentifier().compareTo(o.getIdentifier());
    }
}
