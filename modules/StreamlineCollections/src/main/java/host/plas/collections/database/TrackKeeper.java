package host.plas.collections.database;

import host.plas.collections.StreamlineCollections;
import host.plas.collections.data.Track;
import host.plas.collections.data.TrackedPlayer;
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
 * Stores one {@link Track}'s progress in two tables: the amounts table (amount per player and id,
 * with the player's last known name for leaderboards) and, for tracks with levels to claim, the
 * claims table (one row per claimed level). Every track uses the same columns, so the id column
 * keeps the {@code CollectionId} name the collections tables shipped with.
 */
public class TrackKeeper extends DBKeeper<TrackedPlayer> {
    private final Track track;

    public TrackKeeper(Track track) {
        super(track.getAmountsTable(), id -> new TrackedPlayer(track, id));
        this.track = track;
    }

    /** Replaces the table names in {@code sql}: {@code %amounts%} and {@code %claims%}, prefixed. */
    private String tables(String sql) {
        String claims = track.getClaimsTable() == null ? "" : track.getClaimsTable();
        return injectTablePrefix(sql.replace("%amounts%", "%table_prefix%" + track.getAmountsTable())
                .replace("%claims%", "%table_prefix%" + claims));
    }

    private boolean hasClaims() {
        return track.getClaimsTable() != null;
    }

