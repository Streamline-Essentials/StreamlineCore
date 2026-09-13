package singularity.interfaces.audiences.real;

import lombok.Getter;
import lombok.Setter;
import singularity.interfaces.audiences.getters.PlayerGetter;
import singularity.interfaces.audiences.messaging.IChatter;

/**
 * Abstract base representation of a real, in-game player on any supported platform.
 *
 * <p>{@code RealPlayer} extends {@link RealSender} with player-specific access via a
 * {@link PlayerGetter} supplier and adds the {@link IChatter} capability for richer
 * in-game communication.  Console-directed output methods are intentionally no-ops here
 * because players are never the server console.</p>
 *
 * @param <P> the platform-native player type (e.g. {@code Player} on Spigot)
 */
@Getter @Setter
public abstract class RealPlayer<P> extends RealSender<P> implements IChatter {

    /**
     * The supplier used to retrieve the underlying platform-native player object on demand.
     */
    private final PlayerGetter<P> playerGetter;

    /**
     * Constructs a {@code RealPlayer} backed by the given player supplier.
     *
     * @param playerGetter a non-null supplier that returns the platform-native player object
     */
    public RealPlayer(PlayerGetter<P> playerGetter) {
        super(playerGetter);
        this.playerGetter = playerGetter;
    }

    /**
     * Returns the underlying platform-native player object by invoking the {@link PlayerGetter}.
     *
     * @return the platform-native player, never {@code null} as long as the getter is valid
     */
    public P getPlayer() {
        return playerGetter.get();
    }

    /**
     * No-op implementation: players are not the server console, so console messages
     * directed through this path are silently discarded.
     *
     * @param message the message that would be sent to the console (ignored)
     */
    public void sendConsoleMessageNonNull(String message) {
        // do nothing
    }

    /**
     * No-op implementation: players do not write to the server log, so log messages
     * directed through this path are silently discarded.
     *
     * @param message the message that would be written to the log (ignored)
     */
    public void sendLogMessage(String message) {
        // do nothing
    }
}
