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
import singularity.client.ClientBridge;
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
 * {@code /streamlineclient (menu|hud|<command> (args ...))} and its alias {@code /slclient}: a
 * client command that opens the client module menu or the HUD editor, or runs a Streamline
 * command, picked by any of its aliases, as the local player.
 *
 * <p>{@code menu} and {@code hud} belong to the client and work anywhere, remote servers
 * included; they win over a module command with the same alias. Any other command runs as a
 * player, and a client has a player only on the integrated server of a world it hosts
 * (singleplayer, or opened to LAN), so that is where it runs, on the server thread, as the
 * host's {@link ServerPlayer}. Its output reaches chat the usual way.</p>
 *
 * <p>This class only touches classes both sides have; each loader registers the labels with
 * its own client-command API and forwards here.</p>
 */
public final class ClientCommand {

    public static final String[] LABELS = {"streamlineclient", "slclient"};

    /** Subcommands the client handles itself, ahead of any module alias. */
    public static final String MENU = "menu";
    public static final String HUD = "hud";

    private static final String USAGE = "&eUsage: &f/%s (menu|hud|<command> (args ...))"
            + "%n&7menu &f- client module settings  &7hud &f- move and scale HUD elements";
    private static final String NO_CLIENT_UI = "&cThe Streamline client menu is not available in this game.";
    private static final String NOT_HOSTING = "&cStreamline runs only in a world this game hosts: "
            + "open a singleplayer world, or open one to LAN.";
    /**
     * Streamline makes profiles only for UUIDs it can resolve to a name while the server
     * authenticates, as an integrated server does, so an account Mojang never issued (a
     * development launch, for one) gets none.
     */
    private static final String NO_PROFILE = "&cStreamline has no player profile for you in this world.";

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

    /**
     * Forge's and NeoForge's client source stacks override {@code sendSuccess} to show chat;
     * {@code sendSystemMessage} goes to whatever source they wrap.
     */
    private static int run(String label, CommandContext<CommandSourceStack> ctx, String line, Executor clientThread) {
        CommandSourceStack source = ctx.getSource();
        if (source.getEntity() == null) return 0;
        run(label, source.getEntity().getUUID(), line,
                message -> clientThread.execute(() -> source.sendSuccess(() -> message, false)));
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
            for (String usageLine : String.format(USAGE, label).split("\\R")) {
                reply.accept(LegacyText.parse(usageLine));
            }
            return;
        }

        if (runBuiltIn(args[0], reply)) return;

        MinecraftServer server = BasePlugin.getServer();
        if (server == null || UserManager.getInstance() == null) {
            reply.accept(LegacyText.parse(NOT_HOSTING));
            return;
        }

        server.execute(() -> {
            CosmicSender sender = senderOf(server, player);
            if (sender == null) {
                reply.accept(LegacyText.parse(NO_PROFILE));
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
     * Runs {@code menu} or {@code hud}, which need no world. Called on the client thread, from
     * the command callback.
     *
     * @return whether {@code name} was one of them
     */
    private static boolean runBuiltIn(String name, Consumer<Component> reply) {
        boolean menu = name.equalsIgnoreCase(MENU);
        boolean hud = name.equalsIgnoreCase(HUD);
        if (! menu && ! hud) return false;

        if (! ClientBridge.isClient()) {
            reply.accept(LegacyText.parse(NO_CLIENT_UI));
        } else if (menu) {
            ClientBridge.openMenu();
        } else {
            ClientBridge.openHudEditor();
        }
        return true;
    }

    /**
     * Suggests the word being typed in {@code builder}'s remaining input: {@code menu},
     * {@code hud} or a command alias for the first word, otherwise whatever that command
     * completes.
     */
    public static CompletableFuture<Suggestions> suggest(UUID player, SuggestionsBuilder builder) {
        try {
            String input = builder.getRemaining();
            String[] args = input.isEmpty() ? new String[]{""} : input.split(" ", -1);

            ConcurrentSkipListSet<String> completions;
            if (args.length <= 1) {
                completions = new ConcurrentSkipListSet<>(CommandHandler.getAllAliases());
                completions.add(MENU);
                completions.add(HUD);
            } else {
                if (args[0].equalsIgnoreCase(MENU) || args[0].equalsIgnoreCase(HUD)) return builder.buildFuture();
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
