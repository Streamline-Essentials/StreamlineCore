package net.streamline.platform.listeners;

import host.plas.bou.scheduling.TaskManager;
import net.streamline.platform.handlers.GameplayHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import singularity.Singularity;
import singularity.data.players.location.CosmicLocation;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.events.player.CosmicCommandPreprocessEvent;
import singularity.events.player.CosmicDeathEvent;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

/**
 * Bridges the Bukkit events behind {@link singularity.interfaces.IGameplayHandler} features:
 * deaths, commands before they run, nickname display and god mode.
 */
public class GameplayListener implements Listener {

    private final GameplayHandler handler;

    public GameplayListener(GameplayHandler handler) {
        this.handler = handler;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location loc = player.getLocation();
        UserUtils.getPlayer(player.getUniqueId().toString()).ifPresent(cp -> ModuleUtils.fireEvent(new CosmicDeathEvent(cp,
                new CosmicLocation(new CosmicServer(Singularity.getServerName()), new PlayerWorld(loc.getWorld().getName()),
                        new WorldPosition(loc.getX(), loc.getY(), loc.getZ()), new PlayerRotation(loc.getYaw(), loc.getPitch())))));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        UserUtils.getPlayer(event.getPlayer().getUniqueId().toString()).ifPresent(cp -> {
            CosmicCommandPreprocessEvent cosmic = new CosmicCommandPreprocessEvent(cp, event.getMessage());
            ModuleUtils.fireEvent(cosmic);
            if (cosmic.isCancelled()) event.setCancelled(true);
        });
    }

    /** A second later, once Streamline has loaded the player's data, so a saved nickname shows. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        TaskManager.schedule(player, () -> handler.refreshDisplayName(player.getUniqueId().toString()), 20L);
    }

    /**
     * God mode: no damage except what bypasses invulnerability in vanilla too, so a player in
     * the void or hit by {@code /kill} is not stuck alive.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (! (event.getEntity() instanceof Player) || ! handler.isGod(event.getEntity().getUniqueId())) return;
        String cause = event.getCause().name();
        if (cause.equals("VOID") || cause.equals("KILL") || cause.equals("SUICIDE")) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent event) {
        if (! handler.isGod(event.getEntity().getUniqueId())) return;
        if (event.getFoodLevel() < event.getEntity().getFoodLevel()) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handler.forget(event.getPlayer());
    }
}
