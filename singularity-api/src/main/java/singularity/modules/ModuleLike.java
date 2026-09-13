package singularity.modules;

import singularity.interfaces.IModuleLike;
import org.jetbrains.annotations.NotNull;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * Abstract bridge between the PF4J {@link Plugin} lifecycle and the Streamline
 * {@link IModuleLike} contract.
 *
 * <p>All concrete module types ({@link CosmicModule}, {@link SimpleModule},
 * etc.) extend this class.  It delegates PF4J plugin management to the
 * superclass while exposing the Streamline-specific comparison and lifecycle
 * methods defined by {@link IModuleLike}.
 */
public abstract class ModuleLike extends Plugin implements IModuleLike {

    /**
     * Constructs a new {@code ModuleLike} instance wrapping the supplied PF4J
     * plugin wrapper.
     *
     * @param wrapper the PF4J wrapper that provides descriptor and class-loader
     *                information for this plugin; must not be {@code null}
     */
    public ModuleLike(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Modules are compared lexicographically by their {@link #getIdentifier()}
     * string.
     */
    @Override
    public int compareTo(@NotNull IModuleLike o) {
        return CharSequence.compare(getIdentifier(), o.getIdentifier());
    }
}
