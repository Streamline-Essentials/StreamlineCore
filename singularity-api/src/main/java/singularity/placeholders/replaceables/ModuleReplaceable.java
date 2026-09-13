package singularity.placeholders.replaceables;

import lombok.Getter;
import lombok.NonNull;
import singularity.modules.ModuleLike;
import singularity.placeholders.callbacks.PlaceholderCallback;

/**
 * A {@link GenericReplaceable} that is owned by a specific {@link ModuleLike}.
 *
 * <p>The placeholder key is automatically prefixed with the module's identifier
 * (using {@code "_"} as the default separator), making it easy for modules to
 * register their own namespaced placeholders without manually constructing the
 * full key.</p>
 */
@Getter
public class ModuleReplaceable extends GenericReplaceable {

    /**
     * The module that owns this replaceable.
     */
    @NonNull
    final ModuleLike module;

    /**
     * Creates a module-owned replaceable with an explicit prefix and separator.
     *
     * @param prefix    the namespace prefix prepended to the placeholder key
     * @param separator the string placed between the prefix and {@code from}
     * @param from      the literal placeholder key suffix
     * @param callback  the callback invoked when the placeholder is matched
     * @param module    the owning module; must not be {@code null}
     */
    public ModuleReplaceable(String prefix, String separator, String from, PlaceholderCallback callback, @NonNull ModuleLike module) {
        super(prefix + separator + from, callback);
        this.module = module;
    }

    /**
     * Creates a module-owned replaceable using the module's own identifier as the
     * prefix and {@code "_"} as the separator.
     *
     * @param from     the literal placeholder key suffix
     * @param callback the callback invoked when the placeholder is matched
     * @param module   the owning module; must not be {@code null}
     */
    public ModuleReplaceable(String from, PlaceholderCallback callback, @NonNull ModuleLike module) {
        this(module.getIdentifier(), "_", from, callback, module);
    }
}
