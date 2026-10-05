package gg.drak.tacoessentials.data;

import singularity.Singularity;
import singularity.database.CoreDBOperator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * TacoEssentials' tables in Streamline's main database (SQLite or MySQL, as configured in
 * StreamlineCore), so admins can view and edit offline players' data with any SQL client.
 * Every statement is written in the dialect both engines accept: {@code REPLACE INTO} for
 * upserts, {@code VARCHAR} keys and backtick-quoted names.
 *
 * <p>Calls are synchronous; commands and login handling call them on the server thread, as
 * the rest of Streamline does.</p>
 */
public final class TacoDatabase {

    /** The kind of a history row: an earlier position for {@code /back}, or a death. */
    public static final String BACK = "back";
    public static final String DEATH = "death";

    /** Keys of the server-wide locations. */
    public static final String SPAWN = "spawn";
    public static final String FIRST_SPAWN = "first_spawn";

    private static final String LOC_COLUMNS = "`world` VARCHAR(128) NOT NULL, `x` DOUBLE NOT NULL, `y` DOUBLE NOT NULL, "
            + "`z` DOUBLE NOT NULL, `yaw` FLOAT NOT NULL, `pitch` FLOAT NOT NULL";

    /** Orders history rows; millisecond time alone collides when two are written in one tick. */
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() * 1000);

    private TacoDatabase() {}

    /** A named location: a home or a warp. */
    public static final class Named {
        private final String name;
        private final Loc location;

        Named(String name, Loc location) {
            this.name = name;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public Loc getLocation() {
            return location;
        }
    }

    /** What the database knows about a player who has joined at least once. */
    public static final class PlayerRecord {
        private final String uuid;
        private final String name;
        private final boolean muted;
        private final boolean fly;
        private final Loc lastLocation;

        PlayerRecord(String uuid, String name, boolean muted, boolean fly, Loc lastLocation) {
            this.uuid = uuid;
            this.name = name;
            this.muted = muted;
            this.fly = fly;
            this.lastLocation = lastLocation;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public boolean isMuted() {
            return muted;
        }

        public boolean isFly() {
            return fly;
        }

        /** Where the player logged out last, or {@code null} if never recorded. */
        public Loc getLastLocation() {
            return lastLocation;
        }
    }

    public static void ensureTables() {
        db().execute(
                "CREATE TABLE IF NOT EXISTS `" + table("players") + "` (`uuid` VARCHAR(36) NOT NULL PRIMARY KEY, "
                        + "`name` VARCHAR(32) NOT NULL, `muted` INT NOT NULL DEFAULT 0, `fly` INT NOT NULL DEFAULT 0, "
                        + "`last_world` VARCHAR(128), `last_x` DOUBLE, `last_y` DOUBLE, `last_z` DOUBLE, "
                        + "`last_yaw` FLOAT, `last_pitch` FLOAT);;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("homes") + "` (`uuid` VARCHAR(36) NOT NULL, "
                        + "`name` VARCHAR(64) NOT NULL, " + LOC_COLUMNS + ", PRIMARY KEY (`uuid`, `name`));;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("warps") + "` (`name` VARCHAR(64) NOT NULL PRIMARY KEY, "
                        + LOC_COLUMNS + ", `created_by` VARCHAR(36));;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("history") + "` (`uuid` VARCHAR(36) NOT NULL, "
                        + "`kind` VARCHAR(8) NOT NULL, `seq` BIGINT NOT NULL, " + LOC_COLUMNS
                        + ", PRIMARY KEY (`uuid`, `kind`, `seq`));;"
                        + "CREATE TABLE IF NOT EXISTS `" + table("server_locations") + "` (`loc_key` VARCHAR(32) NOT NULL PRIMARY KEY, "
                        + LOC_COLUMNS + ", `set_by` VARCHAR(36));;",
                s -> {});
    }

    // ---- players ----

    /** Records the player's current name, creating their row on first sight. */
    public static void recordPlayer(String uuid, String name) {
        if (player(uuid).isPresent()) {
            update("UPDATE `" + table("players") + "` SET `name` = ? WHERE `uuid` = ?;", name, uuid);
        } else {
            update("INSERT INTO `" + table("players") + "` (`uuid`, `name`) VALUES (?, ?);", uuid, name);
        }
    }

    public static Optional<PlayerRecord> player(String uuid) {
        return queryOne("SELECT * FROM `" + table("players") + "` WHERE `uuid` = ?;", TacoDatabase::readPlayer, uuid);
    }

    /** Case-insensitive lookup by the name the player last joined with. */
    public static Optional<PlayerRecord> playerByName(String name) {
        return queryOne("SELECT * FROM `" + table("players") + "` WHERE LOWER(`name`) = LOWER(?);", TacoDatabase::readPlayer, name);
    }

    public static List<String> knownNames() {
        return queryAll("SELECT `name` FROM `" + table("players") + "` ORDER BY `name`;", rs -> rs.getString("name"));
    }

    public static void setMuted(String uuid, boolean muted) {
        update("UPDATE `" + table("players") + "` SET `muted` = ? WHERE `uuid` = ?;", muted ? 1 : 0, uuid);
    }

    public static void setFly(String uuid, boolean fly) {
        update("UPDATE `" + table("players") + "` SET `fly` = ? WHERE `uuid` = ?;", fly ? 1 : 0, uuid);
    }

    public static void saveLastLocation(String uuid, Loc loc) {
        update("UPDATE `" + table("players") + "` SET `last_world` = ?, `last_x` = ?, `last_y` = ?, `last_z` = ?, "
                + "`last_yaw` = ?, `last_pitch` = ? WHERE `uuid` = ?;",
                loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), uuid);
    }

    private static PlayerRecord readPlayer(ResultSet rs) throws SQLException {
        String world = rs.getString("last_world");
        Loc last = world == null ? null : new Loc(world, rs.getDouble("last_x"), rs.getDouble("last_y"),
                rs.getDouble("last_z"), rs.getFloat("last_yaw"), rs.getFloat("last_pitch"));
        return new PlayerRecord(rs.getString("uuid"), rs.getString("name"), rs.getInt("muted") != 0,
                rs.getInt("fly") != 0, last);
    }

    // ---- homes ----

    public static List<Named> homes(String uuid) {
        return queryAll("SELECT * FROM `" + table("homes") + "` WHERE `uuid` = ? ORDER BY `name`;",
                rs -> new Named(rs.getString("name"), readLoc(rs)), uuid);
    }

    public static Optional<Loc> home(String uuid, String name) {
        return queryOne("SELECT * FROM `" + table("homes") + "` WHERE `uuid` = ? AND `name` = ?;", TacoDatabase::readLoc, uuid, name);
    }

    public static void setHome(String uuid, String name, Loc loc) {
        update("REPLACE INTO `" + table("homes") + "` (`uuid`, `name`, `world`, `x`, `y`, `z`, `yaw`, `pitch`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);", uuid, name, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch());
    }

    /** @return whether a home was deleted */
    public static boolean deleteHome(String uuid, String name) {
        if (home(uuid, name).isEmpty()) return false;
        update("DELETE FROM `" + table("homes") + "` WHERE `uuid` = ? AND `name` = ?;", uuid, name);
        return true;
    }

    // ---- warps ----

    public static List<Named> warps() {
        return queryAll("SELECT * FROM `" + table("warps") + "` ORDER BY `name`;", rs -> new Named(rs.getString("name"), readLoc(rs)));
    }

    public static Optional<Loc> warp(String name) {
        return queryOne("SELECT * FROM `" + table("warps") + "` WHERE `name` = ?;", TacoDatabase::readLoc, name);
    }

    public static void setWarp(String name, Loc loc, String createdBy) {
        update("REPLACE INTO `" + table("warps") + "` (`name`, `world`, `x`, `y`, `z`, `yaw`, `pitch`, `created_by`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);", name, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(), createdBy);
    }

    /** @return whether a warp was deleted */
    public static boolean deleteWarp(String name) {
        if (warp(name).isEmpty()) return false;
        update("DELETE FROM `" + table("warps") + "` WHERE `name` = ?;", name);
        return true;
    }

    // ---- server-wide locations ----

    public static Optional<Loc> serverLocation(String key) {
        return queryOne("SELECT * FROM `" + table("server_locations") + "` WHERE `loc_key` = ?;", TacoDatabase::readLoc, key);
    }

    public static void setServerLocation(String key, Loc loc, String setBy) {
        update("REPLACE INTO `" + table("server_locations") + "` (`loc_key`, `world`, `x`, `y`, `z`, `yaw`, `pitch`, `set_by`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?);", key, loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(), setBy);
    }

    // ---- history (/back and deaths) ----

    /** Adds a history entry and drops the oldest beyond {@code keep}. */
    public static void push(String uuid, String kind, Loc loc, int keep) {
        update("INSERT INTO `" + table("history") + "` (`uuid`, `kind`, `seq`, `world`, `x`, `y`, `z`, `yaw`, `pitch`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);", uuid, kind, SEQUENCE.incrementAndGet(), loc.getWorld(),
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        // The inner select is wrapped in a derived table because MySQL cannot LIMIT an IN subquery directly.
        update("DELETE FROM `" + table("history") + "` WHERE `uuid` = ? AND `kind` = ? AND `seq` NOT IN "
                + "(SELECT `seq` FROM (SELECT `seq` FROM `" + table("history") + "` WHERE `uuid` = ? AND `kind` = ? "
                + "ORDER BY `seq` DESC LIMIT " + Math.max(1, keep) + ") AS `newest`);", uuid, kind, uuid, kind);
    }

    /** The nth newest entry (1 = newest). */
    public static Optional<Loc> history(String uuid, String kind, int n) {
        return queryOne("SELECT * FROM `" + table("history") + "` WHERE `uuid` = ? AND `kind` = ? "
                + "ORDER BY `seq` DESC LIMIT 1 OFFSET " + Math.max(0, n - 1) + ";", TacoDatabase::readLoc, uuid, kind);
    }

    public static int historyCount(String uuid, String kind) {
        return queryOne("SELECT COUNT(*) AS `n` FROM `" + table("history") + "` WHERE `uuid` = ? AND `kind` = ?;",
                rs -> rs.getInt("n"), uuid, kind).orElse(0);
    }

    /** Removes and returns the newest entry. */
    public static Optional<Loc> pop(String uuid, String kind) {
        Optional<Long> seq = queryOne("SELECT `seq` FROM `" + table("history") + "` WHERE `uuid` = ? AND `kind` = ? "
                + "ORDER BY `seq` DESC LIMIT 1;", rs -> rs.getLong("seq"), uuid, kind);
        if (seq.isEmpty()) return Optional.empty();
        Optional<Loc> loc = queryOne("SELECT * FROM `" + table("history") + "` WHERE `uuid` = ? AND `kind` = ? AND `seq` = ?;",
                TacoDatabase::readLoc, uuid, kind, seq.get());
        update("DELETE FROM `" + table("history") + "` WHERE `uuid` = ? AND `kind` = ? AND `seq` = ?;", uuid, kind, seq.get());
        return loc;
    }

    // ---- plumbing ----

    private static Loc readLoc(ResultSet rs) throws SQLException {
        return new Loc(rs.getString("world"), rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                rs.getFloat("yaw"), rs.getFloat("pitch"));
    }

    private interface Row<T> {
        T read(ResultSet rs) throws SQLException;
    }

    private static CoreDBOperator db() {
        return Singularity.getMainDatabase();
    }

    private static String table(String name) {
        return db().getConnectorSet().getTablePrefix() + "taco_" + name;
    }

    private static Consumer<PreparedStatement> bind(Object... params) {
        return stmt -> {
            try {
                for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private static void update(String sql, Object... params) {
        db().executeSingle(sql, bind(params));
    }

    private static <T> Optional<T> queryOne(String sql, Row<T> row, Object... params) {
        AtomicReference<T> out = new AtomicReference<>();
        db().executeQuery(sql, bind(params), rs -> {
            try {
                if (rs.next()) out.set(row.read(rs));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        return Optional.ofNullable(out.get());
    }

    private static <T> List<T> queryAll(String sql, Row<T> row, Object... params) {
        List<T> out = new ArrayList<>();
        db().executeQuery(sql, bind(params), rs -> {
            try {
                while (rs.next()) out.add(row.read(rs));
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        return out;
    }
}
