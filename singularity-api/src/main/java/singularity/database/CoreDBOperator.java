package singularity.database;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.players.events.SavePlayerEvent;
import singularity.data.players.events.SaveSenderEvent;
import singularity.data.players.location.PlayerRotation;
import singularity.data.players.location.PlayerWorld;
import singularity.data.players.location.WorldPosition;
import singularity.data.server.CosmicServer;
import singularity.data.teleportation.TPTicket;
import singularity.data.update.UpdateType;
import singularity.data.update.defaults.DefaultUpdaters;
import singularity.data.uuid.UuidInfo;
import singularity.database.servers.SavedServer;
import singularity.database.servers.UpdateInfo;
import singularity.interfaces.ISingularityExtension;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

/**
 * Concrete {@link DBOperator} implementation that handles all StreamlineCore database
 * operations, including player data persistence, UUID tracking, cross-server update
 * signalling, server registry entries, and teleportation tickets.
 *
 * <p>All long-running I/O is performed asynchronously via {@link CompletableFuture}
 * to avoid blocking the server thread. A short-lived Caffeine {@link AsyncCache} is
 * used to deduplicate concurrent load requests for the same player UUID.</p>
 */
public class CoreDBOperator extends DBOperator {

    /**
     * A 10-second write-after-access Caffeine cache that prevents duplicate
     * asynchronous load operations for the same player UUID from hitting the
     * database simultaneously.
     */
    @Getter @Setter
    private static AsyncCache<String, Optional<CosmicSender>> loadingPlayers = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .buildAsync();
            ;

