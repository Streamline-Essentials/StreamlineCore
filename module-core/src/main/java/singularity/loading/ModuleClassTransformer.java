package singularity.loading;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Rewrites the bytecode of one module class before it is defined: platforms whose host jar
 * relocates shaded libraries apply the same relocation, and Minecraft 1.8.9 also rewrites
 * module classes for Java 8. Server modules and client modules load through
 * {@link TransformingPluginClassLoader} when one is installed.
 */
@FunctionalInterface
public interface ModuleClassTransformer {
    /**
     * @param className binary name of the class being defined
     * @param classBytes the class file as read from the module jar
     * @return the class file to define
     */
    byte[] transform(String className, byte[] classBytes);

    /**
     * Rewrites one module class into the classes to define for it: the class itself, plus
     * any helper classes the rewrite introduces, which the module's loader then defines
     * when they are first asked for.
     *
     * @param className binary name of the class being defined
     * @param classBytes the class file as read from the module jar
     * @param classFiles reads a class file the module can see (its own or its parent
     *                   loader's) by internal name ({@code a/b/C}), or {@code null} if there
     *                   is none
     * @return class files by binary name, including {@code className}
     */
    default Map<String, byte[]> transformAll(String className, byte[] classBytes, Function<String, byte[]> classFiles) {
        return Collections.singletonMap(className, transform(className, classBytes));
    }
}
