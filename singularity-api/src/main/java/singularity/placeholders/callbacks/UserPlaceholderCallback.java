package singularity.placeholders.callbacks;

import singularity.data.console.CosmicSender;

import java.util.function.BiFunction;

/**
 * A functional callback interface used by user-aware placeholder replaceables.
 *
 * <p>Implementors receive both a {@link CallbackString} describing the matched
 * placeholder and the {@link CosmicSender} on whose behalf the placeholder is
 * being resolved. The callback must return the replacement string.
 *
 * <p>As a {@link java.util.function.BiFunction}, this interface can be expressed
 * as a lambda or method reference.
 *
 * <p>Example usage:
 * <pre>{@code
 * UserPlaceholderCallback callback = (cs, sender) -> sender.getCurrentName();
 * }</pre>
 *
 * @see PlaceholderCallback
 * @see RATCallback
 */
@FunctionalInterface
public interface UserPlaceholderCallback extends BiFunction<CallbackString, CosmicSender, String>, RATCallback {
}
