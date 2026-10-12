package net.streamline.platform.listeners;

import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.GfxCanvas;

/**
 * Draws Streamline's HUD after the rest of the GUI, from {@code RenderGuiEvent.Post} on the
 * game bus: Forge 1.20.1 and the NeoForge fork of it. Client-only.
 */
public final class ForgeHudHook {

    private ForgeHudHook() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Post e) ->
                StreamlineClient.renderHud(GfxCanvas.wrap(e.getGuiGraphics())));
    }
}
