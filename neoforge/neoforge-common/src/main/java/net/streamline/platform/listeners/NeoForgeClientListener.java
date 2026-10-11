package net.streamline.platform.listeners;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.streamline.client.Keybinds;
import net.streamline.client.StreamlineClient;
import net.streamline.client.compat.GfxCanvas;
import net.streamline.platform.commands.ClientCommand;

/**
 * Client-only NeoForge registrations. Touches client classes, so it is registered only on
 * a client.
 */
public class NeoForgeClientListener {

    /** @param modBus the mod's own bus, where key mappings are registered */
    public NeoForgeClientListener(IEventBus modBus) {
        StreamlineClient.init();
        modBus.addListener((RegisterKeyMappingsEvent e) -> {
            for (KeyMapping mapping : Keybinds.all()) e.register(mapping);
        });
    }

    /** The first client tick comes once the game is up, at the title screen. */
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        ModEvents.onClientStarted();
        Keybinds.tick();
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
