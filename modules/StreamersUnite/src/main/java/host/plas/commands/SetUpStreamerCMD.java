package host.plas.commands;

import host.plas.StreamersUnite;
import host.plas.data.StreamerSetup;
import host.plas.managers.StreamerUtils;
import singularity.command.ModuleCommand;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandArgument;
import singularity.command.context.CommandContext;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public class SetUpStreamerCMD extends ModuleCommand {
    public SetUpStreamerCMD() {
        super(StreamersUnite.getInstance(),
                "set-up-streamer",
                "streamersunite.command.set-up-streamer",
                "sustreamer");
    }

    @Override
    public void run(CommandContext<CosmicCommand> commandContext) {
        if (commandContext.getArgs().isEmpty()) {
            commandContext.sendMessage("&cUsage: /sustreamer <add|remove|set|list> [args]");
            return;
        }

        String action = commandContext.getStringArg(0);

        switch (action) {
            case "add":
                if (commandContext.getArgs().size() != 3) {
                    commandContext.sendMessage("&cUsage: /sustreamer add <player> <link>");
                    return;
                }

                String aplayer = commandContext.getStringArg(1);
                String alink = commandContext.getStringArg(2);

                Optional<CosmicSender> aoptional = StreamerUtils.getOrGetSenderByName(aplayer);
                if (aoptional.isEmpty()) {
                    commandContext.getSender().sendMessage("&cThat player does not exist!");
                    return;
                }
                CosmicSender asender = aoptional.get();

                StreamerSetup setup = new StreamerSetup(UUID.fromString(asender.getUuid()), new ArrayList<>(), new ArrayList<>(), alink);

                StreamersUnite.getStreamerConfig().saveSetup(setup);

                commandContext.sendMessage("&eAdded &d" + aplayer + " &eto the streamer list.");
                return;
            case "remove":
                if (commandContext.getArgs().size() != 2) {
                    commandContext.sendMessage("&cUsage: /sustreamer remove <player>");
                    return;
                }

                String rplayer = commandContext.getStringArg(1);

                Optional<CosmicSender> roptional = StreamerUtils.getOrGetSenderByName(rplayer);
                if (roptional.isEmpty()) {
                    commandContext.getSender().sendMessage("&cThat player does not exist!");
                    return;
                }
                CosmicSender rsender = roptional.get();

                StreamersUnite.getStreamerConfig().deleteSetup(rsender.getUuid());

                commandContext.sendMessage("&eRemoved &d" + rplayer + " &efrom the streamer list.");
                return;
            case "set-link":
                if (commandContext.getArgs().size() != 3) {
                    commandContext.sendMessage("&cUsage: /sustreamer set-link <player> <link>");
                    return;
                }

                String splayer = commandContext.getStringArg(1);
                String slink = commandContext.getStringArg(2);

                Optional<CosmicSender> soptional = StreamerUtils.getOrGetSenderByName(splayer);
                if (soptional.isEmpty()) {
                    commandContext.getSender().sendMessage("&cThat player does not exist!");
                    return;
                }
                CosmicSender ssender = soptional.get();

                StreamerSetup ssetup = StreamersUnite.getStreamerConfig().getSetup(ssender.getUuid()).orElse(null);
                if (ssetup == null) {
                    commandContext.sendMessage("&cPlayer not found.");
                    return;
                }

                ssetup.setStreamLink(slink);

                StreamersUnite.getStreamerConfig().saveSetup(ssetup);

                commandContext.sendMessage("&eSet the stream link for &d" + splayer + " &eto &d" + slink + "&e.");
                return;
            case "commands":
                if (commandContext.getArgs().size() < 5) {
                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                    return;
                }

                String cplayer = commandContext.getStringArg(1);
                String ccommand = commandContext.getStringArg(2);
                String caction = commandContext.getStringArg(3);

                Optional<CosmicSender> coptional = StreamerUtils.getOrGetSenderByName(cplayer);
                if (coptional.isEmpty()) {
                    commandContext.getSender().sendMessage("&cThat player does not exist!");
                    return;
                }
                CosmicSender csender = coptional.get();

                StreamerSetup csetup = StreamersUnite.getStreamerConfig().getSetup(csender.getUuid()).orElse(null);
                if (csetup == null) {
                    commandContext.sendMessage("&cPlayer not found.");
                    return;
                }

                switch (ccommand) {
                    case "live":
                        switch (caction) {
                            case "add":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                StringBuilder caBuilder = new StringBuilder();

                                for (CommandArgument argument : commandContext.getArgs()) {
                                    if (argument.getIndex() < 4) continue;

                                    caBuilder.append(argument.getContent()).append(" ");
                                }

                                String caCmd = caBuilder.toString().trim();

                                if (caCmd.isBlank() || caCmd.isEmpty()) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                csetup.addCommand(StreamerSetup.CommandType.LIVE, caCmd);

                                commandContext.sendMessage("&eAdded command to the live commands.");
                                break;
                            case "insert":
                                if (commandContext.getArgs().size() < 6) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int ciindex = 0;
                                try {
                                    ciindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                StringBuilder ciBuilder = new StringBuilder();

                                for (CommandArgument argument : commandContext.getArgs()) {
                                    if (argument.getIndex() < 5) continue;

                                    ciBuilder.append(argument.getContent()).append(" ");
                                }

                                String ciCmd = ciBuilder.toString().trim();

                                if (ciCmd.isBlank() || ciCmd.isEmpty()) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                csetup.insertCommand(StreamerSetup.CommandType.LIVE, ciindex, ciCmd);

                                commandContext.sendMessage("&eInserted command into the live commands at index &d" + ciindex + "&e.");
                                break;
                            case "remove":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int crindex = 0;

                                try {
                                    crindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                csetup.removeCommand(StreamerSetup.CommandType.LIVE, crindex);

                                commandContext.sendMessage("&eRemoved command from the live commands at index &d" + crindex + "&e.");
                                break;
                            case "up":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int cuindex = 0;

                                try {
                                    cuindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                csetup.upCommand(StreamerSetup.CommandType.LIVE, cuindex);

                                commandContext.sendMessage("&eMoved command up in the live commands at index &d" + cuindex + " &eto index &d" + (cuindex - 1) + "&e.");
                                break;
                            case "down":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int cdindex = 0;

                                try {
                                    cdindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                csetup.downCommand(StreamerSetup.CommandType.LIVE, cdindex);

                                commandContext.sendMessage("&eMoved command down in the live commands at index &d" + cdindex + " &eto index &d" + (cdindex + 1) + "&e.");
                                break;
                            default:
                                commandContext.sendMessage("&cInvalid action!");
                                break;
                        }
                        break;
                    case "offline":
                        switch (caction) {
                            case "add":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                StringBuilder caBuilder = new StringBuilder();

                                for (CommandArgument argument : commandContext.getArgs()) {
                                    if (argument.getIndex() < 5) continue;

                                    caBuilder.append(argument.getContent()).append(" ");
                                }

                                String caCmd = caBuilder.toString().trim();

                                if (caCmd.isBlank() || caCmd.isEmpty()) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                csetup.addCommand(StreamerSetup.CommandType.OFFLINE, caCmd);

                                commandContext.sendMessage("&eAdded command to the offline commands.");
                                break;
                            case "insert":
                                if (commandContext.getArgs().size() < 6) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int ciindex = 0;

                                try {
                                    ciindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                StringBuilder ciBuilder = new StringBuilder();

                                for (CommandArgument argument : commandContext.getArgs()) {
                                    if (argument.getIndex() < 6) continue;

                                    ciBuilder.append(argument.getContent()).append(" ");
                                }

                                String ciCmd = ciBuilder.toString().trim();

                                if (ciCmd.isBlank() || ciCmd.isEmpty()) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                csetup.insertCommand(StreamerSetup.CommandType.OFFLINE, ciindex, ciCmd);

                                commandContext.sendMessage("&eInserted command into the offline commands at index &d" + ciindex + "&e.");
                                break;
                            case "remove":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int crindex = 0;

                                try {
                                    crindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                csetup.removeCommand(StreamerSetup.CommandType.OFFLINE, crindex);

                                commandContext.sendMessage("&eRemoved command from the offline commands at index &d" + crindex + "&e.");
                                break;
                            case "up":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int cuindex = 0;

                                try {
                                    cuindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                csetup.upCommand(StreamerSetup.CommandType.OFFLINE, cuindex);

                                commandContext.sendMessage("&eMoved command up in the offline commands at index &d" + cuindex + " &eto index &d" + (cuindex - 1) + "&e.");
                                break;
                            case "down":
                                if (commandContext.getArgs().size() < 5) {
                                    commandContext.sendMessage("&cUsage: /sustreamer commands <player> <live|offline> <add|remove|insert|up|down> <cmd|line>");
                                    return;
                                }

                                int cdindex = 0;

                                try {
                                    cdindex = commandContext.getIntArg(4).get();
                                } catch (Exception e) {
                                    commandContext.sendMessage("&cInvalid index.");
                                    return;
                                }

                                csetup.downCommand(StreamerSetup.CommandType.OFFLINE, cdindex);

                                commandContext.sendMessage("&eMoved command down in the offline commands at index &d" + cdindex + " &eto index &d" + (cdindex + 1) + "&e.");
                                break;
                            default:
                                commandContext.sendMessage("&cInvalid action!");
                                break;
                        }
                        break;
                    default:
                        commandContext.sendMessage("&cInvalid type. Should be 'live' or 'offline'.");
                        return;
                }
                return;
            case "list":
                List<StreamerSetup> setups = StreamersUnite.getStreamerConfig().getSetups();

                if (setups.isEmpty()) {
                    commandContext.sendMessage("&cNo streamers found.");
                    return;
                }

                commandContext.sendMessage("&eStreamer List:");
                for (StreamerSetup lsetup : setups) {
                    CosmicSender lplayer = StreamerUtils.getOrGetSender(lsetup.getStreamerUuid().toString()).orElse(null);
                    if (lplayer == null) continue;

                    String playerName = lplayer.getUuid();
                    try {
                        playerName = lplayer.getDisplayName();
                    } catch (Exception e) {
                        // do nothing
                    }

                    commandContext.sendMessage("&e- &d" + lsetup.getStreamerUuid() + " &7(&c" + playerName + "&7)");
                }
                return;
            default:
                commandContext.sendMessage("&cUsage: /sustreamer <add|remove|set|list> [args]");
                return;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext commandContext) {
        ConcurrentSkipListSet<String> tab = new ConcurrentSkipListSet<>();

        if (commandContext.getArgs().size() <= 1) {
            tab.add("add");
            tab.add("remove");
            tab.add("set-link");
            tab.add("commands");
            tab.add("list");
        }
        if (commandContext.getArgs().size() == 2) {
            switch (commandContext.getStringArg(0)) {
                case "add":
                    for (CosmicPlayer player : UserUtils.getOnlinePlayers().values()) {
                        if (player.getCurrentName() != null &&
                                ! player.getCurrentName().isBlank() &&
                                ! player.getCurrentName().isEmpty())
                            tab.add(player.getCurrentName());
                    }
                    break;
                case "remove":
                case "set-link":
                case "commands":
                    for (StreamerSetup setup : StreamersUnite.getStreamerConfig().getSetups()) {
                        CosmicSender player = UserUtils.getOrCreateSender(setup.getStreamerUuid().toString()).orElse(null);
                        if (player == null) continue;

                        if (player.getCurrentName() != null &&
                                    ! player.getCurrentName().isBlank() &&
                                    ! player.getCurrentName().isEmpty())
                                tab.add(player.getCurrentName());
                    }
                    break;
                case "list":
                    break;
            }
        }
        if (commandContext.getArgs().size() == 3) {
            switch (commandContext.getStringArg(0)) {
                case "add":
                case "set-link":
                    tab.add("https://twitch.tv/");
                    tab.add("https://youtube.com/");
                    break;
                case "remove":
                    break;
                case "commands":
                    tab.add("live");
                    tab.add("offline");
                    break;
                case "list":
                    break;
            }
        }
        if (commandContext.getArgs().size() == 4) {
            switch (commandContext.getStringArg(0)) {
                case "add":
                case "remove":
                case "set-link":
                case "list":
                    break;
                case "commands":
                    tab.add("add");
                    tab.add("remove");
                    tab.add("insert");
                    tab.add("up");
                    tab.add("down");
                    break;
            }
        }
        if (commandContext.getArgs().size() == 5) {
            switch (commandContext.getStringArg(0)) {
                case "add":
                case "remove":
                case "set-link":
                case "list":
                    break;
                case "commands":
                    switch (commandContext.getStringArg(3)) {
                        case "add":
                            tab.add("command");
                            break;
                        case "remove":
                        case "insert":
                        case "up":
                        case "down":
                            String player = commandContext.getStringArg(1);

                            Optional<CosmicSender> optional = StreamerUtils.getOrGetSenderByName(player);
                            if (optional.isEmpty()) {
                                break;
                            }
                            CosmicSender s = optional.get();

                            StreamerSetup setup = StreamersUnite.getStreamerConfig().getSetup(s.getUuid()).orElse(null);
                            if (setup == null) {
                                break;
                            }

                            for (int i = 0; i < setup.getGoLiveCommands().size(); i++) {
                                tab.add(String.valueOf(i));
                            }
                            break;
                    }
                    break;
            }
        }
        if (commandContext.getArgs().size() == 6) {
            switch (commandContext.getStringArg(0)) {
                case "add":
                case "remove":
                case "set-link":
                case "list":
                    break;
                case "commands":
                    switch (commandContext.getStringArg(3)) {
                        case "add":
                            tab.add("argument" + (commandContext.getArgs().size() - 1 - 5));
                            break;
                        case "remove":
                        case "up":
                        case "down":
                            break;
                        case "insert":
                            tab.add("command");
                            break;
                    }
                    break;
            }
        }
        if (commandContext.getArgs().size() >= 7) {
            switch (commandContext.getStringArg(0)) {
                case "add":
                case "remove":
                case "set-link":
                case "list":
                    break;
                case "commands":
                    switch (commandContext.getStringArg(3)) {
                        case "add":
                            tab.add("argument" + (commandContext.getArgs().size() - 1 - 5));
                            break;
                        case "remove":
                        case "up":
                        case "down":
                            break;
                        case "insert":
                            tab.add("argument" + (commandContext.getArgs().size() - 1 - 6));
                            break;
                    }
                    break;
            }
        }

        return tab;
    }
}
