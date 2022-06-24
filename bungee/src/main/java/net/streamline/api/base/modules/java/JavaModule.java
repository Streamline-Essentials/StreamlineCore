package net.streamline.api.base.modules.java;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import net.streamline.api.base.BasePlugin;
import net.streamline.api.base.command.Command;
import net.streamline.api.base.command.CommandExecutor;
import net.streamline.api.base.command.ModuleCommand;
import net.streamline.api.base.configs.StorageResource;
import net.streamline.api.base.modules.ModuleBase;
import net.streamline.api.base.modules.ModuleDescriptionFile;
import net.streamline.api.base.modules.ModuleLoader;
import net.streamline.api.base.modules.ModuleLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Java module
 */
public abstract class JavaModule extends ModuleBase {
    private boolean isEnabled = false;
    private ModuleLoader loader = null;
    private BasePlugin server = null;
    private File file = null;
    private ModuleDescriptionFile description = null;
    private File dataFolder = null;
    private ClassLoader classLoader = null;
    private boolean naggable = true;
    private StorageResource<?> newConfig = null;
    private File configFile = null;
    private ModuleLogger logger = null;

    public JavaModule() {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if (!(classLoader instanceof ModuleClassLoader)) {
            throw new IllegalStateException("JavaModule requires " + ModuleClassLoader.class.getName());
        }
        ((ModuleClassLoader) classLoader).initialize(this);
    }

    protected JavaModule(@NotNull final JavaModuleLoader loader, @NotNull final ModuleDescriptionFile description, @NotNull final File dataFolder, @NotNull final File file) {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if (classLoader instanceof ModuleClassLoader) {
            throw new IllegalStateException("Cannot use initialization constructor at runtime");
        }
        init(loader, loader.server, description, dataFolder, file, classLoader);
    }

    /**
     * Returns the folder that the module data's files are located in. The
     * folder may not yet exist.
     *
     * @return The folder.
     */
    @NotNull
    @Override
    public final File getDataFolder() {
        return dataFolder;
    }

    /**
     * Gets the associated ModuleLoader responsible for this module
     *
     * @return ModuleLoader that controls this module
     */
    @NotNull
    @Override
    public final ModuleLoader getModuleLoader() {
        return loader;
    }

    /**
     * Returns the Server instance currently running this module
     *
     * @return Server running this module
     */
    @NotNull
    @Override
    public final BasePlugin getServer() {
        return server;
    }

    /**
     * Returns a value indicating whether or not this module is currently
     * enabled
     *
     * @return true if this module is enabled, otherwise false
     */
    @Override
    public final boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Returns the file which contains this module
     *
     * @return File containing this module
     */
    @NotNull
    protected File getFile() {
        return file;
    }

    /**
     * Returns the module.yaml file containing the details for this module
     *
     * @return Contents of the module.yaml file
     */
    @NotNull
    @Override
    public final ModuleDescriptionFile getDescription() {
        return description;
    }

    @NotNull
    public StorageResource<?> getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    /**
     * Provides a reader for a text file located inside the jar.
     * <p>
     * The returned reader will read text with the UTF-8 charset.
     *
     * @param file the filename of the resource to load
     * @return null if {@link #getResource(String)} returns null
     * @throws IllegalArgumentException if file is null
     * @see ClassLoader#getResourceAsStream(String)
     */
    @Nullable
    protected final Reader getTextResource(@NotNull String file) {
        final InputStream in = getResource(file);

        return in == null ? null : new InputStreamReader(in, Charsets.UTF_8);
    }

    @Override
    public void reloadConfig() {
        newConfig.reloadResource();
    }

    @Override
    public void saveConfig() {
        getConfig().sync();
    }

