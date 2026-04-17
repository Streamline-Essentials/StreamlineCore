package singularity.events.server;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;

/**
 * Base event representing a player login action on the server.
 *
 * <p>This class serves as the common parent for all login-related events in
 * the Singularity event hierarchy (e.g. {@link LoginReceivedEvent} and
 * {@link LoginCompletedEvent}). Listeners may target this type to handle any
 * stage of the login flow.</p>
 */
public class LoginEvent extends CosmicSenderEvent {

    /**
     * Constructs a new {@code LoginEvent} for the given player.
     *
     * @param player the player that is logging in; must not be {@code null}
     */
    public LoginEvent(CosmicPlayer player) {
        super(player);
    }
}
