package host.plas.commands;

import lombok.Getter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
//import singularity.savables.users.OperatorUser;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineUtilities;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class SudoOpCommand extends ModuleCommand {
    private final String messageResult;

    public SudoOpCommand() {
        super(StreamlineUtilities.getInstance(),
                "proxysudoop",
                "streamline.command.sudo-op.default",
                "pso", "psudoop", "psuop"
        );

        messageResult = getCommandResource().getOrSetDefault("messages.result.sudoer", "&eYou have successfully ran the command &f%this_input%&e as &f%streamline_parse_%this_other%:::*/*streamline_user_formatted*/*% &ewith operator privileges&8!");
    }

    @Override
    public void run(CosmicSender sender, String[] strings) {
        if (strings.length < 2) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String username = strings[0];
        String command = ModuleUtils.argsToStringMinus(strings, 0);
        CosmicSender other = ModuleUtils.getOrGetUserByName(username).orElse(null);
        if (other == null) {
            ModuleUtils.sendMessage(sender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

//        OperatorUser operatorUser = new OperatorUser(other);
//
//        ModuleUtils.runAs(operatorUser, command);
        ModuleUtils.sendMessage(sender, getWithOther(sender, messageResult, other));
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        return ModuleUtils.getOnlinePlayerNames();
    }
}
