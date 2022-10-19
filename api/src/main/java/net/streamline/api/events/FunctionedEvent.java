package net.streamline.api.events;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.function.Function;

public class FunctionedEvent<T extends StreamlineEvent> implements Comparable<FunctionedEvent<?>> {
    @Getter @Setter
    Function<T, Boolean> function;
    @Getter
    final Date loadedAt;
    @Getter
    final Class<T> clazz;

    public FunctionedEvent(Function<T, Boolean> function, Class<T> clazz) {
        this.loadedAt = new Date();
        this.clazz = clazz;
        setFunction(function);
    }

    public boolean fire(T t) {
        return getFunction().apply(t);
    }

    @Override
    public int compareTo(@NotNull FunctionedEvent<?> o) {
        return Long.compare(getLoadedAt().getTime(), o.getLoadedAt().getTime());
    }
}
