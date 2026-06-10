package net.streamline.platform.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.streamline.base.StreamlineNeoForge;
import net.streamline.platform.commands.ProperCommand;
import singularity.data.uuid.UuidManager;
import singularity.events.server.ServerStartEvent;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class PlatformListener {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        try {
            StreamlineNeoForge.getInstance().onServerEnable();
        } catch (Exception e) {
            MessageUtils.logWarning("Error during server enable: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        try {
            BaseEventHandler.fireEvent(new ServerStartEvent());
        } catch (Exception e) {
            MessageUtils.logWarning("Error firing ServerStartEvent: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        try {
            StreamlineNeoForge.getInstance().onServerDisable();
        } catch (Exception e) {
            MessageUtils.logWarning("Error during server disable: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            String uuid = player.getStringUUID();
            String name = player.getName().getString();
            String resolvedIp = "";
            try {
                SocketAddress addr = player.connection.getRemoteAddress();
                if (addr instanceof InetSocketAddress inet) resolvedIp = inet.getHostString();
            } catch (Exception ignored) {}

            final String ip = resolvedIp;
            UuidManager.cachePlayer(uuid, name, ip);

            UserUtils.getOrCreatePlayer(uuid).ifPresent(cp -> {
                cp.setCurrentName(name);
                cp.setCurrentIp(ip);
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    cp.setServerName(server.getMotd());
                }
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player join for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        try {
            String uuid = player.getStringUUID();
            UserUtils.getOrCreatePlayer(uuid).ifPresent(cp -> {
                cp.save();
                UserUtils.unloadSender(cp);
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player quit for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Commands are registered dynamically after modules load via registerCommand()
    }

    public static void registerCommand(ProperCommand command) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        try {
            server.getCommands()
                    .getDispatcher().register(command.buildBrigadier());
        } catch (Exception e) {
            MessageUtils.logWarning("Error registering command '" + command.getParent().getBase() + "': " + e.getMessage());
        }
    }
}
