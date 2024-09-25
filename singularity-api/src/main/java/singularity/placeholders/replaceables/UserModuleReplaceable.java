package singularity.placeholders.replaceables;

import lombok.Getter;
import lombok.NonNull;
import singularity.modules.ModuleLike;
import singularity.placeholders.callbacks.UserPlaceholderCallback;

@Getter
public class UserModuleReplaceable extends UserReplaceable {
    ModuleLike module;

    public UserModuleReplaceable(String prefix, String separator, String from, UserPlaceholderCallback callback, @NonNull ModuleLike module) {
        super(prefix + separator + from, callback);
        this.module = module;
    }

    public UserModuleReplaceable(String from, UserPlaceholderCallback callback, @NonNull ModuleLike module) {
        this(module.getIdentifier(), "_", from, callback, module);
    }
}
