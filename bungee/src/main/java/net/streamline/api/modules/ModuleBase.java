package net.streamline.api.modules;

import net.streamline.api.modules.java.JavaModule;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a base {@link Module}
 * <p>
 * Extend this class if your module is not a {@link
 * JavaModule}
 */
public abstract class ModuleBase implements Module {
    @Override
    public final int hashCode() {
        return getName().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Module)) {
            return false;
        }
        return getName().equals(((Module) obj).getName());
    }

    @Override
    @NotNull
    public final String getName() {
        return getDescription().getName();
    }
}
