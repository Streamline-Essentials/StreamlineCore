package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

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
    public void run(StreamlineUser sender, String[] args) {
        if (args.length < 1) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        StreamlinePlayer other = UserUtils.getOrGetPlayerByName(playerName);

        if (other == null) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (args.length == 1) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, other,
                    getWithOther(sender, this.messageGet, playerName));
            return;
        }

        String action = args[2];
        int amount = 0;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (Exception e) {
            e.printStackTrace();
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
            return;
        }

        switch (action) {
            case "set" -> {
                other.setPlaySeconds(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageSet, other));
            }
            case "add" -> {
                other.addPlaySecond(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageAdd, other));
            }
            case "remove" -> {
                other.removePlaySecond(amount);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageRemove, other));
            }
            default -> {
                SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
            }
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamlineUser sender, String[] args) {
        if (args.length <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }
        if (args.length == 2) {
            return new ConcurrentSkipListSet<>(List.of("set", "add", "remove"));
        }
        if (args.length == 3) {
            return getIntegerArgument();
        }

        return new ConcurrentSkipListSet<>();
    }
}
