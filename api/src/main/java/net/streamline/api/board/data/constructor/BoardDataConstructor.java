package net.streamline.api.board.data.constructor;

import java.util.function.Function;

public interface BoardDataConstructor<T, R> extends Function<T, R> {
}
