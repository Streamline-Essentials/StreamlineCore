package net.streamline.base.commands;

import net.streamline.api.command.ICommandSender;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;

import java.util.ArrayList;
import java.util.List;

public class PXPCommand extends StreamlineCommand {
    private String messageLevelGet;
    private String messageLevelSet;
    private String messageLevelAdd;
    private String messageLevelRemove;
    private String messageXPGet;
    private String messageXPSet;
    private String messageXPAdd;
    private String messageXPRemove;

    public PXPCommand() {
        super(
                "proxyexperience",
                "A command to update players' levels / experience for the proxy!",
                "/proxyexperience <player> level [add|remove|set] [number]",
                "streamline.command.proxyexperience.default",
                "pexp", "proxyxp", "pxp", "px"
        );

        this.messageLevelGet = this.commandResource.getOrSetDefault("messages.level.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");
        this.messageLevelSet = this.commandResource.getOrSetDefault("messages.level.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");
        this.messageLevelAdd = this.commandResource.getOrSetDefault("messages.level.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");
        this.messageLevelRemove = this.commandResource.getOrSetDefault("messages.level.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &clevel&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_level*/*%");

        this.messageXPGet = this.commandResource.getOrSetDefault("messages.xp.get",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
        this.messageXPSet = this.commandResource.getOrSetDefault("messages.xp.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
        this.messageXPAdd = this.commandResource.getOrSetDefault("messages.xp.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
        this.messageXPRemove = this.commandResource.getOrSetDefault("messages.xp.set",
                "&d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*%&e&8'&es new &cxp&8: " +
                        "&a%streamline_parse_%this_other%:::*/*streamline_user_xp_total*/*%");
    }

    @Override
    public void run(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = args[0];
        SavablePlayer player = Streamline.getInstance().getSavedPlayer(playerName);

        if (player == null) {
            MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_SELF.get());
            return;
        }

        String type = args[1].toLowerCase();

        switch (type) {
            case "level" -> {
                if (args.length == 2) {
                    MessagingUtils.sendMessage(sender, Streamline.getInstance().getUUIDFromName(playerName),
                            getWithOther(sender, this.messageLevelGet, playerName));
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

                SavablePlayer savablePlayer = UserManager.getOrGetPlayer(Streamline.getInstance().getUUIDFromName(playerName));
                if (savablePlayer == null) {
                    MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                switch (action) {
                    case "set" -> {
                        savablePlayer.setLevel(amount);
                        MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageLevelSet, playerName));
                    }
                    case "add" -> {
                        savablePlayer.addLevel(amount);
                        MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageLevelAdd, playerName));
                    }
                    case "remove" -> {
                        savablePlayer.removeLevel(amount);
                        MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageLevelRemove, playerName));
                    }
                    default -> {
                        MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                    }
                }
            }
            case "xp" -> {
                if (args.length == 2) {
                    MessagingUtils.sendMessage(sender, Streamline.getInstance().getUUIDFromName(playerName),
                            getWithOther(sender, this.messageXPGet, playerName));
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

                SavablePlayer savablePlayer = UserManager.getOrGetPlayer(Streamline.getInstance().getUUIDFromName(playerName));
                if (savablePlayer == null) {
                    MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
                    return;
                }

                switch (action) {
                    case "set" -> {
                        savablePlayer.setTotalXP(amount);
                        MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageXPSet, playerName));
                    }
                    case "add" -> {
                        savablePlayer.addTotalXP(amount);
                        MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageXPAdd, playerName));
                    }
                    case "remove" -> {
                        savablePlayer.removeTotalXP(amount);
                        MessagingUtils.sendMessage(sender, getWithOther(sender, this.messageXPRemove, playerName));
                    }
                    default -> {
                        MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                    }
                }
            }
            default -> {
                MessagingUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
            }
        }
    }

    @Override
    public List<String> doTabComplete(ICommandSender sender, String[] args) {
        if (args.length <= 1) {
            return Streamline.getInstance().getOnlinePlayerNames();
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
