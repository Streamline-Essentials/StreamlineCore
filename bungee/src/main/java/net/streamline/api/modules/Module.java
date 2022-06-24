package net.streamline.api.modules;

import net.streamline.api.BasePlugin;
import net.streamline.api.command.TabExecutor;
import net.streamline.api.configs.StorageResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Represents a Module
 * <p>
 * The use of {@link ModuleBase} is recommended for actual Implementation
 */
public interface Module extends TabExecutor {
    /**
     * Returns the folder that the module data's files are located in. The
     * folder may not yet exist.
     *
     * @return The folder
     */
    @NotNull
    public File getDataFolder();

    /**
     * Returns the module.yaml file containing the details for this module
     *
     * @return Contents of the module.yaml file
     */
    @NotNull
    public ModuleDescriptionFile getDescription();

    /**
     * Gets a {@link StorageResource} for this module, read through
     * "config.yml"
     * <p>
     * If there is a default config.yml embedded in this module, it will be
     * provided as a default for this Configuration.
     *
     * @return Module configuration
     */
    @NotNull
    public StorageResource<?> getConfig();

    /**
     * Gets an embedded resource in this module
     *
     * @param filename Filename of the resource
     * @return File if found, otherwise null
     */
    @Nullable
    public InputStream getResource(@NotNull String filename);

    /**
     * Saves the {@link StorageResource} retrievable by {@link #getConfig()}.
     */
    public void saveConfig();

    /**
     * Saves the raw contents of the default config.yml file to the location
     * retrievable by {@link #getConfig()}.
     * <p>
     * This should fail silently if the config.yml already exists.
     */
    public void saveDefaultConfig();

    /**
     * Saves the raw contents of any resource embedded with a module's .jar
     * file assuming it can be found using {@link #getResource(String)}.
     * <p>
     * The resource is saved into the module's data folder using the same
     * hierarchy as the .jar file (subdirectories are preserved).
     *
     * @param resourcePath the embedded resource path to look for within the
     *     module's .jar file. (No preceding slash).
     * @param replace if true, the embedded resource will overwrite the
     *     contents of an existing file.
     * @throws IllegalArgumentException if the resource path is null, empty,
     *     or points to a nonexistent resource.
     */
    public void saveResource(@NotNull String resourcePath, boolean replace);

    /**
     * Discards any data in {@link #getConfig()} and reloads from disk.
     */
    public void reloadConfig();

    /**
     * Gets the associated ModuleLoader responsible for this module
     *
     * @return ModuleLoader that controls this module
     */
    @NotNull
    public ModuleLoader getModuleLoader();

    /**
     * Returns the Server instance currently running this module
     *
     * @return Server running this module
     */
    @NotNull
    public BasePlugin getBase();

    /**
     * Returns a value indicating whether or not this module is currently
     * enabled
     *
     * @return true if this module is enabled, otherwise false
     */
    public boolean isEnabled();

    /**
     * Called when this module is disabled
     */
    public void onDisable();

    /**
     * Called after a module is loaded but before it has been enabled.
     * <p>
     * When multiple modules are loaded, the onLoad() for all modules is
     * called before any onEnable() is called.
     */
    public void onLoad();

    /**
     * Called when this module is enabled
     */
    public void onEnable();

    /**
     * Simple boolean if we can still nag to the logs about things
     *
     * @return boolean whether we can nag
     */
    public boolean isNaggable();

    /**
     * Set naggable state
     *
     * @param canNag is this module still naggable?
     */
    public void setNaggable(boolean canNag);

    /**
     * Returns the module logger associated with this server's logger. The
     * returned logger automatically tags all log messages with the module's
     * name.
     *
     * @return Logger associated with this module
     */
    @NotNull
    public Logger getLogger();

    /**
     * Returns the name of the module.
     * <p>
     * This should return the bare name of the module and should be used for
     * comparison.
     *
     * @return name of the module
     */
    @NotNull
    public String getName();
}