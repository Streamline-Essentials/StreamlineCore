package host.plas.bukkit;

import host.plas.MobGriefControl;
import host.plas.config.GriefConfig;
import net.streamline.apib.SLAPIB;
import org.bukkit.Bukkit;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

/**
 * Bukkit-side enforcement of {@link GriefConfig}. Only loaded on Spigot; every other
 * platform lacks the {@code org.bukkit} classes this references.
 *
 * <p>Block damage is removed by emptying an explosion's block list rather than
 * cancelling it, so the blast still knocks back and hurts entities. Entity damage is
 * removed per hit through {@link EntityDamageByEntityEvent}.
 */
public class GriefListener implements Listener {
    public GriefListener() {
        Bukkit.getPluginManager().registerEvents(this, SLAPIB.getPlugin());
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    private static GriefConfig config() {
        return MobGriefControl.getGriefConfig();
    }

    private static boolean isGhastFireball(Entity entity) {
        return entity instanceof Fireball && ((Fireball) entity).getShooter() instanceof Ghast;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        GriefConfig config = config();

        boolean blockDamage;
        if (entity instanceof Creeper) blockDamage = config.isCreeperBlockDamage();
        else if (isGhastFireball(entity)) blockDamage = config.isGhastBlockDamage();
        else if (entity instanceof WitherSkull) blockDamage = config.isWitherSkullBlockDamage();
        // The wither's own explosion is the blast it lets off when spawned.
        else if (entity instanceof Wither) blockDamage = config.isWitherSelfBlockDamage();
        else return;

        if (! blockDamage) event.blockList().clear();
    }

    /**
     * A ghast fireball's blast is incendiary; the fire it spreads is block damage too.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (isGhastFireball(event.getEntity()) && ! config().isGhastBlockDamage()) {
            event.setFire(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChangeBlock(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Enderman) {
            // A pick-up turns the block into air; the reverse is the enderman placing
            // a block it already carries, which stays allowed so it is not stuck holding it.
            if (event.getTo().isAir() && ! config().isEndermanPickUpBlocks()) {
                event.setCancelled(true);
            }
        } else if (entity instanceof Wither) {
            if (! config().isWitherSelfBlockDamage()) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        GriefConfig config = config();

        boolean entityDamage;
        if (damager instanceof Creeper) entityDamage = config.isCreeperEntityDamage();
        else if (isGhastFireball(damager)) entityDamage = config.isGhastEntityDamage();
        else if (damager instanceof WitherSkull) entityDamage = config.isWitherSkullEntityDamage();
        else return;

        if (! entityDamage) event.setCancelled(true);
    }
}
