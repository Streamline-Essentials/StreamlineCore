package gg.drak.thebase.async;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface TaskLike<T extends TaskLike<?>> extends Comparable<TaskLike> {
    long getId();

    long getDelay();

    long getPeriod();

    Consumer<T> getConsumer();

    void setDelay(long delay);

    void setPeriod(long period);

    void setConsumer(Consumer<T> runnable);

    @Override
    default int compareTo(@NotNull TaskLike o) {
        return Long.compare(getId(), o.getId());
    }
}
