package net.streamline.api.modules;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.streamline.api.command.ModuleCommand;
import net.streamline.api.events.modules.ModuleLoadEvent;
import net.streamline.api.modules.dependencies.Dependency;
import net.streamline.base.Streamline;
import net.streamline.utils.JarFiles;
import net.streamline.utils.MessagingUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class ModuleManager {
    public static TreeMap<String, BundledModule> loadedModules = new TreeMap<>();
    public static TreeMap<String, BundledModule> enabledModules = new TreeMap<>();

    public static void loadModule(@NonNull BundledModule module) {
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

    public static void registerModule(BundledModule module) {
        Preconditions.checkNotNull(module, "module parameter cannot be null.");
        loadModule(module);
    }

    public static BundledModule registerModule(File moduleFile) throws IOException, ReflectiveOperationException {
        if (!moduleFile.getName().endsWith(".jar"))
            throw new IllegalArgumentException("The given file is not a valid jar file.");

        String moduleName = moduleFile.getName().replace(".jar", "");

        ModuleClassLoader moduleClassLoader = new ModuleClassLoader(moduleFile);

        //noinspection deprecation
        Optional<Class<?>> moduleClass = JarFiles.getClasses(moduleFile.toURL(), BundledModule.class, moduleClassLoader).stream().findFirst();

        if (moduleClass.isEmpty())
            throw new IllegalArgumentException("The file " + moduleName + " is not a valid module.");

        BundledModule module = createInstance(moduleClass.get());
        module.initModuleLoader(moduleFile, moduleClassLoader);

        registerModule(module);

        return module;
    }

    private static BundledModule createInstance(Class<?> clazz) throws ReflectiveOperationException {
        Preconditions.checkArgument(BundledModule.class.isAssignableFrom(clazz), "Class " + clazz + " is not a BundledModule.");

        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterCount() == 0) {
                if (!constructor.isAccessible())
                    constructor.setAccessible(true);

                return (BundledModule) constructor.newInstance();
            }
        }

        throw new IllegalArgumentException("Class " + clazz + " has no valid constructors.");
    }

    public static List<ModuleCommand> getCommandsForModule(BundledModule module) {
        List<ModuleCommand> r = new ArrayList<>();

        for (ModuleCommand command : Streamline.getLoadedModuleCommands().values()) {
            if (command.getOwningModule().identifier().equals(module.identifier())) r.add(command);
        }

        return r;
    }

    public static void unloadCommandsForModule(BundledModule module) {
        List<ModuleCommand> commands = getCommandsForModule(module);

        for (ModuleCommand command : commands) {
            if (command.isEnabled()) command.disable();
        }
    }

    public static void restartModules() {
        for (BundledModule module : enabledModules.values()) {
            module.restart();
        }
    }

    public static void startModules() {
        for (BundledModule module : orderModules().values()) {
            if (enabledModules.containsKey(module.identifier())) continue;
            module.start();
            enabledModules.put(module.identifier(), module);
        }
    }

    public static void stopModules() {
        for (BundledModule module : enabledModules.values()) {
            module.stop();
            enabledModules.remove(module.identifier());
        }
    }

    public static TreeMap<Integer, BundledModule> orderModules() {
        return orderModules(loadedModules.values().stream().toList());
    }

    public static TreeMap<Integer, BundledModule> orderModules(BundledModule... from) {
        return orderModules(Arrays.stream(from).toList());
    }

    public static TreeMap<Integer, BundledModule> orderModules(List<BundledModule> from) {
        TreeMap<Integer, BundledModule> r = new TreeMap<>();
        List<BundledModule> independents = new ArrayList<>();

        TreeSet<String> identified = new TreeSet<>();
        from.forEach(a -> identified.add(a.identifier()));

        for (BundledModule module : from) {
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
}
