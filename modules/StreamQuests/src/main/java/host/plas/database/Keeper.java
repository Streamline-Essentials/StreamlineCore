package host.plas.database;

import host.plas.data.players.QuestPlayer;
import host.plas.data.require.RequirementType;
import lombok.Getter;
import lombok.Setter;
import singularity.database.modules.DBKeeper;
import singularity.utils.MessageUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class Keeper extends DBKeeper<QuestPlayer> {
    /** Set once the MySQL column widths have been checked for this keeper. */
    private final AtomicBoolean mysqlMigrated = new AtomicBoolean(false);

    public Keeper() {
        super("quest_players", QuestPlayer::new);
    }

    // Quest identifiers and requirement values (item, block and entity keys such as
    // "minecraft:light_weighted_pressure_plate") outgrow 36 characters. 191 is the widest
    // utf8mb4 VARCHAR that still fits an index prefix on older InnoDB row formats.
    @Override
    public void ensureMysqlTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_players` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `Points` DOUBLE NOT NULL," +
                "  PRIMARY KEY (`Uuid`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_completed` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `Quest` VARCHAR(191) NOT NULL," +
                "  `CompletedAt` BIGINT NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `Quest`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%quest_values` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `Type` VARCHAR(36) NOT NULL," +
                "  `Value` VARCHAR(191) NOT NULL," +
                "  `Amount` DOUBLE NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `Type`, `Value`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;";

        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(statement, stmt -> {});

        if (mysqlMigrated.compareAndSet(false, true)) migrateMysql();
    }

    /**
     * Widens the {@code Quest} and {@code Value} key columns of tables created by earlier
     * releases of this module, which declared them {@code VARCHAR(36)}. The width is checked
     * against {@code INFORMATION_SCHEMA} first, since an {@code ALTER TABLE} rebuilds the
     * table.
     */
    private void migrateMysql() {
        String[][] columns = {
                { "quest_completed", "Quest" },
                { "quest_values", "Value" },
        };
        for (String[] column : columns) {
            String table = getDatabase().getConnectorSet().getTablePrefix() + column[0];

            AtomicReference<Long> length = new AtomicReference<>();
            getDatabase().executeQuery("SELECT `CHARACTER_MAXIMUM_LENGTH` FROM `INFORMATION_SCHEMA`.`COLUMNS` " +
                    "WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = ? AND `COLUMN_NAME` = ?;", stmt -> {
                try {
                    stmt.setString(1, table);
                    stmt.setString(2, column[1]);
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to bind column lookup", e);
                }
            }, result -> {
                try {
                    if (result.next()) length.set(result.getLong(1));
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to read the width of " + table + "." + column[1], e);
                }
            });

            if (length.get() != null && length.get() < 191) {
                getDatabase().executeSingle("ALTER TABLE `" + table + "` MODIFY `" + column[1] + "` VARCHAR(191) NOT NULL;", stmt -> {});
            }
        }
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
        saveBoth(questPlayer, "INSERT INTO `%table_prefix%quest_players` (`Uuid`, `Points`) VALUES ( ?, ? ) " +
                "ON DUPLICATE KEY UPDATE `Points` = VALUES(`Points`);");
    }

    @Override
    public void saveSqlite(QuestPlayer questPlayer) {
        saveBoth(questPlayer, "INSERT OR REPLACE INTO `%table_prefix%quest_players` (`Uuid`, `Points`) VALUES ( ?, ? );");
    }

    /**
     * Writes the player's points and replaces their completed quests and requirement
     * values, in one transaction.
     *
     * <p>The child rows are deleted and re-inserted rather than upserted so that an
     * uncompleted quest or a reset value leaves the database too. One connection and one
     * transaction keep a concurrent load -- another server, or a quick rejoin -- from
     * reading the tables between the delete and the inserts.</p>
     */
    private void saveBoth(QuestPlayer questPlayer, String playerUpsert) {
        String uuid = questPlayer.getIdentifier();

        try (Connection connection = getDatabase().getConnection()) {
            if (connection == null) {
                MessageUtils.logWarning("Could not obtain a connection to save quest player " + uuid);
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(playerUpsert))) {
                    stmt.setString(1, uuid);
                    stmt.setDouble(2, questPlayer.getPoints());
                    stmt.executeUpdate();
                }

                for (String delete : new String[] {
                        "DELETE FROM `%table_prefix%quest_completed` WHERE `Uuid` = ?;",
                        "DELETE FROM `%table_prefix%quest_values` WHERE `Uuid` = ?;" }) {
                    try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(delete))) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                }

                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(
                        "INSERT INTO `%table_prefix%quest_completed` (`Uuid`, `Quest`, `CompletedAt`) VALUES ( ?, ?, ? );"))) {
                    for (Map.Entry<String, Date> entry : questPlayer.getCompletedQuests().entrySet()) {
                        stmt.setString(1, uuid);
                        stmt.setString(2, entry.getKey());
                        stmt.setLong(3, entry.getValue().getTime());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(
                        "INSERT INTO `%table_prefix%quest_values` (`Uuid`, `Type`, `Value`, `Amount`) VALUES ( ?, ?, ?, ? );"))) {
                    for (Map.Entry<RequirementType, ? extends Map<String, Double>> typed : questPlayer.getQuestValues().entrySet()) {
                        for (Map.Entry<String, Double> entry : typed.getValue().entrySet()) {
                            stmt.setString(1, uuid);
                            stmt.setString(2, typed.getKey().name());
                            stmt.setString(3, entry.getKey());
                            stmt.setDouble(4, entry.getValue());
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                MessageUtils.logWarning("Failed to save quest player " + uuid, e);
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to save quest player " + uuid, e);
        }
    }

    @Override
    public Optional<QuestPlayer> loadMysql(String s) {
        return loadBoth(s);
    }

    @Override
    public Optional<QuestPlayer> loadSqlite(String s) {
        return loadBoth(s);
    }

    /**
     * Each query runs only after the previous one has returned its connection: nesting one
     * inside another's result callback needs a second connection while the first is still
     * held, which with SQLite's single-connection pool waits out the pool timeout and fails.
     */
    public Optional<QuestPlayer> loadBoth(String identifier) {
        String statement = "SELECT * FROM `%table_prefix%quest_players` WHERE `Uuid` = ?;";
        statement = statement.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        AtomicReference<Double> points = new AtomicReference<>();
        getDatabase().executeQuery(statement, stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                if (resultSet.next()) points.set(resultSet.getDouble("Points"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        if (points.get() == null) return Optional.empty();

        QuestPlayer questPlayer = new QuestPlayer(identifier);
        questPlayer.setPoints(points.get());

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
                    questPlayer.getCompletedQuests().put(resultSet1.getString("Quest"), new Date(resultSet1.getLong("CompletedAt")));
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

                    RequirementType requirementType;
                    try {
                        requirementType = RequirementType.valueOf(type);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    questPlayer.setValue(requirementType, value, amount);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return Optional.of(questPlayer);
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
