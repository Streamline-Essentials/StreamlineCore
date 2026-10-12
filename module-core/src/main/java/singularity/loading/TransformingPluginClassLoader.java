package singularity.loading;

import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link PluginClassLoader} that passes every class it defines from the module jar
 * through a {@link ModuleClassTransformer}.
 */
public class TransformingPluginClassLoader extends PluginClassLoader {
    private final ModuleClassTransformer transformer;

    /** Helper classes a transform produced, by binary name, until they are defined. */
    private final Map<String, byte[]> pendingClasses = new ConcurrentHashMap<>();

    /** Packages this loader has defined. */
    private final Set<String> definedPackages = ConcurrentHashMap.newKeySet();

    public TransformingPluginClassLoader(PluginManager pluginManager, PluginDescriptor descriptor,
                                         ClassLoader parent, ModuleClassTransformer transformer) {
        super(pluginManager, descriptor, parent);
        this.transformer = transformer;
    }

    /**
     * Reads a resource without the JVM's shared jar cache: it is keyed by path, so after a
     * module is unloaded and its jar replaced or reloaded, a cached handle to the earlier
     * (closed or rewritten) file would be read instead of this loader's jar.
     */
    private static byte[] readUncached(URL resource) throws IOException {
        URLConnection connection = resource.openConnection();
        connection.setUseCaches(false);
        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        }
    }

    /** A class file this module can see, by internal name, or {@code null}. */
    private byte[] classFile(String internalName) {
        String path = internalName + ".class";
        try {
            URL own = findResource(path);
            if (own != null) return readUncached(own);
            ClassLoader parent = getParent();
            URL inherited = parent == null ? null : parent.getResource(path);
            return inherited == null ? null : readUncached(inherited);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = pendingClasses.remove(name);
        if (bytes == null) {
            URL resource = findResource(name.replace('.', '/') + ".class");
            if (resource == null) throw new ClassNotFoundException(name);

            try {
                bytes = readUncached(resource);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }

            Map<String, byte[]> classes = transformer.transformAll(name, bytes, this::classFile);
            bytes = classes.get(name);
            if (bytes == null) throw new ClassNotFoundException(name);
            for (Map.Entry<String, byte[]> extra : classes.entrySet()) {
                if (! extra.getKey().equals(name)) pendingClasses.putIfAbsent(extra.getKey(), extra.getValue());
            }
        }

        // Each package is defined once, tracked here rather than through
        // getDefinedPackage, which Java 8 (Minecraft 1.8.9's runtime) does not have.
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            String packageName = name.substring(0, lastDot);
            if (definedPackages.add(packageName)) {
                try {
                    definePackage(packageName, null, null, null, null, null, null, null);
                } catch (IllegalArgumentException ignored) {
                    // The package came with a class defined before this loader tracked it.
                }
            }
        }

        URL[] urls = getURLs();
        CodeSource source = new CodeSource(urls.length > 0 ? urls[0] : null, (CodeSigner[]) null);
        return defineClass(name, bytes, 0, bytes.length, source);
    }
}
