package host.plas.events;

import lombok.Getter;
import lombok.Setter;
import singularity.events.modules.ModuleEvent;
import singularity.data.console.CosmicSender;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.StreamlineMessaging;

@Setter
@Getter
public class ChannelMessageEvent extends ModuleEvent {
    private ConfiguredChatChannel chatChannel;
    private CosmicSender sender;
    private String message;

    public ChannelMessageEvent(ConfiguredChatChannel chatChannel, CosmicSender sender, String message) {
        super(StreamlineMessaging.getInstance());
        setChatChannel(chatChannel);
        setSender(sender);
        setMessage(message);
    }
}
