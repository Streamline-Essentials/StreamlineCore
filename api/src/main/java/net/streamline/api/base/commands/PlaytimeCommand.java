package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.context.CommandContext;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.utils.UserUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class PlaytimeCommand extends StreamlineCommand {
    private final String messageGet;
    private final String messageSet;
    private final String messageAdd;
    private final String messageRemove;

    public PlaytimeCommand() {
        super(
                "streamline-base",
                "proxyplaytime",
                "streamline.command.playtime.default",
                "pplaytime", "pplay", "proxyplay"
        );

        this.messageGet = this.getCommandResource().getOrSetDefault("messages.playtime.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime&8: " +
                        "&r%streamline_user_play_seconds% &dseconds");
        this.messageSet = this.getCommandResource().getOrSetDefault("messages.playtime.set",
                "&eSet &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime &eto &a%this_value% &dseconds&8!");
        this.messageAdd = this.getCommandResource().getOrSetDefault("messages.playtime.add",
                "&eAdded &a%this_value% &dseconds &eto &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime&8!");
        this.messageRemove = this.getCommandResource().getOrSetDefault("messages.playtime.remove",
                "&eRemoved &a%this_value% &dseconds &efrom &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cplaytime&8!");
    }

    @Override
    public void run(CommandContext<StreamlineCommand> context) {
        if (context.getArgCount() < 1) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = context.getStringArg(0);
        StreamPlayer other = UserUtils.getOrCreatePlayerByName(playerName).orElse(null);

        if (other == null) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (context.getArgCount() == 1) {
            other.sendMessage(getWithOther(context.getSender(), this.messageGet, playerName));
            return;
        }

        String action = context.getStringArg(2);
        int amount = 0;
        try {
            amount = Integer.parseInt(context.getStringArg(3));
        } catch (Exception e) {
            e.printStackTrace();
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set":
                other.setPlaySeconds(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageSet, other));
                break;
            case "add":
                other.addPlaySecond(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageAdd, other));
                break;
            case "remove":
                other.removePlaySecond(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageRemove, other));
                break;
            default:
                context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<StreamlineCommand> context) {
        if (context.getArgCount() <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }
        if (context.getArgCount() == 2) {
            return new ConcurrentSkipListSet<>(List.of("set", "add", "remove"));
        }
        if (context.getArgCount() == 3) {
            return getIntegerArgument();
        }

        return new ConcurrentSkipListSet<>();
    }
}
