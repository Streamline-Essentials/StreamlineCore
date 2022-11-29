package net.streamline.api.placeholders.replaceables;

import lombok.Getter;
import lombok.NonNull;
import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.placeholders.RATRegistry;
import net.streamline.api.placeholders.callbacks.PlaceholderCallback;
import net.streamline.api.placeholders.callbacks.UserPlaceholderCallback;
import net.streamline.api.savables.users.StreamlineUser;

public class UserModuleReplaceable extends UserReplaceable {
    @Getter
    ModuleLike module;

    public UserModuleReplaceable(String prefix, String separator, String from, UserPlaceholderCallback callback, @NonNull ModuleLike module) {
        super(prefix + separator + from, callback);
        this.module = module;
    }

    public UserModuleReplaceable(String from, UserPlaceholderCallback callback, @NonNull ModuleLike module) {
        this(module.getIdentifier(), "_", from, callback, module);
    }
}