    @Override
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {
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
                in.close();
            } else {
                logger.log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    @Nullable
    @Override
    public InputStream getResource(@NotNull String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Returns the ClassLoader which holds this module
     *
     * @return ClassLoader holding this module
     */
    @NotNull
    protected final ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the enabled state of this module
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


    final void init(@NotNull ModuleLoader loader, @NotNull BasePlugin server, @NotNull ModuleDescriptionFile description, @NotNull File dataFolder, @NotNull File file, @NotNull ClassLoader classLoader) {
        this.loader = loader;
        this.server = server;
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
    public boolean onCommand(@NotNull CommandExecutor sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandExecutor sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }

    /**
     * Gets the command with the given name, specific to this module. Commands
     * need to be registered in the {@link ModuleDescriptionFile#getCommands()
     * ModuleDescriptionFile} to exist at runtime.
     *
     * @param name name or alias of the command
     * @return the module command if found, otherwise null
     */
    @Nullable
    public ModuleCommand getCommand(@NotNull String name) {
        String alias = name.toLowerCase(java.util.Locale.ENGLISH);
        ModuleCommand command = getServer().getModuleCommand(alias);

        if (command == null || command.getModule() != this) {
            command = getServer().getModuleCommand(description.getName().toLowerCase(java.util.Locale.ENGLISH) + ":" + alias);
        }

        if (command != null && command.getModule() == this) {
            return command;
        } else {
            return null;
        }
    }

    @Override
    public void onLoad() {}

    @Override
    public void onDisable() {}

    @Override
    public void onEnable() {}

    @Override
    public final boolean isNaggable() {
        return naggable;
    }

    @Override
    public final void setNaggable(boolean canNag) {
        this.naggable = canNag;
    }

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @NotNull
    @Override
    public String toString() {
        return description.getFullName();
    }

    /**
     * This method provides fast access to the module that has {@link
     * #getProvidingModule(Class) provided} the given module class, which is
     * usually the module that implemented it.
     * <p>
     * An exception to this would be if module's jar that contained the class
     * does not extend the class, where the intended module would have
     * resided in a different jar / classloader.
     *
     * @param <T> a class that extends JavaModule
     * @param clazz the class desired
     * @return the module that provides and implements said class
     * @throws IllegalArgumentException if clazz is null
     * @throws IllegalArgumentException if clazz does not extend {@link
     *     JavaModule}
     * @throws IllegalStateException if clazz was not provided by a module,
     *     for example, if called with
     *     <code>JavaModule.getModule(JavaModule.class)</code>
     * @throws IllegalStateException if called from the static initializer for
     *     given JavaModule
     * @throws ClassCastException if module that provided the class does not
     *     extend the class
     */
    @NotNull
    public static <T extends JavaModule> T getModule(@NotNull Class<T> clazz) {
        Preconditions.checkArgument(clazz != null, "Null class cannot have a module");
        if (!JavaModule.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz + " does not extend " + JavaModule.class);
        }
        final ClassLoader cl = clazz.getClassLoader();
        if (!(cl instanceof ModuleClassLoader)) {
            throw new IllegalArgumentException(clazz + " is not initialized by " + ModuleClassLoader.class);
        }
        JavaModule module = ((ModuleClassLoader) cl).module;
        if (module == null) {
            throw new IllegalStateException("Cannot get module for " + clazz + " from a static initializer");
        }
        return clazz.cast(module);
    }

    /**
     * This method provides fast access to the module that has provided the
     * given class.
     *
     * @param clazz a class belonging to a module
     * @return the module that provided the class
     * @throws IllegalArgumentException if the class is not provided by a
     *     JavaModule
     * @throws IllegalArgumentException if class is null
     * @throws IllegalStateException if called from the static initializer for
     *     given JavaModule
     */
    @NotNull
    public static JavaModule getProvidingModule(@NotNull Class<?> clazz) {
        Preconditions.checkArgument(clazz != null, "Null class cannot have a module");
        final ClassLoader cl = clazz.getClassLoader();
        if (!(cl instanceof ModuleClassLoader)) {
            throw new IllegalArgumentException(clazz + " is not provided by " + ModuleClassLoader.class);
        }
        JavaModule module = ((ModuleClassLoader) cl).module;
        if (module == null) {
            throw new IllegalStateException("Cannot get module for " + clazz + " from a static initializer");
        }
        return module;
    }
}
