package host.plas.bukkit;

import net.streamline.apib.SLAPIB;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import host.plas.StreamlineDiscord;
import host.plas.bukkit.events.BukkitListener;

public class BukkitAdapter {
    public static void init() {
        StreamlineDiscord.getInstance().logInfo("Initializing BukkitAdapter...");

        Plugin plugin = getPlugin();

        Bukkit.getPluginManager().registerEvents(new BukkitListener(), plugin);

        StreamlineDiscord.getInstance().logInfo("Initialized BukkitAdapter.");
    }

    public static SLAPIB getAPI() {
        return SLAPIB.getInstance();
    }

    public static Plugin getPlugin() {
        return SLAPIB.getPlugin();
    }
}
