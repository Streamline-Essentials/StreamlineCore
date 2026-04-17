package singularity.events.server.world;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.objects.world.CosmicBlock;

/**
 * Fired when a player or other {@link CosmicSender} breaks a block in the
 * world.
 *
 * <p>Extends {@link BlockEvent} with a reference to the actor responsible for
 * the block break. In typical gameplay this will be a player, but the type is
 * broadened to {@link CosmicSender} to support automation or command-block
 * scenarios.</p>
 */
@Getter @Setter
public class BlockBreakEvent extends BlockEvent {

    /**
     * The sender (usually a player) that broke the block.
     */
    private CosmicSender player;

    /**
     * Constructs a new {@code BlockBreakEvent}.
     *
     * @param player the {@link CosmicSender} that broke the block; must not be
     *               {@code null}
     * @param block  the {@link CosmicBlock} that was broken; must not be
     *               {@code null}
     */
    public BlockBreakEvent(CosmicSender player, CosmicBlock block) {
        super(block);
        this.player = player;
    }
}
