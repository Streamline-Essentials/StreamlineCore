package singularity.interfaces.audiences.getters;

/**
 * A {@link SenderGetter} specialised for retrieving online player instances of type {@code P}.
 *
 * <p>Extends {@link SenderGetter} to distinguish player-specific lookups (by UUID or name)
 * from generic command-sender lookups, allowing platform implementations to handle each
 * case appropriately.</p>
 *
 * @param <P> the platform player type returned by this getter; must be a subtype of
 *            the corresponding command-sender type {@code C}
 */
public interface PlayerGetter<P> extends SenderGetter<P> {
}
