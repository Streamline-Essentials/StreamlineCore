package net.streamline.api.base.commands;

import net.streamline.api.SLAPI;
import net.streamline.api.command.StreamlineCommand;
import net.streamline.api.command.context.CommandContext;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.players.StreamPlayer;
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
    public void run(CommandContext<StreamlineCommand> context) {
        if (context.getArgCount() < 2) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = context.getStringArg(0);
        StreamPlayer player = UserUtils.getOrCreatePlayerByName(playerName).orElse(null);

        if (player == null) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        String type = context.getStringArg(1).toLowerCase();

        switch (type) {
            case "level":
                if (context.getArgCount() == 2) {
                    player.sendMessage(getWithOther(context.getSender(), this.messageLevelGet, player));
                    return;
                }

                if (context.getArgCount() < 4) {
                    context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String action = context.getStringArg(2);
                int amount = 0;
                try {
                    amount = Integer.parseInt(context.getStringArg(3));
                } catch (Exception e) {
                    e.printStackTrace();
                    context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
                    return;
                }

                switch (action) {
                    case "set":
                        player.setLevel(amount);
                        context.sendMessage(getWithOther(context.getSender(), this.messageLevelSet, player));
                        break;
                    case "add":
                        player.addLevel(amount);
                        context.sendMessage(getWithOther(context.getSender(), this.messageLevelAdd, player));
                        break;
                    case "remove":
                        player.removeLevel(amount);
                        context.sendMessage(getWithOther(context.getSender(), this.messageLevelRemove, player));
                        break;
                    default:
                        context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                        break;
                }
                break;
            case "xp":
                if (context.getArgCount() == 2) {
                    player.sendMessage(getWithOther(context.getSender(), this.messageXPGet, player));
                    return;
                }

                if (context.getArgCount() < 4) {
                    context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
                    return;
                }

                String action2 = context.getStringArg(2);
                double amount2 = 0;
                try {
                    amount2 = Double.parseDouble(context.getStringArg(3));
                } catch (Exception e) {
                    e.printStackTrace();
                    context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_NUMBER.get());
                    return;
                }

                switch (action2) {
                    case "set":
                        player.setExperience(amount2);
                        context.sendMessage(getWithOther(context.getSender(), this.messageXPSet, player));
                        break;
                    case "add":
                        player.addExperience(amount2);
                        context.sendMessage(getWithOther(context.getSender(), this.messageXPAdd, player));
                        break;
                    case "remove":
                        player.removeExperience(amount2);
                        context.sendMessage(getWithOther(context.getSender(), this.messageXPRemove, player));
                        break;
                    default:
                        context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                        break;
                }
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
            return new ConcurrentSkipListSet<>(List.of("level", "xp"));
        }
        if (context.getArgCount() == 3) {
            return new ConcurrentSkipListSet<>(List.of("set", "add", "remove"));
        }
        if (context.getArgCount() == 4) {
            if (context.getStringArg(1).equals("xp")) return getDoubleArgument();
            return getIntegerArgument();
        }

        return new ConcurrentSkipListSet<>();
    }
}
