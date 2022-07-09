package net.streamline.base.commands;

import net.streamline.api.BasePlugin;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;
import net.streamline.utils.UUIDUtils;

import java.util.ArrayList;
import java.util.Arrays;
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
    public void run(SavableUser sender, String[] args) {
        if (args.length < 2) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        SavableUser other = UserManager.getOrGetUser(UUIDUtils.swapToUUID(playerName));

        if (args.length == 2) {
            MessagingUtils.sendMessage(sender, BasePlugin.getUUIDFromName(playerName),
                    getWithOther(sender, this.messageGet, playerName));
            return;
        }

        if (args.length < 4) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String action = args[1];
        int amount = 0;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (Exception e) {
            e.printStackTrace();
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set" -> {
                other.setPoints(amount);
                MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageAdd, playerName));
            }
            case "add" -> {
                other.addPoints(amount);
                MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageAdd, playerName));
            }
            case "remove" -> {
                other.removePoints(amount);
                MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageRemove, playerName));
            }
            default -> {
                MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
            }
        }
    }

    @Override
    public List<String> doTabComplete(SavableUser sender, String[] args) {
        if (args.length <= 1) {
            return BasePlugin.getOnlinePlayerNames();
        }
        if (args.length == 2) {
            return List.of("set", "add", "remove");
        }

        return new ArrayList<>();
    }
}
