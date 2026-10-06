package singularity.events.entity;

import lombok.Getter;
import lombok.Setter;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;

/**
 * An explosion caused by an entity is about to go off. Setting {@link #setBlockDamage(boolean)}
 * to {@code false} keeps the blast but leaves every block, and the fire an incendiary blast
 * would start, untouched; entities are still hurt and knocked back. Cancelling the event is
 * the same as turning block damage off; to spare entities, cancel the matching
 * {@link CosmicEntityDamagedByEntityEvent}s.
 *
 * <p>Explosions with no entity behind them (beds, respawn anchors) are not reported.</p>
 */
@Getter
public class CosmicExplosionEvent extends CosmicEntityEvent {
    /** The entity that exploded: {@code minecraft:creeper}, {@code minecraft:fireball}, {@code minecraft:wither_skull}... */
    private final String entityType;
    /**
     * The entity responsible for it, such as the {@code minecraft:ghast} that shot a fireball;
     * the exploding entity itself for mobs such as creepers and withers. {@code null} when unknown.
     */
    private final String ownerType;

    @Setter
    private boolean blockDamage = true;

    public CosmicExplosionEvent(PlayerWorld world, WorldPosition position, String entityType, String ownerType) {
        super(world, position);
        this.entityType = entityType;
        this.ownerType = ownerType;
    }

    /** Whether the explosion may still break blocks once every listener ran. */
    public boolean breaksBlocks() {
        return blockDamage && ! isCancelled();
    }
}
