package singularity.data.players.events;

import singularity.data.players.CosmicPlayer;

/**
 * Event fired when a {@link CosmicPlayer} is unloaded from memory.
 *
 * <p>This is the player-specific counterpart of {@link UnloadSenderEvent}. It
 * provides typed access to the {@link CosmicPlayer} that is being evicted from
 * the active player cache, typically on disconnect.</p>
 */
public class UnloadPlayerEvent extends UnloadSenderEvent {

    /**
     * Constructs an {@code UnloadPlayerEvent} for the player that is being unloaded.
     *
     * @param player the {@link CosmicPlayer} that is being unloaded from memory
     */
    public UnloadPlayerEvent(CosmicPlayer player) {
        super(player);
    }

    /**
     * Returns the {@link CosmicPlayer} that is being unloaded, obtained by casting
     * the underlying sender reference.
     *
     * @return the {@link CosmicPlayer} being unloaded
     */
    public CosmicPlayer getPlayer() {
        return (CosmicPlayer) super.getSender();
    }
}
