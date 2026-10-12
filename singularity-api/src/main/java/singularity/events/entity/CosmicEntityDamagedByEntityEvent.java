package singularity.events.entity;

import lombok.Getter;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;

/**
 * An entity is about to take damage that another entity caused: a melee hit, a projectile,
 * an explosion. Cancelling it ({@link #setCancelled(boolean)}) prevents the damage, and the
 * knockback and effects that come with it.
 *
 * <p>On Forge and NeoForge only living entities (mobs, players, armor stands) take damage
 * through a hookable event, so damage to item frames, paintings, dropped items and the like
 * is reported on Spigot alone. Fabric does not fire it.</p>
 */
@Getter
public class CosmicEntityDamagedByEntityEvent extends CosmicEntityEvent {
    /** The entity being hurt. */
    private final String entityType;
    private final String entityUuid;
    /** The entity that dealt the damage directly: the attacker, the arrow, the fireball, the exploding creeper. */
    private final String damagerType;
    /**
     * The entity responsible for it: the shooter of a projectile, otherwise the damager
     * itself. {@code null} when unknown, such as for an arrow from a dispenser.
     */
    private final String attackerType;
    private final double damage;

    public CosmicEntityDamagedByEntityEvent(PlayerWorld world, WorldPosition position, String entityType, String entityUuid,
                                            String damagerType, String attackerType, double damage) {
        super(world, position);
        this.entityType = entityType;
        this.entityUuid = entityUuid;
        this.damagerType = damagerType;
        this.attackerType = attackerType;
        this.damage = damage;
    }
}
