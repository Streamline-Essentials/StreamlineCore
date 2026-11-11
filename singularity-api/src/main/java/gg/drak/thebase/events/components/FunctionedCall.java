package gg.drak.thebase.events.components;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.function.Function;

public class FunctionedCall<T extends BaseEvent> implements Comparable<FunctionedCall<?>> {
    @Getter @Setter
    Function<T, Boolean> function;
    @Getter
    final Date loadedAt;
    @Getter
    final Class<T> clazz;

    public FunctionedCall(Function<T, Boolean> function, Class<T> clazz) {
        this.loadedAt = new Date();
        this.clazz = clazz;
        setFunction(function);
    }

    public boolean fire(T t) {
        return getFunction().apply(t);
    }

    @Override
    public int compareTo(@NotNull FunctionedCall<?> o) {
        return Long.compare(getLoadedAt().getTime(), o.getLoadedAt().getTime());
    }
}
