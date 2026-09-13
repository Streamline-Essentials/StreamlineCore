package singularity.utils;

import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.holders.HoldersHolder;
import singularity.holders.builtin.CosmicGeyserHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class providing UUID and player-identity helpers used across the
 * Singularity platform.
 *
 * <p>Methods handle the console pseudo-player, offline/online mode, and
 * Bedrock (Geyser) player detection in addition to standard UUID operations.
 */
public class UuidUtils {
    /**
     * Generates a random {@link UUID}.
     *
     * @return a new random {@link UUID}
     */
    public static UUID randomUuid() {
        return UUID.randomUUID();
    }

    /**
     * Generates a random UUID and returns it as a formatted string.
     *
     * @return a new random UUID string in the standard {@code 8-4-4-4-12} format
     */
    public static String randomStringUuid() {
        return randomUuid().toString();
    }

    /**
     * Checks whether the given string is a valid UUID.
     *
     * @param thing the string to test
     * @return {@code true} if {@code thing} can be parsed as a {@link java.util.UUID}; {@code false} otherwise
     */
    public static boolean isUuid(String thing) {
        try {
            UUID.fromString(thing);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Resolves a player name (or already-valid UUID string) to its UUID string.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If {@code name} is already a UUID string it is returned as-is.</li>
     *   <li>If {@code name} matches the console discriminator the console UUID is returned.</li>
     *   <li>If {@code name} is a Bedrock player name the Geyser-mapped UUID is returned.</li>
     *   <li>Otherwise the Mojang API is queried via {@link UUIDFetcher}.</li>
     * </ol>
     *
     * @param name the player name or UUID string to resolve
     * @return the corresponding UUID string, or {@code null} if it cannot be resolved
     */
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

    /**
     * Resolves a UUID string (or console discriminator / name) to the player's
     * current username.
     *
     * @param uuid the UUID string, console discriminator, or console name to resolve
     * @return the current username, or {@code null} if it cannot be resolved
     */
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

    /**
     * Returns {@code true} if the given string matches the console name or UUID.
     *
     * @param thing the string to test
     * @return {@code true} if {@code thing} is the console name or console UUID
     */
    public static boolean isConsole(String thing) {
        return thing.equals(getConsoleName()) || thing.equals(getConsoleUUID());
    }

    /**
     * Returns the configured display name used to represent the server console.
     *
     * @return the console name as defined in the main config
     */
    public static String getConsoleName() {
        return GivenConfigs.getMainConfig().getConsoleName();
    }

    /**
     * Returns the configured UUID string used to identify the server console.
     *
     * @return the console discriminator UUID string as defined in the main config
     */
    public static String getConsoleUUID() {
        return GivenConfigs.getMainConfig().getConsoleDiscriminator();
    }

    /**
     * Returns whether the server is running in offline (cracked) mode.
     *
     * @return {@code true} if the server is in offline mode
     */
    public static boolean isOfflineMode() {
        return Singularity.isOfflineMode();
    }

    /**
     * Returns whether the no-internet flag is set in the main config, which
     * disables all outbound API calls for UUID/name resolution.
     *
     * @return {@code true} if internet connectivity checks are disabled
     */
    public static boolean isNoInternet() {
        return GivenConfigs.getMainConfig().isNoInternet();
    }

    /**
     * Determines whether a {@link CosmicSender} represents a valid, verifiable player.
     *
     * <p>For non-player senders (i.e. console), validity requires matching both the
     * configured console UUID and console name. For players, online-mode validity is
     * confirmed by resolving the UUID against the Mojang API unless the server is in
     * offline mode or the player is a Bedrock player.
     *
     * @param sender the sender to validate; may be {@code null}
     * @return {@code true} if the sender is considered valid
     */
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

    /**
     * Checks whether a player name corresponds to a real Minecraft account.
     *
     * <p>Always returns {@code true} in offline mode, when internet is disabled,
     * or when the name belongs to a Bedrock player.
     *
     * @param playerName the username to validate; may be {@code null}
     * @return {@code true} if the name resolves to a valid Minecraft account
     */
    public static boolean isValidPlayerName(String playerName) {
        if (playerName == null) return false;
        if (playerName.isBlank()) return false;

        if (isNoInternet()) return true;
        if (isOfflineMode()) return true;

        if (isBedrockName(playerName)) return true;

        UUID uuid = UUIDFetcher.getUUID(playerName);
        return uuid != null;
    }

    /**
     * Checks whether a UUID string corresponds to a real Minecraft account.
     *
     * <p>Always returns {@code true} in offline mode, when internet is disabled,
     * or when the UUID belongs to a Bedrock player.
     *
     * @param uuid the UUID string to validate; may be {@code null}
     * @return {@code true} if the UUID resolves to a valid Minecraft account
     */
    public static boolean isValidPlayerUUID(String uuid) {
        if (uuid == null) return false;
        if (uuid.isBlank()) return false;

        if (isNoInternet()) return true;
        if (isOfflineMode()) return true;

        if (isBedrockUUID(uuid)) return true;

        String name = UUIDFetcher.getName(uuid);
        return name != null;
    }

    /**
     * Returns an {@link Optional} wrapping the active {@link CosmicGeyserHolder},
     * or an empty Optional if Geyser is not present.
     *
     * @return an {@link Optional} containing the Geyser holder, or empty if unavailable
     */
    public static Optional<CosmicGeyserHolder> getGeyserHolder() {
        return Optional.ofNullable(HoldersHolder.getGeyserHolder());
    }

    /**
     * Returns whether the given UUID string belongs to a Bedrock (Geyser) player.
     *
     * @param uuid the UUID string to test
     * @return {@code true} if the UUID is a Geyser-mapped Bedrock UUID; {@code false} if
     *         Geyser is unavailable or the UUID is a Java player UUID
     */
    public static boolean isBedrockUUID(String uuid) {
        return getGeyserHolder().map(h -> h.isBedrockUUID(uuid)).orElse(false);
    }

    /**
     * Returns whether the given username belongs to a Bedrock (Geyser) player,
     * typically identified by a configurable prefix (e.g. a leading dot).
     *
     * @param name the username to test
     * @return {@code true} if the name matches the Bedrock player naming pattern;
     *         {@code false} if Geyser is unavailable or the name is a Java player name
     */
    public static boolean isBedrockName(String name) {
        return getGeyserHolder().map(h -> h.isBedrockName(name)).orElse(false);
    }

    /**
     * Returns the prefix string used to identify Bedrock player usernames.
     *
     * @return the Bedrock player prefix, or {@code null} if Geyser is unavailable
     */
    public static String getBedrockPrefix() {
        return getGeyserHolder().map(CosmicGeyserHolder::getBedrockPrefix).orElse(null);
    }

    /**
     * Resolves the Bedrock username that corresponds to the given Geyser UUID string.
     *
     * @param uuid the Geyser-mapped UUID string of the Bedrock player
     * @return the Bedrock player's username, or {@code null} if Geyser is unavailable
     *         or the UUID is not recognized
     */
    public static String getUsernameFromBedrockUUID(String uuid) {
        return getGeyserHolder().map(h -> h.getUsernameFromBedrockUUID(uuid)).orElse(null);
    }

    /**
     * Resolves the Geyser-mapped UUID string for the given Bedrock player username.
     *
     * @param name the Bedrock player's username
     * @return the Geyser-mapped UUID string, or {@code null} if Geyser is unavailable
     *         or the username is not recognized
     */
    public static String getBedrockUUIDFromUsername(String name) {
        return getGeyserHolder().map(h -> h.getBedrockUUIDFromUsername(name)).orElse(null);
    }
}
