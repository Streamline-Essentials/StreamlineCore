package net.streamline.platform.listeners;

import com.mojang.brigadier.CommandDispatcher;
import gg.drak.thebase.events.BaseEventHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.commands.CommandRegistry;
import net.streamline.platform.handlers.TheBaseShutdown;
import net.streamline.platform.savables.UserManager;
import singularity.data.uuid.UuidManager;
import singularity.events.server.ServerStartEvent;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

/**
 * The loader-agnostic reaction to every server and player event Streamline cares about.
 * Each loader's listener translates its own events into calls here, so behaviour is
 * identical on Fabric, Forge and NeoForge.
 */
public final class ModEvents {

    private ModEvents() {}

    public static void onServerStarting(MinecraftServer server) {
        try {
            BasePlugin.getInstance().onServerEnable(server);
        } catch (Exception e) {
            MessageUtils.logWarning("Error during server enable: " + e.getMessage());
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        try {
            BaseEventHandler.fireEvent(new ServerStartEvent());
        } catch (Exception e) {
            MessageUtils.logWarning("Error firing ServerStartEvent: " + e.getMessage());
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        try {
            BasePlugin.getInstance().onServerDisable();
        } catch (Exception e) {
            MessageUtils.logWarning("Error during server disable: " + e.getMessage());
        }
    }

    public static void onServerStopped(MinecraftServer server) {
        BasePlugin.setServer(null);
        TheBaseShutdown.stopQueuedTasks();
    }

    public static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandRegistry.registerAll(dispatcher);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        try {
            String uuid = player.getStringUUID();
            String name = player.getName().getString();
            String resolvedIp = "";
            try {
                String ip = UserManager.getIp(player);
                if (ip != null) resolvedIp = ip;
            } catch (Exception ignored) {}

            final String ip = resolvedIp;
            UuidManager.cachePlayer(uuid, name, ip);

            UserUtils.getOrCreatePlayer(uuid).ifPresent(cp -> {
                cp.setCurrentName(name);
                cp.setCurrentIp(ip);
                MinecraftServer server = BasePlugin.getServer();
                if (server != null) cp.setServerName(server.getMotd());
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player join for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    public static void onPlayerQuit(ServerPlayer player) {
        try {
            UserUtils.getOrCreatePlayer(player.getStringUUID()).ifPresent(cp -> {
                cp.save();
                UserUtils.unloadSender(cp);
            });
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player quit for " + player.getName().getString() + ": " + e.getMessage());
        }
    }
}
