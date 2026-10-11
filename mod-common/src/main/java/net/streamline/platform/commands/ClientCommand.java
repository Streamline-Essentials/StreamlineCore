package net.streamline.platform.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import gg.drak.thebase.utils.StringUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import net.streamline.platform.savables.UserManager;
import net.streamline.platform.text.LegacyText;
import singularity.command.CommandHandler;
import singularity.command.CosmicCommand;
import singularity.data.console.CosmicSender;
import singularity.utils.MessageUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * {@code /streamlineclient <command> (args ...)} and its alias {@code /slclient}: a client
 * command that runs a Streamline command, picked by any of its aliases, as the local player.
 *
 * <p>The game client has no Streamline of its own; Streamline runs on the integrated server
 * of a world this client hosts (singleplayer, or opened to LAN), so that is where the command
 * runs, on the server thread, as the host's {@link ServerPlayer}. Its output reaches chat
 * the usual way. On a remote server there is nothing to run it against.</p>
 *
 * <p>This class only touches classes both sides have; each loader registers the labels with
 * its own client-command API and forwards here.</p>
 */
public final class ClientCommand {

    public static final String[] LABELS = {"streamlineclient", "slclient"};

    private static final String USAGE = "&eUsage: &f/%s <command> (args ...)";
    private static final String NOT_HOSTING = "&cStreamline runs only in a world this game hosts: "
            + "open a singleplayer world, or open one to LAN.";

    private ClientCommand() {}

    /**
     * Registers every label into a client-command dispatcher whose sources are
     * {@link CommandSourceStack}s with the local player as their entity, as Forge and NeoForge
     * build them.
     *
     * @param clientThread the client's main thread, where replies are shown
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Executor clientThread) {
        for (String label : LABELS) {
            dispatcher.register(Commands.literal(label)
                    .executes(ctx -> run(label, ctx, "", clientThread))
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                            .suggests((ctx, builder) -> ctx.getSource().getEntity() == null
                                    ? builder.buildFuture()
                                    : suggest(ctx.getSource().getEntity().getUUID(), builder))
                            .executes(ctx -> run(label, ctx, StringArgumentType.getString(ctx, "args"), clientThread))));
        }
    }

    private static int run(String label, CommandContext<CommandSourceStack> ctx, String line, Executor clientThread) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() == null) return 0;
        run(label, source.getEntity().getUUID(), line,
                message -> clientThread.execute(() -> source.sendSystemMessage(message)));
        return 1;
    }

    /**
     * Runs {@code line} for the local player.
     *
     * @param label  the label typed, for the usage message
     * @param player the local player's UUID
     * @param line   everything after the label: the Streamline command and its arguments
     * @param reply  shows a message to the local player; may be called from the server thread
     */
    public static void run(String label, UUID player, String line, Consumer<Component> reply) {
        String[] args = line == null ? new String[0] : line.trim().split(" +");
        if (args.length == 0 || args[0].isEmpty()) {
            reply.accept(LegacyText.parse(String.format(USAGE, label)));
            return;
        }

        MinecraftServer server = BasePlugin.getServer();
        if (server == null || UserManager.getInstance() == null) {
            reply.accept(LegacyText.parse(NOT_HOSTING));
            return;
        }

        server.execute(() -> {
            CosmicSender sender = senderOf(server, player);
            if (sender == null) {
                reply.accept(LegacyText.parse(NOT_HOSTING));
                return;
            }

            CosmicCommand command = CommandHandler.getCommandByAlias(args[0]);
            if (command == null) {
                reply.accept(LegacyText.parse("&cCommand not found: &f" + args[0]));
                return;
            }

            try {
                command.baseRun(sender, StringUtils.argsMinus(args, 0));
            } catch (Exception e) {
                MessageUtils.logWarning("Error executing command '" + command.getBase() + "' from /" + label + ": " + e.getMessage());
                reply.accept(LegacyText.parse("&cThat command failed; see the log for details."));
            }
        });
    }

    /**
     * Suggests the word being typed in {@code builder}'s remaining input: a command alias for
     * the first word, otherwise whatever that command completes.
     */
    public static CompletableFuture<Suggestions> suggest(UUID player, SuggestionsBuilder builder) {
        try {
            String input = builder.getRemaining();
            String[] args = input.isEmpty() ? new String[]{""} : input.split(" ", -1);

            ConcurrentSkipListSet<String> completions;
            if (args.length <= 1) {
                completions = CommandHandler.getAllAliases();
            } else {
                MinecraftServer server = BasePlugin.getServer();
                CosmicCommand command = CommandHandler.getCommandByAlias(args[0]);
                CosmicSender sender = server == null || UserManager.getInstance() == null ? null : senderOf(server, player);
                if (command == null || sender == null) return builder.buildFuture();
                completions = command.baseTabComplete(sender, StringUtils.argsMinus(args, 0));
            }
            if (completions == null) return builder.buildFuture();

            // Brigadier replaces only from the builder's start, so suggestions for a later
            // word are offset to where that word begins.
            SuggestionsBuilder wordBuilder = builder.createOffset(builder.getStart() + input.lastIndexOf(' ') + 1);
            MessageUtils.getCompletion(completions, args[args.length - 1]).forEach(wordBuilder::suggest);
            return wordBuilder.buildFuture();
        } catch (Exception e) {
            return builder.buildFuture();
        }
    }

    private static CosmicSender senderOf(MinecraftServer server, UUID player) {
        if (server.getPlayerList() == null) return null;
        ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player);
        if (serverPlayer == null) return null;
        return UserManager.getInstance().getOrCreateSender(serverPlayer).orElse(null);
    }
}
