package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.TacoDatabase;
import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.result.CommandResult;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.uuid.UuidManager;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

/**
 * A TacoEssentials command: a name, its permission node {@code tacoessentials.command.<name>},
 * a handler and an optional tab completer. Failures thrown as {@link Msg.Fail} are shown to
 * the sender as errors.
 */
public class TacoCommand extends ModuleCommand {

    public interface Handler {
        void run(Ctx ctx) throws Msg.Fail;
    }

    public interface Completer {
        /**
         * @param ctx  the context
         * @param arg  zero-based index of the argument being completed
         * @return candidates; filtering by what has been typed happens afterwards
         */
        Collection<String> complete(Ctx ctx, int arg);
    }

    private final Handler handler;
    private final Completer completer;

    public TacoCommand(String name, Handler handler, Completer completer) {
        super(TacoEssentials.getInstance(), name, Perms.command(name));
        this.handler = handler;
        this.completer = completer;
    }

    public TacoCommand(String name, Handler handler) {
        this(name, handler, null);
    }

    @Override
    public CommandResult<?> resultedRun(CommandContext<CosmicCommand> context) {
        Ctx ctx = new Ctx(context);
        try {
            handler.run(ctx);
            return success();
        } catch (Msg.Fail fail) {
            ctx.sender().sendMessage(Msg.error(fail.getMessage()));
            return failure();
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> out = new ConcurrentSkipListSet<>();
        if (completer == null) return out;
        String[] raw = context.getArgsArray();
        int index = Math.max(0, raw.length - 1);
        Collection<String> candidates = completer.complete(new Ctx(context), index);
        if (candidates == null) return out;
        String typed = raw.length == 0 ? "" : raw[raw.length - 1];
        out.addAll(MessageUtils.getCompletion(new ArrayList<>(candidates), typed));
        return out;
    }

    /** A command invocation: who ran it and with which arguments. */
    public static final class Ctx {

        private final CosmicSender sender;
        private final List<String> args;

        Ctx(CommandContext<CosmicCommand> context) {
            this.sender = context.getSender();
            // An invocation without arguments arrives as one empty argument.
            this.args = Arrays.stream(context.getArgsArray()).filter(a -> ! a.isEmpty()).collect(Collectors.toList());
        }

        public CosmicSender sender() {
            return sender;
        }

        /** The running player, or a failure when the console ran a player-only command. */
        public CosmicPlayer player() throws Msg.Fail {
            if (sender instanceof CosmicPlayer) return (CosmicPlayer) sender;
            throw Msg.fail("Only players can use this command.");
        }

        public boolean isPlayer() {
            return sender instanceof CosmicPlayer;
        }

        public int count() {
            return args.size();
        }

        /** The argument, or {@code null} when absent. */
        public String arg(int index) {
            return index < args.size() ? args.get(index) : null;
        }

        /** Arguments from {@code index} on, joined by spaces, or {@code null} when absent. */
        public String rest(int index) {
            return index < args.size() ? String.join(" ", args.subList(index, args.size())) : null;
        }

        public String require(int index, String usage) throws Msg.Fail {
            String value = arg(index);
            if (value == null) throw Msg.fail("Usage: " + usage);
            return value;
        }

        public int requireInt(int index, String usage) throws Msg.Fail {
            String value = require(index, usage);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw Msg.fail("'" + value + "' is not a number. Usage: " + usage);
            }
        }

        public boolean has(String permission) {
            return sender.hasPermission(permission);
        }

        public void reply(String message) {
            sender.sendMessage(message);
        }
    }

    // ---- shared helpers ----

    /** An online player by name, case-insensitively. */
    public static CosmicPlayer onlinePlayer(String name) throws Msg.Fail {
        for (CosmicPlayer player : Singularity.getInstance().getPlatform().getOnlinePlayers()) {
            if (player.getCurrentName().equalsIgnoreCase(name)) return player;
        }
        throw Msg.fail("No online player named " + name + ".");
    }

    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        Singularity.getInstance().getPlatform().getOnlinePlayers().forEach(p -> names.add(p.getCurrentName()));
        return names;
    }

    /** A player who has joined at least once, online or not, as (uuid, name). */
    public static String[] knownPlayer(String name) throws Msg.Fail {
        Optional<TacoDatabase.PlayerRecord> record = TacoDatabase.playerByName(name);
        if (record.isPresent()) return new String[] {record.get().getUuid(), record.get().getName()};
        Optional<String> uuid = UuidManager.getUuidFromName(name);
        if (uuid.isPresent()) {
            String known = UserUtils.getPlayer(uuid.get()).map(CosmicPlayer::getCurrentName).orElse(name);
            TacoDatabase.recordPlayer(uuid.get(), known);
            return new String[] {uuid.get(), known};
        }
        throw Msg.fail("No player named " + name + " has joined this server.");
    }

    /** Names a command can offer for a player argument: everyone who has joined. */
    public static List<String> knownNames() {
        List<String> names = TacoDatabase.knownNames();
        for (String online : onlineNames()) if (! names.contains(online)) names.add(online);
        return names;
    }

    /** Lower-cased name restricted to letters, digits, '_' and '-'. */
    public static String normalizeName(String raw, String what) throws Msg.Fail {
        String name = raw.toLowerCase();
        if (! name.matches("[a-z0-9_-]{1,32}")) {
            throw Msg.fail("A " + what + " name may only use letters, digits, '_' and '-' (up to 32).");
        }
        return name;
    }
}
