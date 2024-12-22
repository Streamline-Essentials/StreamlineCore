package singularity.configs.given;

import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.data.server.CosmicServer;
import singularity.database.servers.SavedServer;
import singularity.utils.UuidUtils;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

import java.util.UUID;

@Getter @Setter
public class ServerConfigHandler extends SimpleConfiguration {
    public ServerConfigHandler() {
        super("server-config.yml", Singularity.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {
    }

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public String getName() {
        reloadResource();

        return getOrSetDefault("server.name", "Proxy");
    }

    private String getUUIDFromConfig() {
        reloadResource();

        return getOrSetDefault("server.uuid", getRandomUUID());
    }

    public String getUUID() {
        String uuid = getUUIDFromConfig();
        uuid = ensureProperUUID(uuid);
        return uuid;
    }

    public String ensureProperUUID(String uuid) {
        if (uuid.equals("00000000-0000-0000-0000-000000000000") || uuid.isBlank() || ! UuidUtils.isUuid(uuid)) {
            uuid = getRandomUUID();
            write("server.uuid", uuid);
        }

        return uuid;
    }

    public SavedServer getServer() {
        return new SavedServer(getUUID(), getName(), Singularity.getInstance().getPlatform().getServerType());
    }
}
