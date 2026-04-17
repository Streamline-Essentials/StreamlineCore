package singularity.data.players.events;

import singularity.data.console.CosmicSender;

/**
 * Event fired when a {@link CosmicSender} is loaded from the database into memory.
 *
 * <p>This event is dispatched after the sender's persisted data has been read and
 * hydrated into a {@link CosmicSender} object but before it is made available to
 * the rest of the system. Listeners can use this event to inject additional data
 * or react to a sender becoming active.</p>
 *
 * <p>For player-specific load events, see the subclass {@link LoadPlayerEvent}.</p>
 */
public class LoadSenderEvent extends CosmicSenderEvent {

    /**
     * Constructs a {@code LoadSenderEvent} for the sender that was just loaded.
     *
     * @param player the {@link CosmicSender} that was loaded from the database
     */
    public LoadSenderEvent(CosmicSender player) {
        super(player);
    }
}
