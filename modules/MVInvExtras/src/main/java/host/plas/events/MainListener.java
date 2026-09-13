package host.plas.events;

import host.plas.utils.MVUtils;
import net.playavalon.mythicdungeons.api.events.dungeon.DungeonDisposeEvent;
import net.playavalon.mythicdungeons.api.events.dungeon.DungeonStartEvent;
import net.streamline.apib.SLAPIB;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class MainListener implements Listener {
    public MainListener() {
        Bukkit.getPluginManager().registerEvents(this, SLAPIB.getPlugin());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        MVUtils.onWorldLoad(event.getWorld());
    }

    @EventHandler
    public void onDungeonUnload(DungeonDisposeEvent event) {
        MVUtils.onDungeonUnload(event.getInstance());
    }

    @EventHandler
    public void onDungeonLoad(DungeonStartEvent event) {
        MVUtils.onDungeonLoad(event.getInstance());
    }
}
