package net.streamline.api.modules;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.command.CommandHandler;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.events.*;
import net.streamline.api.events.modules.ModuleLoadEvent;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.utils.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.pf4j.*;
import tv.quaint.events.BaseEventHandler;
import tv.quaint.events.BaseEventListener;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
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

        File[] files = SLAPI.getModuleFolder().listFiles();
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
        manager = new JarPluginManager(SLAPI.getModuleFolder().toPath()) {
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
            "&a&m&l                                                 %newline%" +
                    "               &c&lStreamline &5&lCore%newline%" +
                    "&eWe noticed you do not have any modules&7... &eThis%newline%" +
                    "&eplugin works best by having modules&7. &eModules%newline%" +
                    "&eare installable content &b(&7you can create your own&5!&b)%newline%" +
                    "&ethat either expand upon the core plugin or add%newline%" +
                    "&ecompletely new content to your server&b(&es&b)&7. &eYou %newline%" +
                    "&ecan check them out on our &9&lDiscord &eor on the%newline%" +
                    "&e&7&cSpigotMC &ewebsite&7! &eThank you&7!%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&cDisable &ethis message in your &bmain-config.yml%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&7Streamline &9&lDiscord &dHub&7: &bhttps://dsc.gg/streamline%newline%" +
                    "&7Streamline &5&lModules &bGuide&7: &bhttps://github.com/Streamline-Essentials/StreamlineWiki/wiki/Modules#download%newline%" +
                    "&a&m&l                                                 "
            ;

    public static void loadModule(@NonNull ModuleLike module) {
        if (getLoadedModules().containsKey(module.identifier())) {
            MessageUtils.logWarning("Module '" + module.identifier() + "' by '" + module.getAuthorsStringed() + "' could not be loaded: identical identifiers");
            return;
        }

        getLoadedModules().put(module.identifier(), module);
        if (module instanceof StreamlineModule m) ModuleUtils.fireEvent(new ModuleLoadEvent(m));
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
        safePluginManager().loadPlugins();
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
            safePluginManager().stopPlugin(module.identifier());
            unregisterHandlersOf(module);
            getLoadedModules().remove(module.identifier());
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
            if (moduleCommand.getOwningModule().identifier().equals(module.identifier())) r.add(moduleCommand);
        });

        return r;
    }

    public static void unloadCommandsForModule(ModuleLike module) {
        getCommandsForModule(module).forEach(command -> {
            if (command.isLoaded()) command.disable();
        });
    }

    public static void reapplyModule(String id) {
        if (id.equals(SLAPI.getBaseModule().identifier())) {
            if (SLAPI.getBaseModule() != null) SLAPI.getBaseModule().stop();
            BaseModule module = new BaseModule();
            SLAPI.setBaseModule(module);
            return;
        }
        Path path = safePluginManager().getPlugin(id).getPluginPath();
        safePluginManager().unloadPlugin(id);
        safePluginManager().loadPlugin(path);
        safePluginManager().startPlugin(id);
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

    public static void fireEvent(@NotNull StreamlineEvent event) {
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
}
