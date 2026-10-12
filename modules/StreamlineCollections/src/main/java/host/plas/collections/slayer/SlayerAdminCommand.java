package host.plas.collections.slayer;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.commands.Commands;
import host.plas.collections.data.StatReader;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongUnaryOperator;

/**
 * {@code /slayeradmin (sync <all|<player>>|set <player> <mob|all> <kills>|add <player> <mob|all> <kills>
 * |remove <player> <mob|all> <kills>|reset <player> (mob))}. Edits apply live to players loaded on
 * this server and to the database for anyone else; syncing reads statistics, so it only covers
 * players online here.
 */
public class SlayerAdminCommand extends ModuleCommand {
    private static final List<String> EDITS = List.of("set", "add", "remove");

    public SlayerAdminCommand() {
        super(StreamlineCollections.getInstance(), "slayeradmin", "streamline.command.slayeradmin.default");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        String[] args = Commands.args(context);
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        if (action.equals("sync") && args.length >= 2) {
            sync(context, args[1]);
        } else if (action.equals("reset") && args.length >= 2) {
            reset(context, args);
        } else if (EDITS.contains(action) && args.length >= 4) {
            edit(context, args, action);
        } else {
            context.sendMessage(SlayerManager.message("admin-usage"));
        }
    }

    private void edit(CommandContext<CosmicCommand> context, String[] args, String action) {
        Optional<String> target = Commands.uuidOf(args[1]);
        if (target.isEmpty()) {
            context.sendMessage(SlayerManager.message("unknown-player").replace("%player%", args[1]));
            return;
        }
        Collection<SlayerDefinition> mobs = mobs(context, args[2]);
        if (mobs == null) return;

        long value;
        try {
            value = Long.parseLong(args[3]);
        } catch (NumberFormatException exception) {
            value = -1L;
        }
        if (value < 0) {
            context.sendMessage(SlayerManager.message("admin-invalid-amount").replace("%amount%", args[3]));
            return;
        }

        long amount = value;
        LongUnaryOperator change;
        switch (action) {
            case "set":
                change = kills -> amount;
                break;
            case "add":
                change = kills -> kills + amount;
                break;
            default:
                change = kills -> kills - amount;
                break;
        }
        String mob = mobs.size() == 1 ? mobs.iterator().next().getDisplayName() : "every mob";
        SlayerManager.modify(target.get(), mobs, change).whenComplete((done, error) -> report(context, error,
                SlayerManager.message("admin-" + action).replace("%player%", args[1]).replace("%mob%", mob)
                        .replace("%amount%", String.valueOf(amount))));
    }

    private void reset(CommandContext<CosmicCommand> context, String[] args) {
        Optional<String> target = Commands.uuidOf(args[1]);
        if (target.isEmpty()) {
            context.sendMessage(SlayerManager.message("unknown-player").replace("%player%", args[1]));
            return;
        }
        SlayerDefinition mob = null;
        if (args.length >= 3 && ! args[2].equalsIgnoreCase("all")) {
            mob = SlayerCatalog.get(args[2]);
            if (mob == null) {
                context.sendMessage(SlayerManager.message("unknown").replace("%mob%", args[2]));
                return;
            }
        }
        String label = mob == null ? "every mob" : mob.getDisplayName();
        SlayerManager.reset(target.get(), mob).whenComplete((done, error) -> report(context, error,
                SlayerManager.message("admin-reset").replace("%player%", args[1]).replace("%mob%", label)));
    }

    /** Raises one online player's kills, or everyone online here, to their vanilla statistics. */
    private void sync(CommandContext<CosmicCommand> context, String who) {
        List<CosmicPlayer> players = new ArrayList<>();
        if (who.equalsIgnoreCase("all")) {
            players.addAll(UserUtils.getOnlinePlayers().values());
        } else {
            Optional<CosmicPlayer> player = Commands.uuidOf(who).flatMap(UserUtils::getOrGetPlayer);
            if (player.isEmpty() || ! player.get().isOnline() || SlayerManager.getLoaded(player.get().getUuid()).isEmpty()) {
                context.sendMessage(SlayerManager.message("not-online").replace("%player%", who));
                return;
            }
            players.add(player.get());
        }

        AtomicInteger synced = new AtomicInteger();
        StatReader.submit("sync slayer kills", () -> {
            for (CosmicPlayer player : players) {
                if (SlayerManager.sync(player) >= 0) synced.incrementAndGet();
            }
        }).thenRun(() -> context.sendMessage(SlayerManager.message("sync-done").replace("%count%", String.valueOf(synced.get()))));
    }

    /** One mob by id, or every mob for {@code all}; null (after telling the sender) when unknown. */
    private Collection<SlayerDefinition> mobs(CommandContext<CosmicCommand> context, String raw) {
        if (raw.equalsIgnoreCase("all")) return SlayerCatalog.all();
        SlayerDefinition definition = SlayerCatalog.get(raw);
        if (definition == null) {
            context.sendMessage(SlayerManager.message("unknown").replace("%mob%", raw));
            return null;
        }
        return List.of(definition);
    }

    private void report(CommandContext<CosmicCommand> context, Throwable error, String success) {
        if (error != null) {
            StreamlineCollections.getInstance().logWarning("/slayeradmin failed: " + error.getMessage());
            context.sendMessage("&cThat failed; see the console.");
        } else {
            context.sendMessage(success);
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.addAll(List.of("sync", "set", "add", "remove", "reset"));
        } else if (args.length == 2) {
            if (action.equals("sync")) options.add("all");
            options.addAll(ModuleUtils.getOnlinePlayerNames());
        } else if (args.length == 3 && (EDITS.contains(action) || action.equals("reset"))) {
            options.add("all");
            options.addAll(SlayerCatalog.ids());
        } else if (args.length == 4 && EDITS.contains(action)) {
            options.addAll(List.of("1", "10", "100", "1000"));
        }
        return Commands.complete(options, args);
    }
}
