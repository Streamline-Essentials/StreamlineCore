package net.streamline.api.base.module;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.base.ratapi.LuckpermsExpansion;
import singularity.Singularity;
import net.streamline.api.base.listeners.BaseListener;
import net.streamline.api.base.ratapi.StreamlineExpansion;
import net.streamline.api.base.text.HexInit;
import singularity.modules.ModuleManager;
import singularity.modules.SimpleModule;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;

import java.util.Collections;
import java.util.List;

/**
 * The built-in base module of StreamlineCore.
 *
 * <p>This {@link singularity.modules.SimpleModule} is programmatically
 * constructed (not loaded from an external JAR) and is therefore always present
 * when the platform plugin starts.  It bootstraps the framework's core
 * infrastructure:
 * <ul>
 *   <li>Registers the cross-platform event listener ({@link BaseListener}).</li>
 *   <li>Registers the {@link StreamlineExpansion} and {@link LuckpermsExpansion}
 *       RAT placeholder expansions.</li>
 *   <li>Initialises hex-colour parsing policies via {@link HexInit}.</li>
 * </ul>
 * The module identifier is {@code streamline-base} and it is marked as
 * non-malleable (it cannot be unloaded or reloaded by end-users).
 */
public class BaseModule extends SimpleModule {

    /**
     * The singleton instance of this module, set during {@link #onLoad()}.
     */
    @Getter
    private static BaseModule instance;

    /**
     * The cross-platform event listener registered during {@link #onLoad()}.
     */
    @Getter @Setter
    private static BaseListener baseListener;

    /**
     * The Streamline-specific RAT placeholder expansion registered at load time.
     */
    @Getter @Setter
    private static StreamlineExpansion streamlineExpansion;

    /**
     * The LuckPerms RAT placeholder expansion registered at load time.
     */
    @Getter @Setter
    private static LuckpermsExpansion luckpermsExpansion;

    /**
     * Constructs the base module by building a synthetic {@link org.pf4j.PluginDescriptor}
     * and wrapping it in a {@link org.pf4j.PluginWrapper} so that PF4J can manage
     * it alongside externally-loaded modules.  The module is immediately added to
     * the enabled-modules registry and marked as non-malleable.
     */
    public BaseModule() {
        super(new PluginWrapper(ModuleManager.safePluginManager(), new PluginDescriptor() {
            @Override
            public String getPluginId() {
                return "streamline-base";
            }

            @Override
            public String getPluginDescription() {
                return "Base module.";
            }

            @Override
            public String getPluginClass() {
                return "singularity.base.module.BaseModule";
            }

            @Override
            public String getVersion() {
                return "0.0.1";
            }

            @Override
            public String getRequires() {
                return "";
            }

            @Override
            public String getProvider() {
                return "";
            }

            @Override
            public String getLicense() {
                return "";
            }

            @Override
            public List<PluginDependency> getDependencies() {
                return Collections.emptyList();
            }
        }, Singularity.getModuleFolder().toPath(), Singularity.getInstance().getClass().getClassLoader()));

        setMalleable(false);
        setEnabled(true);
        ModuleManager.getEnabledModules().put(getIdentifier(), this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sets the static singleton reference, constructs and registers the
     * {@link BaseListener}, creates the {@link StreamlineExpansion} and
     * {@link LuckpermsExpansion} placeholder expansions, and initialises
     * hex-colour parsing policies via {@link HexInit#init()}.</p>
     */
    @Override
    public void onLoad() {
        instance = this;
        setBaseListener(new BaseListener());
//        ModuleUtils.listen(getBaseListener(), this);

        setStreamlineExpansion(new StreamlineExpansion());
        setLuckpermsExpansion(new LuckpermsExpansion());

//        GivenConfigs.getMainConfig().getHexPolicies().forEach(TextManager::registerHexPolicy);
        HexInit.init();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Activates the {@link StreamlineExpansion} by calling its
     * {@link StreamlineExpansion#init()} method so that all registered
     * placeholder replaceables become live.</p>
     */
    @Override
    public void onEnable() {
        // nothing right now.
        getStreamlineExpansion().init();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Deactivates the {@link StreamlineExpansion} by calling its
     * {@link StreamlineExpansion#stop()} method.</p>
     */
    @Override
    public void onDisable() {
        // nothing right now.
        getStreamlineExpansion().stop();
    }
}
