package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import lombok.NonNull;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.placeholders.RATRegistry;
import net.streamline.api.placeholders.callbacks.PlaceholderCallback;

public class ModuleReplaceable extends GenericReplaceable {
    @Getter @NonNull
    final ModuleLike module;

    public ModuleReplaceable(String prefix, String separator, String from, PlaceholderCallback callback, @NonNull ModuleLike module) {
        super(prefix + separator + from, callback);
        this.module = module;
    }

    public ModuleReplaceable(String from, PlaceholderCallback callback, @NonNull ModuleLike module) {
        this(module.getIdentifier(), "_", from, callback, module);
    }
}