    /**
     * Dedicated pool for all database I/O.
     *
     * <p>Database work must not run on {@link java.util.concurrent.ForkJoinPool#commonPool()}:
     * several operations block on nested futures (for example {@code loadPlayer} joins
     * {@code exists}), and the common pool's parallelism is {@code cores - 1}, which is a
     * single thread on a 2-core host. Blocking JDBC calls there starve the pool and can
     * deadlock. Threads are daemons so they never keep the JVM alive.</p>
     */
    @Getter
    private static final ExecutorService databaseExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            runnable -> {
                Thread thread = new Thread(runnable, "Singularity Database Worker");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * Creates a new {@code CoreDBOperator} using the supplied connection configuration
     * and registers it under the plugin label {@code "StreamlineCore"}.
     *
     * @param set the {@link ConnectorSet} describing the database connection
     */
    public CoreDBOperator(ConnectorSet set) {
        super(set, "StreamlineCore");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Also discards any in-flight player load results, so that a later reconnect
     * does not serve entries cached against the closed pool.</p>
     */
    @Override
    public void close() {
        super.close();

        getLoadingPlayers().synchronous().invalidateAll();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes the {@code CREATE_DATABASE} statement for the current connector
     * type. Does nothing if the statement is {@code null} or blank.</p>
     */
    @Override
    public void ensureDatabase() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_DATABASE, this.getConnectorSet());
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        failIfErrored(this.execute(s1, stmt -> {}), "create the database");
    }

    /**
     * Throws when any statement in a schema batch failed.
     *
     * <p>{@link #execute} reports failures as {@link ExecutionResult#ERROR} instead of
     * throwing, so schema setup would otherwise appear to succeed and never be retried.
     *
     * @param results the results of the executed batch
     * @param action  a short description of what was attempted, used in the message
     */
    private void failIfErrored(List<ExecutionResult> results, String action) {
        if (results.stream().anyMatch(r -> r == ExecutionResult.ERROR)) {
            throw new IllegalStateException("Failed to " + action + "; see the logged SQL errors above.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes the {@code CREATE_TABLES} statement for the current connector
     * type. Does nothing if the statement is {@code null} or blank.</p>
     */
    @Override
    public void ensureTables() {
        String s1 = Statements.getStatement(Statements.StatementType.CREATE_TABLES, this.getConnectorSet());
        if (s1 == null) return;
        if (s1.isBlank() || s1.isEmpty()) return;

        failIfErrored(this.execute(s1, stmt -> {}), "create the database tables");

        migrateSchema();
    }

    /**
     * Applies idempotent schema migrations to pre-existing MySQL installations.
     *
     * <p>{@code CREATE TABLE IF NOT EXISTS} cannot alter a table that already exists,
     * so schema corrections only reach fresh databases unless they are migrated
     * explicitly. Each step checks the current state first and is safe to re-run.
     * SQLite needs none of this: its types are dynamic and its {@code update} table
     * has always declared the composite key.</p>
     */
    private void migrateSchema() {
        if (getType() != DatabaseType.MYSQL) return;

        migrateUpdateTable();
        migratePlayerColumns();
    }

    /**
     * Widens the {@code players} columns that were originally declared too narrow:
     * {@code CurrentIP} could not hold an IPv6 address ({@code VARCHAR(15)}), and
     * {@code PlaySeconds} was an {@code INT} that silently wrapped the long value
     * written to it.
     */
    private void migratePlayerColumns() {
        String table = this.getConnectorSet().getTablePrefix() + "players";

        if (! columnMatches(table, "CurrentIP", "varchar", 45L)) {
            MessageUtils.logInfo("Widening '" + table + "'.CurrentIP to hold IPv6 addresses.");
            this.executeSingle("ALTER TABLE `" + table + "` MODIFY CurrentIP VARCHAR(45);", stmt -> {});
        }

        if (! columnMatches(table, "PlaySeconds", "bigint", null)) {
            MessageUtils.logInfo("Widening '" + table + "'.PlaySeconds to BIGINT.");
            this.executeSingle("ALTER TABLE `" + table + "` MODIFY PlaySeconds BIGINT;", stmt -> {});
        }
    }

    /**
     * Checks whether a column already has the expected type, so a migration can be
     * skipped when it has run before.
     *
     * @param table     the table to inspect
     * @param column    the column to inspect
     * @param dataType  the expected {@code DATA_TYPE}, lower-case (e.g. {@code varchar})
     * @param maxLength the expected character length, or {@code null} to ignore length
     * @return {@code true} if the column already matches, or if its state could not be
     *         determined -- in which case no migration should be attempted
     */
    private boolean columnMatches(String table, String column, String dataType, Long maxLength) {
        AtomicBoolean matches = new AtomicBoolean(true);

        this.executeQuery(
                "SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?;",
                stmt -> {
                    try {
                        stmt.setString(1, table);
                        stmt.setString(2, column);
                    } catch (Exception e) {
                        MessageUtils.logWarning("Failed to bind column lookup for " + table + "." + column, e);
                    }
                },
                rs -> {
                    try {
                        if (! rs.next()) return; // Column absent: leave it alone.

                        boolean typeOk = dataType.equalsIgnoreCase(rs.getString("DATA_TYPE"));
                        boolean lengthOk = maxLength == null
                                || maxLength == rs.getLong("CHARACTER_MAXIMUM_LENGTH");

                        matches.set(typeOk && lengthOk);
                    } catch (Exception e) {
                        MessageUtils.logWarning("Failed to read column metadata for " + table + "." + column, e);
                    }
                });

        return matches.get();
    }

    /**
     * Brings the {@code update} table on pre-existing MySQL installations in line with
     * the current schema.
     *
     * <p>The table was originally created without a primary key, so the
     * {@code ON DUPLICATE KEY UPDATE} in {@code PUT_UPDATE} could never fire and every
     * post appended a new row. {@code CREATE TABLE IF NOT EXISTS} cannot repair an
     * existing table, so the duplicate rows are collapsed (keeping the most recent per
     * {@code Type}/{@code Identifier} pair) and the composite primary key is added.</p>
     *
     * <p>Idempotent and safe to run repeatedly: it does nothing once the key exists.
     * SQLite is unaffected -- its table has always declared the key.</p>
     */
    private void migrateUpdateTable() {
        if (getType() != DatabaseType.MYSQL) return;

        String table = this.getConnectorSet().getTablePrefix() + "update";

        AtomicBoolean hasPrimaryKey = new AtomicBoolean(false);
        this.executeQuery(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = 'PRIMARY';",
                stmt -> {
                    try {
                        stmt.setString(1, table);
                    } catch (Exception e) {
                        MessageUtils.logWarning("Failed to bind table name while checking the update table", e);
                    }
                },
                rs -> {
                    try {
                        if (rs.next()) hasPrimaryKey.set(rs.getInt(1) > 0);
                        else hasPrimaryKey.set(true); // Unknown: do not attempt to migrate.
                    } catch (Exception e) {
                        MessageUtils.logWarning("Failed to read update table metadata", e);
                        hasPrimaryKey.set(true);
                    }
                });

        if (hasPrimaryKey.get()) return;

        MessageUtils.logInfo("Migrating the '" + table + "' table: removing duplicates and adding a primary key.");

        // Collapse duplicates by rebuilding the table with one row per (Type, Identifier),
        // keeping the newest PostDate. A self-join on PostDate alone would leave ties
        // behind, and InnoDB exposes no row id to break them, so the rebuild is done
        // through a grouped copy instead.
        String temp = table + "_migrating";

        this.executeSingle("DROP TABLE IF EXISTS `" + temp + "`;", stmt -> {});

        ExecutionResult dedupe = this.executeSingle(
                "CREATE TABLE `" + temp + "` (" +
                        "Type VARCHAR(191) NOT NULL, " +
                        "Identifier VARCHAR(36) NOT NULL, " +
                        "ServerUuid VARCHAR(36), " +
                        "PostDate BIGINT, " +
                        "PRIMARY KEY (Type, Identifier), " +
                        "INDEX `idx_update_ServerUuid` (ServerUuid), " +
                        "INDEX `idx_update_PostDate` (PostDate)" +
                        ") AS SELECT Type, Identifier, " +
                        "SUBSTRING_INDEX(GROUP_CONCAT(ServerUuid ORDER BY PostDate DESC), ',', 1) AS ServerUuid, " +
                        "MAX(PostDate) AS PostDate " +
                        "FROM `" + table + "` " +
                        "WHERE Type IS NOT NULL AND Identifier IS NOT NULL " +
                        "GROUP BY Type, Identifier;", stmt -> {});

        if (dedupe == ExecutionResult.ERROR) {
            MessageUtils.logWarning("Could not rebuild '" + table + "'; leaving it untouched.");
            this.executeSingle("DROP TABLE IF EXISTS `" + temp + "`;", stmt -> {});
            return;
        }

        // Timestamped so a second migration never collides with an earlier backup.
        String backup = table + "_old_" + System.currentTimeMillis();

        ExecutionResult swap = this.executeSingle(
                "RENAME TABLE `" + table + "` TO `" + backup + "`, " +
                        "`" + temp + "` TO `" + table + "`;", stmt -> {});

        if (swap == ExecutionResult.ERROR) {
            MessageUtils.logWarning("Could not swap in the migrated '" + table + "'; leaving it untouched.");
            this.executeSingle("DROP TABLE IF EXISTS `" + temp + "`;", stmt -> {});
            return;
        }

        // The pre-migration rows are kept rather than dropped, so the original data can
        // be recovered if the grouped rebuild lost anything unexpectedly. It is safe to
        // delete by hand once the migration is confirmed good.
        MessageUtils.logInfo("Migrated the '" + table + "' table successfully. "
                + "The original rows were preserved as '" + backup + "' and can be dropped once verified.");
    }

    /**
     * Persists all sections of a {@link CosmicPlayer} (main data, meta, location,
     * permissions) to the database and fires a {@link SavePlayerEvent}.
     *
     * @param player the player whose data should be saved
     * @param async  if {@code true} the save is performed on a background thread;
     *               if {@code false} the calling thread blocks until completion
     */
    public void savePlayer(CosmicPlayer player, boolean async) {
        if (async) {
            CompletableFuture.runAsync(() -> {
                savePlayerSync(player);

                new SavePlayerEvent(player).fire();
            }, getDatabaseExecutor());
        } else {
            // Run inline rather than dispatching and blocking: this path is often
            // already on a database worker, and waiting there would tie up a thread
            // for the duration of another worker's task.
            savePlayerSync(player);

            new SavePlayerEvent(player).fire();
        }
    }

    /**
     * Asynchronously persists all sections of a {@link CosmicPlayer} to the database
     * and fires a {@link SavePlayerEvent}. Equivalent to
     * {@link #savePlayer(CosmicPlayer, boolean)} with {@code async = true}.
     *
     * @param player the player whose data should be saved
     */
    public void savePlayer(CosmicPlayer player) {
        savePlayer(player, true);
    }

    /**
     * Checks whether the given player UUID has ever had their {@code ProxyTouched}
     * flag set in the database.
     *
     * @param uuid the player UUID to check
     * @return {@code true} if the player is recorded as proxy-touched (or the check
     *         could not be executed); {@code false} if they are not
     */
    public boolean isPlayerTouched(String uuid) {
        ensureUsable();

        String s1 = Statements.getStatement(Statements.StatementType.PLAYER_IS_TOUCHED, this.getConnectorSet());
        if (s1 == null) return true;
        if (s1.isBlank() || s1.isEmpty()) return true;

        AtomicBoolean atomicBoolean = new AtomicBoolean(true);

        this.executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, rs -> {
            try {
                if (rs.next()) {
                    boolean isTouched = rs.getBoolean("ProxyTouched");

                    atomicBoolean.set(isTouched);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return atomicBoolean.get();
    }

    /**
     * Writes main, meta, location, and permissions rows for a {@link CosmicPlayer}
     * to the database and posts a cross-server player update signal.
     *
     * @param player the player to persist
     * @return a {@link CompletableFuture} that resolves to {@code true} on success
     *         or {@code false} if any statement was unavailable
     */
    private CompletableFuture<Boolean> savePlayerAsync(CosmicPlayer player) {
        return CompletableFuture.supplyAsync(() -> savePlayerSync(player), getDatabaseExecutor());
    }

    /**
     * Writes main, meta, location, and permissions rows for a {@link CosmicPlayer} on
     * the calling thread and posts a cross-server player update signal.
     *
     * @param player the player to persist
     * @return {@code true} on success, or {@code false} if any statement was unavailable
     */
    private boolean savePlayerSync(CosmicPlayer player) {
        {
            String s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_MAIN, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, player.getUuid());
                    stmt.setLong(2, player.getFirstJoinDate().getTime());
                    stmt.setLong(3, player.getLastJoinDate().getTime());
                    stmt.setString(4, player.getCurrentName());
                    stmt.setString(5, player.getCurrentIp());
                    stmt.setLong(6, player.getPlaySeconds());
                    stmt.setBoolean(7, player.isProxyTouched());

                    // Repeat everything except the Uuid parameter for MySql.
                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setLong(8, player.getFirstJoinDate().getTime());
                        stmt.setLong(9, player.getLastJoinDate().getTime());
                        stmt.setString(10, player.getCurrentName());
                        stmt.setString(11, player.getCurrentIp());
                        stmt.setLong(12, player.getPlaySeconds());
                        stmt.setBoolean(13, player.isProxyTouched());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if (player.getMeta() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_META, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getMeta().getNickname());
                        stmt.setString(3, player.getMeta().getPrefix());
                        stmt.setString(4, player.getMeta().getSuffix());

                        // Repeat everything except the Uuid parameter (which is the first parameter) for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setString(5, player.getMeta().getNickname());
                            stmt.setString(6, player.getMeta().getPrefix());
                            stmt.setString(7, player.getMeta().getSuffix());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            if (player.getLocation() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_LOCATION, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, player.getUuid());
                        stmt.setString(2, player.getLocation().getServerName());
                        stmt.setString(3, player.getLocation().getWorldName());
                        stmt.setDouble(4, player.getLocation().getX());
                        stmt.setDouble(5, player.getLocation().getY());
                        stmt.setDouble(6, player.getLocation().getZ());
                        stmt.setFloat(7, player.getLocation().getYaw());
                        stmt.setFloat(8, player.getLocation().getPitch());

                        // Repeat everything except the Uuid parameter for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setString(9, player.getLocation().getServerName());
                            stmt.setString(10, player.getLocation().getWorldName());
                            stmt.setDouble(11, player.getLocation().getX());
                            stmt.setDouble(12, player.getLocation().getY());
                            stmt.setDouble(13, player.getLocation().getZ());
                            stmt.setFloat(14, player.getLocation().getYaw());
                            stmt.setFloat(15, player.getLocation().getPitch());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            if (player.getPermissions() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_PERMISSIONS, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, player.getUuid());
                        stmt.setBoolean(2, player.getPermissions().isBypassingPermissions());

                        // Repeat everything except the Uuid parameter for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setBoolean(3, player.getPermissions().isBypassingPermissions());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            DefaultUpdaters.getPlayerUpdater().update(player.getUuid());

            return true;
        }
    }

    /**
     * Persists a {@link CosmicSender} (non-player) to the database and fires a
     * {@link SaveSenderEvent}. If the sender is actually a {@link CosmicPlayer},
     * this delegates to {@link #savePlayer(CosmicPlayer, boolean)}.
     *
     * @param sender the sender whose data should be saved
     * @param async  if {@code true} the save runs on a background thread;
     *               if {@code false} the calling thread blocks until completion
     */
    public void saveSender(CosmicSender sender, boolean async) {
        if (sender instanceof CosmicPlayer) {
            savePlayer((CosmicPlayer) sender, async);
            return;
        }

        if (async) {
            CompletableFuture.runAsync(() -> {
                saveSenderSync(sender);

                new SaveSenderEvent(sender).fire();
            }, getDatabaseExecutor());
        } else {
            // Run inline rather than dispatching and blocking; see savePlayer.
            saveSenderSync(sender);

            new SaveSenderEvent(sender).fire();
        }
    }

    /**
     * Asynchronously persists a {@link CosmicSender} (non-player) to the database
     * and fires a {@link SaveSenderEvent}. Equivalent to
     * {@link #saveSender(CosmicSender, boolean)} with {@code async = true}.
     *
     * @param sender the sender whose data should be saved
     */
    public void saveSender(CosmicSender sender) {
        saveSender(sender, true);
    }

    /**
     * Writes main, meta, location (zeroed-out), and permissions rows for a
     * {@link CosmicSender} (console or non-player entity) to the database, then
     * signals a cross-server update.
     *
     * @param sender the sender to persist
     * @return a {@link CompletableFuture} resolving to {@code true} on success or
     *         {@code false} if any statement was unavailable
     */
    private CompletableFuture<Boolean> saveSenderAsync(CosmicSender sender) {
        return CompletableFuture.supplyAsync(() -> saveSenderSync(sender), getDatabaseExecutor());
    }

    /**
     * Writes main, meta, location (zeroed-out), and permissions rows for a
     * {@link CosmicSender} on the calling thread, then signals a cross-server update.
     *
     * @param sender the sender to persist
     * @return {@code true} on success, or {@code false} if any statement was unavailable
     */
    private boolean saveSenderSync(CosmicSender sender) {
        {
            String s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_MAIN, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, sender.getUuid());
                    stmt.setLong(2, sender.getFirstJoinDate().getTime());
                    stmt.setLong(3, sender.getLastJoinDate().getTime());
                    stmt.setString(4, sender.getCurrentName());
                    stmt.setString(5, "--null");
                    stmt.setLong(6, sender.getPlaySeconds());
                    stmt.setBoolean(7, sender.isProxyTouched());

                    // Repeat everything except the Uuid parameter for MySql.
                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setLong(8, sender.getFirstJoinDate().getTime());
                        stmt.setLong(9, sender.getLastJoinDate().getTime());
                        stmt.setString(10, sender.getCurrentName());
                        stmt.setString(11, "--null");
                        stmt.setLong(12, sender.getPlaySeconds());
                        stmt.setBoolean(13, sender.isProxyTouched());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if (sender.getMeta() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_META, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, sender.getUuid());
                        stmt.setString(2, sender.getMeta().getNickname());
                        stmt.setString(3, sender.getMeta().getPrefix());
                        stmt.setString(4, sender.getMeta().getSuffix());

                        // Repeat everything except the Uuid parameter (which is the first parameter) for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setString(5, sender.getMeta().getNickname());
                            stmt.setString(6, sender.getMeta().getPrefix());
                            stmt.setString(7, sender.getMeta().getSuffix());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            // A non-player sender has no real location; a zeroed placeholder row is
            // written so the location table always has a matching entry.
            {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_LOCATION, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, sender.getUuid());
                        stmt.setString(2, "--null");
                        stmt.setString(3, "--null");
                        stmt.setDouble(4, 0d);
                        stmt.setDouble(5, 0d);
                        stmt.setDouble(6, 0d);
                        stmt.setFloat(7, 0f);
                        stmt.setFloat(8, 0f);

                        // Repeat everything except the Uuid parameter for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setString(9, "--null");
                            stmt.setString(10, "--null");
                            stmt.setDouble(11, 0d);
                            stmt.setDouble(12, 0d);
                            stmt.setDouble(13, 0d);
                            stmt.setFloat(14, 0f);
                            stmt.setFloat(15, 0f);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            if (sender.getPermissions() != null) {
                s1 = Statements.getStatement(Statements.StatementType.PUSH_PLAYER_PERMISSIONS, this.getConnectorSet());
                if (s1 == null) return false;
                if (s1.isBlank() || s1.isEmpty()) return false;

                this.execute(s1, stmt -> {
                    try {
                        stmt.setString(1, sender.getUuid());
                        stmt.setBoolean(2, sender.getPermissions().isBypassingPermissions());

                        // Repeat everything except the Uuid parameter for MySql.
                        if (getType() == DatabaseType.MYSQL) {
                            stmt.setBoolean(3, sender.getPermissions().isBypassingPermissions());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            DefaultUpdaters.getPlayerUpdater().update(sender.getUuid());

            return true;
        }
    }

    /**
     * Asynchronously loads a {@link CosmicPlayer} from the database by UUID,
     * creating a new default record if no existing entry is found. Concurrent
     * requests for the same UUID are deduplicated via the {@link #loadingPlayers}
     * cache.
     *
     * @param uuid the player UUID to load
     * @return a {@link CompletableFuture} resolving to an {@link Optional} containing
     *         the loaded (or newly created) player, or empty if the load statements
     *         are unavailable
     */
    public CompletableFuture<Optional<CosmicSender>> loadPlayer(String uuid) {
        // get(key, mappingFunction) is atomic, so concurrent callers for the same uuid
        // share a single load rather than racing as they did with getIfPresent-then-put.
        // The entry is evicted as soon as the load finishes: this cache exists only to
        // collapse in-flight duplicates, never to serve results. Holding completed
        // loads would make reload() and the cross-server updater read stale data.
        CompletableFuture<Optional<CosmicSender>> future =
                getLoadingPlayers().get(uuid, (key, executor) -> CompletableFuture.supplyAsync(() -> {
                ensureUsable();

                if (! existsSync(uuid)) {
                    CosmicPlayer player = UserUtils.createPlayer(uuid);
                    savePlayer(player, false);

                    return Optional.of(player);
                }

                CosmicPlayer player = new CosmicPlayer(uuid);

                String s1 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_MAIN, this.getConnectorSet());
                if (s1 == null) return Optional.empty();
                if (s1.isBlank() || s1.isEmpty()) return Optional.empty();

                this.executeQuery(s1, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.setFirstJoinMillis(rs.getLong("FirstJoin"));
                            player.setLastJoinMillis(rs.getLong("LastJoin"));
                            player.setCurrentName(rs.getString("CurrentName"));
                            player.setCurrentIp(rs.getString("CurrentIP"));
                            player.setPlaySeconds(rs.getLong("PlaySeconds"));
                            player.setProxyTouched(rs.getBoolean("ProxyTouched"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String s2 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_META, this.getConnectorSet());
                if (s2 == null) return Optional.empty();
                if (s2.isBlank() || s2.isEmpty()) return Optional.empty();

                this.executeQuery(s2, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.getMeta().setNickname(rs.getString("Nickname"));
                            player.getMeta().setPrefix(rs.getString("Prefix"));
                            player.getMeta().setSuffix(rs.getString("Suffix"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String s4 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_LOCATION, this.getConnectorSet());
                if (s4 == null) return Optional.empty();
                if (s4.isBlank() || s4.isEmpty()) return Optional.empty();

                this.executeQuery(s4, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.getLocation().setServerName(rs.getString("Server"));
                            player.getLocation().setWorldName(rs.getString("World"));
                            player.getLocation().setX(rs.getDouble("X"));
                            player.getLocation().setY(rs.getDouble("Y"));
                            player.getLocation().setZ(rs.getDouble("Z"));
                            player.getLocation().setYaw(rs.getFloat("Yaw"));
                            player.getLocation().setPitch(rs.getFloat("Pitch"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String s5 = Statements.getStatement(Statements.StatementType.PULL_PLAYER_PERMISSIONS, this.getConnectorSet());
                if (s5 == null) return Optional.empty();
                if (s5.isBlank() || s5.isEmpty()) return Optional.empty();

                this.executeQuery(s5, stmt -> {
                    try {
                        stmt.setString(1, uuid);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, rs -> {
                    try {
                        if (rs.next()) {
                            player.getPermissions().setBypassingPermissions(rs.getBoolean("BypassingPermissions"));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                return Optional.of(player);
            }, getDatabaseExecutor()));

        // Evict once complete, so the next caller re-reads the database. Done here
        // rather than inside the supplier, which would evict the entry while the
        // cache was still populating it.
        return future.whenComplete((result, error) -> getLoadingPlayers().synchronous().invalidate(uuid));
    }

    /**
     * Asynchronously checks whether a player record with the given UUID already
     * exists in the main player table.
     *
     * @param uuid the player UUID to query
     * @return a {@link CompletableFuture} resolving to {@code true} if the player
     *         exists, or {@code false} otherwise
     */
    public CompletableFuture<Boolean> exists(String uuid) {
        return CompletableFuture.supplyAsync(() -> existsSync(uuid), getDatabaseExecutor());
    }

    /**
     * Checks on the calling thread whether a player record with the given UUID exists.
     *
     * <p>Used by operations that are already running on a database worker thread, so
     * they do not block one worker while waiting on another.</p>
     *
     * @param uuid the player UUID to query
     * @return {@code true} if the player exists, {@code false} otherwise
     */
    public boolean existsSync(String uuid) {
        {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PLAYER_EXISTS, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to bind uuid for exists check", e);
                }
            }, rs -> {
                try {
                    // PLAYER_EXISTS is a SELECT EXISTS(...) -- it always returns exactly one
                    // row holding 0 or 1, so the row's value must be read. Testing rs.next()
                    // alone would always be true and report every player as existing.
                    if (rs.next()) atomicBoolean.set(rs.getBoolean(1));
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to read exists result", e);
                    atomicBoolean.set(false);
                }
            });

            return atomicBoolean.get();
        }
    }

    /**
     * Asynchronously persists a {@link UuidInfo} record (UUID, usernames, IPs) to
     * the database.
     *
     * @param uuidInfo the UUID info to save
     * @return a {@link CompletableFuture} resolving to {@code true} on success or
     *         {@code false} if the statement was unavailable
     */
    public CompletableFuture<Boolean> saveUuidInfo(UuidInfo uuidInfo) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUSH_UUID_INFO, this.getConnectorSet());
            if (s1 == null) return false;
            if (s1.isBlank() || s1.isEmpty()) return false;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, uuidInfo.getUuid());
                    stmt.setString(2, uuidInfo.computableNames());
                    stmt.setString(3, uuidInfo.computableIps());

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(4, uuidInfo.computableNames());
                        stmt.setString(5, uuidInfo.computableIps());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return true;
        }, getDatabaseExecutor());
    }

    /**
     * Asynchronously loads the {@link UuidInfo} record for the given UUID from the
     * database.
     *
     * @param uuid the player UUID to query
     * @return a {@link CompletableFuture} resolving to an {@link Optional} containing
     *         the found record, or empty if no entry exists
     */
    public CompletableFuture<Optional<UuidInfo>> loadUuidInfo(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_UUID_INFO, this.getConnectorSet());
            if (s1 == null) return Optional.empty();
            if (s1.isBlank() || s1.isEmpty()) return Optional.empty();

            AtomicReference<Optional<UuidInfo>> uuidInfo = new AtomicReference<>(Optional.empty());
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        String names = rs.getString("Usernames");
                        String ips = rs.getString("Ips");

                        UuidInfo info = new UuidInfo(uuid, names, ips);

                        uuidInfo.set(Optional.of(info));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return uuidInfo.get();
        }, getDatabaseExecutor());
    }

    /**
     * Asynchronously loads every {@link UuidInfo} row from the database.
     *
     * @return a {@link CompletableFuture} resolving to a thread-safe set containing
     *         all UUID info records; never {@code null} but may be empty
     */
    public CompletableFuture<ConcurrentSkipListSet<UuidInfo>> pullAllUuidInfo() {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_ALL_UUID_INFO, this.getConnectorSet());
            if (s1 == null) return new ConcurrentSkipListSet<>();
            if (s1.isBlank() || s1.isEmpty()) return new ConcurrentSkipListSet<>();

            AtomicReference<ConcurrentSkipListSet<UuidInfo>> uuids = new AtomicReference<>(new ConcurrentSkipListSet<>());
            this.executeQuery(s1, stmt -> {}, rs -> {
                try {
                    while (rs.next()) {
                        String uuid = rs.getString("Uuid");
                        String names = rs.getString("Usernames");
                        String ips = rs.getString("Ips");

                        UuidInfo info = new UuidInfo(uuid, names, ips);

                        uuids.get().add(info);
                    }
                } catch (SQLException e) {
                    if (e.getMessage().contains("duplicate")) return;
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return uuids.get();
        }, getDatabaseExecutor());
    }

    /**
     * Fires-and-forgets an asynchronous task that removes the pending update record
     * for the given update type and resource identifier from the database.
     *
     * @param updateType the update type whose record should be cleared
     * @param identifier the resource identifier to clear
     */
    public void clearUpdateAsync(UpdateType<?> updateType, String identifier) {
        CompletableFuture.runAsync(() -> clearUpdate(updateType, identifier).join(), getDatabaseExecutor());
    }

    /**
     * Asynchronously removes the pending update record for the given update type and
     * resource identifier from the database.
     *
     * @param updateType the update type whose record should be removed
     * @param identifier the resource identifier to clear
     * @return a {@link CompletableFuture} that completes when the deletion is done
     */
    public CompletableFuture<Void> clearUpdate(UpdateType<?> updateType, String identifier) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.CLEAR_UPDATE, this.getConnectorSet());

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, updateType.getIdentifier());
                    stmt.setString(2, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, getDatabaseExecutor());
    }

    /**
     * Fires-and-forgets an asynchronous task that writes a new update record for the
     * given update type and resource identifier, stamped with the current server UUID
     * and timestamp.
     *
     * @param updateType the update type to post an update for
     * @param identifier the resource identifier that changed
     */
    public void postUpdateAsync(UpdateType<?> updateType, String identifier) {
        CompletableFuture.runAsync(() -> postUpdate(updateType, identifier).join(), getDatabaseExecutor());
    }

    /**
     * Asynchronously writes a new update record for the given update type and
     * resource identifier, stamped with the current server UUID and the current
     * system time.
     *
     * @param updateType the update type to post an update for
     * @param identifier the resource identifier that changed
     * @return a {@link CompletableFuture} that completes when the insert is done
     */
    public CompletableFuture<Void> postUpdate(UpdateType<?> updateType, String identifier) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUT_UPDATE, this.getConnectorSet());

            SavedServer server = GivenConfigs.getServer();

            long time = System.currentTimeMillis();

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, updateType.getIdentifier());
                    stmt.setString(2, identifier);
                    stmt.setString(3, server.getIdentifier());
                    stmt.setLong(4, time);

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(5, server.getIdentifier());
                        stmt.setLong(6, time);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, getDatabaseExecutor());
    }

    /**
     * Asynchronously queries the update table for the most recent record matching
     * the given update type and resource identifier.
     *
     * @param updateType the update type to check
     * @param identifier the resource identifier to look up
     * @return a {@link CompletableFuture} resolving to an {@link Optional} containing
     *         the latest {@link UpdateInfo}, or empty if no record exists
     */
    public CompletableFuture<Optional<UpdateInfo>> checkUpdate(UpdateType<?> updateType, String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.CHECK_UPDATE, this.getConnectorSet());

            AtomicReference<Optional<UpdateInfo>> date = new AtomicReference<>(Optional.empty());

            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, updateType.getIdentifier());
                    stmt.setString(2, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        try {
                            String serverUuid = rs.getString("ServerUuid");
                            long time = rs.getLong("PostDate");
                            Date d = new Date(time);

                            UpdateInfo info = new UpdateInfo(d, serverUuid);

                            date.set(Optional.of(info));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return date.get();
        }, getDatabaseExecutor());
    }

    /**
     * Fires-and-forgets an asynchronous task that upserts a {@link SavedServer} row
     * in the servers table.
     *
     * @param server the server to persist
     */
    public void putServerAsync(SavedServer server) {
        CompletableFuture.runAsync(() -> putServer(server).join(), getDatabaseExecutor());
    }

    /**
     * Asynchronously upserts a {@link SavedServer} row (UUID, name, type) in the
     * servers table.
     *
     * @param server the server to persist
     * @return a {@link CompletableFuture} that completes when the upsert is done
     */
    public CompletableFuture<Void> putServer(SavedServer server) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUT_SERVER, this.getConnectorSet());

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, server.getIdentifier());
                    stmt.setString(2, server.getName());
                    stmt.setString(3, server.getType().name());

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(4, server.getName());
                        stmt.setString(5, server.getType().name());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, getDatabaseExecutor());
    }

    /**
     * Asynchronously loads a {@link SavedServer} record from the database by its
     * UUID string.
     *
     * @param uuid the server UUID to query
     * @return a {@link CompletableFuture} resolving to an {@link Optional} containing
     *         the server if found, or empty otherwise
     */
    public CompletableFuture<Optional<SavedServer>> pullServer(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_SERVER, this.getConnectorSet());
            if (s1.isBlank()) return Optional.empty();

            AtomicReference<Optional<SavedServer>> server = new AtomicReference<>(Optional.empty());
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        try {
                            String name = rs.getString("Name");
                            String type = rs.getString("Type");

                            ISingularityExtension.ServerType t = ISingularityExtension.ServerType.valueOf(type);

                            SavedServer s = new SavedServer(uuid, name, t);

                            server.set(Optional.of(s));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return server.get();
        }, getDatabaseExecutor());
    }

    /**
     * Fires-and-forgets an asynchronous task that upserts a {@link TPTicket} row in
     * the teleportation tickets table.
     *
     * @param ticket the teleportation ticket to persist
     */
    public void postTPTicketAsync(TPTicket ticket) {
        CompletableFuture.runAsync(() -> postTPTicket(ticket).join(), getDatabaseExecutor());
    }

    /**
     * Asynchronously upserts a {@link TPTicket} (player UUID, target server, world,
     * coordinates, rotation, and creation timestamp) in the teleportation tickets
     * table.
     *
     * @param ticket the teleportation ticket to persist
     * @return a {@link CompletableFuture} that completes when the upsert is done
     */
    public CompletableFuture<Void> postTPTicket(TPTicket ticket) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PUT_TP_TICKET, this.getConnectorSet());
            if (s1.isBlank()) return;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, ticket.getIdentifier());
                    stmt.setString(2, ticket.getTargetServer().getIdentifier());
                    stmt.setString(3, ticket.getTargetWorld().getIdentifier());
                    stmt.setDouble(4, ticket.getTargetLocation().getX());
                    stmt.setDouble(5, ticket.getTargetLocation().getY());
                    stmt.setDouble(6, ticket.getTargetLocation().getZ());
                    stmt.setFloat(7, ticket.getTargetRotation().getYaw());
                    stmt.setFloat(8, ticket.getTargetRotation().getPitch());
                    stmt.setLong(9, ticket.getCreateDate().getTime());

                    if (getType() == DatabaseType.MYSQL) {
                        stmt.setString(10, ticket.getTargetServer().getIdentifier());
                        stmt.setString(11, ticket.getTargetWorld().getIdentifier());
                        stmt.setDouble(12, ticket.getTargetLocation().getX());
                        stmt.setDouble(13, ticket.getTargetLocation().getY());
                        stmt.setDouble(14, ticket.getTargetLocation().getZ());
                        stmt.setFloat(15, ticket.getTargetRotation().getYaw());
                        stmt.setFloat(16, ticket.getTargetRotation().getPitch());
                        stmt.setLong(17, ticket.getCreateDate().getTime());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, getDatabaseExecutor());
    }

    /**
     * Fires-and-forgets an asynchronous task that deletes the teleportation ticket
     * for the given player UUID.
     *
     * @param uuid the player UUID whose ticket should be removed
     */
    public void clearTPTicketAsync(String uuid) {
        CompletableFuture.runAsync(() -> clearTPTicket(uuid).join(), getDatabaseExecutor());
    }

    /**
     * Asynchronously deletes the teleportation ticket associated with the given
     * player UUID from the database.
     *
     * @param uuid the player UUID whose ticket should be removed
     * @return a {@link CompletableFuture} that completes when the deletion is done
     */
    public CompletableFuture<Void> clearTPTicket(String uuid) {
        return CompletableFuture.runAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.CLEAR_TP_TICKET, this.getConnectorSet());
            if (s1.isBlank()) return;

            this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, getDatabaseExecutor());
    }

    /**
     * Asynchronously retrieves the pending {@link TPTicket} for the given player
     * UUID, if one exists.
     *
     * @param uuid the player UUID to query
     * @return a {@link CompletableFuture} resolving to an {@link Optional} containing
     *         the ticket, or empty if none is pending
     */
    public CompletableFuture<Optional<TPTicket>> getTPTicket(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_TP_TICKET, this.getConnectorSet());
            if (s1.isBlank()) return Optional.empty();

            AtomicReference<Optional<TPTicket>> ticket = new AtomicReference<>(Optional.empty());
            this.executeQuery(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, rs -> {
                try {
                    if (rs.next()) {
                        try {
                            String serverName = rs.getString("ServerName");
                            String worldName = rs.getString("WorldName");
                            double x = rs.getDouble("X");
                            double y = rs.getDouble("Y");
                            double z = rs.getDouble("Z");
                            float yaw = rs.getFloat("Yaw");
                            float pitch = rs.getFloat("Pitch");
                            long time = rs.getLong("PostDate");

                            CosmicServer server = new CosmicServer(serverName);
                            PlayerWorld world = new PlayerWorld(worldName);
                            WorldPosition position = new WorldPosition(x, y, z);
                            PlayerRotation rotation = new PlayerRotation(yaw, pitch);

                            Date d = new Date(time);

                            TPTicket t = new TPTicket(uuid, server, world, position, rotation, d);

                            ticket.set(Optional.of(t));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            return ticket.get();
        }, getDatabaseExecutor());
    }

    /**
     * Asynchronously loads every {@link TPTicket} row from the database.
     * Logs the count of collected tickets at debug level when results are present.
     *
     * @return a {@link CompletableFuture} resolving to a thread-safe set containing
     *         all pending teleportation tickets; never {@code null} but may be empty
     */
    public CompletableFuture<ConcurrentSkipListSet<TPTicket>> pullAllTPTickets() {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.PULL_ALL_TP_TICKETS, this.getConnectorSet());
            if (s1.isBlank()) return new ConcurrentSkipListSet<>();

            ConcurrentSkipListSet<TPTicket> tickets = new ConcurrentSkipListSet<>();
            this.executeQuery(s1, stmt -> {}, rs -> {
                try {
                    while (rs.next()) {
                        try {
                            String uuid = rs.getString("Uuid");
                            String serverName = rs.getString("ServerName");
                            String worldName = rs.getString("WorldName");
                            double x = rs.getDouble("X");
                            double y = rs.getDouble("Y");
                            double z = rs.getDouble("Z");
                            float yaw = rs.getFloat("Yaw");
                            float pitch = rs.getFloat("Pitch");
                            long time = rs.getLong("PostDate");

                            CosmicServer server = new CosmicServer(serverName);
                            PlayerWorld world = new PlayerWorld(worldName);
                            WorldPosition position = new WorldPosition(x, y, z);
                            PlayerRotation rotation = new PlayerRotation(yaw, pitch);

                            Date d = new Date(time);

                            TPTicket t = new TPTicket(uuid, server, world, position, rotation, d);

                            tickets.add(t);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            if (! tickets.isEmpty()) MessageUtils.logDebug("Collected " + tickets.size() + " teleportation tickets.");

            return tickets;
        }, getDatabaseExecutor());
    }

    /**
     * Asynchronously deletes all database rows associated with the given player UUID.
     * Equivalent to {@link #delete(String, boolean)} with {@code async = true}.
     *
     * @param uuid the player UUID whose data should be removed
     */
    public void delete(String uuid) {
        delete(uuid, true);
    }

    /**
     * Deletes all database rows associated with the given player UUID.
     *
     * @param uuid  the player UUID whose data should be removed
     * @param async if {@code true} the deletion runs on a background thread;
     *              if {@code false} the calling thread blocks until completion
     */
    public void delete(String uuid, boolean async) {
        if (async) {
            CompletableFuture.runAsync(() -> deletePlayer(uuid).join(), getDatabaseExecutor());
        } else {
            deletePlayer(uuid).join();
        }
    }

    /**
     * Asynchronously removes all rows for the given player UUID from every player
     * table (main, meta, location, permissions, and any others covered by the
     * {@code DROP_PLAYER} statement).
     *
     * @param uuid the player UUID to delete
     * @return a {@link CompletableFuture} resolving to {@code true} if the deletion
     *         succeeded, or {@code false} if the statement was blank or no rows were
     *         affected
     */
    public CompletableFuture<Boolean> deletePlayer(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureUsable();

            String s1 = Statements.getStatement(Statements.StatementType.DROP_PLAYER, this.getConnectorSet());
            if (s1 == null || s1.isBlank()) return false;

            // DROP_PLAYER is a ';;'-delimited batch of DELETEs, so it must go through
            // execute() to be split. Each sub-statement takes a single uuid parameter;
            // routing it through executeQuery() would both fail to split the batch and
            // run DML through executeQuery(), which drivers reject.
            List<ExecutionResult> results = this.execute(s1, stmt -> {
                try {
                    stmt.setString(1, uuid);
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to bind uuid for player deletion", e);
                }
            });

            boolean success = ! results.isEmpty() && results.stream().noneMatch(r -> r == ExecutionResult.ERROR);

            if (success) MessageUtils.logDebug("Deleted player data for " + uuid + ".");

            return success;
        }, getDatabaseExecutor());
    }
}
