package singularity.events.server.world;

import lombok.Getter;
import lombok.Setter;
import singularity.data.console.CosmicSender;
import singularity.objects.world.CosmicBlock;

@Getter @Setter
public class BlockPlaceEvent extends BlockEvent {
    private CosmicSender player;

    public BlockPlaceEvent(CosmicSender player, CosmicBlock block) {
        super(block);
        this.player = player;
    }
}
