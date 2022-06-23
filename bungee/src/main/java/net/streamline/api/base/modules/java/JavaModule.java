package net.streamline.api.base.modules.java;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.streamline.api.base.BasePlugin;
import net.streamline.api.base.command.ModuleCommand;
import net.streamline.api.base.modules.*;
import org.apache.commons.lang3.Validate;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class JavaModule extends BaseModule {
    private boolean isEnabled = false;
    private ModuleLoader loader = null;
    private BasePlugin base = null;
    private File file = null;
    private ModuleDescriptionFile description = null;
    private File dataFolder = null;
    private ClassLoader classLoader = null;
    private boolean naggable = true;
    /*private FileConfiguration newConfig = null;*/
    private File configFile = null;
    private ModuleLogger logger = null;

    public JavaModule() {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if(!(classLoader instanceof ModuleClassLoader)) {
            throw new IllegalStateException("JavaModule requires " + ModuleClassLoader.class.getName());
        }
        ((ModuleClassLoader) classLoader).initialize(this);
    }

    protected JavaModule(final JavaModuleLoader loader, final ModuleDescriptionFile description, final File dataFolder, final File file) {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if (classLoader instanceof ModuleClassLoader) {
            throw new IllegalStateException("Cannot use initialization constructor at runtime");
        }
        init(loader, loader.base, description, dataFolder, file, classLoader);
    }

    /**
     * Returns the folder that the plugin data's files are located in. The
     * folder may not yet exist.
     *
     * @return The folder.
     */
    @Override
    public final File getDataFolder() {
        return dataFolder;
    }

    /**
     * Gets the associated PluginLoader responsible for this plugin
     *
     * @return PluginLoader that controls this plugin
     */
    @Override
    public final ModuleLoader getPluginLoader() {
        return loader;
    }

    /**
     * Returns the Server instance currently running this plugin
     *
     * @return Server running this plugin
     */
    @Override
    public final BasePlugin getBase() {
        return base;
    }

    /**
     * Returns a value indicating whether or not this plugin is currently
     * enabled
     *
     * @return true if this plugin is enabled, otherwise false
     */
    @Override
    public final boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Returns the file which contains this plugin
     *
     * @return File containing this plugin
     */
    protected File getFile() {
        return file;
    }

    /**
     * Returns the plugin.yaml file containing the details for this plugin
     *
     * @return Contents of the plugin.yaml file
     */
    @Override
    public final ModuleDescriptionFile getDescription() {
        return description;
    }

    private boolean isStrictlyUTF8() {
        return getDescription().getAwareness().contains(ModuleAwareness.Flags.UTF8);
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + file);
        }

        File outFile = new File(dataFolder, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(dataFolder, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                ((InputStream) in).close();
            } else {
                logger.log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    @Override
    public void reloadConfig() {

    }

    @Override
    public ModuleLoader getModuleLoader() {
        return null;
    }

    @Override
    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = ((URL) url).openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    @Override
    public void saveConfig() {

    }

    @Override
    public void saveDefaultConfig() {

    }

    /**
     * Returns the ClassLoader which holds this plugin
     *
     * @return ClassLoader holding this plugin
     */
    protected final ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the enabled state of this plugin
     *
     * @param enabled true if enabled, otherwise false
     */
    protected final void setEnabled(final boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;

            if (isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    final void init(ModuleLoader loader, BasePlugin base, ModuleDescriptionFile description, File dataFolder, File file, ClassLoader classLoader) {
        this.loader = loader;
        this.base = base;
        this.file = file;
        this.description = description;
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
        this.configFile = new File(dataFolder, "config.yml");
        this.logger = new ModuleLogger(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

    /**
     * Gets the command with the given name, specific to this plugin. Commands
     * need to be registered in the {@link ModuleDescriptionFile#getCommands()
     * PluginDescriptionFile} to exist at runtime.
     *
     * @param name name or alias of the command
     * @return the plugin command if found, otherwise null
     */
    public ModuleCommand getCommand(String name) {
        String alias = name.toLowerCase();
        ModuleCommand command = getBase().getModuleCommand(alias);

        if (command == null || command.getModule() != this) {
            command = getBase().getModuleCommand(description.getName().toLowerCase() + ":" + alias);
        }

        if (command != null && command.getModule() == this) {
            return command;
        } else {
            return null;
        }
    }

    @Override public void onLoad() {}
    @Override public void onDisable() {}
    @Override public void onEnable() {}
    @Override public final boolean isNaggable() {return naggable;}
    @Override public final void setNaggable(boolean canNag) {this.naggable = canNag;}
    @Override public final Logger getLogger() {return logger;}
    @Override public String toString() {return description.getFullName();}
    /**
     * This method provides fast access to the module that has {@link
     * #getModule(Class)}  provided} the given module class, which is
     * usually the module that implemented it.
     * <p>
     * An exception to this would be if module's jar that contained the class
     * does not extend the class, where the intended module would have
     * resided in a different jar / classloader.
     *
     * @param clazz the class desired
     * @return the plugin that provides and implements said class
     * @throws IllegalArgumentException if clazz is null
     * @throws IllegalArgumentException if clazz does not extend {@link
     *     JavaModule}
     * @throws IllegalStateException if clazz was not provided by a plugin,
     *     for example, if called with
     *     <code>JavaPlugin.getPlugin(JavaPlugin.class)</code>
     * @throws IllegalStateException if called from the static initializer for
     *     given JavaPlugin
     * @throws ClassCastException if plugin that provided the class does not
     *     extend the class
     */
    public static <T extends JavaModule> T getModule(Class<T> clazz) {
        Validate.notNull(clazz, "Null class cannot have a plugin");
        if (!JavaModule.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz + " does not extend " + JavaModule.class);
        }
        final ClassLoader cl = clazz.getClassLoader();
        if (!(cl instanceof ModuleClassLoader)) {
            throw new IllegalArgumentException(clazz + " is not initialized by " + ModuleClassLoader.class);
        }
        JavaModule plugin = ((ModuleClassLoader) cl).plugin;
        if (plugin == null) {
            throw new IllegalStateException("Cannot get plugin for " + clazz + " from a static initializer");
        }
        return clazz.cast(plugin);
    }


}
