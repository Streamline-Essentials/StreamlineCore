package net.streamline.platform.modules;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import singularity.modules.ModuleManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Applies the mod jar's library relocations to module classes as they load.
 *
 * <p>The mod jar relocates the libraries it shades so they cannot collide with other mods'
 * copies, but modules are built once for every platform against the original package names.
 * This rewrites those references to the relocated packages. The mapping is read from
 * {@value #RESOURCE}, which the build writes from the same table it hands to Shadow; it is
 * not spelled out in code because Shadow rewrites matching string constants in this jar.</p>
 */
public final class RelocatingModuleTransformer extends Remapper implements ModuleManager.ModuleClassTransformer {

    static final String RESOURCE = "streamline-relocations.properties";

    /** Internal-name prefixes ({@code a/b/}) to their replacements, longest first. */
    private final List<String[]> prefixes;

    private RelocatingModuleTransformer(List<String[]> prefixes) {
        this.prefixes = prefixes;
    }

    /**
     * Installs the transformer for modules loaded from now on. Does nothing when the jar
     * carries no relocations.
     */
    public static void install() {
        Properties properties = new Properties();
        try (InputStream in = RelocatingModuleTransformer.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) return;
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (properties.isEmpty()) return;

        List<String[]> prefixes = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            prefixes.add(new String[] {
                    entry.getKey().toString().replace('.', '/') + "/",
                    entry.getValue().toString().replace('.', '/') + "/",
            });
        }
        prefixes.sort(Comparator.comparingInt((String[] pair) -> pair[0].length()).reversed());

        ModuleManager.setModuleClassTransformer(new RelocatingModuleTransformer(prefixes));
    }

    @Override
    public byte[] transform(String className, byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, this), 0);
        return writer.toByteArray();
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
