package singularity.board.data.constructor;

import java.util.function.Function;

/**
 * A functional interface that describes a constructor for board data. It extends
 * {@link Function} so that implementations can be used as lambda expressions or
 * method references that transform an input of type {@code T} into an output of
 * type {@code R}.
 *
 * @param <T> the type of the input to the constructor
 * @param <R> the type of the result produced by the constructor
 */
public interface BoardDataConstructor<T, R> extends Function<T, R> {
}
