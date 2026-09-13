package host.plas.command;

import host.plas.StreamlineDiscord;
import host.plas.discord.DiscordHandler;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.EndPointType;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import singularity.command.CosmicCommand;
import singularity.command.ModuleCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;

public class CreateChannelCommandMC extends ModuleCommand {
    String messageReplySet;
    String messageReplyRemove;
    String messageReplyInfo;
    String messageReplyNone;

    public CreateChannelCommandMC() {
        super(StreamlineDiscord.getInstance(),
                "creatediscordchannel",
                "streamline.command.create-channel.default",
                "cdiscordchannel", "cdiscordc", "cdiscord", "cdc", "create-channel", "createchannel", "createc"
        );

        messageReplySet = getCommandResource().getOrSetDefault("messages.reply.set",
                "&eLinked &b%this_channel_id% &eto &b%this_type% &ewith an identifier of &b%this_identifier% &eand a format of " +
                        "&b%this_format%%newline%&7More information and help at &bhttps://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup#setting-and-removing-channel-routes");
        messageReplyRemove = getCommandResource().getOrSetDefault("messages.reply.remove",
                "&eRemoved &b%this_channel_id% &efrom &b%this_type% &ewith an identifier of &b%this_identifier% &eand a format of " +
                        "&b%this_format%%newline%&7More information and help at &bhttps://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup#setting-and-removing-channel-routes");
        messageReplyInfo = getCommandResource().getOrSetDefault("messages.reply.info",
                "&eThis channel is linked to &b%this_type% &ewith an identifier of &b%this_identifier% &eand a format of " +
                        "&b%this_format%%newline%&7More information and help at &bhttps://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup#setting-and-removing-channel-routes");
        messageReplyNone = getCommandResource().getOrSetDefault("messages.reply.none",
                "&eWe could not find any route like that...%newline%&7More information and help at &bhttps://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup#setting-and-removing-channel-routes");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        CosmicSender streamlineUser = context.getSender();

        String action = context.getStringArg(0).toLowerCase();
        switch (action) {
            case "set":
                if (context.getArgCount() < 6) {
                    messageInfo(streamlineUser);
                    return;
                }

                EndPointType typeIn;

                try {
                    typeIn = EndPointType.valueOf(context.getStringArg(2).toUpperCase());
                } catch (Exception e) {
                    messageInfo(streamlineUser);
                    return;
                }

                EndPointType typeOut;

                try {
                    typeOut = EndPointType.valueOf(context.getStringArg(4).toUpperCase());
                } catch (Exception e) {
                    messageInfo(streamlineUser);
                    return;
                }

                String outputFormat = typeIn.equals(EndPointType.DISCORD_TEXT) ? StreamlineDiscord.getConfig().getDefaultFormatFromDiscord()
                        : StreamlineDiscord.getConfig().getDefaultFormatFromMinecraft();
                String inputFormat = typeOut.equals(EndPointType.DISCORD_TEXT) ? StreamlineDiscord.getConfig().getDefaultFormatFromDiscord()
                        : StreamlineDiscord.getConfig().getDefaultFormatFromMinecraft();

                EndPoint input = new EndPoint();
                input.setType(EndPointType.DISCORD_TEXT);
                input.setIdentifier(context.getStringArg(1));
                input.setToFormat(outputFormat);

                EndPoint output = new EndPoint();
                output.setType(typeIn);
                output.setIdentifier(context.getStringArg(3));
                output.setToFormat(inputFormat);

                Route toInput = new Route();
                toInput.setInput(input);
                toInput.setOutput(output);

                Route toOutput = new Route();
                toOutput.setInput(output);
                toOutput.setOutput(input);

                toInput.save();
                toOutput.save();

                messageSet(streamlineUser, toOutput);
                break;
            case "remove":
                if (context.getArgCount() == 1) {
                    DiscordHandler.getAllCurrentRoutes(streamlineUser).forEach(Route::drop);
                    return;
                }

                if (context.getArgCount() < 6) {
                    messageInfo(streamlineUser);
                    return;
                }

                EndPointType typeInRemove;

                try {
                    typeInRemove = EndPointType.valueOf(context.getStringArg(2).toUpperCase());
                } catch (Exception e) {
                    messageInfo(streamlineUser);
                    return;
                }

                ConcurrentSkipListSet<Route> routes = new ConcurrentSkipListSet<>();
                routes.addAll(RouteLoader.getBackAndForthRoute(typeInRemove, context.getStringArg(1),
                        EndPointType.DISCORD_TEXT, context.getStringArg(3)));

                Optional<Route> thing = routes.stream().findFirst();

                if (thing.isEmpty()) {
                    ModuleUtils.sendMessage(streamlineUser, ModuleUtils.replaceAllPlayerBungee(streamlineUser, messageReplyNone));
                    return;
                }

//                EndPoint point = thing.get().getOutput();

                routes.forEach(route -> {
                    route.drop();
                    messageRemove(streamlineUser, thing.get());
                });

                break;
            default:
                messageInfo(streamlineUser);
                break;
        }
    }

    public void messageInfo(CosmicSender sender) {
        if (! (sender instanceof CosmicPlayer)) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
            return;
        }
        CosmicPlayer player = (CosmicPlayer) sender;

        DiscordHandler.getAllCurrentRoutes(player).forEach(route -> {
            ModuleUtils.sendMessage(sender, getWithOther(messageReplyInfo, player, route));
        });
    }

    public void messageSet(CosmicSender sender, Route route) {
        if (! (sender instanceof CosmicPlayer)) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
            return;
        }
        CosmicPlayer player = (CosmicPlayer) sender;

        ModuleUtils.sendMessage(sender, getWithOther(messageReplySet, player, route));
    }

    public void messageRemove(CosmicSender sender, Route route) {
        if (! (sender instanceof CosmicPlayer)) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.PLAYER_SELF.get());
            return;
        }
        CosmicPlayer player = (CosmicPlayer) sender;

        ModuleUtils.sendMessage(sender, getWithOther(messageReplyRemove, player, route));
    }

    public String getWithOther(String message, CosmicSender user, Route route) {
        return getWithOther(user, message
                .replace("%this_input_type%", route.getInput().getType().toString())
                .replace("%this_input_channel_id%", route.getInput().getType().toString())
                .replace("%this_input_identifier%", route.getInput().getEndPointIdentifier())
                .replace("%this_output_type%", route.getOutput().getType().toString())
                .replace("%this_output_channel_id%", route.getOutput().getType().toString())
                .replace("%this_output_identifier%", route.getOutput().getEndPointIdentifier()), user)
                .replace("%this_input_format%", route.getInput().getToFormat())
                .replace("%this_output_format%", route.getOutput().getToFormat());
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() <= 1) {
            return new ConcurrentSkipListSet<>(Arrays.asList("info", "set", "remove"));
        }
        if (context.getArgCount() == 2) {
            return new ConcurrentSkipListSet<>(List.of("identifier"));
        }
        if (context.getArgCount() == 3) {
            return DiscordHandler.allEndPointTypesAsStrings();
        }
        if (context.getArgCount() == 4) {
            return new ConcurrentSkipListSet<>(List.of("identifier"));
        }
        if (context.getArgCount() == 5) {
            return DiscordHandler.allEndPointTypesAsStrings();
        }

        return new ConcurrentSkipListSet<>();
    }
}
