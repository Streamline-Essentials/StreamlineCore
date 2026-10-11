package net.streamline.platform.listeners;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.streamline.client.StreamlineClient;
import net.streamline.platform.commands.ClientCommand;

/**
 * Client-only Forge registrations, for Forge versions on EventBus 7. Touches client classes,
 * so it is loaded only on a client.
 */
public final class ForgeClientListener {

    private ForgeClientListener() {}

    public static void register() {
        StreamlineClient.init();
        ForgeHudHook.register();
        // The first client tick comes once the game is up, at the title screen.
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> ModEvents.onClientStarted());
        RegisterClientCommandsEvent.BUS.addListener(e ->
                ClientCommand.register(e.getDispatcher(), Minecraft.getInstance()));
    }
}