    @Override
    public void ensureMysqlTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%amounts%` (" +
                "  `Uuid` VARCHAR(36) NOT NULL," +
                "  `CollectionId` VARCHAR(191) NOT NULL," +
                "  `Name` VARCHAR(16)," +
                "  `Amount` BIGINT NOT NULL DEFAULT 0," +
                "  PRIMARY KEY (`Uuid`, `CollectionId`)," +
                "  INDEX `%amounts%_board` (`CollectionId`, `Amount`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;";
        if (hasClaims()) {
            statement += "CREATE TABLE IF NOT EXISTS `%claims%` (" +
                    "  `Uuid` VARCHAR(36) NOT NULL," +
                    "  `CollectionId` VARCHAR(191) NOT NULL," +
                    "  `Level` INT NOT NULL," +
                    "  `ClaimedAt` BIGINT NOT NULL," +
                    "  PRIMARY KEY (`Uuid`, `CollectionId`, `Level`)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;;";
        }

        getDatabase().execute(tables(statement), stmt -> {});
    }

    @Override
    public void ensureSqliteTables() {
        String statement = "CREATE TABLE IF NOT EXISTS `%amounts%` (" +
                "  `Uuid` TEXT NOT NULL," +
                "  `CollectionId` TEXT NOT NULL," +
                "  `Name` TEXT," +
                "  `Amount` INTEGER NOT NULL DEFAULT 0," +
                "  PRIMARY KEY (`Uuid`, `CollectionId`)" +
                ");;" +
                "CREATE INDEX IF NOT EXISTS `%amounts%_board` ON `%amounts%` (`CollectionId`, `Amount`);;";
        if (hasClaims()) {
            statement += "CREATE TABLE IF NOT EXISTS `%claims%` (" +
                    "  `Uuid` TEXT NOT NULL," +
                    "  `CollectionId` TEXT NOT NULL," +
                    "  `Level` INTEGER NOT NULL," +
                    "  `ClaimedAt` INTEGER NOT NULL," +
                    "  PRIMARY KEY (`Uuid`, `CollectionId`, `Level`)" +
                    ");;";
        }

        getDatabase().execute(tables(statement), stmt -> {});
    }

    @Override
    public void saveMysql(TrackedPlayer player) {
        saveBoth(player);
    }

    @Override
    public void saveSqlite(TrackedPlayer player) {
        saveBoth(player);
    }

    /**
     * Replaces the player's amounts and claims in one transaction. Rows are deleted and
     * re-inserted so a wipe leaves the database too; the single transaction keeps a concurrent
     * load from reading between the delete and the inserts. Claim times already stored are kept.
     */
    private void saveBoth(TrackedPlayer player) {
        String uuid = player.getIdentifier();
        Map<String, Long> amounts = new HashMap<>(player.getAmounts());
        List<String> claims = new ArrayList<>(player.getClaimed());

        try (Connection connection = getDatabase().getConnection()) {
            if (connection == null) {
                StreamlineCollections.getInstance().logWarning("No database connection to save " + track.getAmountsTable() + " for " + uuid);
                return;
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                Map<String, Long> claimTimes = new HashMap<>();
                if (hasClaims()) try (PreparedStatement stmt = connection.prepareStatement(tables(
                        "SELECT `CollectionId`, `Level`, `ClaimedAt` FROM `%claims%` WHERE `Uuid` = ?;"))) {
                    stmt.setString(1, uuid);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            claimTimes.put(TrackedPlayer.claimKey(rs.getString(1), rs.getInt(2)), rs.getLong(3));
                        }
                    }
                }

                List<String> deletes = new ArrayList<>();
                deletes.add("DELETE FROM `%amounts%` WHERE `Uuid` = ?;");
                if (hasClaims()) deletes.add("DELETE FROM `%claims%` WHERE `Uuid` = ?;");
                for (String delete : deletes) {
                    try (PreparedStatement stmt = connection.prepareStatement(tables(delete))) {
                        stmt.setString(1, uuid);
                        stmt.executeUpdate();
                    }
                }

                String name = player.getName() == null || player.getName().isEmpty() ? null : player.getName();
                try (PreparedStatement stmt = connection.prepareStatement(tables(
                        "INSERT INTO `%amounts%` (`Uuid`, `CollectionId`, `Name`, `Amount`) VALUES ( ?, ?, ?, ? );"))) {
                    for (Map.Entry<String, Long> entry : amounts.entrySet()) {
                        if (entry.getValue() == null || entry.getValue() == 0) continue;
                        stmt.setString(1, uuid);
                        stmt.setString(2, entry.getKey());
                        stmt.setString(3, name);
                        stmt.setLong(4, entry.getValue());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                long now = System.currentTimeMillis();
                if (hasClaims()) try (PreparedStatement stmt = connection.prepareStatement(tables(
                        "INSERT INTO `%claims%` (`Uuid`, `CollectionId`, `Level`, `ClaimedAt`) VALUES ( ?, ?, ?, ? );"))) {
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
                StreamlineCollections.getInstance().logWarning("Failed to save " + track.getAmountsTable() + " for " + uuid + ": " + e.getMessage());
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (Exception e) {
            StreamlineCollections.getInstance().logWarning("Failed to save " + track.getAmountsTable() + " for " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public Optional<TrackedPlayer> loadMysql(String identifier) {
        return loadBoth(identifier);
    }

    @Override
    public Optional<TrackedPlayer> loadSqlite(String identifier) {
        return loadBoth(identifier);
    }

    /**
     * Reads amounts, then claims. Each query runs after the previous one returned its connection:
     * SQLite's pool holds a single connection.
     */
    public Optional<TrackedPlayer> loadBoth(String identifier) {
        TrackedPlayer player = new TrackedPlayer(track, identifier);
        AtomicBoolean found = new AtomicBoolean(false);

        getDatabase().executeQuery(tables(
                "SELECT `CollectionId`, `Name`, `Amount` FROM `%amounts%` WHERE `Uuid` = ?;"), stmt -> {
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
                StreamlineCollections.getInstance().logWarning("Failed to read " + track.getAmountsTable() + " for " + identifier + ": " + e.getMessage());
            }
        });

        if (hasClaims()) getDatabase().executeQuery(tables(
                "SELECT `CollectionId`, `Level` FROM `%claims%` WHERE `Uuid` = ?;"), stmt -> {
            try {
                stmt.setString(1, identifier);
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to bind claims load: " + e.getMessage());
            }
        }, rs -> {
            try {
                while (rs.next()) {
                    found.set(true);
                    player.getClaimed().add(TrackedPlayer.claimKey(rs.getString("CollectionId"), rs.getInt("Level")));
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
        getDatabase().executeQuery(tables(
                "SELECT 1 FROM `%amounts%` WHERE `Uuid` = ? LIMIT 1;"), stmt -> {
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

    /** One row of a leaderboard. {@code score} is what it ranks by; {@code amount} is that rounded down. */
    public static final class Ranked {
        public final String uuid;
        public final String name;
        public final long amount;
        public final double score;

        public Ranked(String uuid, String name, double score) {
            this.uuid = uuid;
            this.name = name;
            this.score = score;
            this.amount = (long) score;
        }
    }

    /**
     * The top {@code limit} players by the weighted sum of their amounts: each id's amount times
     * its weight. The weights are bound as parameters, so a weight change applies to everyone at
     * once without rewriting any row.
     */
    public List<Ranked> topWeighted(Map<String, Double> weights, int limit) {
        List<Ranked> rows = new ArrayList<>();
        if (weights.isEmpty()) return rows;

        ensureTables();

        List<Map.Entry<String, Double>> entries = new ArrayList<>(weights.entrySet());
        StringBuilder cases = new StringBuilder("CASE `CollectionId`");
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            cases.append(" WHEN ? THEN ?");
            in.append(i == 0 ? "?" : ", ?");
        }
        cases.append(" ELSE 0 END");

        String sql = tables("SELECT `Uuid`, MAX(`Name`) AS `Name`, SUM(`Amount` * " + cases + ") AS `Total` " +
                "FROM `%amounts%` WHERE `CollectionId` IN (" + in + ") " +
                "GROUP BY `Uuid` HAVING SUM(`Amount` * " + cases + ") > 0 ORDER BY `Total` DESC LIMIT " + Math.max(1, limit) + ";");

        getDatabase().executeQuery(sql, stmt -> {
            try {
                int index = 1;
                for (Map.Entry<String, Double> entry : entries) {
                    stmt.setString(index++, entry.getKey());
                    stmt.setDouble(index++, entry.getValue());
                }
                for (Map.Entry<String, Double> entry : entries) stmt.setString(index++, entry.getKey());
                for (Map.Entry<String, Double> entry : entries) {
                    stmt.setString(index++, entry.getKey());
                    stmt.setDouble(index++, entry.getValue());
                }
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to bind weighted leaderboard query: " + e.getMessage());
            }
        }, rs -> {
            try {
                while (rs.next()) rows.add(new Ranked(rs.getString("Uuid"), rs.getString("Name"), rs.getDouble("Total")));
            } catch (Exception e) {
                StreamlineCollections.getInstance().logWarning("Failed to read weighted leaderboard: " + e.getMessage());
            }
        });
        return rows;
    }

    /** The top {@code limit} players by the summed amount of {@code collectionIds}. */
    public List<Ranked> top(List<String> collectionIds, int limit) {
        List<Ranked> rows = new ArrayList<>();
        if (collectionIds.isEmpty()) return rows;

        ensureTables();

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < collectionIds.size(); i++) in.append(i == 0 ? "?" : ", ?");

        String sql = tables("SELECT `Uuid`, MAX(`Name`) AS `Name`, SUM(`Amount`) AS `Total` " +
                "FROM `%amounts%` WHERE `CollectionId` IN (" + in + ") " +
                "GROUP BY `Uuid` HAVING SUM(`Amount`) > 0 ORDER BY `Total` DESC LIMIT " + Math.max(1, limit) + ";");

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
