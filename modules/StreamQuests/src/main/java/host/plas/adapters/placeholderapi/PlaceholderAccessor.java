package host.plas.adapters.placeholderapi;

import host.plas.bou.compat.papi.PAPICompat;
import host.plas.bou.utils.UuidUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import singularity.data.console.CosmicSender;

import java.util.UUID;

public class PlaceholderAccessor {
    public static boolean isEnabled() {
        return PAPICompat.isEnabled();
    }

    public static String replace(String string) {
        return PAPICompat.replace(string);
    }

    public static String replace(OfflinePlayer player, String string) {
        return PAPICompat.replace(player, string);
    }

    public static String replace(CosmicSender player, String string) {
        try {
            String uuid = player.getUuid();
            if (! UuidUtils.isValidPlayerUUID(uuid)) return replace(string);

            OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(uuid));

            return PAPICompat.replace(p, string);
        } catch (IllegalArgumentException e) {
            return string;
        }
    }
}
