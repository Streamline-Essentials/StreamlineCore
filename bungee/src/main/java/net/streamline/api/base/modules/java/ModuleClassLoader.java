package net.streamline.api.base.modules.java;

import net.md_5.bungee.api.plugin.Plugin;
import net.streamline.api.base.BasePlugin;
import net.streamline.api.base.Streamline;
import net.streamline.api.base.modules.InvalidModuleException;
import net.streamline.api.base.modules.ModuleDescriptionFile;
import org.apache.commons.lang3.Validate;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleClassLoader extends URLClassLoader {
    private final JavaModuleLoader loader;
    private final Map<String, Class<?>> classes = new java.util.concurrent.ConcurrentHashMap<String, Class<?>>(); // Spigot
    private final ModuleDescriptionFile description;
    private final File dataFolder;
    private final File file;
    final JavaModule plugin;
    private JavaModule pluginInit;
    private IllegalStateException pluginState;

    ModuleClassLoader(final JavaModuleLoader loader, final ClassLoader parent, final ModuleDescriptionFile description, final File dataFolder, final File file) throws Throwable {
        super(new URL[] {file.toURI().toURL()}, parent);
        Validate.notNull(loader, "Loader cannot be null");

        this.loader = loader;
        this.description = description;
        this.dataFolder = dataFolder;
        this.file = file;

        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.getMain(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidModuleException("Cannot find main class `" + description.getMain() + "'", ex);
            }

            Class<? extends JavaModule> moduleClass;
            try {
                moduleClass = jarClass.asSubclass(JavaModule.class);
            } catch (ClassCastException ex) {
                throw new InvalidModuleException("main class `" + description.getMain() + "' does not extend JavaPlugin", ex);
            }

            plugin = moduleClass.getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException ex) {
            throw new InvalidModuleException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidModuleException("Abnormal plugin type", ex);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            if (checkGlobal) {
                result = loader.getClassByName(name);
            }

            if (result == null) {
                result = super.findClass(name);

                if (result != null) {
                    loader.setClass(name, result);
                }
            }

            classes.put(name, result);
        }

        return result;
    }

    Set<String> getClasses() {
        return classes.keySet();
    }

    synchronized void initialize(JavaModule javaModule) {
        Validate.notNull(javaModule, "Initializing plugin cannot be null");
        Validate.isTrue(javaModule.getClass().getClassLoader() == this, "Cannot initialize plugin outside of this class loader");
        if (this.plugin != null || this.pluginInit != null) {
            throw new IllegalArgumentException("Plugin already initialized!", pluginState);
        }

        pluginState = new IllegalStateException("Initial initialization");
        this.pluginInit = javaModule;

        javaModule.init(loader, loader.server, description, dataFolder, file, this);
    }
}
