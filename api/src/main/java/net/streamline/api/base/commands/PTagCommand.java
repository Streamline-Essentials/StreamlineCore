package net.streamline.api.base.commands;

import singularity.Singularity;
import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.MainMessagesHandler;
import singularity.data.players.CosmicPlayer;
import singularity.modules.ModuleUtils;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class PTagCommand extends CosmicCommand {
    private final String messageTagsGet;
    private final String messageTagsAdd;
    private final String messageTagsRemove;

    public PTagCommand() {
        super(
                "streamline-base",
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
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 2) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String playerName = context.getStringArg(0);
        CosmicPlayer other = UserUtils.getOrCreatePlayerByName(playerName).orElse(null);

        if (other == null) {
            ModuleUtils.sendMessage(context.getSender(), MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        if (context.getArgCount() == 1) {
            context.sendMessage(getWithOther(context.getSender(), this.messageTagsGet, playerName));
            return;
        }

        if (context.getArgCount() < 3) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String action = context.getStringArg(1);
        String[] actions = MessageUtils.argsToStringMinus(context.getArgsArray(), 0, 1).split(" ");

        switch (action) {
            case "add":
                Arrays.stream(actions).forEach(other::addTag);
                context.sendMessage(getWithOther(context.getSender(), this.messageTagsAdd, other));
                break;
            case "remove":
                Arrays.stream(actions).forEach(other::removeTag);
                context.sendMessage(getWithOther(context.getSender(), this.messageTagsRemove, other));
                break;
            default:
                context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
                break;
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() <= 1) {
            return Singularity.getInstance().getPlatform().getOnlinePlayerNames();
        }
        if (context.getArgCount() == 2) {
            return new ConcurrentSkipListSet<>(List.of("add", "remove"));
        }

        return new ConcurrentSkipListSet<>();
    }
}
