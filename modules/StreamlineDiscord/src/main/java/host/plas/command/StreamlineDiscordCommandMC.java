package host.plas.command;

import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordCommand;
import host.plas.discord.DiscordHandler;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.modules.ModuleUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@code /streamlinediscord admin (slashcommands (reregister|enable <slashcommand>|disable <slashcommand>)|commands (enable <command>|disable <command>))}
 *
 * <p>Manages the bot's Discord commands from Minecraft. Changes are saved to each
 * command's {@code discord-commands/<id>/<id>.yml} ({@code enabled}, {@code slash-enabled})
 * and applied to the running bot; Discord's slash command list is synced off the
 * calling thread and the result is reported back to the sender.</p>
 */
public class StreamlineDiscordCommandMC extends ModuleCommand {
    public static final String USAGE = "&eUsage&8: &b/streamlinediscord admin (slashcommands (reregister|enable <slashcommand>|disable <slashcommand>)|commands (enable <command>|disable <command>))";

    public StreamlineDiscordCommandMC() {
        super(StreamlineDiscord.getInstance(),
                "streamlinediscord",
                "streamline.command.streamlinediscord.admin",
                "sldiscord"
        );
    }

    private static String arg(CommandContext<CosmicCommand> context, int index) {
        if (context.getArgCount() <= index) return "";
        String value = context.getStringArg(index);
        return value == null ? "" : value.toLowerCase();
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        CosmicSender sender = context.getSender();

        if (! arg(context, 0).equals("admin")) {
            ModuleUtils.sendMessage(sender, USAGE);
            return;
        }

        String group = arg(context, 1);
        String action = arg(context, 2);
        String target = arg(context, 3);

        if (group.equals("slashcommands")) {
            switch (action) {
                case "reregister":
                    reregister(sender);
                    return;
                case "enable":
                case "disable":
                    if (target.isEmpty()) break;
                    setSlashEnabled(sender, target, action.equals("enable"));
                    return;
                default:
                    break;
            }
        } else if (group.equals("commands")) {
            if ((action.equals("enable") || action.equals("disable")) && ! target.isEmpty()) {
                setEnabled(sender, target, action.equals("enable"));
                return;
            }
        }

        ModuleUtils.sendMessage(sender, USAGE);
    }

    private static boolean slashCommandsOn() {
        return StreamlineDiscord.getConfig().getBotLayout().isSlashCommandsEnabled();
    }

    private static boolean checkConnected(CosmicSender sender) {
        if (DiscordHandler.getDiscordAPI() != null && ! DiscordHandler.getKnownCommands().isEmpty()) return true;
        ModuleUtils.sendMessage(sender, "&cThe Discord bot is not connected, so its commands cannot be changed right now.");
        return false;
    }

    private static DiscordCommand findCommand(CosmicSender sender, String identifier) {
        DiscordCommand command = DiscordHandler.getKnownCommand(identifier);
        if (command == null) {
            ModuleUtils.sendMessage(sender, "&cThere is no Discord command called &b" + identifier + "&c. Known commands&8: &7"
                    + String.join("&8, &7", DiscordHandler.getKnownCommands().keySet()));
        }
        return command;
    }

    private static void syncAndReport(CosmicSender sender, boolean force) {
        DiscordHandler.syncSlashCommandsAsync(force).thenAccept(result ->
                ModuleUtils.sendMessage(sender, (result.success ? "&a" : "&c") + result.message));
    }

    private void reregister(CosmicSender sender) {
        if (! checkConnected(sender)) return;
        if (! slashCommandsOn()) {
            ModuleUtils.sendMessage(sender, "&cSlash commands are turned off (&bbot.slash-commands&c in config.yml).");
            return;
        }

        ModuleUtils.sendMessage(sender, "&eRe-registering every slash command with Discord...");
        syncAndReport(sender, true);
    }

