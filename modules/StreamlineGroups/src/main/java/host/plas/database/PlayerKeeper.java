package host.plas.database;

import host.plas.data.chats.ChatType;
import host.plas.data.player.GroupedPlayer;
import net.streamline.api.SLAPI;
import singularity.database.DatabaseType;
import singularity.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerKeeper extends DBKeeper<GroupedPlayer> {
    public PlayerKeeper() {
        super("grouped_players", GroupedPlayer::new);
    }

    @Override
    public void ensureMysqlTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%grouped_players` (" +
                "`Uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "`ChatType` TEXT NOT NULL, " +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, smt -> {});
    }

    @Override
    public void ensureSqliteTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%grouped_players` (" +
                "`Uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "`ChatType` TEXT NOT NULL, " +
                ");;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, smt -> {});
    }

    @Override
    public void saveMysql(GroupedPlayer obj) {
        String statement = "INSERT INTO `%table_prefix%grouped_players` " +
                "(`Uuid`, `ChatType`) " +
                "VALUES " +
                "( ?, ? )" +
                "ON DUPLICATE KEY UPDATE " +
                "`ChatType` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, obj.getIdentifier());
                stmt.setString(2, obj.getChatType().name());

                stmt.setString(3, obj.getChatType().name());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void saveSqlite(GroupedPlayer obj) {
        String statement = "INSERT OR REPLACE INTO `%table_prefix%grouped_players` " +
                "(`Uuid`, `ChatType`) " +
                "VALUES " +
                "( ?, ? );";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, obj.getIdentifier());
                stmt.setString(2, obj.getChatType().name());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Optional<GroupedPlayer> loadMysql(String identifier) {
        return loadBoth(identifier);
    }

    @Override
    public Optional<GroupedPlayer> loadSqlite(String identifier) {
        return loadBoth(identifier);
    }

    public Optional<GroupedPlayer> loadBoth(String identifier) {
        String statement = "SELECT * FROM `%table_prefix%grouped_players` WHERE `Uuid` = ?;";

        statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

        AtomicReference<Optional<GroupedPlayer>> user = new AtomicReference<>(Optional.empty());
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
                    String chatTypeStr = result.getString("ChatType");

                    ChatType chatType;
                    try {
                        chatType = ChatType.valueOf(chatTypeStr);
                    } catch (IllegalArgumentException e) {
                        chatType = ChatType.NOT_SET;
                    }

                    GroupedPlayer u = new GroupedPlayer(uuid);
                    u.setChatType(chatType);

                    user.set(Optional.of(u));
                }

                result.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    public CompletableFuture<ConcurrentSkipListSet<GroupedPlayer>> pullAllChatters() {
        if (SLAPI.getMainDatabase().getConnectorSet().getType() == DatabaseType.MYSQL) {
            return pullAllThingsMySql();
        } else {
            return pullAllThingsSqlite();
        }
    }

    public CompletableFuture<ConcurrentSkipListSet<GroupedPlayer>> pullAllThingsMySql() {
        return pullAllThingsBoth();
    }

    public CompletableFuture<ConcurrentSkipListSet<GroupedPlayer>> pullAllThingsSqlite() {
        return pullAllThingsBoth();
    }

    public CompletableFuture<ConcurrentSkipListSet<GroupedPlayer>> pullAllThingsBoth() {
        String statement = "SELECT `Uuid` FROM `%table_prefix%grouped_players`;";

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

        AtomicReference<ConcurrentSkipListSet<GroupedPlayer>> things = new AtomicReference<>(new ConcurrentSkipListSet<>());
        uuids.forEach((uuid) -> {
            load(uuid).join().ifPresent(things.get()::add);
        });

        return CompletableFuture.completedFuture(things.get());
    }
}