package host.plas.database;

import host.plas.essentials.users.UtilitiesUser;
import net.streamline.api.SLAPI;
import singularity.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Keeper extends DBKeeper<UtilitiesUser> {
        public Keeper() {
            super("utilities_users", UtilitiesUser::new);
        }

        @Override
        public void ensureMysqlTables() {
            String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%utilities_users` (" +
                    "`Uuid` VARCHAR(36) NOT NULL PRIMARY KEY, " +
                    "`Homes` TEXT NOT NULL, " +
                    "`LastServer` VARCHAR(36) NOT NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8;;";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().execute(statement, stmt -> {});
        }

        @Override
        public void ensureSqliteTables() {
            String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%utilities_users` (" +
                    "`Uuid` TEXT NOT NULL, " +
                    "`Homes` TEXT NOT NULL, " +
                    "`LastServer` TEXT NOT NULL, " +
                    "PRIMARY KEY (`uuid`)" +
                    ");;";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().execute(statement, stmt -> {});
        }

        @Override
        public void saveMysql(UtilitiesUser obj) {
            String statement = "INSERT INTO `%table_prefix%utilities_users` " +
                    "(`Uuid`, `Homes`, `LastServer`) " +
                    "VALUES " +
                    "( ?, ?, ? ) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "`Homes` = ?, " +
                    "`LastServer` = ?;";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().execute(statement, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, obj.computableHomes());
                    stmt.setString(3, obj.getLastServerForDB());

                    stmt.setString(4, obj.computableHomes());
                    stmt.setString(5, obj.getLastServerForDB());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public void saveSqlite(UtilitiesUser obj) {
            String statement = "INSERT OR REPLACE INTO `%table_prefix%utilities_users` " +
                    "(`Uuid`, `Homes`, `LastServer`) " +
                    "VALUES " +
                    "( ?, ?, ? );";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            getDatabase().execute(statement, stmt -> {
                try {
                    stmt.setString(1, obj.getUuid());
                    stmt.setString(2, obj.computableHomes());
                    stmt.setString(3, obj.getLastServerForDB());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @Override
        public Optional<UtilitiesUser> loadMysql(String identifier) {
            String statement = "SELECT * FROM `%table_prefix%utilities_users` WHERE `uuid` = ?;";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());

            AtomicReference<Optional<UtilitiesUser>> user = new AtomicReference<>(Optional.empty());
            getDatabase().executeQuery(statement, stmt -> {
                try {
                    stmt.setString(1, identifier);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, result -> {
                try {
                    if (result.next()) {
                        String uuid = result.getString("Uuid");
                        String homes = result.getString("Homes");
                        String lastServer = result.getString("LastServer");

                        UtilitiesUser u = new UtilitiesUser(uuid);
                        u.setHomes(UtilitiesUser.computableHomes(homes));
                        u.setLastServerFromDB(lastServer);

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
        public Optional<UtilitiesUser> loadSqlite(String identifier) {
            String statement = "SELECT * FROM `%table_prefix%utilities_users` WHERE `uuid` = ?;";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());
            statement = statement.replace("%uuid%", identifier);

            AtomicReference<Optional<UtilitiesUser>> user = new AtomicReference<>(Optional.empty());
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
                        String homes = result.getString("Homes");
                        String lastServer = result.getString("LastServer");

                        UtilitiesUser u = new UtilitiesUser(uuid);
                        u.setHomes(UtilitiesUser.computableHomes(homes));
                        u.setLastServerFromDB(lastServer);

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
            String statement = "SELECT * FROM `%table_prefix%utilities_users` WHERE `uuid` = ?;";

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

        @Override
        public boolean existsSqlite(String identifier) {
            String statement = "SELECT * FROM `%table_prefix%utilities_users` WHERE `uuid` = ?;";

            statement = statement.replace("%table_prefix%", SLAPI.getMainDatabase().getConnectorSet().getTablePrefix());
            statement = statement.replace("%uuid%", identifier);

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
    }