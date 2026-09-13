package host.plas.events.streamline.bot;

import host.plas.StreamlineDiscord;
import singularity.events.modules.ModuleEvent;

public class BotStoppedEvent extends ModuleEvent {
    public BotStoppedEvent() {
        super(StreamlineDiscord.getInstance());
    }
}
