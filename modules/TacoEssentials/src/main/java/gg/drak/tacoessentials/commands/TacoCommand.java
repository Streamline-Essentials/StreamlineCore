package gg.drak.tacoessentials.commands;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.data.Loc;
import gg.drak.tacoessentials.data.TacoDatabase;
import gg.drak.tacoessentials.teleport.Teleports;
import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.result.CommandResult;
import singularity.command.context.CommandContext;
import singularity.configs.CommandResource;
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

    /**
     * The aliases this module has ever put in the command's config file. The file's
     * {@code basic.aliases} wins over the code once it exists, so an alias the module starts
     * offering in a later version is added to it once; one an admin removes stays removed.
     */
    private static final String OFFERED_ALIASES = "tacoessentials.offered-aliases";

    /** Aliases share the command's permission node. */
    public TacoCommand(String name, Handler handler, Completer completer, String... aliases) {
        super(TacoEssentials.getInstance(), name, Perms.command(name), aliases);
        this.handler = handler;
        this.completer = completer;
        offerAliases(aliases);
    }

    private void offerAliases(String[] offered) {
        if (offered.length == 0) return;
        CommandResource resource = getCommandResource();
        List<String> stored = resource.getResource().getStringList(OFFERED_ALIASES);
        List<String> known = stored == null ? new ArrayList<>() : new ArrayList<>(stored);
        List<String> current = new ArrayList<>(Arrays.asList(getAliases() == null ? new String[0] : getAliases()));
        boolean changed = false;
        for (String alias : offered) {
            if (known.contains(alias)) continue;
            known.add(alias);
            changed = true;
            if (! current.contains(alias)) current.add(alias);
        }
        if (! changed) return;
        resource.write(OFFERED_ALIASES, known);
        resource.write("basic.aliases", current);
        setAliases(current.toArray(new String[0]));
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
            if (name.equalsIgnoreCase(player.getCurrentName())) return player;
        }
        throw Msg.fail("No online player named " + name + ".");
    }

    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        Singularity.getInstance().getPlatform().getOnlinePlayers().forEach(p -> {
            if (p.getCurrentName() != null) names.add(p.getCurrentName());
        });
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

    /** A player named on the command line who has joined at least once, and is online when {@link #online} is set. */
    public static final class Target {
        public final String uuid;
        public final String name;
        /** The player, or {@code null} while offline. */
        public final CosmicPlayer online;

        Target(String uuid, String name, CosmicPlayer online) {
            this.uuid = uuid;
            this.name = name;
            this.online = online;
        }

        public boolean isSelf(Ctx ctx) {
            return ctx.isPlayer() && ctx.sender().getUuid().equals(uuid);
        }
    }

    /** The online or known player by name. */
    public static Target target(String name) throws Msg.Fail {
        for (CosmicPlayer player : Singularity.getInstance().getPlatform().getOnlinePlayers()) {
            if (name.equalsIgnoreCase(player.getCurrentName())) return new Target(player.getUuid(), player.getCurrentName(), player);
        }
        String[] known = knownPlayer(name);
        CosmicPlayer online = UserUtils.getPlayer(known[0]).filter(CosmicPlayer::isOnline).orElse(null);
        return new Target(known[0], known[1], online);
    }

    /** The runner as a target, or a failure when the console ran it. */
    public static Target self(Ctx ctx) throws Msg.Fail {
        CosmicPlayer player = ctx.player();
        return new Target(player.getUuid(), player.getCurrentName(), player);
    }

    /**
     * The player named at {@code index}, or the runner when the argument is absent. Naming
     * someone else needs {@code othersPermission}.
     */
    public static Target targetOrSelf(Ctx ctx, int index, String othersPermission, boolean allowOffline) throws Msg.Fail {
        String name = ctx.arg(index);
        if (name == null) return self(ctx);
        if (ctx.isPlayer() && name.equalsIgnoreCase(((CosmicPlayer) ctx.sender()).getCurrentName())) return self(ctx);
        if (othersPermission != null && ! ctx.has(othersPermission)) throw Msg.fail("You may only use this on yourself.");
        Target target = target(name);
        if (target.online == null && ! allowOffline) throw Msg.fail(target.name + " is not online.");
        return target;
    }

    /** Names a command can offer for a player argument: everyone who has joined. */
    public static List<String> knownNames() {
        List<String> names = TacoDatabase.knownNames();
        for (String online : onlineNames()) if (! names.contains(online)) names.add(online);
        return names;
    }

    /** Accepts a world's full name or, for namespaced dimension ids, the part after the colon. */
    public static String resolveWorld(String name) throws Msg.Fail {
        List<String> worlds = Teleports.gameplay().getWorldNames();
        for (String world : worlds) if (world.equalsIgnoreCase(name)) return world;
        for (String world : worlds) if (Loc.shortWorld(world).equalsIgnoreCase(name)) return world;
        throw Msg.fail("No world named " + name + ". Worlds: " + String.join(", ", worlds));
    }

    /** {@code 1d 2h 3m}, {@code 4m 5s} or {@code 6s}. */
    public static String duration(long seconds) {
        long days = seconds / 86400;
        long hours = seconds % 86400 / 3600;
        long minutes = seconds % 3600 / 60;
        long secs = seconds % 60;
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
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
