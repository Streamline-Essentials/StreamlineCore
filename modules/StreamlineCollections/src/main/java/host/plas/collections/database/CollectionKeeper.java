package host.plas.collections.database;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.CollectionPlayer;
import singularity.database.modules.DBKeeper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores collection progress in two tables: {@code collections} (amount per player and
 * collection, with the player's last known name for leaderboards) and {@code collection_claims}
 * (one row per claimed level).
 */
public class CollectionKeeper extends DBKeeper<CollectionPlayer> {

    public CollectionKeeper() {
        super("collections", CollectionPlayer::new);
    }

    @Override
    public void ensureMysqlTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%collections` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `CollectionId` VARCHAR(191) NOT NULL," +
                "  `Name` VARCHAR(16)," +
                "  `Amount` BIGINT NOT NULL DEFAULT 0," +
                "  PRIMARY KEY (`Uuid`, `CollectionId`)," +
                "  INDEX `%table_prefix%collections_board` (`CollectionId`, `Amount`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%collection_claims` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `CollectionId` VARCHAR(191) NOT NULL," +
                "  `Level` INT NOT NULL," +
                "  `ClaimedAt` BIGINT NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `CollectionId`, `Level`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;";

        getDatabase().execute(injectTablePrefix(statement), stmt -> {});
    }

    @Override
    public void ensureSqliteTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%table_prefix%collections` (" +
                "  `Uuid` TEXT NOT NULL," +
                "  `CollectionId` TEXT NOT NULL," +
                "  `Name` TEXT," +
                "  `Amount` INTEGER NOT NULL DEFAULT 0," +
                "  PRIMARY KEY (`Uuid`, `CollectionId`)" +
                ");;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%collections_board` ON `%table_prefix%collections` (`CollectionId`, `Amount`);;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%collection_claims` (" +
                "  `Uuid` TEXT NOT NULL," +
                "  `CollectionId` TEXT NOT NULL," +
                "  `Level` INTEGER NOT NULL," +
                "  `ClaimedAt` INTEGER NOT NULL," +
                "  PRIMARY KEY (`Uuid`, `CollectionId`, `Level`)" +
                ");;";

        getDatabase().execute(injectTablePrefix(statement), stmt -> {});
    }

    @Override
    public void saveMysql(CollectionPlayer player) {
        saveBoth(player);
    }

    @Override
    public void saveSqlite(CollectionPlayer player) {
        saveBoth(player);
    }

    /**
     * Replaces the player's amounts and claims in one transaction. Rows are deleted and
     * re-inserted so a wipe leaves the database too; the single transaction keeps a concurrent
     * load from reading between the delete and the inserts. Claim times already stored are kept.
     */
    private void saveBoth(CollectionPlayer player) {
        String uuid = player.getIdentifier();
        Map<String, Long> amounts = new HashMap<>(player.getAmounts());
        List<String> claims = new ArrayList<>(player.getClaimed());

        try (Connection connection = getDatabase().getConnection()) {
            if (connection == null) {
                StreamlineCollections.getInstance().logWarning("No database connection to save collections for " + uuid);
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Map<String, Long> claimTimes = new HashMap<>();
                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(
                        "SELECT `CollectionId`, `Level`, `ClaimedAt` FROM `%table_prefix%collection_claims` WHERE `Uuid` = ?;"))) {
                    stmt.setString(1, uuid);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            claimTimes.put(CollectionPlayer.claimKey(rs.getString(1), rs.getInt(2)), rs.getLong(3));
                        }
                    }
                }

                for (String delete : new String[] {
                        "DELETE FROM `%table_prefix%collections` WHERE `Uuid` = ?;",
                        "DELETE FROM `%table_prefix%collection_claims` WHERE `Uuid` = ?;" }) {
                    try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(delete))) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                }

                String name = player.getName() == null || player.getName().isEmpty() ? null : player.getName();
                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(
                        "INSERT INTO `%table_prefix%collections` (`Uuid`, `CollectionId`, `Name`, `Amount`) VALUES ( ?, ?, ?, ? );"))) {
                    for (Map.Entry<String, Long> entry : amounts.entrySet()) {
                        if (entry.getValue() == null || entry.getValue() <= 0) continue;
                        stmt.setString(1, uuid);
                        stmt.setString(2, entry.getKey());
                        stmt.setString(3, name);
                        stmt.setLong(4, entry.getValue());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                long now = System.currentTimeMillis();
                try (PreparedStatement stmt = connection.prepareStatement(injectTablePrefix(
                        "INSERT INTO `%table_prefix%collection_claims` (`Uuid`, `CollectionId`, `Level`, `ClaimedAt`) VALUES ( ?, ?, ?, ? );"))) {
                    for (String claim : claims) {
                        int split = claim.lastIndexOf(':');
                        if (split < 0) continue;
                        int level;
                        try {
                            level = Integer.parseInt(claim.substring(split + 1));
                        } catch (NumberFormatException e) {
                            continue;
                        }
                        stmt.setString(1, uuid);
                        stmt.setString(2, claim.substring(0, split));
                        stmt.setInt(3, level);
                        stmt.setLong(4, claimTimes.getOrDefault(claim, now));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                StreamlineCollections.getInstance().logWarning("Failed to save collections for " + uuid + ": " + e.getMessage());
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Exception e) {
            StreamlineCollections.getInstance().logWarning("Failed to save collections for " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public Optional<CollectionPlayer> loadMysql(String identifier) {
        return loadBoth(identifier);
    }

    @Override
    public Optional<CollectionPlayer> loadSqlite(String identifier) {
        return loadBoth(identifier);
    }

    /**
     * Reads amounts, then claims. Each query runs after the previous one returned its connection:
     * SQLite's pool holds a single connection.
     */
    public Optional<CollectionPlayer> loadBoth(String identifier) {
        CollectionPlayer player = new CollectionPlayer(identifier);
        AtomicBoolean found = new AtomicBoolean(false);

        getDatabase().executeQuery(injectTablePrefix(
                "SELECT `CollectionId`, `Name`, `Amount` FROM `%table_prefix%collections` WHERE `Uuid` = ?;"), stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to bind collections load: " + e.getMessage());
            }
        }, rs -> {
            try {
                while (rs.next()) {
                    found.set(true);
                    player.getAmounts().put(rs.getString("CollectionId"), rs.getLong("Amount"));
                    String name = rs.getString("Name");
                    if (name != null && ! name.isEmpty()) player.setName(name);
                }
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to read collections for " + identifier + ": " + e.getMessage());
            }
        });

        getDatabase().executeQuery(injectTablePrefix(
                "SELECT `CollectionId`, `Level` FROM `%table_prefix%collection_claims` WHERE `Uuid` = ?;"), stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to bind claims load: " + e.getMessage());
            }
        }, rs -> {
            try {
                while (rs.next()) {
                    found.set(true);
                    player.getClaimed().add(CollectionPlayer.claimKey(rs.getString("CollectionId"), rs.getInt("Level")));
                }
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to read claims for " + identifier + ": " + e.getMessage());
            }
        });

        if (! found.get()) return Optional.empty();
        player.setDirty(false);
        return Optional.of(player);
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
        AtomicReference<Boolean> exists = new AtomicReference<>(false);
        getDatabase().executeQuery(injectTablePrefix(
                "SELECT 1 FROM `%table_prefix%collections` WHERE `Uuid` = ? LIMIT 1;"), stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to bind collections lookup: " + e.getMessage());
            }
        }, rs -> {
            try {
                exists.set(rs.next());
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to look up collections for " + identifier + ": " + e.getMessage());
            }
        });
        return exists.get();
    }

    /** One row of a leaderboard. */
    public static final class Ranked {
        public final String uuid;
        public final String name;
        public final long amount;

        public Ranked(String uuid, String name, long amount) {
            this.uuid = uuid;
            this.name = name;
            this.amount = amount;
        }
    }

    /** The top {@code limit} players by the summed amount of {@code collectionIds}. */
    public List<Ranked> top(List<String> collectionIds, int limit) {
        List<Ranked> rows = new ArrayList<>();
        if (collectionIds.isEmpty()) return rows;

        ensureTables();

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < collectionIds.size(); i++) in.append(i == 0 ? "?" : ", ?");

        String sql = injectTablePrefix("SELECT `Uuid`, MAX(`Name`) AS `Name`, SUM(`Amount`) AS `Total` " +
                "FROM `%table_prefix%collections` WHERE `CollectionId` IN (" + in + ") " +
                "GROUP BY `Uuid` ORDER BY `Total` DESC LIMIT " + Math.max(1, limit) + ";");

        getDatabase().executeQuery(sql, stmt -> {
            try {
                for (int i = 0; i < collectionIds.size(); i++) stmt.setString(i + 1, collectionIds.get(i));
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to bind leaderboard query: " + e.getMessage());
            }
        }, rs -> {
            try {
                while (rs.next()) rows.add(new Ranked(rs.getString("Uuid"), rs.getString("Name"), rs.getLong("Total")));
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to read leaderboard: " + e.getMessage());
            }
        });
        return rows;
    }
}
