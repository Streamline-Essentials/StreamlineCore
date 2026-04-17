package singularity.data.players.events;

import singularity.data.console.CosmicSender;

/**
 * Event fired when a {@link CosmicSender} record is permanently deleted.
 *
 * <p>Listeners may use this event to clean up any external data that references
 * the sender (e.g. friendlists, statistics, or cached values) before the record
 * is removed from the database.</p>
 *
 * <p>For player-specific deletion events, see the subclass {@link DeletePlayerEvent}.</p>
 */
public class DeleteSenderEvent extends CosmicSenderEvent {

    /**
     * Constructs a {@code DeleteSenderEvent} for the sender that is being deleted.
     *
     * @param player the {@link CosmicSender} that is being deleted
     */
    public DeleteSenderEvent(CosmicSender player) {
        super(player);
    }
}
