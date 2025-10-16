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

public class GivenConfigs {
    @Getter @Setter
    private static MainConfigHandler mainConfig;
    @Getter @Setter
    private static MainMessagesHandler mainMessages;
    @Getter @Setter
    private static WhitelistConfig whitelistConfig;
    @Getter @Setter
    private static DatabaseConfigHandler databaseConfig;
    @Getter @Setter
    private static ServerConfigHandler serverConfig;
    @Getter @Setter
    private static RedisConfigHandler redisConfig;

    @Getter @Setter
    private static File punishmentFolder;

    @Getter @Setter
    private static CoreDBOperator mainDatabase;

    @Getter @Setter
    private static AtomicBoolean databaseReadyAtomic;

    @Getter @Setter
    private static AtomicBoolean redisReadyAtomic;

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

    public static boolean isDatabaseReady() {
        return getDatabaseReadyAtomic() != null && getDatabaseReadyAtomic().get();
    }

    public static boolean isRedisReady() {
        return getRedisReadyAtomic() != null && getRedisReadyAtomic().get();
    }

    public static void setDatabaseReady(boolean ready) {
        if (getDatabaseReadyAtomic() == null) {
            setDatabaseReadyAtomic(new AtomicBoolean(ready));
        } else {
            getDatabaseReadyAtomic().set(ready);
        }
    }

    public static void setRedisReady(boolean ready) {
        if (getRedisReadyAtomic() == null) {
            setRedisReadyAtomic(new AtomicBoolean(ready));
        } else {
            getRedisReadyAtomic().set(ready);
        }
    }

    public static void waitUntilDatabaseReady() {
        while (! isDatabaseReady()) {
            Thread.onSpinWait();
        }
    }

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

    public static void ensureFolders() {
        setPunishmentFolder(new File(Singularity.getInstance().getDataFolder(), "punishments" + File.separator));
        ensureFolder(getPunishmentFolder());
    }

    public static void ensureFolder(File folder) {
        if (folder == null) return;
        if (! folder.isDirectory()) return;

        folder.mkdirs();
    }

    public static void reloadData() {
        getMainConfig().reloadResource();
        getMainMessages().reloadResource();
        for (CosmicSender user : UserUtils.getLoadedSendersSet()) {
            user.save();
            user.reload();
        }
    }

    public static void ensureServer() {
        getServer().push();
    }

    public static SavedServer getServer() {
        return getServerConfig().getServer();
    }

    public static String getServerName() {
        return getServerConfig().getName();
    }

    public static void writeServer(SavedServer server) {
        getServerConfig().writeServer(server);
    }

    public static void writeServerName(String name) {
        getServerConfig().writeName(name);
    }
}