    private void setSlashEnabled(CosmicSender sender, String identifier, boolean enable) {
        if (! checkConnected(sender)) return;
        DiscordCommand command = findCommand(sender, identifier);
        if (command == null) return;

        String id = command.getCommandIdentifier();
        if (command.isSlashEnabled() == enable) {
            ModuleUtils.sendMessage(sender, "&eThe slash command &b/" + id + " &eis already " + (enable ? "&aenabled" : "&cdisabled") + "&e.");
            return;
        }

        command.setSlashEnabled(enable);
        command.saveCommand();

        if (! command.isEnabled()) {
            ModuleUtils.sendMessage(sender, "&eSaved &b/" + id + " &eas a slash command " + (enable ? "&aenabled" : "&cdisabled")
                    + "&e, but the command itself is disabled. Turn it on with &b/streamlinediscord admin commands enable " + id + "&e.");
            return;
        }
        if (! slashCommandsOn()) {
            ModuleUtils.sendMessage(sender, "&eSaved &b/" + id + " &eas a slash command " + (enable ? "&aenabled" : "&cdisabled")
                    + "&e, but slash commands are turned off (&bbot.slash-commands&e in config.yml).");
            return;
        }

        if (enable) DiscordHandler.registerSlashCommand(command);
        else DiscordHandler.unregisterSlashCommand(command);

        ModuleUtils.sendMessage(sender, "&eSlash command &b/" + id + " &eis now " + (enable ? "&aenabled" : "&cdisabled") + "&e. Syncing with Discord...");
        syncAndReport(sender, false);
    }

    private void setEnabled(CosmicSender sender, String identifier, boolean enable) {
        if (! checkConnected(sender)) return;
        DiscordCommand command = findCommand(sender, identifier);
        if (command == null) return;

        String id = command.getCommandIdentifier();
        if (command.isEnabled() == enable) {
            ModuleUtils.sendMessage(sender, "&eThe Discord command &b" + id + " &eis already " + (enable ? "&aenabled" : "&cdisabled") + "&e.");
            return;
        }

        command.setEnabled(enable);
        command.saveCommand();

        if (enable) command.register();
        else command.unregister();

        boolean touchesSlash = slashCommandsOn() && command.isSlashEnabled();
        ModuleUtils.sendMessage(sender, "&eDiscord command &b" + id + " &eis now " + (enable ? "&aenabled" : "&cdisabled") + "&e."
                + (touchesSlash ? " Syncing slash commands with Discord..." : ""));
        if (touchesSlash) syncAndReport(sender, false);
    }

    private static ConcurrentSkipListSet<String> commandIds(Predicate<DiscordCommand> filter) {
        return DiscordHandler.getKnownCommands().values().stream()
                .filter(filter)
                .map(DiscordCommand::getCommandIdentifier)
                .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        int count = context.getArgCount();
        String last = count == 0 ? "" : arg(context, count - 1);

        if (count <= 1) return ModuleUtils.getCompletion(List.of("admin"), last);
        if (! arg(context, 0).equals("admin")) return new ConcurrentSkipListSet<>();
        if (count == 2) return ModuleUtils.getCompletion(List.of("slashcommands", "commands"), last);

        String group = arg(context, 1);
        if (count == 3) {
            if (group.equals("slashcommands")) return ModuleUtils.getCompletion(List.of("reregister", "enable", "disable"), last);
            if (group.equals("commands")) return ModuleUtils.getCompletion(List.of("enable", "disable"), last);
            return new ConcurrentSkipListSet<>();
        }

        if (count == 4) {
            String action = arg(context, 2);
            boolean enable = action.equals("enable");
            if (! enable && ! action.equals("disable")) return new ConcurrentSkipListSet<>();

            if (group.equals("slashcommands")) return ModuleUtils.getCompletion(commandIds(c -> c.isSlashEnabled() != enable), last);
            if (group.equals("commands")) return ModuleUtils.getCompletion(commandIds(c -> c.isEnabled() != enable), last);
        }

        return new ConcurrentSkipListSet<>();
    }
}
