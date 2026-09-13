package host.plas.events.streamline.routes;

import host.plas.discord.data.channeling.Route;
import lombok.Getter;
import lombok.Setter;
import host.plas.StreamlineDiscord;
import singularity.events.modules.ModuleEvent;

@Setter
@Getter
public class DiscordRouteEvent<T extends Route> extends ModuleEvent {
    private T route;

    public DiscordRouteEvent(T route) {
        super(StreamlineDiscord.getInstance());
        setRoute(route);
    }
}
