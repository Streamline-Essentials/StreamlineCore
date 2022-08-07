package net.streamline.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;

import java.util.ArrayList;
import java.util.List;

public class PXPCommand extends StreamlineCommand {
    private final String messageLevelGet;
    private final String messageLevelSet;
    private final String messageLevelAdd;
    private final String messageLevelRemove;
    private final String messageXPGet;
    private final String messageXPSet;
    private final String messageXPAdd;
    private final String messageXPRemove;

    public PXPCommand() {
        super(
                "proxyexperience",
                "streamline.command.proxyexperience.default",
                "pexp", "proxyxp", "pxp", "px"
        );

        this.messageLevelGet = this.getCommandResource().getOrSetDefault("messages.level.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");
        this.messageLevelSet = this.getCommandResource().getOrSetDefault("messages.level.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");
        this.messageLevelAdd = this.getCommandResource().getOrSetDefault("messages.level.add",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");
        this.messageLevelRemove = this.getCommandResource().getOrSetDefault("messages.level.remove",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");

        this.messageXPGet = this.getCommandResource().getOrSetDefault("messages.xp.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
        this.messageXPSet = this.getCommandResource().getOrSetDefault("messages.xp.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
        this.messageXPAdd = this.getCommandResource().getOrSetDefault("messages.xp.add",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
        this.messageXPRemove = this.getCommandResource().getOrSetDefault("messages.xp.remove",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
    }

    @Override
    public void run(StreamlineUser sender, String[] args) {
        if (args.length < 2) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        StreamlinePlayer player = SLAPI.getInstance().getPlatform().getSavedPlayer(playerName);

        if (player == null) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_SELF.get());
            return;
        }

        String type = args[1].toLowerCase();

        switch (type) {
            case "level" -> {
                if (args.length == 2) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, SLAPI.getInstance().getPlatform().getUUIDFromName(playerName),
                            getWithOther(sender, this.messageLevelGet, playerName));
                    return;
                }

                if (args.length < 4) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
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

                StreamlinePlayer StreamlinePlayer = SLAPI.getInstance().getUserManager().getOrGetPlayer(SLAPI.getInstance().getPlatform().getUUIDFromName(playerName));
                if (StreamlinePlayer == null) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                switch (action) {
                    case "set" -> {
                        StreamlinePlayer.setLevel(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageLevelSet, playerName));
                    }
                    case "add" -> {
                        StreamlinePlayer.addLevel(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageLevelAdd, playerName));
                    }
                    case "remove" -> {
                        StreamlinePlayer.removeLevel(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageLevelRemove, playerName));
                    }
                    default -> {
                        SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                    }
                }
            }
            case "xp" -> {
                if (args.length == 2) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, SLAPI.getInstance().getPlatform().getUUIDFromName(playerName),
                            getWithOther(sender, this.messageXPGet, playerName));
                    return;
                }

                if (args.length < 4) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
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

                StreamlinePlayer StreamlinePlayer = SLAPI.getInstance().getUserManager().getOrGetPlayer(SLAPI.getInstance().getPlatform().getUUIDFromName(playerName));
                if (StreamlinePlayer == null) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                switch (action) {
                    case "set" -> {
                        StreamlinePlayer.setTotalXP(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageXPSet, playerName));
                    }
                    case "add" -> {
                        StreamlinePlayer.addTotalXP(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageXPAdd, playerName));
                    }
                    case "remove" -> {
                        StreamlinePlayer.removeTotalXP(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageXPRemove, playerName));
                    }
                    default -> {
                        SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                    }
                }
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
            return List.of("level", "xp");
        }
        if (args.length == 3) {
            return List.of("set", "add", "remove");
        }
        if (args.length == 4) {
            return List.of("-3", "-2", "-1", "0", "1", "2", "3");
        }

        return new ArrayList<>();
    }
}
