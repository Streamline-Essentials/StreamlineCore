package singularity.events.server.world;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.objects.world.CosmicBlock;

@Getter @Setter
public class BlockBreakEvent extends BlockEvent {
    private CosmicSender player;

    public BlockBreakEvent(CosmicSender player, CosmicBlock block) {
        super(block);
        this.player = player;
    }
}
