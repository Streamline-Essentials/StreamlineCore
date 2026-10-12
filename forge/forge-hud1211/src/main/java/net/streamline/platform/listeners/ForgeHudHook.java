package net.streamline.platform.listeners;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.GfxCanvas;

/**
 * Adds Streamline's HUD as a GUI layer. Forge 1.21.1 posts {@code AddGuiOverlayLayersEvent}
 * on the mod bus, so the listener goes there, while the mod is being constructed.
 * Client-only.
 */
public final class ForgeHudHook {

    private ForgeHudHook() {}

    public static void register() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((AddGuiOverlayLayersEvent e) ->
                e.getLayeredDraw().add(ResourceLocation.fromNamespaceAndPath("streamlinecore", "hud"),
                        (graphics, delta) -> StreamlineClient.renderHud(GfxCanvas.wrap(graphics))));
    }
}
