package singularity.data.players.events;

import singularity.data.console.CosmicSender;

/**
 * Event fired when a {@link CosmicSender} is unloaded from memory.
 *
 * <p>This event is dispatched when a sender's in-memory representation is being
 * evicted (for example, when a player disconnects and their data is flushed from
 * the active cache). Listeners can use this event to clean up any transient state
 * they have associated with the sender.</p>
 *
 * <p>For player-specific unload events, see the subclass {@link UnloadPlayerEvent}.</p>
 */
public class UnloadSenderEvent extends CosmicSenderEvent {

    /**
     * Constructs an {@code UnloadSenderEvent} for the sender that is being unloaded.
     *
     * @param user the {@link CosmicSender} that is being unloaded from memory
     */
    public UnloadSenderEvent(CosmicSender user) {
        super(user);
    }
}
