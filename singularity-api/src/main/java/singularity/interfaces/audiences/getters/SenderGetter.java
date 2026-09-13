package singularity.interfaces.audiences.getters;

import java.util.function.Supplier;

/**
 * A {@link Supplier} that provides a platform command-sender instance of type {@code C}.
 *
 * <p>Implementations wrap the platform-specific lookup logic (e.g., retrieving a
 * sender by UUID or name) so that the rest of the framework can obtain a sender
 * without coupling to a particular platform API.</p>
 *
 * @param <C> the platform command-sender type returned by this getter
 */
public interface SenderGetter<C> extends Supplier<C> {

}
