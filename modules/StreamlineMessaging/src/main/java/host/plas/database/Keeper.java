package host.plas.database;

import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.savables.SavableChatter;
import net.streamline.api.SLAPI;
import singularity.database.DatabaseType;
import singularity.database.modules.DBKeeper;
import singularity.utils.MessageUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Keeper extends DBKeeper<SavableChatter> {
    /** Set once the MySQL column and charset upgrades have been checked for this keeper. */
    private final AtomicBoolean mysqlMigrated = new AtomicBoolean(false);

    public Keeper() {
        super("chatters", SavableChatter::new);
    }

    @Override
    public void ensureMysqlTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_main` (" +
                "`Uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "`CurrentChannel` TEXT NOT NULL, " +
                "`ReplyToUuid` VARCHAR(36) NOT NULL, " +
                "`LastMessage` TEXT NOT NULL, " +
                "`LastMessageSent` TEXT NOT NULL, " +
                "`LastMessageReceived` TEXT NOT NULL, " +
                "`AcceptingFriendRequests` BIT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%channel_views` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`Uuid` VARCHAR(36) NOT NULL, " +
                "`Channel` TEXT NOT NULL, " +
                "`Viewed` BIT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_friends` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`FriendUuid` VARCHAR(36) NOT NULL, " +
                "`DateAccepted` BIGINT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_ignores` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`IgnoreUuid` VARCHAR(36) NOT NULL, " +
                "`DateIgnored` BIGINT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%friend_invites` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`FriendUuid` VARCHAR(36) NOT NULL, " +
                "`TicksLeft` BIGINT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%best_friends` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`FriendUuid` VARCHAR(36) NOT NULL, " +
                "`DateSet` BIGINT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, smt -> {});

        if (mysqlMigrated.compareAndSet(false, true)) migrateMysql();
    }

    /**
     * Upgrades tables created by earlier releases of this module.
     *
     * <p>The date columns were {@code INT}, which cannot hold a millisecond timestamp, and
     * the tables were 3-byte {@code utf8}, which rejects any message containing an emoji.
     * Each change is checked against {@code INFORMATION_SCHEMA} first, since an
     * {@code ALTER TABLE} rebuilds the table.</p>
     */
    private void migrateMysql() {
        String[][] dateColumns = {
                { "chatter_friends", "DateAccepted" },
                { "chatter_ignores", "DateIgnored" },
                { "friend_invites", "TicksLeft" },
                { "best_friends", "DateSet" },
        };
        for (String[] column : dateColumns) {
            String type = columnType(table(column[0]), column[1]);
            if (type != null && ! type.equalsIgnoreCase("bigint")) {
                getDatabase().executeSingle("ALTER TABLE `" + table(column[0]) + "` MODIFY `" + column[1] + "` BIGINT NOT NULL;", stmt -> {});
            }
        }

        String[] tables = { "chatter_main", "channel_views", "chatter_friends", "chatter_ignores", "friend_invites", "best_friends" };
        for (String name : tables) {
            String collation = tableCollation(table(name));
            if (collation != null && ! collation.toLowerCase().startsWith("utf8mb4")) {
                getDatabase().executeSingle("ALTER TABLE `" + table(name) + "` CONVERT TO CHARACTER SET utf8mb4;", stmt -> {});
            }
        }
    }

    private String columnType(String table, String column) {
        AtomicReference<String> type = new AtomicReference<>();
        getDatabase().executeQuery("SELECT `DATA_TYPE` FROM `INFORMATION_SCHEMA`.`COLUMNS` " +
                "WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = ? AND `COLUMN_NAME` = ?;", stmt -> {
            try {
                stmt.setString(1, table);
                stmt.setString(2, column);
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to bind column lookup", e);
            }
        }, result -> {
            try {
                if (result.next()) type.set(result.getString(1));
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to read the type of " + table + "." + column, e);
            }
        });
        return type.get();
    }

    private String tableCollation(String table) {
        AtomicReference<String> collation = new AtomicReference<>();
        getDatabase().executeQuery("SELECT `TABLE_COLLATION` FROM `INFORMATION_SCHEMA`.`TABLES` " +
                "WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = ?;", stmt -> {
            try {
                stmt.setString(1, table);
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to bind table lookup", e);
            }
        }, result -> {
            try {
                if (result.next()) collation.set(result.getString(1));
            } catch (Exception e) {
                MessageUtils.logWarning("Failed to read the collation of " + table, e);
            }
        });
        return collation.get();
    }

    private String table(String name) {
        return SLAPI.getMainDatabase().getConnectorSet().getTablePrefix() + name;
    }

    @Override
    public void ensureSqliteTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_main` (" +
                "`Uuid` TEXT NOT NULL PRIMARY KEY, " +
                "`CurrentChannel` TEXT NOT NULL, " +
                "`ReplyToUuid` TEXT NOT NULL, " +
                "`LastMessage` TEXT NOT NULL, " +
                "`LastMessageSent` TEXT NOT NULL, " +
                "`LastMessageReceived` TEXT NOT NULL, " +
                "`AcceptingFriendRequests` BOOLEAN NOT NULL " +
                ");;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%channel_views` (" +
                "`Id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`Uuid` TEXT NOT NULL, " +
                "`Channel` TEXT NOT NULL, " +
                "`Viewed` BOOLEAN NOT NULL " +
                ");;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_friends` (" +
                "`Id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`PlayerUuid` TEXT NOT NULL, " +
                "`FriendUuid` TEXT NOT NULL, " +
                "`DateAccepted` INTEGER NOT NULL " +
                ");;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_ignores` (" +
                "`Id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`PlayerUuid` TEXT NOT NULL, " +
                "`IgnoreUuid` TEXT NOT NULL, " +
                "`DateIgnored` INTEGER NOT NULL " +
                ");;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%friend_invites` (" +
                "`Id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`PlayerUuid` TEXT NOT NULL, " +
                "`FriendUuid` TEXT NOT NULL, " +
                "`TicksLeft` INTEGER NOT NULL " +
                ");;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%best_friends` (" +
                "`Id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`PlayerUuid` TEXT NOT NULL, " +
                "`FriendUuid` TEXT NOT NULL, " +
                "`DateSet` INTEGER NOT NULL " +
                ");;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, smt -> {});
    }

    @Override
    public void saveMysql(SavableChatter obj) {
        saveBoth(obj, "INSERT INTO `%table_prefix%chatter_main` " +
                "(`Uuid`, `CurrentChannel`, `ReplyToUuid`, `LastMessage`, `LastMessageSent`, `LastMessageReceived`, `AcceptingFriendRequests`) " +
                "VALUES ( ?, ?, ?, ?, ?, ?, ? ) " +
                "ON DUPLICATE KEY UPDATE " +
                "`CurrentChannel` = VALUES(`CurrentChannel`), " +
                "`ReplyToUuid` = VALUES(`ReplyToUuid`), " +
                "`LastMessage` = VALUES(`LastMessage`), " +
                "`LastMessageSent` = VALUES(`LastMessageSent`), " +
                "`LastMessageReceived` = VALUES(`LastMessageReceived`), " +
                "`AcceptingFriendRequests` = VALUES(`AcceptingFriendRequests`);");
    }

    @Override
    public void saveSqlite(SavableChatter obj) {
        saveBoth(obj, "INSERT OR REPLACE INTO `%table_prefix%chatter_main` " +
                "(`Uuid`, `CurrentChannel`, `ReplyToUuid`, `LastMessage`, `LastMessageSent`, `LastMessageReceived`, `AcceptingFriendRequests`) " +
                "VALUES ( ?, ?, ?, ?, ?, ?, ? );");
    }

    /**
     * Writes the chatter's main row and replaces every one of its rows in the satellite
     * tables, in one transaction.
     *
     * <p>The satellite tables are keyed by an auto-increment id only, so an upsert can never
     * match an existing row; the owner's rows are deleted and re-inserted instead, which is
     * also how removed friends, ignores and invites leave the database. Running it as one
     * transaction on one connection keeps a concurrent load -- another server, or a quick
     * rejoin -- from reading the table between the delete and the inserts.</p>
     */
    private void saveBoth(SavableChatter obj, String mainUpsert) {
        String uuid = obj.getUuid();

        try (Connection connection = getDatabase().getConnection()) {
            if (connection == null) {
                MessageUtils.logWarning("Could not obtain a connection to save chatter " + uuid);
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(mainUpsert))) {
                    ConfiguredChatChannel channel = obj.getCurrentChatChannel();
                    stmt.setString(1, uuid);
                    stmt.setString(2, channel == null ? "" : channel.getIdentifier());
                    stmt.setString(3, orEmpty(obj.getReplyTo()));
                    stmt.setString(4, orEmpty(obj.getLastMessage()));
                    stmt.setString(5, orEmpty(obj.getLastMessageSent()));
                    stmt.setString(6, orEmpty(obj.getLastMessageReceived()));
                    stmt.setBoolean(7, obj.isAcceptingFriendRequests());
                    stmt.executeUpdate();
                }

                String[] deletes = {
                        "DELETE FROM `%table_prefix%channel_views` WHERE `Uuid` = ?;",
                        "DELETE FROM `%table_prefix%chatter_friends` WHERE `PlayerUuid` = ?;",
                        "DELETE FROM `%table_prefix%chatter_ignores` WHERE `PlayerUuid` = ?;",
                        "DELETE FROM `%table_prefix%friend_invites` WHERE `PlayerUuid` = ?;",
                        "DELETE FROM `%table_prefix%best_friends` WHERE `PlayerUuid` = ?;",
                };
                for (String delete : deletes) {
                    try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(delete))) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                }

                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(
                        "INSERT INTO `%table_prefix%channel_views` (`Uuid`, `Channel`, `Viewed`) VALUES ( ?, ?, ? );"))) {
                    for (Map.Entry<ConfiguredChatChannel, Boolean> entry : obj.getViewing().entrySet()) {
                        stmt.setString(1, uuid);
                        stmt.setString(2, entry.getKey().getIdentifier());
                        stmt.setBoolean(3, entry.getValue());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                insertDated(connection, "INSERT INTO `%table_prefix%chatter_friends` (`PlayerUuid`, `FriendUuid`, `DateAccepted`) VALUES ( ?, ?, ? );",
                        uuid, obj.getFriends());
                insertDated(connection, "INSERT INTO `%table_prefix%chatter_ignores` (`PlayerUuid`, `IgnoreUuid`, `DateIgnored`) VALUES ( ?, ?, ? );",
                        uuid, obj.getIgnoring());
                insertDated(connection, "INSERT INTO `%table_prefix%best_friends` (`PlayerUuid`, `FriendUuid`, `DateSet`) VALUES ( ?, ?, ? );",
                        uuid, obj.getBestFriends());

                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(
                        "INSERT INTO `%table_prefix%friend_invites` (`PlayerUuid`, `FriendUuid`, `TicksLeft`) VALUES ( ?, ?, ? );"))) {
                    obj.getFriendInvites().forEach((invitedUuid, invite) -> {
                        try {
                            stmt.setString(1, uuid);
                            stmt.setString(2, invitedUuid);
                            stmt.setLong(3, invite.getTicksLeft());
                            stmt.addBatch();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    obj.getStoredInvites().forEach((invitedUuid, ticksLeft) -> {
                        if (obj.getFriendInvites().containsKey(invitedUuid)) return;
                        try {
                            stmt.setString(1, uuid);
                            stmt.setString(2, invitedUuid);
                            stmt.setLong(3, ticksLeft);
                            stmt.addBatch();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    });
                    stmt.executeBatch();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                MessageUtils.logWarning("Failed to save chatter " + uuid, e);
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to save chatter " + uuid, e);
        }
    }

    private void insertDated(Connection connection, String statement, String uuid,
                             Map<Date, String> rows) throws Exception {
        try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(statement))) {
            for (Map.Entry<Date, String> entry : rows.entrySet()) {
                stmt.setString(1, uuid);
                stmt.setString(2, entry.getValue());
                stmt.setLong(3, entry.getKey().getTime());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public Optional<SavableChatter> loadMysql(String identifier) {
        return loadBoth(identifier);
    }

    @Override
    public Optional<SavableChatter> loadSqlite(String identifier) {
        return loadBoth(identifier);
    }

    @Override
    public boolean deleteMysql(String identifier) {
        return deleteBoth(identifier);
    }

    @Override
    public boolean deleteSqlite(String identifier) {
        return deleteBoth(identifier);
    }

    /**
     * Removes every row belonging to a chatter. The chatter's data is spread over the
     * main table and five satellite tables, which key on the owning player through
     * different column names, so each is deleted explicitly.
     *
     * @param identifier the chatter's uuid
     * @return {@code true} if every statement ran without error
     */
    public boolean deleteBoth(String identifier) {
        String statement =
                "DELETE FROM `%table_prefix%chatter_main` WHERE `Uuid` = ?;;" +
                "DELETE FROM `%table_prefix%channel_views` WHERE `Uuid` = ?;;" +
                "DELETE FROM `%table_prefix%chatter_friends` WHERE `PlayerUuid` = ?;;" +
                "DELETE FROM `%table_prefix%chatter_ignores` WHERE `PlayerUuid` = ?;;" +
                "DELETE FROM `%table_prefix%friend_invites` WHERE `PlayerUuid` = ?;;" +
                "DELETE FROM `%table_prefix%best_friends` WHERE `PlayerUuid` = ?;;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        return deleteWith(identifier, statement);
    }

    public Optional<SavableChatter> loadBoth(String identifier) {
        String statement = "SELECT * FROM `%table_prefix%chatter_main` WHERE `Uuid` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        AtomicReference<Optional<SavableChatter>> user = new AtomicReference<>(Optional.empty());
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, (result) -> {
            try {
                if (result.next()) {
                    String uuid = result.getString("Uuid");
                    String currentChannel = result.getString("CurrentChannel");
                    String replyToUuid = result.getString("ReplyToUuid");
                    String lastMessage = result.getString("LastMessage");
                    String lastMessageSent = result.getString("LastMessageSent");
                    String lastMessageReceived = result.getString("LastMessageReceived");
                    boolean acceptingFriendRequests = result.getBoolean("AcceptingFriendRequests");

                    SavableChatter u = new SavableChatter(uuid);
                    ConfiguredChatChannel channel = StreamlineMessaging.getChatChannelConfig().getChatChannel(currentChannel);
                    if (channel != null) u.setCurrentChatChannel(channel);

                    u.setReplyTo(replyToUuid);
                    u.setLastMessage(lastMessage);
                    u.setLastMessageSent(lastMessageSent);
                    u.setLastMessageReceived(lastMessageReceived);
                    u.setAcceptingFriendRequests(acceptingFriendRequests);

                    user.set(Optional.of(u));
                }

                result.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        user.get().ifPresent((u) -> {
            String statement1 = "SELECT * FROM `%table_prefix%channel_views` WHERE `Uuid` = ?;";

            statement1 = statement1.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().executeQuery(statement1, stmt -> {
                try {
                    stmt.setString(1, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, (result) -> {
                try {
                    while (result.next()) {
                        String channel = result.getString("Channel");
                        boolean viewed = result.getBoolean("Viewed");

                        u.setViewed(channel, viewed);
                    }

                    result.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            statement1 = "SELECT * FROM `%table_prefix%chatter_friends` WHERE `PlayerUuid` = ?;";

            statement1 = statement1.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().executeQuery(statement1, stmt -> {
                try {
                    stmt.setString(1, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, (result) -> {
                try {
                    while (result.next()) {
                        String friendUuid = result.getString("FriendUuid");
                        long dateAccepted = result.getLong("DateAccepted");

                        u.setFriended(dateAccepted, friendUuid);
                    }

                    result.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            statement1 = "SELECT * FROM `%table_prefix%chatter_ignores` WHERE `PlayerUuid` = ?;";
            statement1 = statement1.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().executeQuery(statement1, stmt -> {
                try {
                    stmt.setString(1, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, (result) -> {
                try {
                    while (result.next()) {
                        String ignoreUuid = result.getString("IgnoreUuid");
                        long dateIgnored = result.getLong("DateIgnored");

                        u.setIgnored(dateIgnored, ignoreUuid);
                    }

                    result.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            statement1 = "SELECT * FROM `%table_prefix%friend_invites` WHERE `PlayerUuid` = ?;";
            statement1 = statement1.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().executeQuery(statement1, stmt -> {
                try {
                    stmt.setString(1, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, (result) -> {
                try {
                    while (result.next()) {
                        String friendUuid = result.getString("FriendUuid");
                        long ticksLeft = result.getLong("TicksLeft");

                        u.setInviteSent(ticksLeft, friendUuid);
                    }

                    result.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            statement1 = "SELECT * FROM `%table_prefix%best_friends` WHERE `PlayerUuid` = ?;";
            statement1 = statement1.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().executeQuery(statement1, stmt -> {
                try {
                    stmt.setString(1, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, (result) -> {
                try {
                    while (result.next()) {
                        String friendUuid = result.getString("FriendUuid");
                        long dateSet = result.getLong("DateSet");

                        u.setBestFriended(dateSet, friendUuid);
                    }

                    result.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            user.set(Optional.of(u));
        });

        return user.get();
    }

    @Override
    public boolean existsMysql(String identifier) {
        return existsBoth(identifier);
    }

    @Override
    public boolean existsSqlite(String identifier) {
        return existsBoth(identifier);
    }

    public boolean existsBoth(String identifier) {
        String statement = "SELECT * FROM `%table_prefix%chatter_main` WHERE `uuid` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        AtomicReference<Boolean> exists = new AtomicReference<>(false);
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, (result) -> {
            try {
                exists.set(result.next());
                result.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return exists.get();
    }

    public CompletableFuture<ConcurrentSkipListSet<SavableChatter>> pullAllChatters() {
        if (SLAPI.getMainDatabase().getConnectorSet().getType() == DatabaseType.MYSQL) {
            return pullAllChattersMySql();
        } else {
            return pullAllChattersSqlite();
        }
    }

    public CompletableFuture<ConcurrentSkipListSet<SavableChatter>> pullAllChattersMySql() {
        return pullAllChattersBoth();
    }

    public CompletableFuture<ConcurrentSkipListSet<SavableChatter>> pullAllChattersSqlite() {
        return pullAllChattersBoth();
    }

    public CompletableFuture<ConcurrentSkipListSet<SavableChatter>> pullAllChattersBoth() {
        String statement = "SELECT `Uuid` FROM `%table_prefix%chatter_main`;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        ConcurrentSkipListSet<String> uuids = new ConcurrentSkipListSet<>();
        getDatabase().executeQuery(statement, stmt -> {}, (result) -> {
            try {
                while (result.next()) {
                    String uuid = result.getString("Uuid");

                    uuids.add(uuid);
                }

                result.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        AtomicReference<ConcurrentSkipListSet<SavableChatter>> chatters = new AtomicReference<>(new ConcurrentSkipListSet<>());
        uuids.forEach((uuid) -> {
            load(uuid).join().ifPresent(chatters.get()::add);
        });

        return CompletableFuture.completedFuture(chatters.get());
    }
}
