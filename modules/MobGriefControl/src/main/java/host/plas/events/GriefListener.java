package host.plas.events;

import gg.drak.thebase.events.BaseEventListener;
import gg.drak.thebase.events.processing.BaseProcessor;
import host.plas.MobGriefControl;
import host.plas.config.GriefConfig;
import singularity.events.entity.CosmicEntityDamagedByEntityEvent;
import singularity.events.entity.CosmicExplosionEvent;
import singularity.events.entity.CosmicMobGriefEvent;

/**
 * Applies {@link GriefConfig} through the cross-platform entity events, which Spigot, Forge
 * and NeoForge backends fire.
 *
 * <p>Block damage is removed by turning off the explosion's block damage rather than cancelling
 * it, so the blast still knocks back and hurts entities. Entity damage is removed per hit.</p>
 */
public class GriefListener implements BaseEventListener {
    private static final String CREEPER = "minecraft:creeper";
    private static final String GHAST = "minecraft:ghast";
    private static final String ENDERMAN = "minecraft:enderman";
    private static final String WITHER = "minecraft:wither";
    private static final String WITHER_SKULL = "minecraft:wither_skull";

    private static GriefConfig config() {
        return MobGriefControl.getGriefConfig();
    }

    @BaseProcessor
    public void onExplosion(CosmicExplosionEvent event) {
        String type = event.getEntityType();
        GriefConfig config = config();

        boolean blockDamage;
        if (CREEPER.equals(type)) blockDamage = config.isCreeperBlockDamage();
        // Ghasts only ever explode through the fireballs they shoot.
        else if (GHAST.equals(event.getOwnerType())) blockDamage = config.isGhastBlockDamage();
        else if (WITHER_SKULL.equals(type)) blockDamage = config.isWitherSkullBlockDamage();
        // The wither's own explosion is the blast it lets off when spawned.
        else if (WITHER.equals(type)) blockDamage = config.isWitherSelfBlockDamage();
        else return;

        if (! blockDamage) event.setBlockDamage(false);
    }

    @BaseProcessor
    public void onMobGrief(CosmicMobGriefEvent event) {
        String type = event.getEntityType();

        if (ENDERMAN.equals(type)) {
            // Putting a carried block back down stays allowed, so an enderman is never stuck holding one.
            if (! event.isPlacing() && ! config().isEndermanPickUpBlocks()) event.setCancelled(true);
        } else if (WITHER.equals(type)) {
            if (! config().isWitherSelfBlockDamage()) event.setCancelled(true);
        }
    }

    @BaseProcessor
    public void onDamage(CosmicEntityDamagedByEntityEvent event) {
        String damager = event.getDamagerType();
        GriefConfig config = config();

        boolean entityDamage;
        if (CREEPER.equals(damager)) entityDamage = config.isCreeperEntityDamage();
        else if (GHAST.equals(event.getAttackerType())) entityDamage = config.isGhastEntityDamage();
        else if (WITHER_SKULL.equals(damager)) entityDamage = config.isWitherSkullEntityDamage();
        else return;

        if (! entityDamage) event.setCancelled(true);
    }
}
