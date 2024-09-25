package singularity.modules;

import com.google.common.base.Preconditions;
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
import tv.quaint.events.BaseEventHandler;
import tv.quaint.events.BaseEventListener;

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

public class ModuleManager {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleLike> loadedModules = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ModuleLike> enabledModules = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private static JarPluginManager pluginManager;

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

    public static List<Path> getModuleFilesAsPaths() {
        List<Path> r = new ArrayList<>();

        getModuleFiles().forEach((s, file) -> {
            r.add(file.toPath());
        });

        return r;
    }

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
                    "&eInstall them by placing them in your server&7'&es%newline%" +
                    "&7\"&bplugins -> StreamlineCore -> modules&7\" &efolder&7.%newline%" +
                    " &eYou can check them out and download them from our%newline%" +
                    " &ewiki&7, &eour &9&lDiscord &eor on the &cSpigotMC%newline%" +
                    "&epages for each&7. &eThank you&7!%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&cDisable &ethis message in the &bmain-config.yml%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&6Our &b&lWiki&7: &bhttps://wiki.plas.host/en/streamline/modules%newline%" +
                    "&6Our &9&lDiscord&7: &bhttps://dsc.gg/streamline%newline%" +
                    "&6Drak&7'&6s &c&lSpigotMC &6Page&7: &bhttps://www.spigotmc.org/resources/authors/393831%newline%" +
                    "&a&m&l                                                 "
            ;

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

    public static void registerExternalModule(@NotNull String jarName) {
        if (! jarName.endsWith(".jar")) jarName += ".jar";
        safePluginManager().loadPlugin(safePluginManager().getPluginsRoot().resolve(jarName));
        PluginWrapper plugin = getPluginWrapperByJarName(jarName);
        safePluginManager().startPlugin(plugin.getPluginId());
    }

    public static PluginWrapper getPluginWrapperByJarName(String jarName) {
        AtomicReference<PluginWrapper> r = new AtomicReference<>();
        safePluginManager().getPlugins().forEach(plugin -> {
            if (plugin.getPluginPath().endsWith(jarName)) r.set(plugin);
        });
        return r.get();
    }

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

    public static ConcurrentSkipListSet<String> getLoadedExternalModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getExternalModuleIdentifiers().forEach(identifier -> {
            PluginWrapper pluginWrapper = getPluginWrapperByJarName(identifier);
            if (pluginWrapper == null) return;
            if (pluginWrapper.getPluginState() != PluginState.DISABLED) r.add(identifier);
        });
        return r;
    }

    public static ConcurrentSkipListSet<String> getUnloadedExternalModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getExternalModuleIdentifiers().forEach(identifier -> {
            if (getLoadedExternalModuleIdentifiers().contains(identifier)) return;
            r.add(identifier);
        });
        return r;
    }

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

    public static void registerModule(ModuleLike module) {
        Preconditions.checkNotNull(module, "module parameter cannot be null.");
        loadModule(module);
        module.logInfo("Registered module!");
    }

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

    public static ModuleLike getModule(String identifier) {
        return getLoadedModules().get(identifier);
    }

    public static ConcurrentSkipListSet<ModuleCommand> getCommandsForModule(ModuleLike module) {
        ConcurrentSkipListSet<ModuleCommand> r = new ConcurrentSkipListSet<>();

        CommandHandler.getLoadedModuleCommands().forEach((s, moduleCommand) -> {
            if (moduleCommand.getOwningModule().getIdentifier().equals(module.getIdentifier())) r.add(moduleCommand);
        });

        return r;
    }

    public static void unloadCommandsForModule(ModuleLike module) {
        getCommandsForModule(module).forEach(command -> {
            if (command.isLoaded()) command.disable();
        });
    }

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

    public static void restartModules() {
        getLoadedModules().forEach((s, moduleLike) -> {
            moduleLike.restart();
        });
    }

    public static void startModules() {
        safePluginManager().startPlugins();
    }

    public static void stopModules() {
        safePluginManager().stopPlugins();
    }

    public static void fireEvent(@NotNull CosmicEvent event) {
        BaseEventHandler.fireEvent(event);
    }

    public static <T extends ModuleLike> void unregisterHandlersOf(T module) {
        BaseEventHandler.unbake(module);
    }

    public static <T extends ModuleLike> void registerEvents(@NotNull BaseEventListener listener, @NotNull T module) {
        BaseEventHandler.bake(listener, module);
    }

    public static boolean hasModule(String identifier) {
        return safePluginManager().getPlugin(identifier) != null;
    }

    public static boolean hasModuleLoaded(String identifier) {
        return getLoadedModules().containsKey(identifier);
    }

    public static boolean hasModuleEnabled(String identifier) {
        return getEnabledModules().containsKey(identifier);
    }

    public static boolean hasModuleLoadedAndEnabled(String identifier) {
        return hasModuleLoaded(identifier) && hasModuleEnabled(identifier);
    }

    public static void unregisterModule(String s, ModuleLike ModuleLike) {
        unregisterModule(ModuleLike);
    }

    public static ConcurrentSkipListSet<ModuleLike> getOnlyMalleableModules() {
        ConcurrentSkipListSet<ModuleLike> r = new ConcurrentSkipListSet<>();

        getLoadedModules().forEach((s, moduleLike) -> {
            if (moduleLike.isMalleable()) r.add(moduleLike);
        });

        return r;
    }

    public static ConcurrentSkipListSet<String> getOnlyMalleableModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlyMalleableModules().forEach(moduleLike -> {
            r.add(moduleLike.getIdentifier());
        });

        return r;
    }

    public static ConcurrentSkipListSet<String> getOnlyMalleableEnabledModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getOnlyMalleableModules().forEach(moduleLike -> {
            if (moduleLike.isEnabled()) r.add(moduleLike.getIdentifier());
        });

        return r;
    }

    public static ConcurrentSkipListSet<String> getLoadedModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getLoadedModules().forEach((s, module) -> r.add(module.getIdentifier()));

        return r;
    }

    public static ConcurrentSkipListSet<String> getEnabledModuleIdentifiers() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getEnabledModules().forEach((s, module) -> r.add(module.getIdentifier()));

        return r;
    }

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
