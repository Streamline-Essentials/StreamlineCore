package net.streamline.base.commands;

import net.streamline.api.BasePlugin;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

public class PlaytimeCommand extends StreamlineCommand {
    private final String messageGet;
    private final String messageSet;
    private final String messageAdd;
    private final String messageRemove;

    public PlaytimeCommand() {
        super(
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
    public void run(SavableUser sender, String[] args) {
        if (args.length < 2) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        SavablePlayer other = BasePlugin.getSavedPlayer(playerName);

        if (other == null) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        String type = args[1].toLowerCase();

        if (args.length == 2) {
            MessagingUtils.sendMessage(sender, BasePlugin.getUUIDFromName(playerName),
                    getWithOther(sender, this.messageGet, playerName));
            return;
        }

        if (args.length < 4) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String action = args[2];
        int amount = 0;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (Exception e) {
            e.printStackTrace();
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set" -> {
                other.setPlaySeconds(amount);
                MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageAdd, playerName));
            }
            case "add" -> {
                other.addPlaySecond(amount);
                MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageAdd, playerName));
            }
            case "remove" -> {
                other.removePlaySecond(amount);
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
