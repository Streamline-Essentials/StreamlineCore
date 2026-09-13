package host.plas.database;

import host.plas.data.players.QuestPlayer;
import host.plas.data.require.RequirementType;
import lombok.Getter;
import lombok.Setter;
import singularity.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class Keeper extends DBKeeper<QuestPlayer> {
    public Keeper() {
        super("quest_players", QuestPlayer::new);
    }

    @Override
    public void ensureMysqlTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_players` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `Points` DOUBLE NOT NULL," +
                "  PRIMARY KEY (`Uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_completed` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `Quest` VARCHAR(36) NOT NULL," +
                "  `CompletedAt` BIGINT NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `Quest`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_values` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `Type` VARCHAR(36) NOT NULL," +
                "  `Value` VARCHAR(36) NOT NULL," +
                "  `Amount` DOUBLE NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `Type`, `Value`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;";

        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {});
    }

    @Override
    public void ensureSqliteTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_players` (" +
                "  `Uuid` TEXT NOT NULL," +
                "  `Points` DOUBLE NOT NULL," +
                "  PRIMARY KEY (`Uuid`)" +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_completed` (" +
                "  `Uuid` TEXT NOT NULL," +
                "  `Quest` TEXT NOT NULL," +
                "  `CompletedAt` REAL NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `Quest`)" +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_values` (" +
                "  `Uuid` TEXT NOT NULL," +
                "  `Type` TEXT NOT NULL," +
                "  `Value` TEXT NOT NULL," +
                "  `Amount` DOUBLE NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `Type`, `Value`)" +
                ");;";

        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {});
    }

    @Override
    public void saveMysql(QuestPlayer questPlayer) {
        String statement = "INSERT INTO `%table_prefix%quest_players` " +
                "(`Uuid`, `Points`) " +
                "VALUES " +
                "( ?, ? ) " +
                "ON DUPLICATE KEY UPDATE " +
                "`Points` = ?;";

        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, questPlayer.getIdentifier());
                stmt.setDouble(2, questPlayer.getPoints());
                stmt.setDouble(3, questPlayer.getPoints());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        questPlayer.getCompletedQuests().forEach((s, date) -> {
            String statement1 = "INSERT INTO `%table_prefix%quest_completed` " +
                    "(`Uuid`, `Quest`, `CompletedAt`) " +
                    "VALUES " +
                    "( ?, ?, ? ) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "`CompletedAt` = ?;";

            statement1 = statement1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

            getDatabase().execute(statement1, stmt -> {
                try {
                    stmt.setString(1, questPlayer.getIdentifier());
                    stmt.setString(2, s);
                    stmt.setLong(3, date.getTime());
                    stmt.setLong(4, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        questPlayer.getQuestValues().forEach((requirementType, map) -> {
            map.forEach((s, aDouble) -> {
                String statement1 = "INSERT INTO `%table_prefix%quest_values` " +
                        "(`Uuid`, `Type`, `Value`, `Amount`) " +
                        "VALUES " +
                        "( ?, ?, ?, ? ) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "`Amount` = ?;";

                statement1 = statement1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

                getDatabase().execute(statement1, stmt -> {
                    try {
                        stmt.setString(1, questPlayer.getIdentifier());
                        stmt.setString(2, requirementType.name());
                        stmt.setString(3, s);
                        stmt.setDouble(4, aDouble);
                        stmt.setDouble(5, aDouble);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    @Override
    public void saveSqlite(QuestPlayer questPlayer) {
        String statement = "INSERT OR REPLACE INTO `%table_prefix%quest_players` " +
                "(`Uuid`, `Points`) " +
                "VALUES " +
                "( ?, ? );";

        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {
            try {
                stmt.setString(1, questPlayer.getIdentifier());
                stmt.setDouble(2, questPlayer.getPoints());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        questPlayer.getCompletedQuests().forEach((s, date) -> {
            String statement1 = "INSERT OR REPLACE INTO `%table_prefix%quest_completed` " +
                    "(`Uuid`, `Quest`, `CompletedAt`) " +
                    "VALUES " +
                    "( ?, ?, ? );";

            statement1 = statement1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

            getDatabase().execute(statement1, stmt -> {
                try {
                    stmt.setString(1, questPlayer.getIdentifier());
                    stmt.setString(2, s);
                    stmt.setLong(3, date.getTime());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        questPlayer.getQuestValues().forEach((requirementType, map) -> {
            map.forEach((s, aDouble) -> {
                String statement1 = "INSERT OR REPLACE INTO `%table_prefix%quest_values` " +
                        "(`Uuid`, `Type`, `Value`, `Amount`) " +
                        "VALUES " +
                        "( ?, ?, ?, ? );";

                statement1 = statement1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

                getDatabase().execute(statement1, stmt -> {
                    try {
                        stmt.setString(1, questPlayer.getIdentifier());
                        stmt.setString(2, requirementType.name());
                        stmt.setString(3, s);
                        stmt.setDouble(4, aDouble);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        });
    }

    @Override
    public Optional<QuestPlayer> loadMysql(String s) {
        return loadBoth(s);
    }

    @Override
    public Optional<QuestPlayer> loadSqlite(String s) {
        return loadBoth(s);
    }

    public Optional<QuestPlayer> loadBoth(String identifier) {
        String statement = "SELECT * FROM `%table_prefix%quest_players` WHERE `Uuid` = ?;";
        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        AtomicReference<Optional<QuestPlayer>> optional = new AtomicReference<>(Optional.empty());
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                if (! resultSet.next()) {
                    optional.set(Optional.empty());
                    return;
                }

                QuestPlayer questPlayer = new QuestPlayer(identifier);
                questPlayer.setPoints(resultSet.getDouble("Points"));

                String statement1 = "SELECT * FROM `%table_prefix%quest_completed` WHERE `Uuid` = ?;";
                statement1 = statement1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

                getDatabase().executeQuery(statement1, stmt -> {
                    try {
                        stmt.setString(1, identifier);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, resultSet1 -> {
                    try {
                        while (resultSet1.next()) {
                            questPlayer.getCompletedQuests().put(resultSet1.getString("Quest"), new java.util.Date(resultSet1.getLong("CompletedAt")));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                String statement2 = "SELECT * FROM `%table_prefix%quest_values` WHERE `Uuid` = ?;";
                statement2 = statement2.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

                getDatabase().executeQuery(statement2, stmt -> {
                    try {
                        stmt.setString(1, identifier);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, resultSet2 -> {
                    try {
                        while (resultSet2.next()) {
                            String type = resultSet2.getString("Type");
                            String value = resultSet2.getString("Value");
                            double amount = resultSet2.getDouble("Amount");

                            try {
                                RequirementType requirementType = RequirementType.valueOf(type);
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            }

                            questPlayer.setValue(RequirementType.valueOf(type), value, amount);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                optional.set(Optional.of(questPlayer));
            } catch (Exception e) {
                e.printStackTrace();
                optional.set(Optional.empty());
            }
        });

        return optional.get();
    }

    @Override
    public boolean existsMysql(String s) {
        return existsBoth(s);
    }

    @Override
    public boolean existsSqlite(String s) {
        return existsBoth(s);
    }

    public boolean existsBoth(String identifier) {
        String statement = "SELECT * FROM `%table_prefix%quest_players` WHERE `Uuid` = ?;";
        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        AtomicReference<Boolean> exists = new AtomicReference<>(false);
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                exists.set(resultSet.next());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return exists.get();
    }
}
