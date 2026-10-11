package net.streamline.platform.listeners;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.GfxCanvas;
import net.streamline.platform.commands.ClientCommand;

/**
 * Client-only NeoForge registrations. Touches client classes, so it is registered only on
 * a client.
 */
public class NeoForgeClientListener {

    public NeoForgeClientListener() {
        StreamlineClient.init();
    }

    /** Draws Streamline's HUD after the rest of the GUI. */
    @SubscribeEvent
    public void onRenderGui(RenderGuiEvent.Post event) {
        StreamlineClient.renderHud(GfxCanvas.wrap(event.getGuiGraphics()));
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommand.register(event.getDispatcher(), Minecraft.getInstance());
    }
}
