package singularity.data.players.events;

import singularity.data.console.CosmicSender;

/**
 * Event fired when a new {@link CosmicSender} record is created for the first time.
 *
 * <p>This event is dispatched immediately after the sender's data is initialised
 * and before it is first persisted to the database. Listeners may use this event
 * to perform one-time setup tasks such as assigning default permissions or
 * sending welcome messages.</p>
 *
 * <p>For player-specific creation events, see the subclass {@link CreatePlayerEvent}.</p>
 */
public class CreateSenderEvent extends CosmicSenderEvent {

    /**
     * Constructs a {@code CreateSenderEvent} for the newly created sender.
     *
     * @param player the {@link CosmicSender} that was just created
     */
    public CreateSenderEvent(CosmicSender player) {
        super(player);
    }
}
