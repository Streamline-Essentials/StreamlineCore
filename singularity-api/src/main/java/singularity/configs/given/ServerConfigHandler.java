package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.database.servers.SavedServer;
import singularity.utils.UuidUtils;

import java.util.UUID;

@Getter @Setter
public class ServerConfigHandler extends SimpleConfiguration {
    public ServerConfigHandler() {
        super("server-config.yml", Singularity.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {
        getServerUuid();
        getServerName();
    }

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public String getServerName() {
        reloadResource();

        return getOrSetDefault("server.name", getServerUuid());
    }

    public String getServerUuidFromConfig() {
        reloadResource();

        return getOrSetDefault("server.uuid", getRandomUUID());
    }

    public String getServerUuid() {
        String uuid = getServerUuidFromConfig();
        uuid = ensureProperUUID(uuid);
        return uuid;
    }

    public String ensureProperUUID(String uuid) {
        return ensureProperUUID(uuid, true);
    }

    public String ensureProperUUID(String uuid, boolean set) {
        if (uuid.equals("00000000-0000-0000-0000-000000000000") || uuid.isBlank() || ! UuidUtils.isUuid(uuid)) {
            uuid = getRandomUUID();
        }

        if (set) setServerUuid(uuid);

        return uuid;
    }

    public SavedServer getServer() {
        return new SavedServer(getServerUuid(), getServerName(), Singularity.getInstance().getPlatform().getServerType());
    }

    public void setServer(SavedServer server) {
        write("server.uuid", server.getIdentifier());
        setServerName(server.getName());
    }

    public void setServerName(String name) {
        write("server.name", name);
    }

    public void setServerUuid(String uuid) {
        setServerUuid(uuid, false);
    }

    public void setServerUuid(String uuid, boolean isEnsureProper) {
        if (! isEnsureProper) {
            uuid = ensureProperUUID(uuid, false);
        }
        write("server.uuid", uuid);
    }
}
