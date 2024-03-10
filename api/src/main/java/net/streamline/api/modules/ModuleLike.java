package net.streamline.api.modules;

import net.streamline.api.interfaces.IModuleLike;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public abstract class ModuleLike extends Plugin implements IModuleLike {
    public ModuleLike(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int compareTo(@NotNull IModuleLike o) {
        return CharSequence.compare(getIdentifier(), o.getIdentifier());
    }
}
