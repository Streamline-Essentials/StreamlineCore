package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

/**
 * Event fired when a {@link CosmicPlayer} record is permanently deleted.
 *
 * <p>This is the player-specific counterpart of {@link DeleteSenderEvent}. It provides
 * typed access to the {@link CosmicPlayer} whose data is being removed.</p>
 */
public class DeletePlayerEvent extends DeleteSenderEvent {

    /**
     * Constructs a {@code DeletePlayerEvent} for the player that is being deleted.
     *
     * @param player the {@link CosmicPlayer} that is being deleted
     */
    public DeletePlayerEvent(CosmicPlayer player) {
        super(player);
    }

    /**
     * Returns the {@link CosmicPlayer} that is being deleted, obtained by casting
     * the underlying sender reference.
     *
     * @return the {@link CosmicPlayer} being deleted
     */
    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
