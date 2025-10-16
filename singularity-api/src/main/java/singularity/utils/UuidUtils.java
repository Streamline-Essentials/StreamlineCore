package singularity.utils;

import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.holders.HoldersHolder;
import singularity.holders.builtin.CosmicGeyserHolder;

import java.util.Optional;
import java.util.UUID;

public class UuidUtils {
    public static UUID randomUuid() {
        return UUID.randomUUID();
    }

    public static String randomStringUuid() {
        return randomUuid().toString();
    }

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
                if (isBedrockName(name)) {
                    return getBedrockUUIDFromUsername(name);
                }

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
            if (isBedrockUUID(uuid)) {
                return getUsernameFromBedrockUUID(uuid);
            }

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

    public static boolean isNoInternet() {
        return GivenConfigs.getMainConfig().isNoInternet();
    }

    public static boolean isValidPlayer(CosmicSender sender) {
        if (sender == null) return false;
        if (sender.getIdentifier() == null || sender.getCurrentName() == null || sender.getCurrentName().isBlank()) return false;

        if (! (sender instanceof CosmicPlayer)) {
            return sender.getIdentifier().equals(getConsoleUUID()) && sender.getCurrentName().equals(getConsoleName());
        }
        CosmicPlayer player = (CosmicPlayer) sender;

        if (isOfflineMode()) return true;

        if (isBedrockUUID(player.getUuid())) return true;

        String name = UUIDFetcher.getName(player.getIdentifier());
        return name != null && ! name.isBlank() && name.equals(player.getCurrentName());
    }

    public static boolean isValidPlayerName(String playerName) {
        if (playerName == null) return false;
        if (playerName.isBlank()) return false;

        if (isNoInternet()) return true;
        if (isOfflineMode()) return true;

        if (isBedrockName(playerName)) return true;

        UUID uuid = UUIDFetcher.getUUID(playerName);
        return uuid != null;
    }

    public static boolean isValidPlayerUUID(String uuid) {
        if (uuid == null) return false;
        if (uuid.isBlank()) return false;

        if (isNoInternet()) return true;
        if (isOfflineMode()) return true;

        if (isBedrockUUID(uuid)) return true;

        String name = UUIDFetcher.getName(uuid);
        return name != null;
    }

    public static Optional<CosmicGeyserHolder> getGeyserHolder() {
        return Optional.ofNullable(HoldersHolder.getGeyserHolder());
    }

    public static boolean isBedrockUUID(String uuid) {
        return getGeyserHolder().map(h -> h.isBedrockUUID(uuid)).orElse(false);
    }

    public static boolean isBedrockName(String name) {
        return getGeyserHolder().map(h -> h.isBedrockName(name)).orElse(false);
    }

    public static String getBedrockPrefix() {
        return getGeyserHolder().map(CosmicGeyserHolder::getBedrockPrefix).orElse(null);
    }

    public static String getUsernameFromBedrockUUID(String uuid) {
        return getGeyserHolder().map(h -> h.getUsernameFromBedrockUUID(uuid)).orElse(null);
    }

    public static String getBedrockUUIDFromUsername(String name) {
        return getGeyserHolder().map(h -> h.getBedrockUUIDFromUsername(name)).orElse(null);
    }
}
