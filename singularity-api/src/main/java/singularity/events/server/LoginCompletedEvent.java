package singularity.events.server;

import singularity.data.players.CosmicPlayer;

/**
 * Fired after a player's login sequence has fully completed and they are
 * considered fully connected and ready to interact with the server.
 *
 * <p>This event extends {@link LoginEvent} and is dispatched as the final
 * step in the login pipeline, after data has been loaded and any pre-login
 * checks have passed.</p>
 */
public class LoginCompletedEvent extends LoginEvent {

    /**
     * Constructs a new {@code LoginCompletedEvent} for the given player.
     *
     * @param player the player whose login has fully completed; must not be
     *               {@code null}
     */
    public LoginCompletedEvent(CosmicPlayer player) {
        super(player);
    }
}
