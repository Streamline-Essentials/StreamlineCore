package net.streamline.platform.listeners;

import com.mojang.brigadier.CommandDispatcher;
import gg.drak.thebase.events.BaseEventHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.commands.CommandRegistry;
import net.streamline.platform.handlers.GameplayHandler;
import net.streamline.platform.handlers.StatusPingHandler;
import net.streamline.platform.handlers.TheBaseShutdown;
import net.streamline.platform.savables.UserManager;
import singularity.Singularity;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.UnloadPlayerEvent;
import singularity.data.players.events.UnloadSenderEvent;
import singularity.data.uuid.UuidManager;
import singularity.events.player.CosmicCommandPreprocessEvent;
import singularity.events.player.CosmicDeathEvent;
import singularity.events.server.CosmicChatEvent;
import singularity.events.server.LoginCompletedEvent;
import singularity.events.server.LogoutEvent;
import singularity.events.server.ServerStartEvent;
import singularity.events.server.ServerStopEvent;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The loader-agnostic reaction to every server and player event Streamline cares about.
 * Each loader's listener translates its own events into calls here, so behaviour is
 * identical on Fabric, Forge and NeoForge.
 */
public final class ModEvents {

    private ModEvents() {}

    /**
     * Classes first touched while the server stops or players leave. A mod loader reads class
     * bytes lazily from the open jar, so if that jar is replaced on disk while the server runs
     * (copying a new build into {@code mods/} before restarting), anything not yet loaded fails
     * with {@code ZipException: invalid LOC header}. Loading these up front keeps the stop path
     * working in that case; {@link TheBaseShutdown} matters most, since without it TheBase's
     * Swing timers keep the JVM from exiting.
     */
    private static final Class<?>[] STOP_PATH_CLASSES = {
            TheBaseShutdown.class,
            LogoutEvent.class,
            ServerStopEvent.class,
            UnloadSenderEvent.class,
            UnloadPlayerEvent.class,
    };

    private static void preloadStopPath() {
        ClassLoader loader = ModEvents.class.getClassLoader();
        for (Class<?> type : STOP_PATH_CLASSES) {
            try {
                Class.forName(type.getName(), true, loader);
            } catch (Throwable t) {
                MessageUtils.logWarning("Could not preload " + type.getName() + ": " + t);
            }
        }
    }

    /**
     * A {@link LinkageError} here almost always means the mod jar changed on disk while the
     * server was running; the message says so instead of leaving a bare stack trace.
     */
    private static void logStopFailure(String what, Throwable t) {
        if (t instanceof LinkageError) {
            MessageUtils.logWarning(what + " failed: " + t + ". The StreamlineCore jar was likely "
                    + "replaced while the server was running; replace mod jars only while the server is stopped.");
        } else {
            MessageUtils.logWarning(what + " failed: " + t.getMessage());
        }
    }

    public static void onServerStarting(MinecraftServer server) {
        try {
            BasePlugin.getInstance().onServerEnable(server);
        } catch (Exception e) {
            MessageUtils.logWarning("Error during server enable: " + e.getMessage());
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        preloadStopPath();
        // The listening channels are bound by now, which they may not be while starting.
        StatusPingHandler.install(server);
        try {
            BaseEventHandler.fireEvent(new ServerStartEvent());
        } catch (Exception e) {
            MessageUtils.logWarning("Error firing ServerStartEvent: " + e.getMessage());
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        try {
            BasePlugin.getInstance().onServerDisable();
        } catch (Throwable t) {
            logStopFailure("Server disable", t);
        }
    }

    public static void onServerStopped(MinecraftServer server) {
        BasePlugin.setServer(null);
        try {
            TheBaseShutdown.stopQueuedTasks();
        } catch (Throwable t) {
            logStopFailure("Stopping TheBase tasks", t);
        }
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
                cp.setLocation(GameplayHandler.locationOf(player));

                ModuleUtils.fireEvent(new LoginCompletedEvent(cp));
            });

            Singularity.gameplay().ifPresent(gameplay -> gameplay.refreshDisplayName(uuid));
        } catch (Exception e) {
            MessageUtils.logWarning("Error on player join for " + player.getName().getString() + ": " + e.getMessage());
        }
    }

    public static void onPlayerQuit(ServerPlayer player) {
        try {
            UserUtils.getOrCreatePlayer(player.getStringUUID()).ifPresent(cp -> {
                cp.setLocation(GameplayHandler.locationOf(player));
                ModuleUtils.fireEvent(new LogoutEvent(cp));

                cp.save();
                UserUtils.unloadSender(cp);
            });
        } catch (Throwable t) {
            logStopFailure("Player quit for " + player.getName().getString(), t);
        }
        Singularity.gameplay().ifPresent(gameplay -> {
            if (gameplay instanceof GameplayHandler) ((GameplayHandler) gameplay).forget(player);
        });
    }

    /**
     * Offers a chat message to Streamline's listeners.
     *
     * @param setMessage applies a rewritten message, on loaders that allow rewriting
     * @return {@code false} if a listener cancelled the message
     */
    public static boolean onChat(ServerPlayer player, String message, Consumer<String> setMessage) {
        Optional<CosmicPlayer> sender = UserUtils.getPlayer(player.getStringUUID());
        if (sender.isEmpty()) return true;
        try {
            CosmicChatEvent event = new CosmicChatEvent(sender.get(), message);
            ModuleUtils.fireEvent(event);
            if (event.isCanceled()) return false;
            if (setMessage != null && ! message.equals(event.getMessage())) setMessage.accept(event.getMessage());
        } catch (Exception e) {
            MessageUtils.logWarning("Error handling chat from " + player.getName().getString() + ": " + e.getMessage());
        }
        return true;
    }

    /**
     * Offers a command a player is about to run to Streamline's listeners.
     *
     * @param commandLine the command as typed, with or without the leading slash
     * @return {@code false} if a listener cancelled the command
     */
    public static boolean onCommand(ServerPlayer player, String commandLine) {
        Optional<CosmicPlayer> sender = UserUtils.getPlayer(player.getStringUUID());
        if (sender.isEmpty()) return true;
        try {
            CosmicCommandPreprocessEvent event = new CosmicCommandPreprocessEvent(sender.get(), commandLine);
            ModuleUtils.fireEvent(event);
            return ! event.isCancelled();
        } catch (Exception e) {
            MessageUtils.logWarning("Error handling command from " + player.getName().getString() + ": " + e.getMessage());
            return true;
        }
    }

    public static void onDeath(ServerPlayer player) {
        UserUtils.getPlayer(player.getStringUUID()).ifPresent(cp -> {
            try {
                ModuleUtils.fireEvent(new CosmicDeathEvent(cp, GameplayHandler.locationOf(player)));
            } catch (Exception e) {
                MessageUtils.logWarning("Error handling death of " + player.getName().getString() + ": " + e.getMessage());
            }
        });
    }
}
