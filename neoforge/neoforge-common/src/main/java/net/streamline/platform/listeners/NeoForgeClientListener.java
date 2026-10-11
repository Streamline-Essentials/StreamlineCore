package net.streamline.platform.listeners;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.streamline.platform.commands.ClientCommand;

/**
 * Client-only NeoForge registrations. Touches client classes, so it is registered only on
 * a client.
 */
public class NeoForgeClientListener {

    /** The first client tick comes once the game is up, at the title screen. */
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        ModEvents.onClientStarted();
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommand.register(event.getDispatcher(), Minecraft.getInstance());
    }
}
