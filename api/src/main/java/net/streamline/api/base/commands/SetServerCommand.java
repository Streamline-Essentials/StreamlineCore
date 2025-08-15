package net.streamline.api.base.commands;

import singularity.command.CosmicCommand;
import singularity.command.context.CommandContext;
import singularity.configs.given.GivenConfigs;
import singularity.configs.given.MainMessagesHandler;

import java.util.concurrent.ConcurrentSkipListSet;

public class SetServerCommand extends CosmicCommand {
    private final String messageResultSet;
    private final String messageResultInvalid;

    public SetServerCommand() {
        super(
                "streamline-base",
                "setserveridentifier",
                "streamline.command.setserveridentifier.default",
                "setidentifier", "setserver"
        );

        this.messageResultSet = this.getCommandResource().getOrSetDefault("messages.result.set",
                "&eSuccessfully set this server's identifier to &7\"&c%this_input%&7\"&8!");
        this.messageResultInvalid = this.getCommandResource().getOrSetDefault("messages.result.invalid",
                "&cInvalid input. Please provide a valid identifier.&8!");
    }

    @Override
    public void run(CommandContext<CosmicCommand> context) {
        if (context.getArgCount() < 1) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }
        if (context.getArgCount() > 1) {
            context.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
            return;
        }

        String input = context.getStringArg(0);
        if (input.isBlank()) {
            context.sendMessage(this.messageResultInvalid);
            return;
        }

        GivenConfigs.writeServerName(input);

        context.sendMessage(this.messageResultSet.replace("%this_input%", input));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CommandContext<CosmicCommand> context) {
        ConcurrentSkipListSet<String> completions = new ConcurrentSkipListSet<>();

        if (context.getArgCount() == 1) {
            completions.add("<server-name>");
        }

        return completions;
    }
}
