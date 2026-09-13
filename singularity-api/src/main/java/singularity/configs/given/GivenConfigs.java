package singularity.configs.given;

import gg.drak.thebase.async.AsyncUtils;
import lombok.Getter;
import lombok.Setter;
import singularity.Singularity;
import singularity.configs.given.whitelist.WhitelistConfig;
import singularity.data.console.CosmicSender;
import singularity.database.ConnectorSet;
import singularity.database.CoreDBOperator;
import singularity.database.servers.SavedServer;
import singularity.redis.OwnRedisClient;
import singularity.utils.UserUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central registry for all framework-provided configuration handlers,
 * the main database operator, and the Redis client readiness flags.
 *
 * <p>Call {@link #init()} once at startup to construct all handlers and
 * kick off the asynchronous database and Redis initialisation. Use
 * {@link #isDatabaseReady()} / {@link #isRedisReady()} (or the blocking
 * {@link #awaitDatabaseReady()} / {@link #waitUntilRedisReady()}) to
 * guard code that depends on those connections.</p>
 */
public class GivenConfigs {

    /** Handler for the primary plugin settings ({@code main-config.yml}). */
    @Getter @Setter
    private static MainConfigHandler mainConfig;

    /** Handler for user-facing message strings ({@code main-messages.yml}). */
    @Getter @Setter
    private static MainMessagesHandler mainMessages;

    /** Handler for the server whitelist ({@code whitelist.json}). */
    @Getter @Setter
    private static WhitelistConfig whitelistConfig;

    /** Handler for the database connection settings ({@code database-config.yml}). */
    @Getter @Setter
    private static DatabaseConfigHandler databaseConfig;

    /** Handler for the server identity settings ({@code server-config.yml}). */
    @Getter @Setter
    private static ServerConfigHandler serverConfig;

    /** Handler for the Redis connection settings ({@code redis-config.yml}). */
    @Getter @Setter
    private static RedisConfigHandler redisConfig;

    /** Directory in which per-player punishment JSON files are stored. */
    @Getter @Setter
    private static File punishmentFolder;

    /** The active database operator used for all framework persistence. */
    @Getter @Setter
    private static CoreDBOperator mainDatabase;

    /**
     * Atomic flag tracking whether the main database has finished initialising.
     * {@code true} once the {@link CoreDBOperator} is ready to accept queries.
     */
    @Getter @Setter
    private static AtomicBoolean databaseReadyAtomic;

    /**
     * Atomic flag tracking whether the Redis client has finished initialising.
     * {@code true} once {@link OwnRedisClient} is connected and usable.
     */
    @Getter @Setter
    private static AtomicBoolean redisReadyAtomic;

    /**
     * Constructs all configuration handlers and asynchronously initialises the
     * database and Redis connections. Must be called once during plugin startup
     * before any other framework code that depends on persistence or messaging.
     */
    public static void init() {
        setDatabaseReadyAtomic(new AtomicBoolean(false));

        setMainConfig(new MainConfigHandler());
        setMainMessages(new MainMessagesHandler());
        setWhitelistConfig(new WhitelistConfig());
        setDatabaseConfig(new DatabaseConfigHandler());
        setServerConfig(new ServerConfigHandler());
        setRedisConfig(new RedisConfigHandler());

        // Initialize main database asynchronously.
        AsyncUtils.executeAsync(() -> {
            try {
                ConnectorSet connectorSet = getDatabaseConfig().getConnectorSet();
                CoreDBOperator operator = new CoreDBOperator(connectorSet);
                setMainDatabase(operator);

                ensureServer();

                setDatabaseReady(true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });

        // Initialize Redis client asynchronously.
        AsyncUtils.executeAsync(() -> {
            try {
                OwnRedisClient.init();

                setRedisReady(true);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Returns whether the main database has finished initialising and is ready
     * to serve queries.
     *
     * @return {@code true} if the database operator is ready; {@code false} otherwise
     */
    public static boolean isDatabaseReady() {
        return getDatabaseReadyAtomic() != null && getDatabaseReadyAtomic().get();
    }

    /**
     * Returns whether the Redis client has finished initialising and is ready
     * to send/receive messages.
     *
     * @return {@code true} if Redis is ready; {@code false} otherwise
     */
    public static boolean isRedisReady() {
        return getRedisReadyAtomic() != null && getRedisReadyAtomic().get();
    }

    /**
     * Sets the database readiness flag, creating the underlying
     * {@link AtomicBoolean} if it has not yet been initialised.
     *
     * @param ready {@code true} to mark the database as ready
     */
    public static void setDatabaseReady(boolean ready) {
        if (getDatabaseReadyAtomic() == null) {
            setDatabaseReadyAtomic(new AtomicBoolean(ready));
        } else {
            getDatabaseReadyAtomic().set(ready);
        }
    }

    /**
     * Sets the Redis readiness flag, creating the underlying
     * {@link AtomicBoolean} if it has not yet been initialised.
     *
     * @param ready {@code true} to mark Redis as ready
     */
    public static void setRedisReady(boolean ready) {
        if (getRedisReadyAtomic() == null) {
            setRedisReadyAtomic(new AtomicBoolean(ready));
        } else {
            getRedisReadyAtomic().set(ready);
        }
    }

    /**
     * Blocks the calling thread in a spin-wait loop until the main database
     * is ready. Prefer using this only from threads that are already off the
     * main server thread.
     */
    public static void awaitDatabaseReady() {
        while (! isDatabaseReady()) {
            Thread.onSpinWait();
        }
    }

    /**
     * Blocks the calling thread with 100 ms sleeps until the Redis client is
     * ready, or until the thread is interrupted (in which case the interrupt
     * flag is re-set and the method returns early).
     */
    public static void waitUntilRedisReady() {
        while (! isRedisReady()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // Exit if interrupted
            }
        }
    }

    /**
     * Creates the punishment folder inside the plugin's data directory if it
     * does not already exist, and stores a reference in {@link #punishmentFolder}.
     */
    public static void ensureFolders() {
        setPunishmentFolder(new File(Singularity.getInstance().getDataFolder(), "punishments" + File.separator));
        ensureFolder(getPunishmentFolder());
    }

    /**
     * Creates the given directory (and any missing parents) if it is a directory
     * path that does not yet exist on disk. Does nothing if {@code folder} is
     * {@code null} or not a directory-type path.
     *
     * @param folder the directory to create
     */
    public static void ensureFolder(File folder) {
        if (folder == null) return;
        if (! folder.isDirectory()) return;

        folder.mkdirs();
    }

    /**
     * Reloads the main configuration and messages files from disk, then saves
     * and reloads every currently loaded {@link CosmicSender} to propagate any
     * changes.
     */
    public static void reloadData() {
        getMainConfig().reloadResource();
        getMainMessages().reloadResource();
        for (CosmicSender user : UserUtils.getLoadedSendersSet()) {
            user.save();
            user.reload();
        }
    }

    /**
     * Pushes the current server identity to the database, creating or updating
     * the {@link SavedServer} record for this node.
     */
    public static void ensureServer() {
        getServer().push();
    }

    /**
     * Returns the {@link SavedServer} representing this server node, as read
     * from the server configuration.
     *
     * @return the current {@link SavedServer} instance
     */
    public static SavedServer getServer() {
        return getServerConfig().getServer();
    }

    /**
     * Returns the human-readable name of this server node from the server
     * configuration.
     *
     * @return the server name string
     */
    public static String getServerName() {
        return getServerConfig().getName();
    }

    /**
     * Persists the name and UUID of the given {@link SavedServer} to the
     * server configuration file.
     *
     * @param server the server whose identity should be written
     */
    public static void writeServer(SavedServer server) {
        getServerConfig().writeServer(server);
    }

    /**
     * Persists the given name to the server configuration file.
     *
     * @param name the new server name to write
     */
    public static void writeServerName(String name) {
        getServerConfig().writeName(name);
    }
}
