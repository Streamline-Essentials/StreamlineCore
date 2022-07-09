package net.streamline.api.modules.java;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import net.streamline.api.modules.ModuleDescriptionFile;
import net.streamline.api.modules.SimpleModuleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;

/**
 * A ClassLoader for modules, to allow shared classes across multiple modules
 */
final class ModuleClassLoader extends URLClassLoader {
    private final JavaModuleLoader loader;
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<String, Class<?>>();
    private final ModuleDescriptionFile description;
    private final File dataFolder;
    private final File file;
    private final JarFile jar;
    private final Manifest manifest;
    private final URL url;
    private final ClassLoader libraryLoader;
    final JavaModule module;
    private JavaModule moduleInit;
    private IllegalStateException moduleState;
    private final Set<String> seenIllegalAccess = Collections.newSetFromMap(new ConcurrentHashMap<>());

    static {
        ClassLoader.registerAsParallelCapable();
    }

    ModuleClassLoader(@NotNull final JavaModuleLoader loader, @Nullable final ClassLoader parent, @NotNull final ModuleDescriptionFile description, @NotNull final File dataFolder, @NotNull final File file, @Nullable ClassLoader libraryLoader) throws IOException, InvalidModuleException, MalformedURLException {
        super(new URL[] {file.toURI().toURL()}, parent);
        Preconditions.checkArgument(loader != null, "Loader cannot be null");

        this.loader = loader;
        this.description = description;
        this.dataFolder = dataFolder;
        this.file = file;
        this.jar = new JarFile(file);
        this.manifest = jar.getManifest();
        this.url = file.toURI().toURL();
        this.libraryLoader = libraryLoader;

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
                throw new InvalidModuleException("main class `" + description.getMain() + "' does not extend JavaModule", ex);
            }

            module = moduleClass.newInstance();
        } catch (IllegalAccessException ex) {
            throw new InvalidModuleException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidModuleException("Abnormal module type", ex);
        }
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass0(name, resolve, true, true);
    }

    Class<?> loadClass0(@NotNull String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException {
        try {
            Class<?> result = super.loadClass(name, resolve);

            // SPIGOT-6749: Library classes will appear in the above, but we don't want to return them to other modules
            if (checkGlobal || result.getClassLoader() == this) {
                return result;
            }
        } catch (ClassNotFoundException ex) {
        }

        if (checkLibraries && libraryLoader != null) {
            try {
                return libraryLoader.loadClass(name);
            } catch (ClassNotFoundException ex) {
            }
        }

        if (checkGlobal) {
            // This ignores the libraries of other modules, unless they are transitive dependencies.
            Class<?> result = loader.getClassByName(name, resolve, description);

            if (result != null) {
                // If the class was loaded from a library instead of a ModuleClassLoader, we can assume that its associated module is a transitive dependency and can therefore skip this check.
                if (result.getClassLoader() instanceof ModuleClassLoader) {
                    ModuleDescriptionFile provider = ((ModuleClassLoader) result.getClassLoader()).description;

                    if (provider != description
                            && !seenIllegalAccess.contains(provider.getName())
                            && !((SimpleModuleManager) loader.server.getModuleManager()).isTransitiveDepend(description, provider)) {

                        seenIllegalAccess.add(provider.getName());
                        if (module != null) {
                            module.getLogger().log(Level.WARNING, "Loaded class {0} from {1} which is not a depend or softdepend of this module.", new Object[]{name, provider.getFullName()});
                        } else {
                            // In case the bad access occurs on construction
                            loader.server.getLogger().log(Level.WARNING, "[{0}] Loaded class {1} from {2} which is not a depend or softdepend of this module.", new Object[]{description.getName(), name, provider.getFullName()});
                        }
                    }
                }

                return result;
            }
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            String path = name.replace('.', '/').concat(".class");
            JarEntry entry = jar.getJarEntry(path);

            if (entry != null) {
                byte[] classBytes;

                try (InputStream is = jar.getInputStream(entry)) {
                    classBytes = ByteStreams.toByteArray(is);
                } catch (IOException ex) {
                    throw new ClassNotFoundException(name, ex);
                }

                classBytes = loader.server.getUnsafe().processClass(description, path, classBytes);

                int dot = name.lastIndexOf('.');
                if (dot != -1) {
                    String pkgName = name.substring(0, dot);
                    if (getPackage(pkgName) == null) {
                        try {
                            if (manifest != null) {
                                definePackage(pkgName, manifest, url);
                            } else {
                                definePackage(pkgName, null, null, null, null, null, null, null);
                            }
                        } catch (IllegalArgumentException ex) {
                            if (getPackage(pkgName) == null) {
                                throw new IllegalStateException("Cannot find package " + pkgName);
                            }
                        }
                    }
                }

                CodeSigner[] signers = entry.getCodeSigners();
                CodeSource source = new CodeSource(url, signers);

                result = defineClass(name, classBytes, 0, classBytes.length, source);
            }

            if (result == null) {
                result = super.findClass(name);
            }

            loader.setClass(name, result);
            classes.put(name, result);
        }

        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    @NotNull
    Collection<Class<?>> getClasses() {
        return classes.values();
    }

    synchronized void initialize(@NotNull JavaModule javaModule) {
        Preconditions.checkArgument(javaModule != null, "Initializing module cannot be null");
        Preconditions.checkArgument(javaModule.getClass().getClassLoader() == this, "Cannot initialize module outside of this class loader");
        if (this.module != null || this.moduleInit != null) {
            throw new IllegalArgumentException("Module already initialized!", moduleState);
        }

        moduleState = new IllegalStateException("Initial initialization");
        this.moduleInit = javaModule;

        javaModule.init(loader, loader.server, description, dataFolder, file, this);
    }
}
