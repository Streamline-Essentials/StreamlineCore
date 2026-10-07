package host.plas.collections.progression;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.commands.Commands;
import host.plas.collections.data.TrackedPlayer;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.LongUnaryOperator;

/**
 * {@code /progression (gui|stars get (player)|exp get <category> (player)|skills (player))}, and for
 * {@code streamline.command.progression.admin}:
 * {@code /progression exp <set|add|remove> <amount> <player> <category>}.
 */
public class ProgressionCommand extends ModuleCommand {
    public static final String ADMIN_PERMISSION = "streamline.command.progression.admin";

    public ProgressionCommand() {
        super(StreamlineCollections.getInstance(), "progression", "streamline.command.progression.default", "prog");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        String[] args = Commands.args(context);
        String sub = arg(args, 0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "":
            case "gui": {
                Optional<CosmicPlayer> viewer = Commands.menuViewer(context.getSender());
                viewer.ifPresent(ProgressionMenus::openOwn);
                break;
            }
            case "stars":
                stars(context, args);
                break;
            case "exp":
            case "experience":
                exp(context, args);
                break;
            case "skills":
                target(context, args, 1).ifPresent(target -> withProgress(context, target, progress -> {
                    context.sendMessage(ProgressionManager.message("skills-header").replace("%player%", target.name));
                    for (Skill skill : Skill.values()) {
                        context.sendMessage(ProgressionManager.message("skills-entry")
                                .replace("%skill%", skill.getDisplayName())
                                .replace("%level%", String.valueOf(ProgressionManager.skillLevel(progress, skill)))
                                .replace("%xp%", ProgressionManager.format(ProgressionManager.skillXp(progress, skill))));
                    }
                }));
                break;
            default:
                context.sendMessage(ProgressionManager.message("usage"));
                break;
        }
    }

    private void stars(CommandContext<CosmicCommand> context, String[] args) {
        String action = arg(args, 1).toLowerCase(Locale.ROOT);
        switch (action) {
            case "get":
                target(context, args, 2).ifPresent(target -> withProgress(context, target, progress ->
                        context.sendMessage(ProgressionManager.message("stars-get").replace("%player%", target.name)
                                .replace("%stars%", String.valueOf(ProgressionManager.stars(progress))))));
                break;
            case "set":
            case "add":
            case "remove":
                context.sendMessage(ProgressionManager.message("stars-derived"));
                break;
            default:
                context.sendMessage(ProgressionManager.message("stars-usage"));
                break;
        }
    }

    private void exp(CommandContext<CosmicCommand> context, String[] args) {
        String action = arg(args, 1).toLowerCase(Locale.ROOT);
        if (action.equals("get")) {
            ProgressionStat stat = ProgressionStat.byKey(arg(args, 2));
            if (stat == null) {
                context.sendMessage(args.length < 3 ? ProgressionManager.message("exp-usage")
                        : ProgressionManager.message("unknown-category").replace("%category%", arg(args, 2)));
                return;
            }
            target(context, args, 3).ifPresent(target -> withProgress(context, target, progress ->
                    context.sendMessage(ProgressionManager.message("exp-get").replace("%player%", target.name)
                            .replace("%category%", stat.getDisplayName())
                            .replace("%amount%", ProgressionManager.format(progress.amount(stat.key()))))));
            return;
        }
        if (! action.equals("set") && ! action.equals("add") && ! action.equals("remove")) {
            context.sendMessage(ProgressionManager.message("exp-usage"));
            return;
        }
        if (! ModuleUtils.hasPermission(context.getSender(), ADMIN_PERMISSION)) {
            context.sendMessage(ProgressionManager.message("no-permission"));
            return;
        }
        if (args.length < 5) {
            context.sendMessage(ProgressionManager.message("exp-usage"));
            return;
        }
        long amount;
        try {
            amount = Math.round(Double.parseDouble(args[2]));
        } catch (NumberFormatException exception) {
            context.sendMessage(ProgressionManager.message("invalid-amount").replace("%amount%", args[2]));
            return;
        }
        ProgressionStat stat = ProgressionStat.byKey(args[4]);
        if (stat == null) {
            context.sendMessage(ProgressionManager.message("unknown-category").replace("%category%", args[4]));
            return;
        }
        Optional<String> uuid = Commands.uuidOf(args[3]);
        if (uuid.isEmpty()) {
            context.sendMessage(ProgressionManager.message("unknown-player").replace("%player%", args[3]));
            return;
        }
        LongUnaryOperator change;
        switch (action) {
            case "set":
                change = current -> amount;
                break;
            case "add":
                change = current -> current + amount;
                break;
            default:
                change = current -> current - amount;
                break;
        }
        String name = args[3];
        ProgressionManager.modify(uuid.get(), stat, change).thenAccept(value -> context.sendMessage(
                ProgressionManager.message("exp-changed").replace("%player%", name)
                        .replace("%category%", stat.getDisplayName()).replace("%amount%", ProgressionManager.format(value))));
    }

