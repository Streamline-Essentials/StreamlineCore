package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

/**
 * Event fired when a {@link CosmicPlayer}'s data is saved to the database.
 *
 * <p>This is the player-specific counterpart of {@link SaveSenderEvent}. It provides
 * typed access to the {@link CosmicPlayer} whose data was persisted.</p>
 */
public class SavePlayerEvent extends SaveSenderEvent {

    /**
     * Constructs a {@code SavePlayerEvent} for the player whose data was saved.
     *
     * @param player the {@link CosmicPlayer} that was saved to the database
     */
    public SavePlayerEvent(CosmicPlayer player) {
        super(player);
    }

    /**
     * Returns the {@link CosmicPlayer} that was saved, obtained by casting the
     * underlying sender reference.
     *
     * @return the saved {@link CosmicPlayer}
     */
    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
