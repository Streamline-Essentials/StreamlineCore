package net.streamline.platform.listeners;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.streamline.client.Keybinds;
import net.streamline.client.StreamlineClient;
import net.streamline.platform.commands.ClientCommand;

/**
 * Client-only Forge registrations, for Forge versions on EventBus 6. Touches client classes,
 * so it is loaded only on a client.
 */
public final class ForgeClientListener {

    private ForgeClientListener() {}

    public static void register() {
        StreamlineClient.init();
        ForgeHudHook.register();
        // The first client tick comes once the game is up, at the title screen.
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            ModEvents.onClientStarted();
            Keybinds.tick();
        });
        // Key mappings are registered on the mod bus, while the mod is being constructed.
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterKeyMappingsEvent e) -> {
            for (KeyMapping mapping : Keybinds.all()) e.register(mapping);
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) ->
                ClientCommand.register(e.getDispatcher(), Minecraft.getInstance()));
    }
}
