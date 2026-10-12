package net.streamline.platform;

import net.streamline.platform.modules.LegacyModuleTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import singularity.client.ClientEnvironment;
import singularity.client.module.ClientModuleManager;
import singularity.utils.StorageUtils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;

/**
 * Streamline on a Minecraft 1.8.9 Forge client.
 *
 * <p>A 1.8.9 client never attaches to a server, integrated or remote, so it runs the lean
 * module core only: {@link ClientModuleManager} with its client modules, the HUD and the
 * module menu. Singularity's users, database, permissions and server modules never start.</p>
 */
public abstract class BasePlugin {

    /** The game's own logger, which reaches the launcher console and latest.log. */
    private static final Logger GAME_LOGGER = LogManager.getLogger("StreamlineCore");

    private static BasePlugin instance;

    private String version;
    private String folderName;

    private boolean booted;
    private boolean shutDown;

    public static BasePlugin getInstance() {
        return instance;
    }

    /** Called once from the mod's constructor. */
    protected void initialize() {
        instance = this;
        LegacyModuleTransformer.install();
        setupProperties();
    }

    /**
     * Starts the client modules, which then load on their own thread. Called once the game has
     * started; later calls do nothing.
     */
    public synchronized void boot() {
        if (booted || shutDown) return;
        booted = true;

        // HUD placements, client module settings and client modules live under
        // mods/<name>/client, as on the other mod loaders.
        ClientEnvironment.setFolder(new File(getDataFolder(), "client"));
        ClientEnvironment.setLog((level, message) -> {
            if (level.intValue() >= Level.WARNING.intValue()) GAME_LOGGER.warn(message);
            else GAME_LOGGER.info(message);
        });
        ClientModuleManager.start();
    }

    /** Stops the client modules for good; the game is exiting. Runs once. */
    public synchronized void shutdownStreamline() {
        if (shutDown) return;
        shutDown = true;
        ClientModuleManager.shutdown();
    }

    /** {@code mods/<name>/} in the game directory. */
    public File getDataFolder() {
        return new File(gg.drak.thebase.storage.StorageUtils.getEnvironmentFolder(),
                "mods" + File.separator + getFolderName() + File.separator);
    }

    private void setupProperties() {
        ConcurrentSkipListMap<String, String> properties = StorageUtils.readProperties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().equals("name")) this.folderName = entry.getValue();
            if (entry.getKey().equals("version")) this.version = entry.getValue();
        }
    }

    public String getFolderName() {
        return folderName != null ? folderName : "StreamlineCore";
    }

    public String getVersion() {
        return version;
    }

    /** Logs to the game's log, which is where a 1.8.9 player looks. */
    public static void log(String message) {
        GAME_LOGGER.info(message);
    }

    /** Logs a warning to the game's log. */
    public static void warn(String message, Throwable error) {
        GAME_LOGGER.warn(message, error);
    }
}
