package singularity.utils;

import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;

import java.util.UUID;

public class UuidUtils {
    public static boolean isUuid(String thing) {
        try {
            UUID.fromString(thing);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String toUuid(String name) {
        if (isUuid(name)) {
            return name;
        } else {
            if (isConsole(name)) {
                return getConsoleUUID();
            } else {
                UUID uuid = UUIDFetcher.getUUID(name);
                if (uuid == null) {
                    return null;
                } else {
                    return uuid.toString();
                }
            }
        }
    }

    public static String toName(String uuid) {
        if (isUuid(uuid)) {
            return UUIDFetcher.getName(uuid);
        } else {
            if (isConsole(uuid)) {
                return getConsoleName();
            } else {
                return null;
            }
        }
    }

    public static boolean isConsole(String thing) {
        return thing.equals(getConsoleName()) || thing.equals(getConsoleUUID());
    }

    public static String getConsoleName() {
        return GivenConfigs.getMainConfig().getConsoleName();
    }

    public static String getConsoleUUID() {
        return GivenConfigs.getMainConfig().getConsoleDiscriminator();
    }

    public static boolean isOfflineMode() {
        return Singularity.isOfflineMode();
    }

    public static boolean isValidPlayer(CosmicSender sender) {
        if (sender == null) return false;
        if (sender.getIdentifier() == null || sender.getCurrentName() == null || sender.getCurrentName().isBlank()) return false;

        if (! (sender instanceof CosmicPlayer)) {
            return sender.getIdentifier().equals(getConsoleUUID()) && sender.getCurrentName().equals(getConsoleName());
        }
        CosmicPlayer player = (CosmicPlayer) sender;

        if (isOfflineMode()) return true;

        String name = UUIDFetcher.getName(player.getIdentifier());
        return name != null && ! name.isBlank() && name.equals(player.getCurrentName());
    }

    public static boolean isValidPlayerName(String playerName) {
        if (playerName == null) return false;
        if (playerName.isBlank()) return false;

        if (isOfflineMode()) return true;

        UUID uuid = UUIDFetcher.getUUID(playerName);
        try {
            return uuid != null;
        } catch (Throwable e) {
            return false; // If the player is not found or has no valid profile
        }
    }

    public static boolean isValidPlayerUUID(String uuid) {
        if (uuid == null) return false;
        if (uuid.isBlank()) return false;

        if (isOfflineMode()) return true;

        String name = UUIDFetcher.getName(UUID.fromString(uuid));
        try {
            return name != null;
        } catch (Throwable e) {
            return false; // If the player is not found or has no valid profile
        }
    }
}
