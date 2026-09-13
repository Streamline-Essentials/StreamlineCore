package host.plas.events.streamline;

import host.plas.StreamlineDiscord;
import singularity.events.modules.ModuleEvent;

public class CosmicDiscordEvent extends ModuleEvent {
    public CosmicDiscordEvent() {
        super(StreamlineDiscord.getInstance());
    }
}
