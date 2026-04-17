package singularity.modules;

import com.google.common.base.Preconditions;
import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.events.BaseEventListener;
import org.pf4j.*;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import singularity.Singularity;
import singularity.command.CommandHandler;
import singularity.command.ModuleCommand;
import singularity.events.*;
import singularity.events.modules.ModuleLoadEvent;
import singularity.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central registry and lifecycle coordinator for all Streamline modules.
 *
 * <p>This class manages two parallel maps — {@code loadedModules} (every
 * module that has been discovered and registered) and {@code enabledModules}
 * (those that are currently active) — as well as the underlying PF4J
 * {@link JarPluginManager} that loads JAR files from the module folder.
 *
 * <p>Typical flow:
 * <ol>
 *   <li>Platform startup calls {@link #registerExternalModules()} to scan the
 *       module folder and load every JAR.</li>
 *   <li>PF4J instantiates each {@link CosmicModule} subclass, whose constructor
 *       calls {@link #registerModule(ModuleLike)}.</li>
 *   <li>The platform then calls {@link #startModules()} to enable all loaded
 *       modules.</li>
 * </ol>
 */
public class ModuleManager {

    /**
     * All modules that have been registered (loaded) by identifier, including
     * both enabled and disabled ones.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleLike> loadedModules = new ConcurrentSkipListMap<>();

    /**
     * Subset of {@link #loadedModules} containing only the currently enabled
     * modules.
     */
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleLike> enabledModules = new ConcurrentSkipListMap<>();

    /**
     * The PF4J plugin manager used to load, start, stop, and unload module
     * JARs from the configured module folder.
     */
    @Getter @Setter
    private static JarPluginManager pluginManager;

    /**
     * Returns a snapshot of all JAR files present in the module folder, keyed
     * by file name.
     *
     * @return a sorted map of file name to {@link File}, empty if the folder
     *         contains no JARs
     */
    public static ConcurrentSkipListMap<String, File> getModuleFiles() {
        ConcurrentSkipListMap<String, File> r = new ConcurrentSkipListMap<>();

        File[] files = Singularity.getModuleFolder().listFiles();
        if (files == null) return r;

        for (File file : files) {
            if (file.isDirectory()) continue;
            if (! file.getName().endsWith(".jar")) continue;
            r.put(file.getName(), file);
        }

        return r;
    }

    /**
     * Returns the module-folder JAR files as a list of {@link Path} objects.
     *
     * @return an ordered list of paths to the module JARs
     */
    public static List<Path> getModuleFilesAsPaths() {
        List<Path> r = new ArrayList<>();

        getModuleFiles().forEach((s, file) -> {
            r.add(file.toPath());
        });

        return r;
    }

    /**
     * Returns the current {@link JarPluginManager}, creating and caching a new
     * one (backed by the configured module folder) if it has not been created
     * yet.
     *
     * @return a non-null, ready-to-use {@link JarPluginManager}
     */
    public static JarPluginManager safePluginManager() {
        JarPluginManager manager = getPluginManager();
        if (manager != null) return manager;
        manager = new JarPluginManager(Singularity.getModuleFolder().toPath()) {
            @Override
            protected PluginLoader createPluginLoader() {
                return new JarPluginLoader(this);
            }

            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new ManifestPluginDescriptorFinder();
            }
        };
        setPluginManager(manager);
        return manager;
    }

    /**
     * The formatted, color-coded message displayed in the server console when
     * no modules are found in the module folder.  Includes download links and
     * social links for Streamline.
     */
    @Getter
    private static final String noModulesMessage =
            "" +
                    "&a&m&l                                                 %newline%" +
                    "               &c&lStreamline &5&lCore%newline%" +
                    "&eIt appears you do not have any modules installed&7. &eThis%newline%" +
                    "&eplugin was meant to be used with modules&7. &eModules%newline%" +
                    "&eare installable content &b(&7you can create your own&5!&b)%newline%" +
                    "&ethat either expand upon the core plugin or add%newline%" +
                    "&ecompletely new content to your server&b(&es&b)&7.%newline%" +
                    "&eInstall them by placing them in your servers&7'%newline%" +
                    "&7\"&bplugins -> StreamlineCore -> modules&7\" &efolder&7.%newline%" +
                    "&eYou can check them out and download them from our%newline%" +
                    "&c&lModule Public Download Stash&e. Thanks! And enjoy!%newline%" +
                    "&r%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&r%newline%" +
                    "&fUse the following to download &6Modules &ddirectly &c->%newline%" +
                    "&6Modules &c&lPublic Download Stash&7: &bhttps://storage.drak.gg/share/75RJ__vUBVQrC_PRfuHeTg%newline%" +
                    "&r%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&r%newline%" +
                    "&6Streamline &b&lWiki&7: &bhttps://wiki.drak.gg/streamline%newline%" +
                    "&6Streamline &9&lDiscord&7: &bhttps://dsc.gg/streamline%newline%" +
                    "&6Drak&7'&6s &c&lSocials &6Page&7: &bhttps://drak.gg/socials%newline%" +
                    "&r%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&cDisable &ethis message in the &bmain-config.yml%newline%" +
                    "&a&m&l                                                 "
            ;

    /**
     * Attempts to add a module to the loaded-module registry.  If a module
     * with the same identifier is already registered and is not malleable the
     * request is rejected with a warning; if it is malleable the existing
     * entry is first unregistered.  Fires a {@link ModuleLoadEvent} for
     * {@link CosmicModule} instances.
     *
     * @param module the module to load; must not be {@code null}
     */
    public static void loadModule(@NonNull ModuleLike module) {
        if (getLoadedModules().containsKey(module.getIdentifier())) {
            if (! getModule(module.getIdentifier()).isMalleable()) {
                MessageUtils.logWarning(
                        "Module '" + module.getIdentifier() + "' by '" + module.getAuthorsStringed() + "' could not be loaded: identical identifiers"
                );
                return;
            } else {
                unregisterModule(module);
            }
        }

        getLoadedModules().put(module.getIdentifier(), module);
        if (module instanceof CosmicModule) ModuleUtils.fireEvent(new ModuleLoadEvent((CosmicModule) module));
    }

    /**
     * Scans the module folder for external JARs, loads them all via PF4J, and
     * prints a notice to the console.  If no JARs are found the
     * {@link #noModulesMessage} is printed instead.
     */
    public static void registerExternalModules() {
//        if (! getLoadedModules().containsKey("streamline-base")) {
//            BaseModule module = new BaseModule();
//            SLAPI.setBaseModule(module);
//            registerModule(module);
//        }
        if (! hasNonBaseModules()) {
            MessageUtils.logInfo(getNoModulesMessage());
            return;
        }

        MessageUtils.logInfo("&rLoading external modules...");
        Date before = new Date();
        int loaded = loadModulesSafe();
        Date after = new Date();
        long millis = after.getTime() - before.getTime();
        MessageUtils.logInfo("&rLoaded &a" + loaded + " &rexternal modules in &a" + millis + "&6ms&f.");
    }

    /**
     * Iterates every JAR in the plugin manager's root folder and loads each
     * one through PF4J, skipping any that fail with a logged error.
     *
     * @return the number of JARs successfully handed off to PF4J
     */
    public static int loadModulesSafe() {
        AtomicInteger i = new AtomicInteger(0);

        File pathFile = safePluginManager().getPluginsRoot().toFile();
        File[] files = pathFile.listFiles();
        if (files == null) return i.get();

        Arrays.stream(files).forEach(file -> {
            if (file.getName().endsWith(".jar")) {
                try {
                    safePluginManager().loadPlugin(file.toPath());

                    i.getAndIncrement();
                } catch (Exception e) {
                    MessageUtils.logSevere("Could not load module '" + file.getName() + "':", e);
                }
            }
        });

        return i.get();
    }

    /**
     * Loads and starts a single external module JAR by file name.
     *
     * @param jarName the JAR file name (with or without the {@code .jar}
     *                extension) located in the plugin manager's root folder
     */
    public static void registerExternalModule(@NotNull String jarName) {
        if (! jarName.endsWith(".jar")) jarName += ".jar";
        safePluginManager().loadPlugin(safePluginManager().getPluginsRoot().resolve(jarName));
        PluginWrapper plugin = getPluginWrapperByJarName(jarName);
        safePluginManager().startPlugin(plugin.getPluginId());
    }

    /**
     * Finds the PF4J {@link PluginWrapper} whose plugin path ends with the
     * given JAR file name.
     *
     * @param jarName the file name to search for
     * @return the matching {@link PluginWrapper}, or {@code null} if not found
     */
    public static PluginWrapper getPluginWrapperByJarName(String jarName) {
        AtomicReference<PluginWrapper> r = new AtomicReference<>();
        safePluginManager().getPlugins().forEach(plugin -> {
            if (plugin.getPluginPath().endsWith(jarName)) r.set(plugin);
        });
        return r.get();
    }

    /**
     * Returns the file names of all JAR files present in the module folder,
     * regardless of whether they have been loaded by PF4J.
     *
     * @return a sorted set of JAR file names
     */
    public static ConcurrentSkipListSet<String> getExternalModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        File[] files = safePluginManager().getPluginsRoot().toFile().listFiles();
        if (files == null) return r;

        Arrays.asList(files).forEach(file -> {
            if (file.isDirectory()) return;
            if (! file.getName().endsWith(".jar")) return;
            r.add(file.getName());
        });
        return r;
    }

    /**
     * Returns the file names of external module JARs that PF4J has loaded
     * and that are not in the {@link PluginState#DISABLED} state.
     *
     * @return a sorted set of loaded (non-disabled) JAR file names
     */
    public static ConcurrentSkipListSet<String> getLoadedExternalModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getExternalModuleIdentifiers().forEach(identifier -> {
            PluginWrapper pluginWrapper = getPluginWrapperByJarName(identifier);
            if (pluginWrapper == null) return;
            if (pluginWrapper.getPluginState() != PluginState.DISABLED) r.add(identifier);
        });
        return r;
    }

    /**
     * Returns the file names of external module JARs that are present in the
     * folder but have not been loaded by PF4J.
     *
     * @return a sorted set of unloaded JAR file names
     */
    public static ConcurrentSkipListSet<String> getUnloadedExternalModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getExternalModuleIdentifiers().forEach(identifier -> {
            if (getLoadedExternalModuleIdentifiers().contains(identifier)) return;
            r.add(identifier);
        });
        return r;
    }

    /**
     * Returns {@code true} if at least one JAR file exists in any of the
     * plugin manager's root paths.
     *
     * @return {@code true} when at least one module JAR is present
     */
    public static boolean hasNonBaseModules() {
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        safePluginManager().getPluginsRoots().forEach(path -> {
            try {
                File[] files = path.toFile().listFiles();
                if (files == null) return;
                Arrays.stream(files).forEach(file -> {
                    if (file.getName().endsWith(".jar")) atomicBoolean.set(true);
                });
            } catch (Exception e) {
                // do nothing
            }
        });

        return atomicBoolean.get();
    }

    /**
     * Validates and delegates to {@link #loadModule(ModuleLike)}, then logs a
     * confirmation message via the module's own logger.
     *
     * @param module the module to register; must not be {@code null}
     * @throws NullPointerException if {@code module} is {@code null}
     */
    public static void registerModule(ModuleLike module) {
        Preconditions.checkNotNull(module, "module parameter cannot be null.");
        loadModule(module);
        module.logInfo("Registered module!");
    }

    /**
     * Fully unloads a module: stops it, unloads the PF4J plugin, unbakes all
     * event handlers, and removes it from the loaded-module registry.
     *
     * @param module the module to unregister
     */
    public static void unregisterModule(ModuleLike module) {
        try {
//            unloadCommandsForModule(module);
            module.stop();
            safePluginManager().unloadPlugin(module.getIdentifier());
            BaseEventHandler.unbake(module);
            getLoadedModules().remove(module.getIdentifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the registered {@link ModuleLike} for the given identifier, or
     * {@code null} if no such module is loaded.
     *
     * @param identifier the module identifier to look up
     * @return the loaded module, or {@code null}
     */
    public static ModuleLike getModule(String identifier) {
        return getLoadedModules().get(identifier);
    }

    /**
     * Returns all {@link ModuleCommand} instances whose owning module matches
     * the given module.
     *
     * @param module the module whose commands should be retrieved
     * @return a sorted set of commands belonging to that module
     */
    public static ConcurrentSkipListSet<ModuleCommand> getCommandsForModule(ModuleLike module) {
        ConcurrentSkipListSet<ModuleCommand> r = new ConcurrentSkipListSet<>();

        CommandHandler.getLoadedModuleCommands().forEach((s, moduleCommand) -> {
            if (moduleCommand.getOwningModule().getIdentifier().equals(module.getIdentifier())) r.add(moduleCommand);
        });

        return r;
    }

    /**
     * Disables all currently-loaded commands belonging to the given module.
     *
     * @param module the module whose commands should be unloaded
     */
    public static void unloadCommandsForModule(ModuleLike module) {
        getCommandsForModule(module).forEach(command -> {
            if (command.isLoaded()) command.disable();
        });
    }

    /**
     * Hot-reloads a single module by identifier: unbakes its event handlers,
     * stops it, unloads the JAR, reloads the JAR, and starts the plugin again.
     * The base module (if any) is skipped.
     *
     * @param id the identifier of the module to reapply
     */
    public static void reapplyModule(String id) {
        ModuleLike moduleLike = getModule(id);

        if (Singularity.getBaseModule() != null) {
            if (moduleLike.equals(Singularity.getBaseModule())) {
                return;
            }
        } else {
            MessageUtils.logWarning("Base module is null!");
        }

        BaseEventHandler.unbake(moduleLike);

        Path path = safePluginManager().getPlugin(id).getPluginPath();
        try {
            safePluginManager().stopPlugin(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            safePluginManager().unloadPlugin(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            safePluginManager().loadPlugin(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            safePluginManager().startPlugin(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calls {@link ModuleLike#restart()} on every currently loaded module.
     */
    public static void restartModules() {
        getLoadedModules().forEach((s, moduleLike) -> {
            moduleLike.restart();
        });
    }

    /**
     * Starts all plugins known to the underlying PF4J plugin manager.
     */
    public static void startModules() {
        safePluginManager().startPlugins();
    }

    /**
     * Stops all plugins known to the underlying PF4J plugin manager.
     */
    public static void stopModules() {
        safePluginManager().stopPlugins();
    }

    /**
     * Fires a {@link CosmicEvent} through the TheBase event bus.
     *
     * @param event the event to fire; must not be {@code null}
     */
    public static void fireEvent(@NotNull CosmicEvent event) {
        BaseEventHandler.fireEvent(event);
    }

    /**
     * Removes all event-handler registrations associated with the given module
     * from the TheBase event bus.
     *
     * @param <T>    a module type extending {@link ModuleLike}
     * @param module the module whose handlers should be removed
     */
    public static <T extends ModuleLike> void unregisterHandlersOf(T module) {
        BaseEventHandler.unbake(module);
    }

    /**
     * Registers a {@link BaseEventListener} against the TheBase event bus,
     * associating it with the given module so it can be removed later via
     * {@link #unregisterHandlersOf(ModuleLike)}.
     *
     * @param <T>      a module type extending {@link ModuleLike}
     * @param listener the listener to register; must not be {@code null}
     * @param module   the owning module; must not be {@code null}
     */
    public static <T extends ModuleLike> void registerEvents(@NotNull BaseEventListener listener, @NotNull T module) {
        BaseEventHandler.bake(listener, module);
    }

    /**
     * Returns {@code true} if PF4J has a plugin registered under the given
     * identifier (regardless of enabled state).
     *
     * @param identifier the plugin identifier to check
     * @return {@code true} if the plugin exists in PF4J
     */
    public static boolean hasModule(String identifier) {
        return safePluginManager().getPlugin(identifier) != null;
    }

    /**
     * Returns {@code true} if a module with the given identifier is present in
     * the loaded-module registry.
     *
     * @param identifier the module identifier to check
     * @return {@code true} if the module is loaded
     */
    public static boolean hasModuleLoaded(String identifier) {
        return getLoadedModules().containsKey(identifier);
    }

    /**
     * Returns {@code true} if a module with the given identifier is present in
     * the enabled-module registry.
     *
     * @param identifier the module identifier to check
     * @return {@code true} if the module is enabled
     */
    public static boolean hasModuleEnabled(String identifier) {
        return getEnabledModules().containsKey(identifier);
    }

    /**
     * Returns {@code true} if a module with the given identifier is both
     * loaded and enabled.
     *
     * @param identifier the module identifier to check
     * @return {@code true} if the module is loaded and enabled
     */
    public static boolean hasModuleLoadedAndEnabled(String identifier) {
        return hasModuleLoaded(identifier) && hasModuleEnabled(identifier);
    }

    /**
     * Convenience overload of {@link #unregisterModule(ModuleLike)} that
     * accepts the identifier string as a first argument (it is ignored).
     *
     * @param s          unused identifier parameter
     * @param ModuleLike the module to unregister
     */
    public static void unregisterModule(String s, ModuleLike ModuleLike) {
        unregisterModule(ModuleLike);
    }

    /**
     * Returns the subset of loaded modules that are marked as malleable (i.e.
     * can be replaced by a module with the same identifier).
     *
     * @return a sorted set of malleable {@link ModuleLike} instances
     */
    public static ConcurrentSkipListSet<ModuleLike> getOnlyMalleableModules() {
        ConcurrentSkipListSet<ModuleLike> r = new ConcurrentSkipListSet<>();

        getLoadedModules().forEach((s, moduleLike) -> {
            if (moduleLike.isMalleable()) r.add(moduleLike);
        });

        return r;
    }

    /**
     * Returns the identifiers of all malleable loaded modules.
     *
     * @return a sorted set of identifier strings
     */
    public static ConcurrentSkipListSet<String> getOnlyMalleableModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlyMalleableModules().forEach(moduleLike -> {
            r.add(moduleLike.getIdentifier());
        });

        return r;
    }

    /**
     * Returns the identifiers of all malleable modules that are also currently
     * enabled.
     *
     * @return a sorted set of identifier strings for enabled malleable modules
     */
    public static ConcurrentSkipListSet<String> getOnlyMalleableEnabledModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlyMalleableModules().forEach(moduleLike -> {
            if (moduleLike.isEnabled()) r.add(moduleLike.getIdentifier());
        });

        return r;
    }

    /**
     * Returns the identifiers of all currently loaded modules.
     *
     * @return a sorted set of identifier strings
     */
    public static ConcurrentSkipListSet<String> getLoadedModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getLoadedModules().forEach((s, module) -> r.add(module.getIdentifier()));

        return r;
    }

    /**
     * Returns the identifiers of all currently enabled modules.
     *
     * @return a sorted set of identifier strings
     */
    public static ConcurrentSkipListSet<String> getEnabledModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getEnabledModules().forEach((s, module) -> r.add(module.getIdentifier()));

        return r;
    }

    /**
     * Returns the identifiers of all loaded modules with color-code prefixes:
     * {@code &a} for malleable+enabled, {@code &c} for malleable+disabled, and
     * {@code &7} for immalleable modules.
     *
     * @return a sorted set of color-coded identifier strings
     */
    public static ConcurrentSkipListSet<String> getColorizedLoadedModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getLoadedModules().forEach((s, module) -> {
            if (module.isMalleable()) {
                if (module.isEnabled()) {
                    r.add("&a" + module.getIdentifier());
                } else {
                    r.add("&c" + module.getIdentifier());
                }
            } else {
                r.add("&7" + module.getIdentifier());
            }
        });

        return r;
    }
}
