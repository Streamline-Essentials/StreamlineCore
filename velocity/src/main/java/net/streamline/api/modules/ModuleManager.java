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
import java.lang.reflect.Constructor;
import java.util.*;

public class ModuleManager {
    public static TreeMap<String, BundledModule> loadedModules = new TreeMap<>();
    public static TreeMap<String, BundledModule> enabledModules = new TreeMap<>();

    public static boolean loadModule(@NonNull BundledModule module) {
        if (loadedModules.containsKey(module.identifier())) {
            MessagingUtils.logWarning("Module '" + module.identifier() + "' by '" + module.getAuthorsStringed() + "' could not be loaded: identical identifiers");
            return false;
        }

        loadedModules.put(module.identifier(), module);
        Streamline.fireEvent(new ModuleLoadEvent(module));
        return true;
    }

    public static void unJarAll(File folder) throws Exception {
        if (! folder.isDirectory()) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        List<File> toUnJar = new ArrayList<>();

        for (File file : files) {
            if (file == null) continue;
            if (file.isFile()) if (file.getName().endsWith(".jar")) toUnJar.add(file);
        }

        for (int i = 0; i < toUnJar.size(); i ++) {
            File file = toUnJar.get(i);
//            JarFile jarFile = new JarFile(file);
//            Enumeration<JarEntry> e = jarFile.entries();
//
//            URL[] urls = { new URL("jar:file:" + file.getPath() + "!/") };
//            URLClassLoader cl = URLClassLoader.newInstance(urls);
//
//            while (e.hasMoreElements()) {
//                JarEntry je = e.nextElement();
//                if(je.isDirectory() || !je.getName().endsWith(".class")){
//                    continue;
//                }
//                // -6 because of .class
//                String className = je.getName().substring(0,je.getName().length()-6);
//                className = className.replace('/', '.');
//                Class c = cl.loadClass(className);
//            }

            ModuleClassLoader moduleClassLoader = new ModuleClassLoader(file);

            //noinspection deprecation
            Optional<Class<?>> moduleClass = JarFiles.getClasses(file.toURL(), BundledModule.class, moduleClassLoader).stream().findFirst();

            if (! moduleClass.isPresent())
                throw new IllegalArgumentException("The file " + file.getName() + " is not a valid module.");

            BundledModule module = createInstance(moduleClass.get());
            module.initModuleLoader(file, moduleClassLoader);

            loadModule(module);
        }
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

    public static void unJarAll() throws Exception {
        unJarAll(Streamline.getModuleFolder());
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
                if (identified.contains(dependency.dependency())) {
                    break;
                }

            }
        }

        independents.forEach(a -> r.put(r.size(), a));

        return r;
    }
}
