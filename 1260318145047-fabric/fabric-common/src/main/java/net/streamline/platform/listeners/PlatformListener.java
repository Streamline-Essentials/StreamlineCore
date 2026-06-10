package net.streamline.platform.listeners;

import gg.drak.thebase.events.BaseEventHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.base.StreamlineFabric;
import net.streamline.platform.commands.ProperCommand;
import net.streamline.platform.savables.UserManager;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
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
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Commands registered dynamically after modules load via registerCommand()
        });
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

    private void onPlayerJoin(ServerPlayer player) {
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
                if (StreamlineFabric.getInstance().getServer() != null) {
                    cp.setServerName(StreamlineFabric.getInstance().getServer().getMotd());
                }
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player join for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    private void onPlayerQuit(ServerPlayer player) {
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

    public static void registerCommand(ProperCommand command) {
        if (StreamlineFabric.getInstance().getServer() == null) return;
        try {
            StreamlineFabric.getInstance().getServer().getCommands()
                    .getDispatcher().register(command.buildBrigadier());
        } catch (Exception e) {
            MessageUtils.logWarning("Error registering command '" + command.getParent().getBase() + "': " + e.getMessage());
        }
    }
}
