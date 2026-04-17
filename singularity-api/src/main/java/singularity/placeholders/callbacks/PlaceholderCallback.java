package singularity.placeholders.callbacks;

import java.util.function.Function;

/**
 * A functional callback interface used by context-free (non-user-specific)
 * placeholder replaceables.
 *
 * <p>Implementors receive a {@link CallbackString} describing the matched
 * placeholder and must return the replacement string. As a
 * {@link java.util.function.Function}, this interface can be expressed as a
 * lambda or method reference.
 *
 * <p>Example usage:
 * <pre>{@code
 * PlaceholderCallback callback = cs -> "Hello, world!";
 * }</pre>
 *
 * @see UserPlaceholderCallback
 * @see RATCallback
 */
@FunctionalInterface
public interface PlaceholderCallback extends Function<CallbackString, String>, RATCallback {
}
