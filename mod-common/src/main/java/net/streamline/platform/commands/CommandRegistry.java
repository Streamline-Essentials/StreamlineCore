package net.streamline.platform.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.streamline.platform.BasePlugin;
import singularity.utils.MessageUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Tracks every Streamline command and keeps the server's Brigadier dispatcher in step with it.
 *
 * <p>Modules register commands while the server is already running, so registration goes
 * straight into the live dispatcher and the command tree is resent to online players.
 * The server rebuilds its dispatcher on {@code /reload}; the loaders call
 * {@link #registerAll(CommandDispatcher)} from their command-registration hook so the
 * rebuilt dispatcher gets every command back.</p>
 *
 * <p>Registering a label that vanilla or another mod already owns would merge into that
 * foreign node, which keeps the foreign node's requirement and redirect (vanilla {@code /w}
 * redirects to {@code /msg}), so the foreign command would keep answering. A Streamline node
 * therefore takes the foreign node's place on the root, the foreign node is held in
 * {@link #DISPLACED}, and it gets its place back once no Streamline command owns the label.</p>
 *
 * <p>Streamline's own nodes are never removed: once added, a node stays in the live
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
    /** Foreign root nodes a Streamline label took the place of, by label. */
    private static final Map<String, CommandNode<CommandSourceStack>> DISPLACED = new ConcurrentHashMap<>();

    private CommandRegistry() {}

    public static void register(ProperCommand command) {
        ProperCommand previous = COMMANDS.put(command.getParent().getIdentifier(), command);
        List<String> released = previous != null && previous != command ? deactivate(previous) : new ArrayList<>();
        for (String label : command.getLabels()) {
            ACTIVE.put(label, command);
        }

        MinecraftServer server = BasePlugin.getServer();
        if (server == null) return;

        // Modules start on a worker thread; the dispatcher belongs to the server thread.
        server.execute(() -> {
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            restoreDisplaced(dispatcher, released);
            registerInto(dispatcher, command);
            resendCommands(server);
        });
    }

    public static void unregister(ProperCommand command) {
        COMMANDS.remove(command.getParent().getIdentifier(), command);
        List<String> released = deactivate(command);

        MinecraftServer server = BasePlugin.getServer();
        if (server != null) server.execute(() -> {
            restoreDisplaced(server.getCommands().getDispatcher(), released);
            resendCommands(server);
        });
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
                .requires(new LabelRequirement(label))
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

    /**
     * Drops the labels {@code command} still owns; labels another command took over stay.
     *
     * @return the labels dropped
     */
    private static List<String> deactivate(ProperCommand command) {
        List<String> released = new ArrayList<>();
        ACTIVE.entrySet().removeIf(entry -> {
            if (entry.getValue() != command) return false;
            released.add(entry.getKey());
            return true;
        });
        return released;
    }

    /**
     * Puts the foreign nodes behind {@code labels} back on the root, except for labels a
     * Streamline command has claimed again since.
     */
    private static void restoreDisplaced(CommandDispatcher<CommandSourceStack> dispatcher, List<String> labels) {
        RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();
        for (String label : labels) {
            if (isActive(label)) continue;
            CommandNode<CommandSourceStack> foreign = DISPLACED.remove(label);
            if (foreign != null) NodeSwap.replace(root, foreign);
        }
    }

    /**
     * Puts a fresh Streamline node in place of a vanilla or other-mod node for {@code label},
     * keeping the foreign node in {@link #DISPLACED}.
     *
     * @return whether the root now holds a Streamline node for {@code label}
     */
    private static boolean displaceForeign(CommandDispatcher<CommandSourceStack> dispatcher, String label) {
        RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();
        CommandNode<CommandSourceStack> existing = root.getChild(label);
        if (existing == null || existing.getRequirement() instanceof LabelRequirement) return false;
        if (! NodeSwap.replace(root, buildBrigadier(label).build())) return false;
        DISPLACED.put(label, existing);
        return true;
    }

    private static void registerInto(CommandDispatcher<CommandSourceStack> dispatcher, ProperCommand command) {
        for (String label : command.getLabels()) {
            try {
                if (! displaceForeign(dispatcher, label)) dispatcher.register(buildBrigadier(label));
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

    /**
     * The requirement on every Streamline node. Its type is what marks a root node as
     * Streamline's own, to be merged into rather than displaced.
     */
    private static final class LabelRequirement implements Predicate<CommandSourceStack> {
        private final String label;

        private LabelRequirement(String label) {
            this.label = label;
        }

        @Override
        public boolean test(CommandSourceStack source) {
            return isActive(label);
        }
    }

    /**
     * Swaps a root child for another of the same name through {@code CommandNode}'s private
     * maps, which Brigadier has no public method for. Root children are literals, held in
     * both {@code children} and {@code literals}; parsing reads {@code literals}, so both
     * change. Overwriting the key keeps the child's place in those insertion-ordered maps,
     * which matters: the server serialises the client's command tree in root order and can
     * only resolve a redirect to a node it has already written, so a node moved behind the
     * nodes redirecting to it (vanilla {@code /msg} behind {@code /w} and {@code /tell}) would
     * reach clients with broken redirects. Brigadier ships without a module descriptor, so its
     * packages are open to reflection on the module-layer loaders too.
     */
    private static final class NodeSwap {
        private static final Field[] MAPS = findMaps();

        private static Field[] findMaps() {
            try {
                Field[] fields = {
                        CommandNode.class.getDeclaredField("children"),
                        CommandNode.class.getDeclaredField("literals"),
                };
                for (Field field : fields) field.setAccessible(true);
                return fields;
            } catch (Exception | LinkageError e) {
                MessageUtils.logWarning("Streamline commands cannot override vanilla or other mods' commands: " + e);
                return null;
            }
        }

        /**
         * Puts {@code node} in the place of the root child sharing its name.
         *
         * @return whether the swap happened
         */
        @SuppressWarnings("unchecked")
        static boolean replace(RootCommandNode<CommandSourceStack> root, CommandNode<CommandSourceStack> node) {
            if (MAPS == null || root.getChild(node.getName()) == null) return false;
            try {
                for (Field field : MAPS) ((Map<String, Object>) field.get(root)).replace(node.getName(), node);
                return true;
            } catch (IllegalAccessException e) {
                return false;
            }
        }
    }
}
