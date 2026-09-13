package host.plas.accessors;

import host.plas.bou.utils.ColorUtils;
import net.streamline.api.SLAPI;
import singularity.data.players.CosmicPlayer;
import singularity.interfaces.ISingularityExtension;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SpigotAccessor {
    public static boolean ensureSafe() {
        return SLAPI.getInstance().getPlatform().getServerType().equals(ISingularityExtension.ServerType.BACKEND);
    }

    public static Player getPlayer(String uuid) {
        if (! ensureSafe()) return null;

        return Bukkit.getPlayer(UUID.fromString(uuid));
    }

    public static void updateCustomName(CosmicPlayer player, String name) {
        if (! ensureSafe()) return;

        updateCustomName(player, name, true);
    }

    public static void updateCustomName(CosmicPlayer player, String name, boolean tabListAlso) {
        if (! ensureSafe()) return;

        Player p = getPlayer(player.getUuid());
        if (p == null) return;

        String colorizedName = colorize(name);

        p.setCustomName(colorizedName);
        p.setDisplayName(colorizedName);
        if (tabListAlso) updateTabList(player, name);
    }

    public static void updateTabList(CosmicPlayer player, String name) {
        if (! ensureSafe()) return;

        Player p = getPlayer(player.getUuid());
        if (p == null) return;

        String colorizedName = colorize(name);

        p.setPlayerListName(colorizedName);
    }

    public static String colorize(String value) {
        return ColorUtils.colorizeHard(value);
    }
}
