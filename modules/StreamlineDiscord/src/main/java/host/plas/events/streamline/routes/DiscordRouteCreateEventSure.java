package host.plas.events.streamline.routes;

import host.plas.discord.data.channeling.Route;

public class DiscordRouteCreateEventSure extends DiscordRouteCreateEventUnsure<Route> {
    public DiscordRouteCreateEventSure(Route route) {
        super(route);
    }
}
