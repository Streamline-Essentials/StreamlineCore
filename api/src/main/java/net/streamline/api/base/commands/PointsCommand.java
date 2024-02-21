package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
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
    public void run(StreamSender sender, String[] args) {
        if (args.length < 1) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        StreamPlayer other = UserUtils.getOrGetPlayerByName(playerName).orElse(null);

        if (other == null) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (args.length == 1) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, other,
                    getWithOther(sender, this.messageGet, playerName));
            return;
        }

        String action = args[1];
        double amount = 0;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (Exception e) {
            e.printStackTrace();
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set":
                other.setPoints(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageSet
                                .replace("%this_value%", args[2])
                        , other));
                break;
            case "add":
                other.addPoints(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageAdd
                                .replace("%this_value%", args[2])
                        , other));
                break;
            case "remove":
                other.removePoints(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageRemove
                                .replace("%this_value%", args[2])
                        , other));
                break;
            default:
                SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamSender sender, String[] args) {
        if (args.length <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }
        if (args.length == 2) {
            return new ConcurrentSkipListSet<>(List.of("set", "add", "remove"));
        }
        if (args.length == 3) {
            return getDoubleArgument();
        }

        return new ConcurrentSkipListSet<>();
    }
}
