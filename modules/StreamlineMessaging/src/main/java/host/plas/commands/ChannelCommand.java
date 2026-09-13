package host.plas.commands;

import host.plas.database.MyLoader;
import lombok.Getter;
import lombok.Setter;
import singularity.command.ModuleCommand;
import singularity.configs.given.MainMessagesHandler;
import singularity.modules.ModuleUtils;
import singularity.data.console.CosmicSender;
import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.savables.SavableChatter;

import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class ChannelCommand extends ModuleCommand {
    private String messageResultSuccess;
    private String messageResultFailure;

    public ChannelCommand() {
        super(StreamlineMessaging.getInstance(),
                "channel",
                "streamline.command.channel",
                "chan", "chat", "chat-channel"
        );

        messageResultSuccess = this.getCommandResource().getOrSetDefault("messages.result",
                "&eYou set your &cchat channel &eto &7'&a%messaging_channel_identifier%&7'&8.");
        messageResultFailure = this.getCommandResource().getOrSetDefault("messages.result",
                "&eYou attempted to set your &cchat channel &eto &7'&a%this_identifier%&7'&8, &ebut was set to &7'&a%messaging_channel_identifier%&7' &edue to an &cerror&8.");
    }

    @Override
    public void run(CosmicSender streamSender, String[] strings) {
        try {
            String identifier;
            if (strings[0].equals("")) {
                identifier = "none";
            } else if (strings.length > 1) {
                ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TOO_MANY.get());
                return;
            } else {
                identifier = strings[0];
            }

            SavableChatter chatter = MyLoader.getInstance().getOrCreate(streamSender.getUuid());
            if (chatter == null) {
                ModuleUtils.sendMessage(streamSender, MainMessagesHandler.MESSAGES.INVALID.USER_SELF.get());
                return;
            }

            ConfiguredChatChannel channel = StreamlineMessaging.getChatChannelConfig().getChatChannel(identifier);
            if (channel == null) {
                ModuleUtils.sendMessage(streamSender, messageResultFailure
                        .replace("%this_identifier%", identifier));
                return;
            }

            chatter.setCurrentChatChannel(channel);

            if (chatter.getCurrentChatChannel().getIdentifier().equals(identifier)) {
                ModuleUtils.sendMessage(streamSender, messageResultSuccess
                        .replace("%this_identifier%", identifier)
                );
            } else {
                ModuleUtils.sendMessage(streamSender, messageResultFailure
                        .replace("%this_identifier%", identifier)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            streamSender.sendMessage(MainMessagesHandler.MESSAGES.INVALID.ARGUMENTS_TYPE_DEFAULT.get());
        }
    }

    @Override
    public ConcurrentSkipListSet<String> doTabComplete(CosmicSender CosmicSender, String[] strings) {
        if (strings.length <= 1) {
            ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

            StreamlineMessaging.getChatChannelConfig().getChatChannels().forEach((a, b) -> {
                if (ModuleUtils.hasPermission(CosmicSender, b.getAccessPermission())) r.add(a);
            });

            return r;
        }

        return new ConcurrentSkipListSet<>();
    }
}
