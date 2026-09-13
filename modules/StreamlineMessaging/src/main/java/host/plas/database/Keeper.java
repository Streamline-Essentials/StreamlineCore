package host.plas.database;

import host.plas.StreamlineMessaging;
import host.plas.configs.ConfiguredChatChannel;
import host.plas.savables.SavableChatter;
import net.streamline.api.SLAPI;
import singularity.database.DatabaseType;
import singularity.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

public class Keeper extends DBKeeper<SavableChatter> {
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
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%channel_views` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`Uuid` VARCHAR(36) NOT NULL, " +
                "`Channel` TEXT NOT NULL, " +
                "`Viewed` BIT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_friends` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`FriendUuid` VARCHAR(36) NOT NULL, " +
                "`DateAccepted` INT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%chatter_ignores` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`IgnoreUuid` VARCHAR(36) NOT NULL, " +
                "`DateIgnored` INT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%friend_invites` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`FriendUuid` VARCHAR(36) NOT NULL, " +
                "`TicksLeft` INT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;" +

                "CREATE TABLE IF NOT EXISTS `%table_prefix%best_friends` (" +
                "`Id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`PlayerUuid` VARCHAR(36) NOT NULL, " +
                "`FriendUuid` VARCHAR(36) NOT NULL, " +
                "`DateSet` INT NOT NULL " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, smt -> {});
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
        String statement = "INSERT INTO `%table_prefix%chatter_main` " +
                "(`Uuid`, `CurrentChannel`, `ReplyToUuid`, `LastMessage`, `LastMessageSent`, `LastMessageReceived`, `AcceptingFriendRequests`) " +
                "VALUES " +
                "( ?, ?, ?, ?, ?, ?, ? )" +
                "ON DUPLICATE KEY UPDATE " +
                "`CurrentChannel` = ?, " +
                "`ReplyToUuid` = ?, " +
                "`LastMessage` = ?, " +
                "`LastMessageSent` = ?, " +
                "`LastMessageReceived` = ?, " +
                "`AcceptingFriendRequests` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, obj.getUuid());
                stmt.setString(2, obj.getCurrentChatChannel().getIdentifier());
                stmt.setString(3, obj.getReplyTo());
                stmt.setString(4, obj.getLastMessage());
                stmt.setString(5, obj.getLastMessageSent());
                stmt.setString(6, obj.getLastMessageReceived());
                stmt.setBoolean(7, obj.isAcceptingFriendRequests());

                stmt.setString(8, obj.getCurrentChatChannel().getIdentifier());
                stmt.setString(9, obj.getReplyTo());
                stmt.setString(10, obj.getLastMessage());
                stmt.setString(11, obj.getLastMessageSent());
                stmt.setString(12, obj.getLastMessageReceived());
                stmt.setBoolean(13, obj.isAcceptingFriendRequests());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        statement = "INSERT INTO `%table_prefix%channel_views` " +
                "(`Uuid`, `Channel`, `Viewed`) " +
                "VALUES " +
                "( ?, ?, ? )" +
                "ON DUPLICATE KEY UPDATE " +
                "`Channel` = ?, " +
                "`Viewed` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement = statement;
        obj.getViewing().forEach((channel, viewed) -> {
            getDatabase().execute(finalStatement, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, channel.getIdentifier());
                    stmt.setBoolean(3, viewed);

                    stmt.setString(4, channel.getIdentifier());
                    stmt.setBoolean(5, viewed);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT INTO `%table_prefix%chatter_friends` " +
                "(`PlayerUuid`, `FriendUuid`, `DateAccepted`) " +
                "VALUES " +
                "( ?, ?, ? )" +
                "ON DUPLICATE KEY UPDATE " +
                "`DateAccepted` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement1 = statement;
        obj.getFriends().forEach((date, friend) -> {
            getDatabase().execute(finalStatement1, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, friend);
                    stmt.setLong(3, date.getTime());

                    stmt.setLong(4, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT INTO `%table_prefix%chatter_ignores` " +
                "(`PlayerUuid`, `IgnoreUuid`, `DateIgnored`) " +
                "VALUES " +
                "( ?, ?, ? )" +
                "ON DUPLICATE KEY UPDATE " +
                "`DateIgnored` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement2 = statement;
        obj.getIgnoring().forEach((date, ignore) -> {
            getDatabase().execute(finalStatement2, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, ignore);
                    stmt.setLong(3, date.getTime());

                    stmt.setLong(4, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT INTO `%table_prefix%friend_invites` " +
                "(`PlayerUuid`, `FriendUuid`, `TicksLeft`) " +
                "VALUES " +
                "( ?, ?, ? )" +
                "ON DUPLICATE KEY UPDATE " +
                "`TicksLeft` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement3 = statement;
        obj.getFriendInvites().forEach((date, invite) -> {
            getDatabase().execute(finalStatement3, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, invite.getInvited().getUuid());
                    stmt.setLong(3, invite.getTicksLeft());

                    stmt.setLong(4, invite.getTicksLeft());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT INTO `%table_prefix%best_friends` " +
                "(`PlayerUuid`, `FriendUuid`, `DateSet`) " +
                "VALUES " +
                "( ?, ?, ? )" +
                "ON DUPLICATE KEY UPDATE " +
                "`DateSet` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement4 = statement;
        obj.getBestFriends().forEach((date, bestFriend) -> {
            getDatabase().execute(finalStatement4, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, bestFriend);
                    stmt.setLong(3, date.getTime());

                    stmt.setLong(4, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public void saveSqlite(SavableChatter obj) {
        String statement = "INSERT OR REPLACE INTO `%table_prefix%chatter_main` " +
                "(`Uuid`, `CurrentChannel`, `ReplyToUuid`, `LastMessage`, `LastMessageSent`, `LastMessageReceived`, `AcceptingFriendRequests`) " +
                "VALUES " +
                "( ?, ?, ?, ?, ?, ?, ? );";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, obj.getUuid());
                stmt.setString(2, obj.getCurrentChatChannel().getIdentifier());
                stmt.setString(3, obj.getReplyTo());
                stmt.setString(4, obj.getLastMessage());
                stmt.setString(5, obj.getLastMessageSent());
                stmt.setString(6, obj.getLastMessageReceived());
                stmt.setBoolean(7, obj.isAcceptingFriendRequests());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        statement = "INSERT OR REPLACE INTO `%table_prefix%channel_views` " +
                "(`Uuid`, `Channel`, `Viewed`) " +
                "VALUES " +
                "( ?, ?, ? );";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement = statement;
        obj.getViewing().forEach((channel, viewed) -> {
            getDatabase().execute(finalStatement, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, channel.getIdentifier());
                    stmt.setBoolean(3, viewed);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT OR REPLACE INTO `%table_prefix%chatter_friends` " +
                "(`PlayerUuid`, `FriendUuid`, `DateAccepted`) " +
                "VALUES " +
                "( ?, ?, ? );";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement1 = statement;
        obj.getFriends().forEach((date, friend) -> {
            getDatabase().execute(finalStatement1, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, friend);
                    stmt.setLong(3, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT OR REPLACE INTO `%table_prefix%chatter_ignores` " +
                "(`PlayerUuid`, `IgnoreUuid`, `DateIgnored`) " +
                "VALUES " +
                "( ?, ?, ? );";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement2 = statement;
        obj.getIgnoring().forEach((date, ignore) -> {
            getDatabase().execute(finalStatement2, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, ignore);
                    stmt.setLong(3, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT OR REPLACE INTO `%table_prefix%friend_invites` " +
                "(`PlayerUuid`, `FriendUuid`, `TicksLeft`) " +
                "VALUES " +
                "( ?, ?, ? );";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement3 = statement;
        obj.getFriendInvites().forEach((date, invite) -> {
            getDatabase().execute(finalStatement3, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, invite.getInvited().getUuid());
                    stmt.setLong(3, invite.getTicksLeft());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        statement = "INSERT OR REPLACE INTO `%table_prefix%best_friends` " +
                "(`PlayerUuid`, `FriendUuid`, `DateSet`) " +
                "VALUES " +
                "( ?, ?, ? );";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        String finalStatement4 = statement;
        obj.getBestFriends().forEach((date, bestFriend) -> {
            getDatabase().execute(finalStatement4, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, bestFriend);
                    stmt.setLong(3, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public Optional<SavableChatter> loadMysql(String identifier) {
        return loadBoth(identifier);
    }

    @Override
    public Optional<SavableChatter> loadSqlite(String identifier) {
        return loadBoth(identifier);
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