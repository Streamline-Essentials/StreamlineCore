package singularity.board.data;

import lombok.Getter;

/**
 * A typed wrapper that holds a sender object of type {@code T} and provides a
 * string identifier for use in serialisation and logging.
 *
 * <p>If the wrapped object is a {@link String} it is used directly as the
 * identifier; otherwise the simple class name of the object is returned.
 *
 * @param <T> the type of the wrapped sender object
 */
@Getter
public class BoardSender<T> {

    /** The wrapped sender object. May be {@code null}. */
    private final T of;

    /**
     * Constructs a {@code BoardSender} wrapping the given object.
     *
     * @param of the sender object to wrap; may be {@code null}
     */
    public BoardSender(T of) {
        this.of = of;
    }

    /**
     * Returns a string identifier for this sender.
     *
     * <ul>
     *   <li>Returns {@code "null"} if the wrapped object is {@code null}.</li>
     *   <li>Returns the string value directly if the wrapped object is a {@link String}.</li>
     *   <li>Otherwise returns the simple class name of the wrapped object.</li>
     * </ul>
     *
     * @return the identifier string; never {@code null}
     */
    public String getIdentifier() {
        if (this.of == null)
            return "null";

        if (this.getOf() instanceof String)
            return (String) this.getOf();

        return this.of.getClass().getSimpleName();
    }
}
