package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PTagCommand extends StreamlineCommand {
    private final String messageTagsGet;
    private final String messageTagsAdd;
    private final String messageTagsRemove;

    public PTagCommand() {
        super(
                "ptag",
                "streamline.command.tag.default",
                "proxytag"
        );

        this.messageTagsGet = this.getCommandResource().getOrSetDefault("messages.tags.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &ctags&8: " +
                        "&r%streamline_user_tags%");
        this.messageTagsAdd = this.getCommandResource().getOrSetDefault("messages.tags.add",
                "&eAdded &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &ctag&8: " +
                        "&a%this_value%");
        this.messageTagsRemove = this.getCommandResource().getOrSetDefault("messages.tags.remove",
                "&eRemoved &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &ctag&8: " +
                        "&a%this_value%");
    }

    @Override
    public void run(StreamlineUser sender, String[] args) {
        if (args.length < 2) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        StreamlineUser other = SLAPI.getInstance().getUserManager().getOrGetUser(playerName);

        if (args.length == 2) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, other,
                    getWithOther(sender, this.messageTagsGet, playerName));
            return;
        }

        if (args.length < 4) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String action = args[1];
        String[] actions = SLAPI.getInstance().getMessenger().argsToStringMinus(args, 0, 1).split(" ");

        switch (action) {
            case "add" -> {
                Arrays.stream(actions).forEach(other::addTag);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageTagsAdd, other));
            }
            case "remove" -> {
                Arrays.stream(actions).forEach(other::removeTag);
                SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageTagsRemove, other));
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
            return List.of("add", "remove");
        }

        return new ArrayList<>();
    }
}
