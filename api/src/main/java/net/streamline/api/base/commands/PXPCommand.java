package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.utils.UserUtils;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

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
                "streamline-base",
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
        StreamlinePlayer player = UserUtils.getOrGetPlayerByName(playerName);

        if (player == null) {
            SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        String type = args[1].toLowerCase();

        switch (type) {
            case "level":
                if (args.length == 2) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, player,
                            getWithOther(sender, this.messageLevelGet, player));
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

                switch (action) {
                    case "set":
                        player.setLevel(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageLevelSet, player));
                        break;
                    case "add":
                        player.addLevel(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageLevelAdd, player));
                        break;
                    case "remove":
                        player.removeLevel(amount);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageLevelRemove, player));
                        break;
                    default:
                        SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                        break;
                }
                break;
            case "xp":
                if (args.length == 2) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, player,
                            getWithOther(sender, this.messageXPGet, player));
                    return;
                }

                if (args.length < 4) {
                    SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String action2 = args[2];
                double amount2 = 0;
                try {
                    amount2 = Double.parseDouble(args[3]);
                } catch (Exception e) {
                    e.printStackTrace();
                    SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
                    return;
                }

                switch (action2) {
                    case "set":
                        player.setTotalXP(amount2);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageXPSet, player));
                        break;
                    case "add":
                        player.addTotalXP(amount2);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageXPAdd, player));
                        break;
                    case "remove":
                        player.removeTotalXP(amount2);
                        SLAPI.getInstance().getMessenger().sendMessage(sender, getWithOther(sender, this.messageXPRemove, player));
                        break;
                    default:
                        SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                        break;
                }
            default:
                SLAPI.getInstance().getMessenger().sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(StreamlineUser sender, String[] args) {
        if (args.length <= 1) {
            return SLAPI.getInstance().getPlatform().getOnlinePlayerNames();
        }
        if (args.length == 2) {
            return new ConcurrentSkipListSet<>(List.of("level", "xp"));
        }
        if (args.length == 3) {
            return new ConcurrentSkipListSet<>(List.of("set", "add", "remove"));
        }
        if (args.length == 4) {
            if (args[1].equals("xp")) return getDoubleArgument();
            return getIntegerArgument();
        }

        return new ConcurrentSkipListSet<>();
    }
}
