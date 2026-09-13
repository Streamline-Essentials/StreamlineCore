package singularity.events.server;

import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.CosmicSenderEvent;

/**
 * Fired when a player disconnects from the server.
 *
 * <p>This event is dispatched during the player's logout sequence and can be
 * used to perform cleanup or persist data associated with the departing
 * player.</p>
 */
public class LogoutEvent extends CosmicSenderEvent {

    /**
     * Constructs a new {@code LogoutEvent} for the given player.
     *
     * @param player the player that is logging out; must not be {@code null}
     */
    public LogoutEvent(CosmicPlayer player) {
        super(player);
    }
}
