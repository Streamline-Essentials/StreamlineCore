package net.streamline.platform.listeners;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.streamline.platform.commands.ClientCommand;

/**
 * Client-only Forge registrations, for Forge versions on EventBus 6. Touches client classes,
 * so it is loaded only on a client.
 */
public final class ForgeClientListener {

    private ForgeClientListener() {}

    public static void register() {
        // The first client tick comes once the game is up, at the title screen.
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> ModEvents.onClientStarted());
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) ->
                ClientCommand.register(e.getDispatcher(), Minecraft.getInstance()));
    }
}
