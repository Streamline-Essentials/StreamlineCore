package host.plas.events.streamline.proxy;

import lombok.Getter;
import lombok.Setter;
import host.plas.StreamlineDiscord;
import host.plas.discord.messaging.DiscordProxiedMessage;
import singularity.events.modules.ModuleEvent;

@Setter
@Getter
public class SimpleDiscordPMessageReceivedEvent extends ModuleEvent {
    private DiscordProxiedMessage message;

    public SimpleDiscordPMessageReceivedEvent(DiscordProxiedMessage message) {
        super(StreamlineDiscord.getInstance());
        setMessage(message);
    }

    public String simplyGetMessage() {
        return getMessage().getMessage();
    }

    public String simplyGetInputType() {
        return getMessage().getInputType();
    }

    public String simplyGetInputIdentifier() {
        return getMessage().getInputIdentifer();
    }
}
