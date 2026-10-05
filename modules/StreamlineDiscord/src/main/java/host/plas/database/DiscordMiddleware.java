package host.plas.database;

import host.plas.StreamlineDiscord;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.verified.VerifiedUser;
import lombok.Getter;
import lombok.Setter;
import singularity.scheduler.ModuleRunnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;

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

    public DiscordMiddleware() {
        super(StreamlineDiscord.getInstance(), 0L, 100L); // Run every 5 seconds (100 ticks)
        setInstance(this);
    }

    // Synchronized so the flush on module disable waits for a batch the timer has in
    // flight instead of skipping it.
    @Override
    public synchronized void run() {
        // Drops go first so a record dropped and re-saved in one batch ends up saved.
        process(droppedRoutes, "drop route", id -> {
            routeCache.remove(id);
            StreamlineDiscord.getRouteKeeper().drop(id);
        });
        process(droppedEndPoints, "drop endpoint", id -> {
            endPointCache.remove(id);
            StreamlineDiscord.getEndPointKeeper().drop(id);
        });
        process(droppedVerifiedUsers, "drop verified user", id -> {
            verifiedUserCache.remove(id);
            StreamlineDiscord.getVerifiedUserKeeper().drop(id);
        });

        process(dirtyRoutes, "save route", id -> {
            Route route = routeCache.get(id);
            if (route != null) StreamlineDiscord.getRouteKeeper().save(route, false);
        });
        process(dirtyEndPoints, "save endpoint", id -> {
            EndPoint endPoint = endPointCache.get(id);
            if (endPoint != null) StreamlineDiscord.getEndPointKeeper().save(endPoint, false);
        });
        process(dirtyVerifiedUsers, "save verified user", id -> {
            VerifiedUser user = verifiedUserCache.get(id);
            if (user != null) StreamlineDiscord.getVerifiedUserKeeper().save(user, false);
        });
    }

    /**
     * Applies {@code action} to every queued id. Each id is isolated: a failure is logged
     * at warning level and the rest of the batch still runs. Errors are caught too, since
     * this also runs on the server thread during module disable, where an escaping
     * {@link LinkageError} stops the server.
     */
    private static void process(ConcurrentSkipListSet<String> queue, String what, Consumer<String> action) {
        queue.forEach(id -> {
            try {
                action.accept(id);
            } catch (Throwable e) {
                StreamlineDiscord.getInstance().logWarning("Middleware: failed to " + what + " '" + id + "': " + e);
                e.printStackTrace();
            } finally {
                queue.remove(id);
            }
        });
    }

    public static void saveRoute(Route route) {
        if (instance == null) {
            writeThrough("route", route.getIdentifier(), () -> StreamlineDiscord.getRouteKeeper().save(route));
            return;
        }
        instance.routeCache.put(route.getIdentifier(), route);
        instance.dirtyRoutes.add(route.getIdentifier());
        instance.droppedRoutes.remove(route.getIdentifier());
    }

    public static void dropRoute(Route route) {
        if (instance == null) {
            String id = route.getIdentifier();
            writeThrough("route", id, () -> CompletableFuture.runAsync(() -> StreamlineDiscord.getRouteKeeper().drop(id)));
            return;
        }
        instance.droppedRoutes.add(route.getIdentifier());
        instance.dirtyRoutes.remove(route.getIdentifier());
        instance.routeCache.remove(route.getIdentifier());
    }

    public static void saveEndPoint(EndPoint endPoint) {
        if (instance == null) {
            writeThrough("endpoint", endPoint.getIdentifier(), () -> StreamlineDiscord.getEndPointKeeper().save(endPoint));
            return;
        }
        instance.endPointCache.put(endPoint.getIdentifier(), endPoint);
        instance.dirtyEndPoints.add(endPoint.getIdentifier());
        instance.droppedEndPoints.remove(endPoint.getIdentifier());
    }

    public static void dropEndPoint(EndPoint endPoint) {
        if (instance == null) {
            String id = endPoint.getIdentifier();
            writeThrough("endpoint", id, () -> CompletableFuture.runAsync(() -> StreamlineDiscord.getEndPointKeeper().drop(id)));
            return;
        }
        instance.droppedEndPoints.add(endPoint.getIdentifier());
        instance.dirtyEndPoints.remove(endPoint.getIdentifier());
        instance.endPointCache.remove(endPoint.getIdentifier());
    }

    public static void saveVerifiedUser(VerifiedUser user) {
        if (instance == null) {
            writeThrough("verified user", user.getIdentifier(), () -> StreamlineDiscord.getVerifiedUserKeeper().save(user));
            return;
        }
        instance.verifiedUserCache.put(user.getIdentifier(), user);
        instance.dirtyVerifiedUsers.add(user.getIdentifier());
        instance.droppedVerifiedUsers.remove(user.getIdentifier());
    }

    public static void dropVerifiedUser(VerifiedUser user) {
        if (instance == null) {
            String id = user.getIdentifier();
            writeThrough("verified user", id, () -> CompletableFuture.runAsync(() -> StreamlineDiscord.getVerifiedUserKeeper().drop(id)));
            return;
        }
        instance.droppedVerifiedUsers.add(user.getIdentifier());
        instance.dirtyVerifiedUsers.remove(user.getIdentifier());
        instance.verifiedUserCache.remove(user.getIdentifier());
    }

    /**
     * Writes straight to the database when there is no middleware to batch the change,
     * so the change is persisted either way.
     */
    private static void writeThrough(String kind, String id, Runnable write) {
        StreamlineDiscord.getInstance().logWarning("Discord middleware is not running; writing " + kind + " '" + id + "' directly.");
        write.run();
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
