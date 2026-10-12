package net.streamline.platform.listeners;

import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.GfxCanvas;

/**
 * Adds Streamline's HUD as a GUI layer on Forge's EventBus 7, where the client posts
 * {@code AddGuiOverlayLayersEvent} on the event's own {@code BUS}. Client-only.
 */
public final class ForgeHudHook {

    private ForgeHudHook() {}

    public static void register() {
        AddGuiOverlayLayersEvent.BUS.addListener(e ->
                e.getLayeredDraw().add(Identifier.fromNamespaceAndPath("streamlinecore", "hud"),
                        (graphics, delta) -> StreamlineClient.renderHud(GfxCanvas.wrap(graphics))));
    }
}
