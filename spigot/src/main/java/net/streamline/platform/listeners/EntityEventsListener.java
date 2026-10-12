package net.streamline.platform.listeners;

import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.events.entity.CosmicEntityDamagedByEntityEvent;
import singularity.events.entity.CosmicEntityEvent;
import singularity.events.entity.CosmicExplosionEvent;
import singularity.events.entity.CosmicMobGriefEvent;
import singularity.gui.CosmicItem;

/**
 * Fires the cross-platform {@code singularity.events.entity} events, which listeners use to
 * prevent the change. Handlers run at {@link EventPriority#HIGH}, so plugins at
 * {@code HIGHEST}/{@code MONITOR} still see the outcome, and skip events no module listens to.
 */
public class EntityEventsListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (! CosmicEntityEvent.hasListeners(CosmicExplosionEvent.class)) return;

        Entity entity = event.getEntity();
        Entity owner = owner(entity);
        CosmicExplosionEvent cosmic = new CosmicExplosionEvent(world(event.getLocation()), position(event.getLocation()),
                key(entity.getType()), owner == null ? null : key(owner.getType())).fire();

        if (! cosmic.breaksBlocks()) event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChangeBlock(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        // Falling blocks landing and players trampling farmland are not mob griefing.
        if (! (entity instanceof LivingEntity) || entity instanceof Player) return;
        if (! CosmicEntityEvent.hasListeners(CosmicMobGriefEvent.class)) return;

        Location at = event.getBlock().getLocation();
        boolean placing = event.getBlock().getType().isAir() && ! event.getTo().isAir();
        CosmicMobGriefEvent cosmic = new CosmicMobGriefEvent(world(at), position(at), key(entity.getType()),
                key(event.getBlock().getType()), placing).fire();

        if (cosmic.isCancelled()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (! CosmicEntityEvent.hasListeners(CosmicEntityDamagedByEntityEvent.class)) return;

        Entity victim = event.getEntity();
        Entity damager = event.getDamager();
        Entity attacker = owner(damager);
        Location at = victim.getLocation();
        CosmicEntityDamagedByEntityEvent cosmic = new CosmicEntityDamagedByEntityEvent(world(at), position(at),
                key(victim.getType()), victim.getUniqueId().toString(), key(damager.getType()),
                attacker == null ? null : key(attacker.getType()), event.getDamage()).fire();

        if (cosmic.isCancelled()) event.setCancelled(true);
    }

    /** The shooter of a projectile, the igniter of TNT, otherwise a living entity itself. */
    private static Entity owner(Entity entity) {
        if (entity instanceof Projectile) {
            Object shooter = ((Projectile) entity).getShooter();
            return shooter instanceof Entity ? (Entity) shooter : null;
        }
        if (entity instanceof TNTPrimed) return ((TNTPrimed) entity).getSource();
        return entity instanceof LivingEntity ? entity : null;
    }

    private static PlayerWorld world(Location location) {
        return new PlayerWorld(location.getWorld() == null ? "" : location.getWorld().getName());
    }

    private static WorldPosition position(Location location) {
        return new WorldPosition(location.getX(), location.getY(), location.getZ());
    }

    private static String key(Keyed keyed) {
        try {
            return keyed.getKey().toString();
        } catch (Throwable t) {
            // Legacy or unknown constants have no key.
            return CosmicItem.normalizeKey(String.valueOf(keyed));
        }
    }
}
