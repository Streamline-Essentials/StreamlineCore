package net.streamline.platform.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import singularity.utils.MessageUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
 * <p>Brigadier offers no way to remove a node, so a node, once added, stays in the live
 * dispatcher. Each node therefore resolves its label through {@link #ACTIVE} on every use:
 * its requirement passes only while some command owns the label, which makes an
 * unregistered command unknown to parsing and absent from the tree sent to clients, and
 * execution and suggestions go to the label's current owner. Re-registering a label merges
 * into the existing node and keeps that node's requirement and suggestion provider, so
 * nothing on a node may capture a particular {@link ProperCommand} — it would outlive the
 * module that created it.</p>
 */
public final class CommandRegistry {

    private static final Map<String, ProperCommand> COMMANDS = new ConcurrentHashMap<>();
    /** Each live label and the command that currently answers to it. */
    private static final Map<String, ProperCommand> ACTIVE = new ConcurrentHashMap<>();

    private CommandRegistry() {}

    public static void register(ProperCommand command) {
        ProperCommand previous = COMMANDS.put(command.getParent().getIdentifier(), command);
        if (previous != null && previous != command) deactivate(previous);
        for (String label : command.getLabels()) {
            ACTIVE.put(label, command);
        }

        MinecraftServer server = BasePlugin.getServer();
        if (server == null) return;

        // Modules start on a worker thread; the dispatcher belongs to the server thread.
        server.execute(() -> {
            registerInto(server.getCommands().getDispatcher(), command);
            resendCommands(server);
        });
    }

    public static void unregister(ProperCommand command) {
        COMMANDS.remove(command.getParent().getIdentifier(), command);
        deactivate(command);

        MinecraftServer server = BasePlugin.getServer();
        if (server != null) server.execute(() -> resendCommands(server));
    }

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        COMMANDS.values().forEach(command -> registerInto(dispatcher, command));
    }

    /**
     * @return whether some registered command answers to {@code label}
     */
    public static boolean isActive(String label) {
        return ACTIVE.containsKey(label);
    }

    /**
     * The Brigadier tree for one label: a literal taking one greedy string argument, so the
     * command sees the same raw space-split arguments it gets on Bukkit-style platforms.
     */
    static LiteralArgumentBuilder<CommandSourceStack> buildBrigadier(String label) {
        return Commands.literal(label)
                .requires(source -> isActive(label))
                .executes(ctx -> execute(label, ctx, new String[0]))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggest(label, ctx, builder))
                        .executes(ctx -> execute(label, ctx, StringArgumentType.getString(ctx, "args").split(" "))));
    }

    private static int execute(String label, CommandContext<CommandSourceStack> ctx, String[] args) {
        ProperCommand command = ACTIVE.get(label);
        return command == null ? 0 : command.execute(ctx, args);
    }

    private static CompletableFuture<Suggestions> suggest(String label, CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ProperCommand command = ACTIVE.get(label);
        return command == null ? builder.buildFuture() : command.getSuggestions(ctx, builder);
    }

    /** Drops the labels {@code command} still owns; labels another command took over stay. */
    private static void deactivate(ProperCommand command) {
        ACTIVE.values().removeIf(owner -> owner == command);
    }

    private static void registerInto(CommandDispatcher<CommandSourceStack> dispatcher, ProperCommand command) {
        for (String label : command.getLabels()) {
            try {
                dispatcher.register(buildBrigadier(label));
            } catch (Exception e) {
                MessageUtils.logWarning("Error registering command '" + label + "': " + e.getMessage());
            }
        }
    }

    private static void resendCommands(MinecraftServer server) {
        // Modules can register while the server is still starting, before it has a player list.
        if (server.getPlayerList() == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }
}
