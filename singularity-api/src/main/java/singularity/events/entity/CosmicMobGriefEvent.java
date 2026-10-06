package singularity.events.entity;

import lombok.Getter;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;

/**
 * A mob is about to change blocks: an enderman picking up or placing a block, a wither
 * smashing blocks around it, a ravager trampling leaves, a sheep eating grass and so on.
 * Cancelling it ({@link #setCancelled(boolean)}) stops the change.
 *
 * <p>What the platforms report differs. Spigot fires this per block change, with
 * {@link #getBlockType()} set, from its entity-change-block event. Forge and NeoForge fire
 * it whenever a mob asks whether it may grief, which is before it has picked a block, so
 * {@link #getBlockType()} is {@code null}; that check also covers the block damage of the
 * mob's own explosions (creepers, ghast fireballs, wither skulls). Fabric does not fire it.</p>
 */
@Getter
public class CosmicMobGriefEvent extends CosmicEntityEvent {
    /** The mob, e.g. {@code minecraft:enderman}. For a projectile, the projectile's type. */
    private final String entityType;
    /** The block being changed, when the platform knows it; otherwise {@code null}. */
    private final String blockType;
    /**
     * Whether the mob is putting a block down rather than removing one, such as an
     * enderman placing the block it carries. {@code false} when the platform cannot tell.
     */
    private final boolean placing;

    public CosmicMobGriefEvent(PlayerWorld world, WorldPosition position, String entityType, String blockType, boolean placing) {
        super(world, position);
        this.entityType = entityType;
        this.blockType = blockType;
        this.placing = placing;
    }
}
