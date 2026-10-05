package singularity.events.player.gameplay;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.objects.world.CosmicBlock;

/**
 * A player placed a block. {@link CosmicBlock#getType()} is the block's namespaced id.
 */
@Getter
public class PlayerPlacedBlockEvent extends PlayerGameplayEvent {
    private final CosmicBlock block;

    public PlayerPlacedBlockEvent(CosmicPlayer player, String gameMode, CosmicBlock block) {
        super(player, gameMode);
        this.block = block;
    }
}
