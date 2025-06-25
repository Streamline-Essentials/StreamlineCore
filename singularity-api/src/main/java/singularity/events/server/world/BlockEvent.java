package singularity.events.server.world;

import lombok.Getter;
import lombok.Setter;
import singularity.events.CosmicEvent;
import singularity.objects.world.CosmicBlock;

@Getter @Setter
public class BlockEvent extends CosmicEvent {
    private CosmicBlock block;

    public BlockEvent(CosmicBlock block) {
        super();
        this.block = block;
    }
}
