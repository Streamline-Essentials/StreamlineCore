package net.streamline.platform.modules;

import net.streamline.platform.BasePlugin;
import net.streamline.libs.asm.ClassReader;
import net.streamline.libs.asm.ClassWriter;
import net.streamline.libs.asm.commons.ClassRemapper;
import net.streamline.libs.asm.commons.Remapper;
import singularity.modules.ModuleManager;
import xyz.wagyourtail.jvmdg.ClassDowngrader;
import xyz.wagyourtail.jvmdg.cli.Flags;
import xyz.wagyourtail.jvmdg.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Makes module classes run on Minecraft 1.8.9's Java 8: applies the mod jar's library
 * relocations, as on the other mod loaders, then rewrites the class for Java 8 with
 * JvmDowngrader, whose stubs for newer Java APIs ship in the mod jar.
 *
 * <p>Modules are built once for every platform, against the libraries' original packages and
 * for Java 11. The relocation table comes from {@value #RESOURCE}, which the build writes from
 * the same table it hands to Shadow; it is not spelled out in code because Shadow rewrites
 * matching string constants in this jar. Downgrading a class can produce helper classes
 * beside it, which {@link #transformAll} hands back to the module's loader.</p>
 */
public final class LegacyModuleTransformer extends Remapper implements ModuleManager.ModuleClassTransformer {

    static final String RESOURCE = "streamline-relocations.properties";

    /** Java 8's class file major version. */
    private static final int JAVA_8 = 52;

    /** Internal-name prefixes ({@code a/b/}) to their replacements, longest first. */
    private final List<String[]> prefixes;

    /** Built on first use: it loads JvmDowngrader's API stubs, which takes a moment. */
    private volatile ClassDowngrader downgrader;

    private LegacyModuleTransformer(List<String[]> prefixes) {
        this.prefixes = prefixes;
    }

    /** Installs the transformer for modules loaded from now on. */
    public static void install() {
        Properties properties = new Properties();
        try (InputStream in = LegacyModuleTransformer.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in != null) properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        List<String[]> prefixes = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            prefixes.add(new String[] {
                    entry.getKey().toString().replace('.', '/') + "/",
                    entry.getValue().toString().replace('.', '/') + "/",
            });
        }
        prefixes.sort(Comparator.comparingInt((String[] pair) -> pair[0].length()).reversed());

        ModuleManager.setModuleClassTransformer(new LegacyModuleTransformer(prefixes));
    }

    @Override
    public byte[] transform(String className, byte[] classBytes) {
        return transformAll(className, classBytes, name -> null).get(className);
    }

    @Override
    public Map<String, byte[]> transformAll(String className, byte[] classBytes, Function<String, byte[]> classFiles) {
        byte[] relocated = relocate(classBytes);
        if (majorVersion(relocated) <= JAVA_8) return Collections.singletonMap(className, relocated);

        Map<String, byte[]> downgraded;
        try {
            downgraded = downgrader().downgrade(
                    new AtomicReference<>(className.replace('.', '/')), relocated, true,
                    name -> classFiles.apply(name));
        } catch (Exception e) {
            throw new IllegalStateException("Could not downgrade module class " + className + " to Java 8", e);
        }
        if (downgraded == null || downgraded.isEmpty()) return Collections.singletonMap(className, relocated);

        Map<String, byte[]> classes = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : downgraded.entrySet()) {
            classes.put(entry.getKey().replace('/', '.'), entry.getValue());
        }
        return classes;
    }

    private byte[] relocate(byte[] classBytes) {
        if (prefixes.isEmpty()) return classBytes;
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, this), 0);
        return writer.toByteArray();
    }

    private ClassDowngrader downgrader() {
        ClassDowngrader current = downgrader;
        if (current != null) return current;
        synchronized (this) {
            if (downgrader == null) {
                Flags flags = new Flags();
                flags.classVersion = JAVA_8;
                flags.logLevel = Logger.Level.WARN;
                downgrader = ClassDowngrader.downgradeTo(flags);
                BasePlugin.log("Module classes are downgraded to Java 8 as they load.");
            }
            return downgrader;
        }
    }

    private static int majorVersion(byte[] classBytes) {
        return ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
    }

    @Override
    public String map(String internalName) {
        for (String[] pair : prefixes) {
            if (internalName.startsWith(pair[0])) {
                return pair[1] + internalName.substring(pair[0].length());
            }
        }
        return internalName;
    }
}
