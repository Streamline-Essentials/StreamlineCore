package net.streamline.api.modules;

import tv.quaint.thebase.lib.google.common.io.ByteStreams;
import net.streamline.api.SLAPI;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ModuleClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    private final JarFile jar;
    private final Manifest manifest;
    private final URL url;

    public ModuleClassLoader(File file) throws IOException {
        super(new URL[]{ file.toURI().toURL() }, SLAPI.getInstance().getClass().getClassLoader());

        this.jar = new JarFile(file);
        this.manifest = jar.getManifest();
        this.url = file.toURI().toURL();
    }

    @Nullable
    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        return url == null ? super.getResource(name) : url;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
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

//                classBytes = loader.server.getUnsafe().processClass(description, path, classBytes);

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

//            loader.setClass(name, result);
            classes.put(name, result);
        }

        return result;
    }

}
