package singularity.data.players.events;

import singularity.data.console.CosmicSender;

/**
 * Event fired when a {@link CosmicSender}'s data is saved to the database.
 *
 * <p>This event is dispatched after the sender's current in-memory state has been
 * written to persistent storage. Listeners can use this event to audit saves,
 * trigger follow-up operations, or replicate data to secondary systems.</p>
 *
 * <p>For player-specific save events, see the subclass {@link SavePlayerEvent}.</p>
 */
public class SaveSenderEvent extends CosmicSenderEvent {

    /**
     * Constructs a {@code SaveSenderEvent} for the sender whose data was saved.
     *
     * @param sender the {@link CosmicSender} that was saved to the database
     */
    public SaveSenderEvent(CosmicSender sender) {
        super(sender);
    }
}
