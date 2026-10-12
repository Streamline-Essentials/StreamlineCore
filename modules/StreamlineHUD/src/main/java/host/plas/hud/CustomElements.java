package host.plas.hud;

import host.plas.StreamlineHUD;
import singularity.client.hud.CustomHudElements;
import singularity.client.hud.HudRegistry;

/**
 * Hands the module menu's add and delete buttons to {@link StreamlineHUD}.
 *
 * <p>{@link CustomHudElements} is newer than the first cores that run client modules. On one of
 * those this class fails to load, so {@link #install} is only ever called inside a
 * {@code catch (LinkageError)}, and the rest of the module never names this class's type.</p>
 */
public final class CustomElements implements CustomHudElements {

    private final StreamlineHUD module;

    private CustomElements(StreamlineHUD module) {
        this.module = module;
    }

    public static void install(StreamlineHUD module) {
        HudRegistry.setCustomElements(module.getIdentifier(), new CustomElements(module));
    }

    @Override
    public String add(String id, String template) {
        return module.addCustom(id, template);
    }

    @Override
    public String remove(String elementId) {
        return module.removeCustom(elementId);
    }
}
