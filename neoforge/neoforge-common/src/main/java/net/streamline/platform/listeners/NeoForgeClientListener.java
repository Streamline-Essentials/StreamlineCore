package net.streamline.platform.listeners;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.streamline.platform.commands.ClientCommand;

/**
 * Client-only NeoForge registrations. Touches client classes, so it is registered only on
 * a client.
 */
public class NeoForgeClientListener {

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        ClientCommand.register(event.getDispatcher(), Minecraft.getInstance());
    }
}
