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

public class PointsCommand extends StreamlineCommand {
    private final String messageGet;
    private final String messageSet;
    private final String messageAdd;
    private final String messageRemove;

    public PointsCommand() {
        super(
                "streamline-base",
                "ppoints",
                "streamline.command.points.default",
                "proxypoints", "ppts"
        );

        this.messageGet = this.getCommandResource().getOrSetDefault("messages.points.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cpoints&8: " +
                        "&r%streamline_user_points%");
        this.messageSet = this.getCommandResource().getOrSetDefault("messages.points.set",
                "&eSet &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cpoints &eto &a%this_value%&8!");
        this.messageAdd = this.getCommandResource().getOrSetDefault("messages.points.add",
                "&eAdded &a%this_value% &eto &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cpoints&8!");
        this.messageRemove = this.getCommandResource().getOrSetDefault("messages.points.remove",
                "&eRemoved &a%this_value% &efrom &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cpoints&8!");
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

        String action = context.getStringArg(1);
        double amount = 0;
        try {
            amount = Double.parseDouble(context.getStringArg(2));
        } catch (Exception e) {
            e.printStackTrace();
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set":
                other.setPoints(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageSet
                                .replace("%this_value%", context.getStringArg(2))
                        , other));
                break;
            case "add":
                other.addPoints(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageAdd
                                .replace("%this_value%", context.getStringArg(2))
                        , other));
                break;
            case "remove":
                other.removePoints(amount);
                context.sendMessage(getWithOther(context.getSender(), this.messageRemove
                                .replace("%this_value%", context.getStringArg(2))
                        , other));
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
            return getDoubleArgument();
        }

        return new ConcurrentSkipListSet<>();
    }
}
