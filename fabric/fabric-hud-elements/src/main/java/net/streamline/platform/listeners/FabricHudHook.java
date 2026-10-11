package net.streamline.platform.listeners;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.GfxCanvas;

/**
 * Draws Streamline's HUD as a Fabric API HUD element, added last so it sits above the
 * vanilla HUD. Fabric API's HUD hook from Minecraft 1.21.6. Client-only.
 */
public final class FabricHudHook {

    private FabricHudHook() {}

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("streamlinecore", "hud"),
                (graphics, tick) -> StreamlineClient.renderHud(GfxCanvas.wrap(graphics)));
    }
}
