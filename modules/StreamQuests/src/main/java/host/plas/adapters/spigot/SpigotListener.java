package host.plas.adapters.spigot;

import host.plas.StreamQuests;
import host.plas.data.QuestManager;
import host.plas.data.players.QuestPlayer;
import host.plas.data.require.RequirementType;
import net.streamline.apib.SLAPIB;
import singularity.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class SpigotListener implements Listener {
    public SpigotListener() {
        Bukkit.getPluginManager().registerEvents(this, SLAPIB.getPlugin());
        MessageUtils.logInfo("Registered Quest's Spigot Listener!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        Material material = event.getBlock().getType();

        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.BREAK_BLOCK, material.name(), 1.0);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        Material material = event.getBlock().getType();

        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.PLACE_BLOCK, material.name(), 1.0);
    }

    @EventHandler
    public void onKillEntity(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        QuestManager.fireQuestEvent(killer.getUniqueId().toString(), RequirementType.KILL_ENTITY, entity.getType().name(), 1.0);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (! (entity instanceof Player)) return;

        Player player = (Player) entity;
        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.DEATHS, "", 1.0);

        EntityDamageEvent causeEvent = player.getLastDamageCause();
        if (causeEvent == null) return;

        Entity killer = causeEvent.getEntity();
        if (killer == null) return;
        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.KILLED_BY_ENTITY, killer.getType().name(), 1.0);
    }

    @EventHandler
    public void onEnterDimension(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        if (from == null) return;
        Location to = event.getTo();
        if (to == null) return;

        World fromWorld = from.getWorld();
        World toWorld = to.getWorld();

        if (toWorld == null) return;
        if (fromWorld != null) {
            if (fromWorld.equals(toWorld)) return;
        }

        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.DIMENSION_JOINS, toWorld.getName(), 1.0);

        World.Environment toEnv = toWorld.getEnvironment();

        QuestManager.fireQuestEvent(player.getUniqueId().toString(), RequirementType.ENVIRONMENT_JOINS, toEnv.name(), 1.0);
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
