package net.streamline.platform.listeners;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.GfxCanvas;

/**
 * Draws Streamline's HUD through Fabric API's {@code HudRenderCallback}, its HUD hook up to
 * Minecraft 1.21.1. Client-only.
 */
public final class FabricHudHook {

    private FabricHudHook() {}

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tick) -> StreamlineClient.renderHud(GfxCanvas.wrap(graphics)));
    }
}
