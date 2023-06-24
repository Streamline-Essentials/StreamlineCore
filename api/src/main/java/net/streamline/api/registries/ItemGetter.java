package net.streamline.api.registries;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface ItemGetter<T, R, G> extends BiFunction<T, R, G> {
    public static <T, R, G> ItemGetter<T, R, G> of(Function<T, R> getter, Function<R, G> converter) {
        return (t, r) -> converter.apply(getter.apply(t));
    }

    public static <T, R, G> ItemGetter<T, R, G> of(Function<T, R> getter) {
        return (t, r) -> (G) getter.apply(t);
    }

    public static <T, R, G> ItemGetter<T, R, G> of(BiFunction<T, R, G> getter) {
        return (ItemGetter<T, R, G>) getter;
    }

    public G get(T thing, R registry);

    @Override
    public default G apply(T t, R r) {
        return get(t, r);
    }
}
