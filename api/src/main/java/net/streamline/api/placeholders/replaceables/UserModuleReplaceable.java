package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import lombok.NonNull;
import net.streamline.api.modules.ModuleLike;
import net.streamline.api.placeholders.callbacks.UserPlaceholderCallback;

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
