package singularity.placeholders.replaceables;

import lombok.Getter;
import lombok.NonNull;
import singularity.modules.ModuleLike;
import singularity.placeholders.callbacks.UserPlaceholderCallback;

/**
 * A {@link UserReplaceable} that is owned by a specific {@link ModuleLike}.
 *
 * <p>Combines the user-context resolution of {@link UserReplaceable} with the
 * module-scoped namespacing of {@link ModuleReplaceable}: the placeholder key is
 * automatically prefixed with the module's identifier (using {@code "_"} as the
 * default separator).</p>
 */
@Getter
public class UserModuleReplaceable extends UserReplaceable {

    /**
     * The module that owns this replaceable.
     */
    ModuleLike module;

    /**
     * Creates a module-owned user replaceable with an explicit prefix and separator.
     *
     * @param prefix    the namespace prefix prepended to the placeholder key
     * @param separator the string placed between the prefix and {@code from}
     * @param from      the literal placeholder key suffix
     * @param callback  the user-aware callback invoked when the placeholder is matched
     * @param module    the owning module; must not be {@code null}
     */
    public UserModuleReplaceable(String prefix, String separator, String from, UserPlaceholderCallback callback, @NonNull ModuleLike module) {
        super(prefix + separator + from, callback);
        this.module = module;
    }

    /**
     * Creates a module-owned user replaceable using the module's own identifier as the
     * prefix and {@code "_"} as the separator.
     *
     * @param from     the literal placeholder key suffix
     * @param callback the user-aware callback invoked when the placeholder is matched
     * @param module   the owning module; must not be {@code null}
     */
    public UserModuleReplaceable(String from, UserPlaceholderCallback callback, @NonNull ModuleLike module) {
        this(module.getIdentifier(), "_", from, callback, module);
    }
}
