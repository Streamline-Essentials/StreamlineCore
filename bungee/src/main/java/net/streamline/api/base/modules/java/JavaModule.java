package net.streamline.api.base.modules.java;

import net.md_5.bungee.api.ProxyServer;
import net.streamline.api.base.modules.*;

import java.io.*;
import java.util.logging.Level;

public class JavaModule extends BaseModule {
    private boolean isEnabled = false;
    private ModuleLoader loader = null;
    private ProxyServer server = null;
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
        init(loader, loader.server, description, dataFolder, file, classLoader);
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
    public final ProxyServer getServer() {
        return server;
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

}
