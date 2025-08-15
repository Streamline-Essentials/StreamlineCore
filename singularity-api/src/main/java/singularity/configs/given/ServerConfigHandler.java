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
        getUuid();
        getName();
    }

    public String getName() {
        reloadResource();

        return getOrSetDefault("server.name", getUuid());
    }

    public String getUuid() {
        reloadResource();

        String uuid = getOrSetDefault("server.uuid", UuidUtils.randomStringUuid());
        if (! isProperUuid(uuid)) {
            uuid = getProperUuid(uuid);
            write("server.uuid", uuid);
        }

        return uuid;
    }

    public boolean isProperUuid(String uuid) {
        return ! uuid.equals("00000000-0000-0000-0000-000000000000") && ! uuid.isBlank() && UuidUtils.isUuid(uuid);
    }

    public String getProperUuid(String uuid) {
        if (! isProperUuid(uuid)) {
            uuid = UuidUtils.randomStringUuid();
        }

        return uuid;
    }

    public SavedServer getServer() {
        return new SavedServer(getUuid(), getName(), Singularity.getInstance().getPlatform().getServerType());
    }

    public void writeName(String name) {
        write("server.name", name);
    }

    public void writeUuid(String uuid) {
        if (! isProperUuid(uuid)) {
            uuid = getProperUuid(uuid);
        }

        write("server.uuid", uuid);
    }

    public void writeServer(SavedServer server) {
        writeName(server.getName());
        writeUuid(server.getUuid());
    }
}
