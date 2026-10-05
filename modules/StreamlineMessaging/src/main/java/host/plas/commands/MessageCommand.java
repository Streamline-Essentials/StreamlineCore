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
public class MessageCommand extends ModuleCommand {
    /** The sender's line for /message and /reply. */
    public static final String DEFAULT_SENDER_FORMAT =
            "&dYOU &9&l→ &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/* &7(&e*/*streamline_user_server*/*&7)%&7: &f%this_message%";
    /**
     * A sender format that early configs were written with, which put the message on a
     * second line. Configs still holding it exactly are moved to {@link #DEFAULT_SENDER_FORMAT}.
     */
    private static final String TWO_LINE_SENDER_FORMAT =
            "&dYOU &9&l→ &d%streamline_parse_%this_other%:::*/*streamline_user_formatted*/* &7(&e*/*streamline_user_server*/*&7)%&7:\n" +
                    "         &f%this_message%";

    /**
     * Reads the sender format at {@code messages.success.sender}, moving a config that still
     * holds {@link #TWO_LINE_SENDER_FORMAT} to {@link #DEFAULT_SENDER_FORMAT}.
     */
    public static String loadSenderFormat(ModuleCommand command) {
        String format = command.getCommandResource().getOrSetDefault("messages.success.sender", DEFAULT_SENDER_FORMAT);
        if (! TWO_LINE_SENDER_FORMAT.equals(format)) return format;
        command.getCommandResource().write("messages.success.sender", DEFAULT_SENDER_FORMAT);
        return DEFAULT_SENDER_FORMAT;
    }

    private String messageSender;
    private String messageRecipient;

    public MessageCommand() {
        super(StreamlineMessaging.getInstance(),
                "message",
                "streamline.command.message",
                "msg", "w", "tell", "whisper"
        );

        messageSender = loadSenderFormat(this);
        messageRecipient = this.getCommandResource().getOrSetDefault("messages.success.recipient",
                "&d%streamline_user_formatted% &7(&e%streamline_user_server%&7) &9&l→ &dYOU&7: &f%this_message%");
    }

    @Override
    public void run(CosmicSender streamSender, String[] strings) {
        if (strings.length < 2) {
            ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_FEW.get());
            return;
        }

        String username = strings[0];

        CosmicSender other = UserUtils.getOrCreateSenderByName(username).orElse(null);
        if (other == null) {
            ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_OTHER.get());
            return;
        }

        String message = ModuleUtils.argsToStringMinus(strings, 0);

        SavableChatter chatter = MyLoader.getInstance().getOrCreate(streamSender.getUuid());
        if (chatter == null) {
            ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_SELF.get());
            return;
        }
        chatter.onMessage(other, message, messageSender, messageRecipient);
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        return ModuleUtils.getOnlinePlayerNames();
    }
}
