package host.plas.database;

import host.plas.StreamlineDiscord;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.verified.VerifiedUser;
import lombok.Getter;
import lombok.Setter;
import singularity.scheduler.ModuleRunnable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordMiddleware extends ModuleRunnable {
    @Getter @Setter
    private static DiscordMiddleware instance;

    @Getter
    private final ConcurrentHashMap<String, Route> routeCache = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, EndPoint> endPointCache = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<String, VerifiedUser> verifiedUserCache = new ConcurrentHashMap<>();

    @Getter
    private final ConcurrentSkipListSet<String> dirtyRoutes = new ConcurrentSkipListSet<>();
    @Getter
    private final ConcurrentSkipListSet<String> dirtyEndPoints = new ConcurrentSkipListSet<>();
    @Getter
    private final ConcurrentSkipListSet<String> dirtyVerifiedUsers = new ConcurrentSkipListSet<>();

    @Getter
    private final ConcurrentSkipListSet<String> droppedRoutes = new ConcurrentSkipListSet<>();
    @Getter
    private final ConcurrentSkipListSet<String> droppedEndPoints = new ConcurrentSkipListSet<>();
    @Getter
    private final ConcurrentSkipListSet<String> droppedVerifiedUsers = new ConcurrentSkipListSet<>();

    private final AtomicBoolean processing = new AtomicBoolean(false);

    public DiscordMiddleware() {
        super(StreamlineDiscord.getInstance(), 0L, 100L); // Run every 5 seconds (100 ticks)
        setInstance(this);
    }

    @Override
    public void run() {
        if (processing.get()) return;
        processing.set(true);

        try {
            // Process Drops first
            if (! droppedRoutes.isEmpty()) {
                droppedRoutes.forEach(id -> {
                    Route route = routeCache.remove(id);
                    StreamlineDiscord.getRouteKeeper().drop(id);
                    droppedRoutes.remove(id);
                });
            }

            if (! droppedEndPoints.isEmpty()) {
                droppedEndPoints.forEach(id -> {
                    EndPoint endPoint = endPointCache.remove(id);
                    StreamlineDiscord.getEndPointKeeper().drop(id);
                    droppedEndPoints.remove(id);
                });
            }

            if (! droppedVerifiedUsers.isEmpty()) {
                droppedVerifiedUsers.forEach(id -> {
                    VerifiedUser user = verifiedUserCache.remove(id);
                    StreamlineDiscord.getVerifiedUserKeeper().drop(id);
                    droppedVerifiedUsers.remove(id);
                });
            }

            // Process Saves
            if (! dirtyRoutes.isEmpty()) {
                dirtyRoutes.forEach(id -> {
                    Route route = routeCache.get(id);
                    if (route != null) StreamlineDiscord.getRouteKeeper().save(route, false);
                    dirtyRoutes.remove(id);
                });
            }

            if (! dirtyEndPoints.isEmpty()) {
                dirtyEndPoints.forEach(id -> {
                    EndPoint endPoint = endPointCache.get(id);
                    if (endPoint != null) StreamlineDiscord.getEndPointKeeper().save(endPoint, false);
                    dirtyEndPoints.remove(id);
                });
            }

            if (! dirtyVerifiedUsers.isEmpty()) {
                dirtyVerifiedUsers.forEach(id -> {
                    VerifiedUser user = verifiedUserCache.get(id);
                    if (user != null) StreamlineDiscord.getVerifiedUserKeeper().save(user, false);
                    dirtyVerifiedUsers.remove(id);
                });
            }
        } catch (Exception e) {
            StreamlineDiscord.getInstance().logWarning("Middleware: Error during batch update: " + e.getMessage());
            e.printStackTrace();
        } finally {
            processing.set(false);
        }
    }

    public static void saveRoute(Route route) {
        if (instance == null) return;
        instance.routeCache.put(route.getIdentifier(), route);
        instance.dirtyRoutes.add(route.getIdentifier());
        instance.droppedRoutes.remove(route.getIdentifier());
    }

    public static void dropRoute(Route route) {
        if (instance == null) return;
        instance.droppedRoutes.add(route.getIdentifier());
        instance.dirtyRoutes.remove(route.getIdentifier());
        instance.routeCache.remove(route.getIdentifier());
    }

    public static void saveEndPoint(EndPoint endPoint) {
        if (instance == null) return;
        instance.endPointCache.put(endPoint.getIdentifier(), endPoint);
        instance.dirtyEndPoints.add(endPoint.getIdentifier());
        instance.droppedEndPoints.remove(endPoint.getIdentifier());
    }

    public static void dropEndPoint(EndPoint endPoint) {
        if (instance == null) return;
        instance.droppedEndPoints.add(endPoint.getIdentifier());
        instance.dirtyEndPoints.remove(endPoint.getIdentifier());
        instance.endPointCache.remove(endPoint.getIdentifier());
    }

    public static void saveVerifiedUser(VerifiedUser user) {
        if (instance == null) return;
        instance.verifiedUserCache.put(user.getIdentifier(), user);
        instance.dirtyVerifiedUsers.add(user.getIdentifier());
        instance.droppedVerifiedUsers.remove(user.getIdentifier());
    }

    public static void dropVerifiedUser(VerifiedUser user) {
        if (instance == null) return;
        instance.droppedVerifiedUsers.add(user.getIdentifier());
        instance.dirtyVerifiedUsers.remove(user.getIdentifier());
        instance.verifiedUserCache.remove(user.getIdentifier());
    }

    public static Optional<Route> getRoute(String id) {
        if (instance == null) return Optional.empty();
        return Optional.ofNullable(instance.routeCache.get(id));
    }

    public static Optional<EndPoint> getEndPoint(String id) {
        if (instance == null) return Optional.empty();
        return Optional.ofNullable(instance.endPointCache.get(id));
    }

    public static Optional<VerifiedUser> getVerifiedUser(String id) {
        if (instance == null) return Optional.empty();
        return Optional.ofNullable(instance.verifiedUserCache.get(id));
    }
}
