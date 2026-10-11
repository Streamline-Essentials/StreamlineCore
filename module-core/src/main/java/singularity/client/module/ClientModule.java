package singularity.client.module;

import org.pf4j.Plugin;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;
import singularity.client.ClientBridge;
import singularity.client.ClientEnvironment;

import java.io.File;
import java.io.InputStream;

/**
 * Base class of a client module: a module jar tagged {@code modded-client} that runs inside
 * the game client, on any server or none.
 *
 * <p>A client has no Streamline server around it: no players, permissions, database,
 * commands or events. A client module has an id, a data folder, a log, and the client API in
 * {@code singularity.client} ({@link ClientBridge}, the HUD and settings registries). Client
 * modules compile against {@code StreamlineCore-ModuleCore} only, so nothing else is in reach.</p>
 *
 * <p>{@link ClientModuleManager} loads, enables, disables and unloads client modules while
 * the game runs, so a module must leave nothing behind in {@link #onDisable()}: stop its
 * threads and drop its static state. HUD elements and settings pages it registered are
 * removed for it.</p>
 */
public abstract class ClientModule extends Plugin {

    private volatile boolean enabled;

    public ClientModule(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Runs once, right after the module is constructed and before it is first enabled. */
    public void onLoad() {
    }

    /** Runs when the module is enabled: at game start, or when the player turns it on. */
    public void onEnable() {
    }

    /** Runs when the module is disabled, unloaded, updated or removed. */
    public void onDisable() {
    }

    /** Enables the module unless it already is. {@link ClientModuleManager} calls this. */
    @Override
    public final synchronized void start() {
        if (enabled) return;
        onEnable();
        enabled = true;
    }

    /**
     * Disables the module unless it already is. Its HUD elements and settings page are
     * dropped even if {@link #onDisable()} fails, so the render thread never calls into a
     * module that is gone.
     */
    @Override
    public final synchronized void stop() {
        if (! enabled) return;
        enabled = false;
        try {
            onDisable();
        } finally {
            ClientBridge.forgetModule(getIdentifier());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PluginDescriptor getDescriptor() {
        return wrapper.getDescriptor();
    }

    /** The module's Plugin-Id. */
    public String getIdentifier() {
        return getDescriptor().getPluginId();
    }

    public String getVersion() {
        return getDescriptor().getVersion();
    }

    /**
     * The module's own folder, {@code client/module-data/<module-id>/}, created on first use.
     * It is kept when the module is updated or removed.
     */
    public File getDataFolder() {
        File folder = ClientEnvironment.file("module-data", getIdentifier());
        if (folder == null) folder = new File("streamline-client-module-data", getIdentifier());
        if (! folder.isDirectory()) folder.mkdirs();
        return folder;
    }

    /** A resource bundled in the module jar, or {@code null}. */
    public InputStream getResourceAsStream(String path) {
        return wrapper.getPluginClassLoader().getResourceAsStream(path);
    }

    public void logInfo(String message) {
        ClientEnvironment.logInfo("[" + getIdentifier() + "] " + message);
    }

    public void logWarning(String message) {
        ClientEnvironment.logWarning("[" + getIdentifier() + "] " + message);
    }
}
