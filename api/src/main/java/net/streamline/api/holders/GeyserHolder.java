package net.streamline.api.holders;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import singularity.holders.CosmicDependencyHolder;
import singularity.holders.builtin.CosmicGeyserHolder;
import singularity.utils.UuidUtils;

import java.util.UUID;

/**
 * Dependency holder that wraps the Floodgate (Geyser) API and provides
 * Bedrock/Java edition player detection and UUID translation. If the
 * Floodgate plugin is absent the holder stays unloaded and all query
 * methods return safe defaults ({@code false}, {@code ""}, or {@code null}).
 */
public class GeyserHolder extends CosmicDependencyHolder<FloodgateApi> implements CosmicGeyserHolder {

    /**
     * Constructs a new {@code GeyserHolder} and attempts to load the
     * {@link FloodgateApi} from the running server. The holder recognises
     * Floodgate under several common plugin names across Spigot, BungeeCord,
     * and Velocity.
     */
    public GeyserHolder() {
        super("geyser", "floodgate", "floodgate-spigot", "floodgate-bungee", "floodgate-velocity");

        tryLoad(this::tryLoadThis);
    }

    /**
     * Attempts to retrieve the singleton {@link FloodgateApi} instance and
     * stores it in this holder if it is non-{@code null}.
     *
     * @return always {@code null} (required by the {@code Supplier<Void>} load contract)
     */
    public Void tryLoadThis() {
        FloodgateApi api = FloodgateApi.getInstance();
        if (api != null) {
            setApi(api);
            return null;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code false} when Floodgate is not loaded. If the supplied
     * string is not a valid UUID it is treated as a Bedrock identifier and
     * {@code true} is returned. Otherwise the UUID is looked up in Floodgate
     * and the result reflects whether a {@link FloodgatePlayer} with a
     * resolved username was found.
     *
     * @param uuid the UUID string to test
     * @return {@code true} if the player is a Bedrock player, {@code false} otherwise
     */
    @Override
    public boolean isBedrockUUID(String uuid) {
        if (getApi() == null) return false;

        if (! UuidUtils.isUuid(uuid)) return true;
        UUID u = UUID.fromString(uuid);

        FloodgatePlayer player = getApi().getPlayer(u);
        return player != null && player.getCorrectUsername() != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code false} when Floodgate is not loaded. Performs a blocking
     * UUID lookup via {@code FloodgateApi.getUuidFor} and returns {@code true}
     * only when a UUID is resolved, indicating a known Bedrock player.
     *
     * @param name the player name to look up
     * @return {@code true} if the name belongs to a Bedrock player, {@code false} otherwise
     */
    @Override
    public boolean isBedrockName(String name) {
        if (getApi() == null) return false;

        UUID uuid = getApi().getUuidFor(name).join();
        return uuid != null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the Bedrock player name prefix configured in Floodgate (e.g.
     * {@code "."}), or {@code null} if Floodgate is not loaded.
     *
     * @return the Bedrock name prefix, or {@code null} if unavailable
     */
    @Override
    public String getBedrockPrefix() {
        if (getApi() == null) return null;

        return getApi().getPlayerPrefix();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the Bedrock username associated with the given UUID string, or
     * an empty string when Floodgate is unavailable or the UUID is invalid.
     * Returns {@code null} when no matching Bedrock player session is found.
     *
     * @param uuid the UUID string of the Bedrock player
     * @return the player's username, {@code ""} if the UUID is invalid or Floodgate
     *         is absent, or {@code null} if the player is not found
     */
    @Override
    public String getUsernameFromBedrockUUID(String uuid) {
        if (getApi() == null) return "";

        if (! UuidUtils.isUuid(uuid)) return "";
        UUID u = UUID.fromString(uuid);

        FloodgatePlayer player = getApi().getPlayer(u);
        if (player != null && player.getCorrectUsername() != null) {
            return player.getCorrectUsername();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs a blocking lookup of the Floodgate UUID for the given username.
     * Returns {@code ""} when Floodgate is absent, or {@code null} when no
     * Bedrock player with that name is known.
     *
     * @param name the Bedrock player name to resolve
     * @return the UUID string for the Bedrock player, {@code ""} if Floodgate
     *         is absent, or {@code null} if no player was found
     */
    @Override
    public String getBedrockUUIDFromUsername(String name) {
        if (getApi() == null) return "";

        UUID uuid = getApi().getUuidFor(name).join();
        if (uuid != null) {
            return uuid.toString();
        }

        return null;
    }
}
