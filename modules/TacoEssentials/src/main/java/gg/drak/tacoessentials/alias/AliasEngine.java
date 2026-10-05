package gg.drak.tacoessentials.alias;

import gg.drak.tacoessentials.TacoEssentials;
import gg.drak.tacoessentials.commands.Msg;
import singularity.Singularity;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.location.CosmicLocation;
import singularity.modules.ModuleUtils;
import singularity.scheduler.ModuleDelayedRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static gg.drak.tacoessentials.teleport.Teleports.gameplay;

/**
 * Runs an alias's lines. Each line is a command, written without the leading {@code /}, run as
 * whoever ran the alias, unless it starts with a directive:
 * <ul>
 *     <li>{@code delay! <seconds>} -- the lines after it run that much later</li>
 *     <li>{@code asConsole! <command>} (or {@code fromConsole!}) -- the console runs it</li>
 *     <li>{@code asPlayer! <command>} -- whoever ran the alias runs it (the default)</li>
 *     <li>{@code msg! <text>} -- tells whoever ran the alias</li>
 *     <li>{@code broadcast! <text>} -- tells every online player</li>
 *     <li>{@code perm:<node>! <line>} -- the rest of the line only runs if they have the node</li>
 * </ul>
 * Variables are filled in when the alias runs: {@code $1}, {@code $2}... for its arguments,
 * {@code $1-} for the first argument onward, {@code [playerName]}, {@code [nickName]},
 * {@code [currentWorld]}, {@code [currentX]}/{@code Y}/{@code Z}, {@code [currentYaw]},
 * {@code [currentPitch]}, {@code [randomPlayer]}, and every Streamline placeholder.
 */
public final class AliasEngine {

    private static final Pattern ARG = Pattern.compile("\\$(\\d+)(-?)");
    private static final Pattern PERM = Pattern.compile("(?i)^perm:([^!\\s]+)!\\s*");

    /**
     * How deeply aliases may run each other on one thread before the chain stops. Lines run
     * synchronously, so an alias that runs itself would otherwise recurse until the stack overflows.
     */
    private static final int MAX_DEPTH = 8;
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private AliasEngine() {}

    public static void run(String name, CosmicSender sender, String[] args) {
        CustomAlias alias = AliasManager.get(name);
        if (alias == null) {
            sender.sendMessage(Msg.error("That alias no longer exists."));
            return;
        }
        if (alias.isRequiresPerm() && ! sender.hasPermission(alias.getPermission())) {
            sender.sendMessage(Msg.error("You don't have permission to use this command."));
            return;
        }
        if (alias.getCommands().isEmpty()) {
            sender.sendMessage(Msg.error("This alias has no commands configured."));
            return;
        }

        int depth = DEPTH.get();
        if (depth >= MAX_DEPTH) {
            sender.sendMessage(Msg.error("Stopped /" + name + ": aliases ran each other " + MAX_DEPTH + " levels deep."));
            return;
        }
        DEPTH.set(depth + 1);
        try {
            List<String> pending = new ArrayList<>();
            long delayTicks = 0;
            for (String raw : alias.getCommands()) {
                String line = fill(raw.trim(), sender, args);
                if (line.toLowerCase(Locale.ROOT).startsWith("delay!")) {
                    flush(pending, sender, delayTicks);
                    pending = new ArrayList<>();
                    delayTicks += secondsToTicks(line.substring("delay!".length()).trim());
                    continue;
                }
                pending.add(line);
            }
            flush(pending, sender, delayTicks);
        } finally {
            DEPTH.set(depth);
        }
    }

    private static void flush(List<String> lines, CosmicSender sender, long delayTicks) {
        if (lines.isEmpty()) return;
        if (delayTicks <= 0) {
            lines.forEach(line -> runLine(line, sender));
            return;
        }
        new ModuleDelayedRunnable(TacoEssentials.getInstance(), delayTicks) {
            @Override
            public void runDelayed() {
                Singularity.getInstance().getPlatform().runOnMainThread(() -> lines.forEach(line -> runLine(line, sender)));
            }
        };
    }

    private static void runLine(String line, CosmicSender sender) {
        Matcher perm = PERM.matcher(line);
        if (perm.find()) {
            if (! sender.hasPermission(perm.group(1))) return;
            line = line.substring(perm.end());
        }

        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.startsWith("asconsole!")) {
            ModuleUtils.getConsole().runCommand(command(line.substring("asconsole!".length())));
        } else if (lower.startsWith("fromconsole!")) {
            ModuleUtils.getConsole().runCommand(command(line.substring("fromconsole!".length())));
        } else if (lower.startsWith("asplayer!")) {
            sender.runCommand(command(line.substring("asplayer!".length())));
        } else if (lower.startsWith("msg!")) {
            sender.sendMessage(line.substring("msg!".length()).trim());
        } else if (lower.startsWith("broadcast!")) {
            String text = line.substring("broadcast!".length()).trim();
            Singularity.getInstance().getPlatform().getOnlinePlayers().forEach(p -> p.sendMessage(text));
            ModuleUtils.getConsole().sendMessage(text);
        } else if (! line.isEmpty()) {
            sender.runCommand(command(line));
        }
    }

    private static String command(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private static long secondsToTicks(String value) {
        try {
            return Math.max(0, Math.round(Double.parseDouble(value) * 20));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Fills in argument variables, the player variables and Streamline placeholders. */
    static String fill(String line, CosmicSender sender, String[] args) {
        String result = fillArgs(line, args);
        result = fillPlayer(result, sender);
        return ModuleUtils.replacePlaceholders(sender, result);
    }

    private static String fillArgs(String line, String[] args) {
        Matcher matcher = ARG.matcher(line);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1)) - 1;
            String value;
            if (! matcher.group(2).isEmpty()) {
                value = index >= 0 && index < args.length ? String.join(" ", java.util.Arrays.copyOfRange(args, index, args.length)) : "";
            } else {
                value = index >= 0 && index < args.length ? args[index] : "";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String fillPlayer(String line, CosmicSender sender) {
        if (! line.contains("[")) return line;
        String result = line;
        if (sender instanceof CosmicPlayer) {
            CosmicPlayer player = (CosmicPlayer) sender;
            result = result.replace("[playerName]", player.getCurrentName()).replace("[playername]", player.getCurrentName());
            result = result.replace("[nickName]", player.getDisplayName());
            Optional<CosmicLocation> here = gameplay().getLocation(player.getUuid());
            if (here.isPresent()) {
                CosmicLocation loc = here.get();
                result = result.replace("[currentWorld]", loc.getWorldName())
                        .replace("[currentX]", coord(loc.getX()))
                        .replace("[currentY]", coord(loc.getY()))
                        .replace("[currentZ]", coord(loc.getZ()))
                        .replace("[currentYaw]", Float.toString(loc.getYaw()))
                        .replace("[currentPitch]", Float.toString(loc.getPitch()));
            }
        } else {
            result = result.replace("[playerName]", "Console").replace("[playername]", "Console");
        }
        if (result.contains("[randomPlayer]")) result = result.replace("[randomPlayer]", randomPlayer());
        return result;
    }

    private static String randomPlayer() {
        List<CosmicPlayer> online = new ArrayList<>(Singularity.getInstance().getPlatform().getOnlinePlayers());
        if (online.isEmpty()) return "";
        return online.get(ThreadLocalRandom.current().nextInt(online.size())).getCurrentName();
    }

    private static String coord(double value) {
        return value == Math.floor(value) ? Long.toString((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }
}
