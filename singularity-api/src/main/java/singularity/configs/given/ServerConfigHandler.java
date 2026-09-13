package singularity.configs.given;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.database.servers.SavedServer;
import singularity.utils.UuidUtils;

import java.util.UUID;

/**
 * Configuration handler for server identity settings, backed by
 * {@code server-config.yml} in the plugin's data folder.
 *
 * <p>Manages the server's human-readable name and its persistent UUID,
 * auto-generating and correcting the UUID when an invalid or blank value is
 * detected. Also controls the {@code auto-correct} flag.</p>
 */
@Getter @Setter
public class ServerConfigHandler extends SimpleConfiguration {

    /**
     * Constructs the handler, loading (or creating) {@code server-config.yml}
     * in the Singularity plugin's data folder as a self-contained resource.
     */
    public ServerConfigHandler() {
        super("server-config.yml", Singularity.getInstance().getDataFolder(), true);
    }

    /**
     * Eagerly reads all server identity settings so that default values are
     * written to the file when it is first created.
     */
    @Override
    public void init() {
        getUuid();
        getName();

        isAutoCorrect();
    }

    /**
     * Returns the server's display name, defaulting to the server UUID if no
     * name has been set. Reloads the resource before reading.
     *
     * @return the configured server name
     */
    public String getName() {
        reloadResource();

        return getOrSetDefault("server.name", getUuid());
    }

    /**
     * Returns the server's persistent UUID string, auto-generating a new random
     * UUID if the stored value is absent, blank, or invalid (e.g. all-zeros).
     * Any correction is persisted immediately. Reloads the resource before reading.
     *
     * @return a valid UUID string uniquely identifying this server
     */
    public String getUuid() {
        reloadResource();

        String uuid = getOrSetDefault("server.uuid", UuidUtils.randomStringUuid());
        if (! isProperUuid(uuid)) {
            uuid = getProperUuid(uuid);
            write("server.uuid", uuid);
        }

        return uuid;
    }

    /**
     * Returns whether the given UUID string is considered valid for use as a
     * server identity (non-blank, not the all-zeros UUID, and parseable as a UUID).
     *
     * @param uuid the UUID string to validate
     * @return {@code true} if the UUID is usable; {@code false} otherwise
     */
    public boolean isProperUuid(String uuid) {
        return ! uuid.equals("00000000-0000-0000-0000-000000000000") && ! uuid.isBlank() && UuidUtils.isUuid(uuid);
    }

    /**
     * Returns a valid UUID string, replacing the given value with a freshly
     * generated random UUID if it fails the validity check.
     *
     * @param uuid the candidate UUID string to evaluate
     * @return the original UUID if valid, or a new random UUID string otherwise
     */
    public String getProperUuid(String uuid) {
        if (! isProperUuid(uuid)) {
            uuid = UuidUtils.randomStringUuid();
        }

        return uuid;
    }

    /**
     * Constructs and returns a {@link SavedServer} representing this server,
     * populated with the current UUID, name, and platform server type.
     *
     * @return a new {@link SavedServer} for this node
     */
    public SavedServer getServer() {
        return new SavedServer(getUuid(), getName(), Singularity.getInstance().getPlatform().getServerType());
    }

    /**
     * Persists the given name to {@code server.name} in the configuration file.
     *
     * @param name the server name to write
     */
    public void writeName(String name) {
        write("server.name", name);
    }

    /**
     * Persists the given UUID to {@code server.uuid} in the configuration file,
     * auto-correcting an invalid value before writing.
     *
     * @param uuid the UUID string to write
     */
    public void writeUuid(String uuid) {
        if (! isProperUuid(uuid)) {
            uuid = getProperUuid(uuid);
        }

        write("server.uuid", uuid);
    }

    /**
     * Persists both the name and UUID of the given {@link SavedServer} to the
     * configuration file.
     *
     * @param server the server whose identity should be written
     */
    public void writeServer(SavedServer server) {
        writeName(server.getName());
        writeUuid(server.getUuid());
    }

    /**
     * Returns whether the server identity should be automatically corrected
     * when inconsistencies are detected, defaulting to {@code true}.
     * Reloads the resource before reading.
     *
     * @return {@code true} if auto-correction is enabled; {@code false} otherwise
     */
    public boolean isAutoCorrect() {
        reloadResource();

        return getOrSetDefault("server.auto-correct", true);
    }
}
