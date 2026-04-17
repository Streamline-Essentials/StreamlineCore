package singularity.board.data.constructor;

import lombok.Getter;
import lombok.Setter;
import singularity.utils.MessageUtils;

/**
 * A holder that pairs a value of type {@code C} with a {@link BoardDataConstructor} capable
 * of producing instances of {@code C}. The constructor can be invoked with or without input
 * data, and any exceptions are caught and logged rather than propagated.
 *
 * @param <C> the type of value this constructable produces
 */
@Setter
@Getter
public class BoardConstructable<C> {
    /** The initial or default value associated with this constructable. */
    private C of;

    /** The constructor function used to build the value of type {@code C}. */
    private BoardDataConstructor<?, C> constructor;

    /**
     * Creates a new {@code BoardConstructable} with the given initial value and constructor.
     *
     * @param of          the initial value
     * @param constructor the constructor function used to produce values of type {@code C}
     */
    public BoardConstructable(C of, BoardDataConstructor<?, C> constructor) {
        this.of = of;
        this.constructor = constructor;
    }

    /**
     * Replaces the constructor on this instance and returns {@code this} for chaining.
     *
     * @param constructor the new constructor to use
     * @return this instance
     */
    public BoardConstructable<C> withConstructor(BoardDataConstructor<?, C> constructor) {
        this.constructor = constructor;
        return this;
    }

    /**
     * Invokes the constructor with a {@code null} input and returns the result.
     * If the constructor throws an exception the error is logged and {@code null} is returned.
     *
     * @return the constructed value, or {@code null} if construction fails
     */
    public C construct() {
        try {
            return constructor.apply(null);
        } catch (Exception e) {
            MessageUtils.logDebug(e);
            return null;
        }
    }

    /**
     * Invokes the constructor with the given data input and returns the result.
     * The stored constructor is cast to {@code BoardDataConstructor<D, C>} before invocation.
     * If the constructor throws an exception the error is logged and {@code null} is returned.
     *
     * @param <D>  the type of the input data
     * @param data the input data to pass to the constructor
     * @return the constructed value, or {@code null} if construction fails
     */
    public <D> C construct(D data) {
        try {
            return ((BoardDataConstructor<D, C>) constructor).apply(data);
        } catch (Exception e) {
            MessageUtils.logDebug(e);
            return null;
        }
    }
}