    private static final class Target {
        private final String uuid;
        private final String name;

        private Target(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    /** The player named at {@code index}, or the sender when there is none there. */
    private Optional<Target> target(CommandContext<CosmicCommand> context, String[] args, int index) {
        if (args.length > index) {
            Optional<String> uuid = Commands.uuidOf(args[index]);
            if (uuid.isEmpty()) {
                context.sendMessage(ProgressionManager.message("unknown-player").replace("%player%", args[index]));
                return Optional.empty();
            }
            return Optional.of(new Target(uuid.get(), args[index]));
        }
        CosmicSender sender = context.getSender();
        if (sender == null || sender.isConsole()) {
            context.sendMessage(ProgressionManager.message("console-player"));
            return Optional.empty();
        }
        return Optional.of(new Target(sender.getUuid(), sender.getCurrentName()));
    }

    private void withProgress(CommandContext<CosmicCommand> context, Target target, Consumer<TrackedPlayer> action) {
        ProgressionManager.snapshot(target.uuid).thenAccept(action).exceptionally(error -> {
            context.sendMessage("&cThat failed; see the console.");
            StreamlineCollections.getInstance().logWarning("/progression failed: " + error.getMessage());
            return null;
        });
    }

    private static String arg(String[] args, int index) {
        return index < args.length ? args[index] : "";
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        String[] args = context.getArgsArray();
        int count = Math.max(1, args.length);
        String first = arg(args, 0).toLowerCase(Locale.ROOT);
        boolean admin = ModuleUtils.hasPermission(context.getSender(), ADMIN_PERMISSION);
        List<String> options = new ArrayList<>();

        if (count == 1) {
            options.addAll(List.of("gui", "stars", "exp", "skills"));
        } else if (first.equals("skills") && count == 2) {
            options.addAll(ModuleUtils.getOnlinePlayerNames());
        } else if (first.equals("stars")) {
            if (count == 2) options.add("get");
            else if (count == 3) options.addAll(ModuleUtils.getOnlinePlayerNames());
        } else if (first.equals("exp") || first.equals("experience")) {
            boolean get = arg(args, 1).equalsIgnoreCase("get");
            if (count == 2) {
                options.add("get");
                if (admin) options.addAll(List.of("set", "add", "remove"));
            } else if (get) {
                if (count == 3) options.addAll(ProgressionManager.categoryKeys());
                else if (count == 4) options.addAll(ModuleUtils.getOnlinePlayerNames());
            } else if (admin) {
                if (count == 3) options.addAll(List.of("1", "10", "100", "1000"));
                else if (count == 4) options.addAll(ModuleUtils.getOnlinePlayerNames());
                else if (count == 5) options.addAll(ProgressionManager.categoryKeys());
            }
        }
        return Commands.complete(options, args);
    }
}
