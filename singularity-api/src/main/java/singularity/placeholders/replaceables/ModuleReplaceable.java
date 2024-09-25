package singularity.placeholders.replaceables;

import lombok.Getter;
import lombok.NonNull;
import singularity.modules.ModuleLike;
import singularity.placeholders.callbacks.PlaceholderCallback;

@Getter
public class ModuleReplaceable extends GenericReplaceable {
    @NonNull
    final ModuleLike module;

    public ModuleReplaceable(String prefix, String separator, String from, PlaceholderCallback callback, @NonNull ModuleLike module) {
        super(prefix + separator + from, callback);
        this.module = module;
    }

    public ModuleReplaceable(String from, PlaceholderCallback callback, @NonNull ModuleLike module) {
        this(module.getIdentifier(), "_", from, callback, module);
    }
}
