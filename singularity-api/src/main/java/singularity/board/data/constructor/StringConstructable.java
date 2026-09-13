package singularity.board.data.constructor;

/**
 * A {@link BoardConstructable} specialised for {@link String} values.
 * The default constructor uses an identity-like function that always returns the
 * original string regardless of the input data.
 */
public class StringConstructable extends BoardConstructable<String> {

    /**
     * Creates a {@code StringConstructable} with an explicit constructor function.
     *
     * @param of          the initial string value
     * @param constructor the constructor function used to produce string values
     */
    public StringConstructable(String of, BoardDataConstructor<?, String> constructor) {
        super(of, constructor);
    }

    /**
     * Creates a {@code StringConstructable} whose constructor always returns the
     * original string {@code of}, ignoring any provided input data.
     *
     * @param of the string value to wrap and return on every construction call
     */
    public StringConstructable(String of) {
        this(of, c -> of);
    }
}
