package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

import java.util.Date;

/**
 * Event fired when a new {@link CosmicPlayer} record is created for the first time.
 *
 * <p>This is the player-specific counterpart of {@link CreateSenderEvent}. It provides
 * typed access to the {@link CosmicPlayer} and exposes the player's first-join date
 * as a convenient shorthand.</p>
 */
public class CreatePlayerEvent extends CreateSenderEvent {

    /**
     * Constructs a {@code CreatePlayerEvent} for the newly created player.
     *
     * @param player the {@link CosmicPlayer} that was just created
     */
    public CreatePlayerEvent(CosmicPlayer player) {
        super(player);
    }

    /**
     * Returns the {@link CosmicPlayer} that was created, obtained by casting the
     * underlying sender reference.
     *
     * @return the newly created {@link CosmicPlayer}
     */
    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }

    /**
     * Returns the date on which the player first joined, as recorded in their
     * {@link CosmicPlayer} data.
     *
     * @return the first-join {@link Date} of the player
     */
    public Date getCreationDate() {
        return getPlayer().getFirstJoinDate();
    }
}
