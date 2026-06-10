package net.streamline.platform.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.streamline.base.StreamlineFabric;
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

    public void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> onServerStart());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> onServerStop());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerQuit(handler.player));
    }

    private void onServerStart() {
        try {
            BaseEventHandler.fireEvent(new ServerStartEvent());
        } catch (Exception e) {
            MessageUtils.logWarning("Error firing ServerStartEvent: " + e.getMessage());
        }
    }

    private void onServerStop() {
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
    }

    private void onPlayerJoin(ServerPlayerEntity player) {
        try {
            String uuid = player.getUuidAsString();
            String name = player.getName().getString();
            String resolvedIp = "";
            try {
                SocketAddress addr = player.networkHandler.getConnection().getAddress();
                if (addr instanceof InetSocketAddress) resolvedIp = ((InetSocketAddress) addr).getHostString();
            } catch (Exception ignored) {}

            final String ip = resolvedIp;
            UuidManager.cachePlayer(uuid, name, ip);

            UserUtils.getOrCreatePlayer(uuid).ifPresent(cp -> {
                cp.setCurrentName(name);
                cp.setCurrentIp(ip);
                if (StreamlineFabric.getInstance().getServer() != null) {
                    cp.setServerName(StreamlineFabric.getInstance().getServer().getServerMotd());
                }
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player join for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    private void onPlayerQuit(ServerPlayerEntity player) {
        try {
            String uuid = player.getUuidAsString();
            UserUtils.getOrCreatePlayer(uuid).ifPresent(cp -> {
                cp.save();
                UserUtils.unloadSender(cp);
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player quit for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    public static void registerCommand(ProperCommand command) {
        if (StreamlineFabric.getInstance().getServer() == null) return;
        try {
            StreamlineFabric.getInstance().getServer().getCommandManager()
                    .getDispatcher().register(command.buildBrigadier());
        } catch (Exception e) {
            MessageUtils.logWarning("Error registering command '" + command.getParent().getBase() + "': " + e.getMessage());
        }
    }
}
