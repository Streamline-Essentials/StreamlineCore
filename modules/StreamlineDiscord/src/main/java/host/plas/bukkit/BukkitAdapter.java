package host.plas.bukkit;

import net.streamline.apib.SLAPIB;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import host.plas.StreamlineDiscord;
import host.plas.bukkit.events.BukkitListener;

/**
 * Registers the module's Bukkit listener. It is registered under StreamlineCore's plugin, which Bukkit never
 * unloads with the module, so {@link #stop()} must remove it: otherwise every reload adds a listener and each
 * advancement or death is posted once per load.
 */
public class BukkitAdapter {
    private static BukkitListener listener;

    public static synchronized void init() {
        StreamlineDiscord.getInstance().logInfo("Initializing BukkitAdapter...");

        stop();
        listener = new BukkitListener();
        Bukkit.getPluginManager().registerEvents(listener, getPlugin());

        StreamlineDiscord.getInstance().logInfo("Initialized BukkitAdapter.");
    }

    public static synchronized void stop() {
        if (listener == null) return;
        HandlerList.unregisterAll(listener);
        listener = null;
    }

    public static SLAPIB getAPI() {
        return SLAPIB.getInstance();
    }

    public static Plugin getPlugin() {
        return SLAPIB.getPlugin();
    }
}
