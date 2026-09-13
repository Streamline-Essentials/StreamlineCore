package singularity.events.server.world;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.objects.world.CosmicBlock;

/**
 * Fired when a player or other {@link CosmicSender} places a block in the
 * world.
 *
 * <p>Extends {@link BlockEvent} with a reference to the actor responsible for
 * the block placement. In typical gameplay this will be a player, but the type
 * is broadened to {@link CosmicSender} to support automation or command-block
 * scenarios.</p>
 */
@Getter @Setter
public class BlockPlaceEvent extends BlockEvent {

    /**
     * The sender (usually a player) that placed the block.
     */
    private CosmicSender player;

    /**
     * Constructs a new {@code BlockPlaceEvent}.
     *
     * @param player the {@link CosmicSender} that placed the block; must not be
     *               {@code null}
     * @param block  the {@link CosmicBlock} that was placed; must not be
     *               {@code null}
     */
    public BlockPlaceEvent(CosmicSender player, CosmicBlock block) {
        super(block);
        this.player = player;
    }
}
