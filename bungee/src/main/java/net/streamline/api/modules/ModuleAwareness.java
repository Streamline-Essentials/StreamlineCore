package net.streamline.api.modules;

import java.util.Set;

/**
 * Represents a concept that a module is aware of.
 * <p>
 * The internal representation may be singleton, or be a parameterized
 * instance, but must be immutable.
 */
public interface ModuleAwareness {
    /**
     * Each entry here represents a particular module's awareness. These can
     * be checked by using {@link ModuleDescriptionFile#getAwareness()}.{@link
     * Set#contains(Object) contains(flag)}.
     */
    public enum Flags implements ModuleAwareness {
        /**
         * This specifies that all (text) resources stored in a module's jar
         * use UTF-8 encoding.
         *
         * @deprecated all modules are now assumed to be UTF-8 aware.
         */
        @Deprecated
        UTF8,
        ;
    }
}
