package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.List;

public class PointsCommand extends StreamlineCommand {
    private final String messageGet;
    private final String messageSet;
    private final String messageAdd;
    private final String messageRemove;

    public PointsCommand() {
        super(
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
    public void run(StreamlineUser sender, String[] args) {
        if (args.length < 1) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        StreamlineUser other = SLAPI.getInstance().getUserManager().getOrGetUserByName(playerName);

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
        int amount = 0;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (Exception e) {
            e.printStackTrace();
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set" -> {
                other.setPoints(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageSet
                                .replace("%this_value%", args[2])
                        , other));
            }
            case "add" -> {
                other.addPoints(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageAdd
                                .replace("%this_value%", args[2])
                        , other));
            }
            case "remove" -> {
                other.removePoints(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageRemove
                                .replace("%this_value%", args[2])
                        , other));
            }
            default -> {
                SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
            }
        }
    }

    @Override
    public List<String> doTabComplete(StreamlineUser sender, String[] args) {
        if (args.length <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }
        if (args.length == 2) {
            return List.of("set", "add", "remove");
        }

        return new ArrayList<>();
    }
}