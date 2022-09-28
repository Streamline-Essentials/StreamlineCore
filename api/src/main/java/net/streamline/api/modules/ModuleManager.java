package net.streamline.api.modules;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.command.CommandHandler;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.events.*;
import net.streamline.api.events.modules.ModuleLoadEvent;
import net.streamline.api.modules.dependencies.Dependency;
import org.jetbrains.annotations.NotNull;
import org.pf4j.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleManager {
    @Getter @Setter
    private static ConcurrentSkipListMap<String, StreamlineModule> loadedModules = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, StreamlineModule> enabledModules = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private static PluginManager pluginManager;

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

    public static PluginManager safePluginManager() {
        PluginManager manager = getPluginManager();
        if (manager != null) return manager;
        manager = new DefaultPluginManager(List.of(SLAPI.getModuleFolder().toPath())) {
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
                    "&ecan check them out on our &9&lDiscord &eor on our%newline%" +
                    "&epublic folder&7! &eThank you&7!%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&cDisable &ethis message in your &bmain-config.yml%newline%" +
                    "&a&m&l                                                 %newline%" +
                    "&7Streamline &9&lDiscord&7: &bhttps://discord.gg/tny494zXfn%newline%" +
                    "&7Streamline &5&lModules&7: &bhttps://www.mediafire.com/folder/fmduksvzxqlcu/modules" +
                    "&a&m&l                                                 "
            ;

    public static void loadModule(@NonNull StreamlineModule module) {
        if (getLoadedModules().containsKey(module.identifier())) {
            SLAPI.getInstance().getMessenger().logWarning("Module '" + module.identifier() + "' by '" + module.getAuthorsStringed() + "' could not be loaded: identical identifiers");
            return;
        }

        getLoadedModules().put(module.identifier(), module);
        ModuleUtils.fireEvent(new ModuleLoadEvent(module));
    }

    public static void registerExternalModules() {
        safePluginManager().loadPlugins();
        safePluginManager().startPlugins();
    }

    public static void registerModule(StreamlineModule module) {
        Preconditions.checkNotNull(module, "module parameter cannot be null.");
        loadModule(module);
    }

    public static void unregisterModule(StreamlineModule module) {
        try {
            safePluginManager().stopPlugin(module.identifier());
            unregisterHandlersOf(module);
            getLoadedModules().remove(module.identifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StreamlineModule getModule(String identifier) {
        return getLoadedModules().get(identifier);
    }

    public static ConcurrentSkipListSet<ModuleCommand> getCommandsForModule(StreamlineModule module) {
        ConcurrentSkipListSet<ModuleCommand> r = new ConcurrentSkipListSet<>();

        CommandHandler.getLoadedModuleCommands().forEach((s, moduleCommand) -> {
            if (moduleCommand.getOwningModule().identifier().equals(module.identifier())) r.add(moduleCommand);
        });

        return r;
    }

    public static void unloadCommandsForModule(StreamlineModule module) {
        getCommandsForModule(module).forEach(command -> {
            if (command.isLoaded()) command.disable();
        });
    }

    public static void reapplyModule(String id) {
        if (id.equals(SLAPI.getBaseModule().identifier())) return;
        Path path = safePluginManager().getPlugin(id).getPluginPath();
        safePluginManager().unloadPlugin(id);
        safePluginManager().loadPlugin(path);
        safePluginManager().startPlugin(id);
    }

    public static void restartModules() {
        safePluginManager().stopPlugins();
        safePluginManager().startPlugins();
    }

    public static void startModules() {
        safePluginManager().startPlugins();
    }

    public static void stopModules() {
        safePluginManager().stopPlugins();
    }

    public static void fireEvent(@NotNull StreamlineEvent event) {
        HandlerList handlers = getEventListeners(event);
        RegisteredListener[] listeners = handlers.getRegisteredListeners();

        for (RegisteredListener registration : listeners) {
            if (! registration.getModule().isEnabled()) {
                continue;
            }

            try {
                registration.callEvent(event);
            } catch (Throwable ex) {
                SLAPI.getInstance().getMessenger().logSevere("Could not pass event '" + event.getEventName() + "' to '" + registration.getModule().identifier() + "' for reason: ");
                ex.printStackTrace();
            }
        }
    }

    @Getter @Setter
    private static ConcurrentHashMap<Class<? extends StreamlineEvent>, HandlerList> registeredHandlers = new ConcurrentHashMap<>();

    public static void unregisterHandlersOf(StreamlineModule module) {
        new ArrayList<>(getRegisteredHandlers().keySet()).forEach(a -> {
            HandlerList list = getRegisteredHandlers().get(a);
            list.unregister(module);
        });
    }

    private static <O extends StreamlineEvent> HandlerList getEventListeners(@NotNull Class<O> type) {
        HandlerList list = getRegisteredHandlers().get(type);
        if (list == null) list = new HandlerList();
        return list;
    }

    private static <O extends StreamlineEvent> HandlerList getEventListeners(O event) {
        HandlerList list = getRegisteredHandlers().get(event.getClass());
        if (list == null) list = new HandlerList();
        return list;
    }

    public static void registerEvents(@NotNull StreamlineListener listener, @NotNull StreamlineModule module) {
        for (Map.Entry<Class<? extends StreamlineEvent>, Set<RegisteredListener>> entry : createRegisteredListeners(listener, module).entrySet()) {
            Class<? extends StreamlineEvent> clazz = entry.getKey();
            if (clazz == null) continue;
            HandlerList list = getEventListeners(clazz);
            if (list == null) list = new HandlerList();
            list.registerAll(entry.getValue());
            getRegisteredHandlers().put(entry.getKey(), list);
        }

    }

    public static Map<Class<? extends StreamlineEvent>, Set<RegisteredListener>> createRegisteredListeners(StreamlineListener listener, StreamlineModule module) {
        Preconditions.checkArgument(module != null, "Plugin can not be null");
        Preconditions.checkArgument(listener != null, "Listener can not be null");

//        boolean useTimings = server.getPluginManager().useTimings();
        Map<Class<? extends StreamlineEvent>, Set<RegisteredListener>> ret = new HashMap<>();
        Set<Method> methods;
        try {
            Method[] publicMethods = listener.getClass().getMethods();
            Method[] privateMethods = listener.getClass().getDeclaredMethods();
            methods = new HashSet<Method>(publicMethods.length + privateMethods.length, 1.0f);
            methods.addAll(Arrays.asList(publicMethods));
            methods.addAll(Arrays.asList(privateMethods));
        } catch (NoClassDefFoundError e) {
            module.logSevere("Module '" + module.identifier() + "' has failed to register events for '" + listener.getClass() + "' because '" + e.getMessage() + "' does not exist.");
            return ret;
        }

        for (final Method method : methods) {
            final EventProcessor eh = method.getAnnotation(EventProcessor.class);
            if (eh == null) continue;
            // Do not register bridge or synthetic methods to avoid event duplication
            // Fixes SPIGOT-893
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            final Class<?> checkClass;
            if (method.getParameterTypes().length != 1 || !StreamlineEvent.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                module.logSevere("Module '" + module.identifier() + "' attempted to register an invalid EventProcessor method signature \"" + method.toGenericString() + "\" in '" + listener.getClass() + "'");
                continue;
            }
            final Class<? extends StreamlineEvent> eventClass = (Class<? extends StreamlineEvent>) checkClass.asSubclass(StreamlineEvent.class);
            method.setAccessible(true);
            Set<RegisteredListener> eventSet = ret.computeIfAbsent(eventClass, k -> new HashSet<RegisteredListener>());

            for (Class<?> clazz = eventClass; StreamlineEvent.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
                // This loop checks for extending deprecated events
                if (clazz.getAnnotation(Deprecated.class) != null) {
                    module.logInfo(
                            String.format(
                                    "'%s' has registered a listener for '%s' on method '%s', but the event is Deprecated. Please notify these authors: %s.",
                                    module.identifier(),
                                    clazz.getName(),
                                    method.toGenericString(),
                                    module.getAuthorsStringed()));
                    break;
                }
            }

            EventExecutor executor = (listener1, event) -> {
                try {
                    if (!eventClass.isAssignableFrom(event.getClass())) {
                        return;
                    }
                    method.invoke(listener1, event);
                } catch (InvocationTargetException ex) {
                    throw new EventException(ex.getCause());
                } catch (Throwable t) {
                    throw new EventException(t);
                }

                event.setCompleted(true);
            };

            eventSet.add(new RegisteredListener(listener, executor, eh.priority(), module, eh.ignoreCancelled()));
        }
        return ret;
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
