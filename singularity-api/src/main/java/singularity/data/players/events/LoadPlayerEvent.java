package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

/**
 * Event fired when a {@link CosmicPlayer} is loaded from the database into memory.
 *
 * <p>This is the player-specific counterpart of {@link LoadSenderEvent}. It provides
 * typed access to the {@link CosmicPlayer} that was loaded.</p>
 */
public class LoadPlayerEvent extends LoadSenderEvent {

    /**
     * Constructs a {@code LoadPlayerEvent} for the player that was just loaded.
     *
     * @param player the {@link CosmicPlayer} that was loaded from the database
     */
    public LoadPlayerEvent(CosmicPlayer player) {
        super(player);
    }

    /**
     * Returns the {@link CosmicPlayer} that was loaded, obtained by casting the
     * underlying sender reference.
     *
     * @return the loaded {@link CosmicPlayer}
     */
    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
