package net.streamline.platform.listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/**
 * Forwards Forge events to {@link ModEvents}, for Forge versions on EventBus 7, where each
 * event type carries its own {@code BUS}.
 */
public final class ForgeListener {

    private ForgeListener() {}

    public static void register() {
        ServerStartingEvent.BUS.addListener(e -> ModEvents.onServerStarting(e.getServer()));
        ServerStartedEvent.BUS.addListener(e -> ModEvents.onServerStarted(e.getServer()));
        ServerStoppingEvent.BUS.addListener(e -> ModEvents.onServerStopping(e.getServer()));
        ServerStoppedEvent.BUS.addListener(e -> ModEvents.onServerStopped(e.getServer()));
        RegisterCommandsEvent.BUS.addListener(e -> ModEvents.onRegisterCommands(e.getDispatcher()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerJoin((ServerPlayer) e.getEntity());
        });
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer) ModEvents.onPlayerQuit((ServerPlayer) e.getEntity());
        });
    }
}
