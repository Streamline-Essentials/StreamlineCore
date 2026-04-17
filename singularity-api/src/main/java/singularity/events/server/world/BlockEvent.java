package singularity.events.server.world;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.objects.world.CosmicBlock;

/**
 * Base event for block-related world interactions.
 *
 * <p>Carries a reference to the {@link CosmicBlock} that is the subject of the
 * interaction. Concrete subclasses such as {@link BlockBreakEvent} and
 * {@link BlockPlaceEvent} extend this class to add actor and context
 * information specific to each interaction type.</p>
 */
@Getter @Setter
public class BlockEvent extends CosmicEvent {

    /**
     * The block that is the subject of this event.
     */
    private CosmicBlock block;

    /**
     * Constructs a new {@code BlockEvent} for the given block.
     *
     * @param block the {@link CosmicBlock} involved in this event; must not be
     *              {@code null}
     */
    public BlockEvent(CosmicBlock block) {
        super();
        this.block = block;
    }
}
