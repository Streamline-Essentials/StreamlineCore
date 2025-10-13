package net.streamline.api.holders;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import singularity.holders.CosmicDependencyHolder;
import singularity.holders.builtin.CosmicGeyserHolder;
import singularity.utils.UuidUtils;

import java.util.UUID;

public class GeyserHolder extends CosmicDependencyHolder<FloodgateApi> implements CosmicGeyserHolder {
    public GeyserHolder() {
        super("geyser", "floodgate", "floodgate-spigot", "floodgate-bungee", "floodgate-velocity");

        tryLoad(this::tryLoadThis);
    }

    public Void tryLoadThis() {
        FloodgateApi api = FloodgateApi.getInstance();
        if (api != null) {
            setApi(api);
            return null;
        }

        return null;
    }

    @Override
    public boolean isBedrockUUID(String uuid) {
        if (getApi() == null) return false;

        if (! UuidUtils.isUuid(uuid)) return true;
        UUID u = UUID.fromString(uuid);

        FloodgatePlayer player = getApi().getPlayer(u);
        return player != null && player.getCorrectUsername() != null;
    }

    @Override
    public boolean isBedrockName(String name) {
        if (getApi() == null) return false;

        UUID uuid = getApi().getUuidFor(name).join();
        return uuid != null;
    }

    @Override
    public String getBedrockPrefix() {
        if (getApi() == null) return null;

        return getApi().getPlayerPrefix();
    }

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
