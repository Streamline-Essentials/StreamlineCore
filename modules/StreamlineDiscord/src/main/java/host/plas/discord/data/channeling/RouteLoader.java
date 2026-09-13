package host.plas.discord.data.channeling;

import lombok.Getter;
import lombok.Setter;
import host.plas.StreamlineDiscord;
import host.plas.database.DiscordMiddleware;
import singularity.database.modules.DBKeeper;
import singularity.loading.Loader;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter @Setter
public class RouteLoader extends Loader<Route> {
    public static RouteLoader getInstance() {
        return StreamlineDiscord.getRouteLoader();
    }

    public static void registerRoute(Route route) {
        getInstance().load(route);
    }

    public static void unregisterRoute(Route route) {
        getInstance().unload(route);
    }

    public static Optional<Route> getRoute(String identifier) {
        Optional<Route> middlewareRoute = DiscordMiddleware.getRoute(identifier);
        if (middlewareRoute.isPresent()) return middlewareRoute;

        return getInstance().get(identifier);
    }

    public static CompletableFuture<Optional<Route>> getOrLoadRouteAsync(String identifier) {
        Optional<Route> optional = getRoute(identifier);
        if (optional.isPresent()) return CompletableFuture.completedFuture(optional);

        return StreamlineDiscord.getRouteKeeper().load(identifier);
    }

    public static void loadAllRoutes() {
        CompletableFuture.runAsync(() -> {
            StreamlineDiscord.getRouteKeeper().loadAllRoutes();

            StreamlineDiscord.getInstance().logInfo("Loaded &a" + getLoadedRoutes().size() + " &froutes.");
        });
    }

    public static ConcurrentSkipListSet<Route> getLoadedRoutes() {
        return getInstance().getLoaded();
    }

    public static ConcurrentSkipListSet<Route> getBackAndForthRoute(EndPointType typeIn, String identifierIn, EndPointType typeOut, String identifierOut) {
        ConcurrentSkipListSet<Route> routes = new ConcurrentSkipListSet<>();
        getLoadedRoutes().forEach(route -> {
            if (route.getInput().getType().equals(typeIn) && route.getInput().getEndPointIdentifier().equals(identifierIn) && route.getOutput().getType().equals(typeOut) && route.getOutput().getEndPointIdentifier().equals(identifierOut)) {
                routes.add(route);
            }
        });

        return routes;
    }

    public static ConcurrentSkipListSet<Route> getRoutesByEndpoint(EndPointType type, String endpointIdentifier) {
        ConcurrentSkipListSet<Route> routes = new ConcurrentSkipListSet<>();
        getLoadedRoutes().forEach(route -> {
            if (
                    (route.getInput().getType().equals(type) && route.getInput().getEndPointIdentifier().equals(endpointIdentifier)) ||
                            (route.getOutput().getType().equals(type) && route.getOutput().getEndPointIdentifier().equals(endpointIdentifier))
            ) {
                routes.add(route);
            }
        });

        return routes;
    }

    public static ConcurrentSkipListSet<Route> getRoutesByDiscordChannel(String discordChannelId) {
        return getRoutesByEndpoint(EndPointType.DISCORD_TEXT, discordChannelId);
    }

    @Override
    public DBKeeper<Route> getKeeper() {
        return StreamlineDiscord.getRouteKeeper();
    }

    @Override
    public Route getConsole() {
        return null;
    }

    @Override
    public void fireLoadEvents(Route route) {
        // No specific load events for routes
    }

    @Override
    public Route instantiate(String s) {
        return new Route(s);
    }

    @Override
    public void fireCreateEvents(Route route) {
        // No specific create events for routes
    }
}
