package net.streamline.api.modules;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.events.*;
import net.streamline.api.events.modules.ModuleLoadEvent;
import net.streamline.api.modules.dependencies.Dependency;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.JarFiles;
import net.streamline.utils.MessagingUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleManager {
    public static TreeMap<String, StreamlineModule> loadedModules = new TreeMap<>();
    public static TreeMap<String, StreamlineModule> enabledModules = new TreeMap<>();

    public static void loadModule(@NonNull StreamlineModule module) {
        if (loadedModules.containsKey(module.identifier())) {
            MessagingUtils.logWarning("Module '" + module.identifier() + "' by '" + module.getAuthorsStringed() + "' could not be loaded: identical identifiers");
            return;
        }

        loadedModules.put(module.identifier(), module);
        ModuleUtils.fireEvent(new ModuleLoadEvent(module));
    }

    public static void registerExternalModules() {
        File[] folderFiles = Streamline.getModuleFolder().listFiles();

        if (folderFiles != null) {
            for (File file : folderFiles) {
                if (! file.isDirectory() && file.getName().endsWith(".jar")) {
                    try {
                        registerModule(file);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public static void registerModule(StreamlineModule module) {
        Preconditions.checkNotNull(module, "module parameter cannot be null.");
        loadModule(module);
    }

    public static StreamlineModule registerModule(File moduleFile) throws IOException, ReflectiveOperationException {
        if (!moduleFile.getName().endsWith(".jar"))
            throw new IllegalArgumentException("The given file is not a valid jar file.");

        String moduleName = moduleFile.getName().replace(".jar", "");

        ModuleClassLoader moduleClassLoader = new ModuleClassLoader(moduleFile);

        //noinspection deprecation
        Optional<Class<?>> moduleClass = JarFiles.getClasses(moduleFile.toURL(), StreamlineModule.class, moduleClassLoader).stream().findFirst();

        if (moduleClass.isEmpty())
            throw new IllegalArgumentException("The file " + moduleName + " is not a valid module.");

        StreamlineModule module = createInstance(moduleClass.get());
        module.initModuleLoader(moduleFile, moduleClassLoader);

        registerModule(module);

        return module;
    }

    public static void unregisterModule(StreamlineModule module) {
        try {
            module.stop();
            unregisterHandlersOf(module);
            loadedModules.remove(module.identifier());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static StreamlineModule getModule(String identifier) {
        return loadedModules.get(identifier);
    }

    private static StreamlineModule createInstance(Class<?> clazz) throws ReflectiveOperationException {
        Preconditions.checkArgument(StreamlineModule.class.isAssignableFrom(clazz), "Class " + clazz + " is not a BundledModule.");

        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterCount() == 0) {
                if (!constructor.isAccessible())
                    constructor.setAccessible(true);

                return (StreamlineModule) constructor.newInstance();
            }
        }

        throw new IllegalArgumentException("Class " + clazz + " has no valid constructors.");
    }

    public static List<ModuleCommand> getCommandsForModule(StreamlineModule module) {
        List<ModuleCommand> r = new ArrayList<>();

        for (ModuleCommand command : Streamline.getLoadedModuleCommands().values()) {
            if (command.getOwningModule().identifier().equals(module.identifier())) r.add(command);
        }

        return r;
    }

    public static void unloadCommandsForModule(StreamlineModule module) {
        List<ModuleCommand> commands = getCommandsForModule(module);

        for (ModuleCommand command : commands) {
            if (command.isEnabled()) command.disable();
        }
    }

    public static void reapplyModule(StreamlineModule module) {
        ModuleManager.unregisterModule(module);
        try {
            module = ModuleManager.registerModule(module.getModuleFile());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        module.restart();
    }

    public static void restartModules() {
        for (StreamlineModule module : new ArrayList<>(enabledModules.values())) {
            module.restart();
        }
    }

    public static void startModules() {
        for (StreamlineModule module : orderModules().values()) {
            if (enabledModules.containsKey(module.identifier())) continue;
            module.start();
        }
    }

    public static void stopModules() {
        for (StreamlineModule module : new ArrayList<>(enabledModules.values())) {
            module.stop();
        }
    }

    public static TreeMap<Integer, StreamlineModule> orderModules() {
        return orderModules(loadedModules.values().stream().toList());
    }

    public static TreeMap<Integer, StreamlineModule> orderModules(StreamlineModule... from) {
        return orderModules(Arrays.stream(from).toList());
    }

    public static TreeMap<Integer, StreamlineModule> orderModules(List<StreamlineModule> from) {
        TreeMap<Integer, StreamlineModule> r = new TreeMap<>();
        List<StreamlineModule> independents = new ArrayList<>();

        TreeSet<String> identified = new TreeSet<>();
        from.forEach(a -> identified.add(a.identifier()));

        for (StreamlineModule module : from) {
            if (module.dependencies().size() <= 0) {
                independents.add(module);
                continue;
            }
            for (Dependency dependency : module.dependencies()) {
                if (identified.contains(dependency.getDependency())) {
                    break;
                }

            }
        }

        independents.forEach(a -> r.put(r.size(), a));

        return r;
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
                MessagingUtils.logSevere("Could not pass event '" + event.getEventName() + "' to '" + registration.getModule().identifier() + "' for reason: ");
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
                    event.complete(null);
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
}
