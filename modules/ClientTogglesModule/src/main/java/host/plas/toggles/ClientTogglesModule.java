package host.plas.toggles;

import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.client.module.ClientModule;

/**
 * Toggle sprint and toggle sneak for game clients. Each has a key, bound on this module's page
 * in {@code /slclient menu}; pressing it keeps sprint or sneak held until it is pressed again,
 * through deaths, respawns and world changes. Two HUD elements and the
 * {@code %clienttogglesmodule_...%} placeholders show what is toggled. Disabling or unloading
 * the module lets go of both.
 */
public class ClientTogglesModule extends ClientModule {
    @Getter @Setter
    private static ClientTogglesModule instance;

    public ClientTogglesModule(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void onEnable() {
        instance = this;
        // Key settings and held game keys need a StreamlineCore that has them; on an older one
        // Toggles fails to link and the module stays idle.
        try {
            Toggles.install(this);
        } catch (LinkageError e) {
            logWarning("This StreamlineCore cannot hold keys for modules; update the mod to use toggle sprint and sneak.");
        }
    }

    @Override
    public void onDisable() {
        try {
            Toggles.uninstall(this);
        } catch (LinkageError ignored) {
            // Never installed.
        }
        // The module can be unloaded while the game runs; a static reference would keep its
        // class loader, and the jar copy it reads from, alive.
        instance = null;
    }
}
