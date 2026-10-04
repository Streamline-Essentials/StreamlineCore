package net.streamline.platform.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import singularity.utils.MessageUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks every Streamline command and keeps the server's Brigadier dispatcher in step with it.
 *
 * <p>Modules register commands while the server is already running, so registration goes
 * straight into the live dispatcher and the command tree is resent to online players.
 * The server rebuilds its dispatcher on {@code /reload}; the loaders call
 * {@link #registerAll(CommandDispatcher)} from their command-registration hook so the
 * rebuilt dispatcher gets every command back.</p>
 *
 * <p>Brigadier offers no way to remove a node, so {@link #unregister(ProperCommand)} only
 * stops the command coming back on the next dispatcher rebuild.</p>
 */
public final class CommandRegistry {

    private static final Map<String, ProperCommand> COMMANDS = new ConcurrentHashMap<>();

    private CommandRegistry() {}

    public static void register(ProperCommand command) {
        COMMANDS.put(command.getParent().getIdentifier(), command);

        MinecraftServer server = BasePlugin.getServer();
        if (server == null) return;

        registerInto(server.getCommands().getDispatcher(), command);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    public static void unregister(ProperCommand command) {
        COMMANDS.remove(command.getParent().getIdentifier());
    }

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        COMMANDS.values().forEach(command -> registerInto(dispatcher, command));
    }

    private static void registerInto(CommandDispatcher<CommandSourceStack> dispatcher, ProperCommand command) {
        for (String label : command.getLabels()) {
            try {
                dispatcher.register(command.buildBrigadier(label));
            } catch (Exception e) {
                MessageUtils.logWarning("Error registering command '" + label + "': " + e.getMessage());
            }
        }
    }
}
