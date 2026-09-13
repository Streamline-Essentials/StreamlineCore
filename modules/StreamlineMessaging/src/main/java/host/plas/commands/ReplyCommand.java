package host.plas.commands;

import host.plas.database.MyLoader;
import lombok.Getter;
import lombok.Setter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineMessaging;
import host.plas.savables.SavableChatter;
import singularity.utils.UserUtils;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class ReplyCommand extends ModuleCommand {
    private String messageSender;
    private String messageRecipient;

    public ReplyCommand() {
        super(StreamlineMessaging.getInstance(),
                "reply",
                "streamline.command.reply",
                "re", "r"
        );

        messageSender = this.getCommandResource().getOrSetDefault("messages.success.sender",
                "&dYOU &9&l→ &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/* &7(&e*/*streamline_user_server*/*&7)%&7:\n" +
                        "         &f%this_message%");
        messageRecipient = this.getCommandResource().getOrSetDefault("messages.success.recipient",
                "&d%streamline_user_formatted% &7(&e%streamline_user_server%&7) &9&l→ &dYOU&7: &f%this_message%");
    }

    @Override
    public void run(CosmicSender streamSender, String[] strings) {
        if (strings[0].isEmpty()) {
            ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }
        String message = ModuleUtils.argsToString(strings);

        SavableChatter chatter = MyLoader.getInstance().getOrCreate(streamSender.getUuid());
        if (chatter == null) {
            ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_SELF.get());
            return;
        }
        CosmicSender other = UserUtils.getOrCreateSender(chatter.getReplyTo()).orElse(null);
        if (other == null) {
            ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }
        chatter.onReply(other, message, messageSender, messageRecipient);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender streamSender, String[] strings) {
        return ModuleUtils.getOnlinePlayerNames();
    }
}
