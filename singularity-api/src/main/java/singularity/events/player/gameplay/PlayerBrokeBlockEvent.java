package singularity.events.player.gameplay;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.objects.world.CosmicBlock;

/**
 * A player broke a block. {@link CosmicBlock#getType()} is the block's namespaced id.
 *
 * <p>For blocks that grow through ages (crops, nether wart, cocoa and the like),
 * {@link #isAgeable()} is set and {@link #isMature()} says whether it was fully grown.</p>
 */
@Getter
public class PlayerBrokeBlockEvent extends PlayerGameplayEvent {
    private final CosmicBlock block;
    private final boolean ageable;
    private final boolean mature;

    public PlayerBrokeBlockEvent(CosmicPlayer player, String gameMode, CosmicBlock block, boolean ageable, boolean mature) {
        super(player, gameMode);
        this.block = block;
        this.ageable = ageable;
        this.mature = mature;
    }

    /** Whether the block was fully grown, or does not grow at all. */
    public boolean isFullyGrown() {
        return ! ageable || mature;
    }
}
