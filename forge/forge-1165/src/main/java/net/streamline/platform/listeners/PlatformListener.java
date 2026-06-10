package net.streamline.platform.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.streamline.base.StreamlineForge;
import net.streamline.platform.commands.ProperCommand;
import singularity.Singularity;
import singularity.data.uuid.UuidManager;
import singularity.events.server.ServerStartEvent;
import singularity.events.server.ServerStopEvent;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class PlatformListener {

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        StreamlineForge.getInstance().onServerEnable();
    }

    @SubscribeEvent
    public void onServerStarted(FMLServerStartedEvent event) {
        try {
            BaseEventHandler.fireEvent(new ServerStartEvent());
        } catch (Exception e) {
            MessageUtils.logWarning("Error firing ServerStartEvent: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerStopping(FMLServerStoppingEvent event) {
        try {
            Singularity.getTpTicketFlusher().cancel();
            Singularity.getTpTicketPuller().cancel();
            UserUtils.syncAllUsers();
            UuidManager.getUuids().forEach(singularity.data.uuid.UuidInfo::save);
            BaseEventHandler.fireEvent(new ServerStopEvent());
            singularity.scheduler.TaskManager.stop();
        } catch (Exception e) {
            MessageUtils.logWarning("Error during server stop: " + e.getMessage());
        }
        StreamlineForge.getInstance().onServerDisable();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        try {
            String uuid = player.getStringUUID();
            String name = player.getName().getString();
            String resolvedIp = "";
            try {
                SocketAddress addr = player.connection.connection.channel().remoteAddress();
                if (addr instanceof InetSocketAddress) resolvedIp = ((InetSocketAddress) addr).getHostString();
            } catch (Exception ignored) {}

            final String ip = resolvedIp;
            UuidManager.cachePlayer(uuid, name, ip);

            UserUtils.getOrCreatePlayer(uuid).ifPresent(cp -> {
                cp.setCurrentName(name);
                cp.setCurrentIp(ip);
                if (StreamlineForge.getInstance() != null
                        && ServerLifecycleHooks.getCurrentServer() != null) {
                    cp.setServerName(ServerLifecycleHooks.getCurrentServer().getMotd());
                }
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player join for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
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
        if (ServerLifecycleHooks.getCurrentServer() == null) return;
        try {
            ServerLifecycleHooks.getCurrentServer()
                    .getCommands()
                    .getDispatcher()
                    .register(command.buildBrigadier());
        } catch (Exception e) {
            MessageUtils.logWarning("Error registering command '" + command.getParent().getBase() + "': " + e.getMessage());
        }
    }
}
