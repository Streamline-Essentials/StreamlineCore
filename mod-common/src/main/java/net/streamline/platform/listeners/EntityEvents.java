package net.streamline.platform.listeners;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.streamline.platform.compat.CompatEnderman;
import net.streamline.platform.compat.McCompat;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.events.CosmicEvent;
import singularity.events.entity.CosmicEntityDamagedByEntityEvent;
import singularity.events.entity.CosmicEntityEvent;
import singularity.events.entity.CosmicExplosionEvent;
import singularity.events.entity.CosmicMobGriefEvent;
import singularity.utils.MessageUtils;

/**
 * Fires the cross-platform {@code singularity.events.entity} events for the mod loaders. Each
 * loader's listener calls in here before the change happens and applies the answer. Every call
 * returns {@code true} (allowed) without building an event when no module listens, because the
 * grief check runs for many mobs every tick.
 */
public final class EntityEvents {

    private EntityEvents() {}

    /**
     * @param direct the exploding entity, may be {@code null}
     * @param owner  the entity responsible for it, may be {@code null}
     * @return whether the explosion may still break blocks
     */
    public static boolean onExplosion(Entity direct, Entity owner) {
        Entity at = direct != null ? direct : owner;
        if (at == null || ! CosmicEntityEvent.hasListeners(CosmicExplosionEvent.class)) return true;

        CosmicExplosionEvent event = new CosmicExplosionEvent(world(at), position(at),
                type(direct != null ? direct : owner), owner == null ? null : type(owner));
        fire(event);
        return event.breaksBlocks();
    }

    /**
     * For loaders whose grief check names only the mob. An enderman asks to grief both to take
     * a block and to put its carried one down, and only ever does the latter while carrying one.
     *
     * @return whether the mob may grief
     */
    public static boolean onMobGrief(Entity entity) {
        return onMobGrief(entity, CompatEnderman.isCarryingBlock(entity));
    }

    /**
     * @param placing whether the mob is known to be putting a block down
     * @return whether the mob may grief
     */
    public static boolean onMobGrief(Entity entity, boolean placing) {
        if (entity == null || entity instanceof Player) return true;
        if (! CosmicEntityEvent.hasListeners(CosmicMobGriefEvent.class)) return true;

        CosmicMobGriefEvent event = new CosmicMobGriefEvent(world(entity), position(entity), type(entity), null, placing);
        fire(event);
        return ! event.isCancelled();
    }

    /**
     * @return whether the damage may go through
     */
    public static boolean onDamaged(LivingEntity victim, DamageSource source, float amount) {
        Entity damager = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if (damager == null) damager = attacker;
        if (damager == null || ! CosmicEntityEvent.hasListeners(CosmicEntityDamagedByEntityEvent.class)) return true;

        CosmicEntityDamagedByEntityEvent event = new CosmicEntityDamagedByEntityEvent(world(victim), position(victim),
                type(victim), victim.getStringUUID(), type(damager), attacker == null ? null : type(attacker), amount);
        fire(event);
        return ! event.isCancelled();
    }

    private static String type(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static PlayerWorld world(Entity entity) {
        return new PlayerWorld(entity.level() instanceof ServerLevel ? McCompat.dimensionId((ServerLevel) entity.level()) : "");
    }

    private static WorldPosition position(Entity entity) {
        return new WorldPosition(entity.getX(), entity.getY(), entity.getZ());
    }

    private static void fire(CosmicEvent event) {
        try {
            event.fire();
        } catch (Throwable t) {
            MessageUtils.logWarning("A listener for " + event.getClass().getSimpleName() + " failed", t);
        }
    }
}
