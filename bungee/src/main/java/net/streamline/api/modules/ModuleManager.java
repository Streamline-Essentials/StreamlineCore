package net.streamline.api.modules;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import net.streamline.api.events.server.ModuleEnableEvent;
import net.streamline.api.events.server.ModuleLoadEvent;
import net.streamline.base.Streamline;
import net.streamline.utils.JarFiles;
import net.streamline.utils.MessagingUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModuleManager {
    public static TreeMap<String, BundledModule> loadedModules = new TreeMap<>();

    public static boolean loadModule(@NonNull BundledModule module) {
        if (loadedModules.containsKey(module.getIdentifier())) {
            MessagingUtils.logWarning("Module '" + module.getIdentifier() + "' by '" + module.getAuthorsStringed() + "' could not be loaded: identical identifiers");
            return false;
        }

        loadedModules.put(module.getIdentifier(), module);
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
}
