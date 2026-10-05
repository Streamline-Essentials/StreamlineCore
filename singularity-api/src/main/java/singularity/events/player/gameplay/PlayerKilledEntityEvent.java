package singularity.events.player.gameplay;

import lombok.Getter;
import singularity.data.players.CosmicPlayer;
import singularity.gui.CosmicItem;

import java.util.ArrayList;
import java.util.List;

/**
 * A player killed an entity. {@link #getDrops()} holds the items it dropped, each with its id and
 * amount, as far as the platform can tell; it may be empty where the loader exposes no drops.
 */
@Getter
public class PlayerKilledEntityEvent extends PlayerGameplayEvent {
    private final String entityType;
    private final List<CosmicItem> drops;

    public PlayerKilledEntityEvent(CosmicPlayer player, String gameMode, String entityType, List<CosmicItem> drops) {
        super(player, gameMode);
        this.entityType = entityType;
        this.drops = drops == null ? new ArrayList<>() : drops;
    }
}
