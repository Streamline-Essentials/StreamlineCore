package net.streamline.platform.listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/**
 * Forwards Forge game-bus events to {@link ModEvents}, for Forge versions on EventBus 6
 * (a single {@link MinecraftForge#EVENT_BUS} carrying every event).
 */
public final class ForgeListener {

    private ForgeListener() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent e) -> ModEvents.onServerStarting(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ModEvents.onServerStarted(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> ModEvents.onServerStopping(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ModEvents.onServerStopped(e.getServer()));
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> ModEvents.onRegisterCommands(e.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerJoin((ServerPlayer) e.getEntity());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerQuit((ServerPlayer) e.getEntity());
        });
    }
}
