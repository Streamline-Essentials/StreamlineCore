package host.plas.adapters.spigot;

import host.plas.StreamQuests;
import host.plas.data.QuestManager;
import host.plas.data.players.QuestPlayer;
import host.plas.data.require.RequirementType;
import net.streamline.apib.SLAPIB;
import singularity.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class SpigotListener implements Listener {
    public SpigotListener() {
        Bukkit.getPluginManager().registerEvents(this, SLAPIB.getPlugin());
        MessageUtils.logInfo("Registered Quest's Spigot Listener!");
    }

    // Breaks, placements, kills and deaths count through Streamline's cross-platform events
    // (QuestsListener); Bukkit is only needed for what those do not carry: the killer's type,
    // world environments and inventory changes.
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        EntityDamageEvent causeEvent = player.getLastDamageCause();
        if (! (causeEvent instanceof EntityDamageByEntityEvent)) return;

        Entity killer = ((EntityDamageByEntityEvent) causeEvent).getDamager();
        if (killer instanceof Projectile && ((Projectile) killer).getShooter() instanceof Entity) {
            killer = (Entity) ((Projectile) killer).getShooter();
        }
        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.KILLED_BY_ENTITY, killer.getType().name(), 1.0);
    }

    @EventHandler
    public void onEnterDimension(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World toWorld = player.getWorld();

        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.DIMENSION_JOINS, toWorld.getName(), 1.0);
        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.ENVIRONMENT_JOINS, toWorld.getEnvironment().name(), 1.0);
    }

//    @EventHandler
//    public void onPlayerLogin(PlayerLoginEvent event) {
//        Player player = event.getPlayer();
//
//        StreamQuests.getLoader().getOrCreate(player.getUniqueId().toString());
//    }
//
//    @EventHandler
//    public void onPlayerQuit(PlayerQuitEvent event) {
//        Player player = event.getPlayer();
//
//        Optional<QuestPlayer> p = StreamQuests.getLoader().get(player.getUniqueId().toString());
//        if (p.isEmpty()) return;
//
//        p.get().save();
//    }

    @EventHandler
    public void onPlayerInvUpdate(InventoryEvent event) {
        try {
            Player player = (Player) event.getViewers().get(0);

            QuestPlayer p = StreamQuests.getLoader().getOrCreate(player.getUniqueId().toString());

            QuestManager.getQuests().forEach(quest -> {
                quest.checkPlayer(p);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
