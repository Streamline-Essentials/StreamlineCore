package net.streamline.api.base.modules;

import net.md_5.bungee.api.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a base {@link Module}
 * <p>
 * Extend this class if your module is not a {@link
 * net.streamline.api.base.modules.java.JavaModule}
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
