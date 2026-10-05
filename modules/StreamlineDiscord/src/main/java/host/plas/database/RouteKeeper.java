package host.plas.database;

import host.plas.StreamlineDiscord;
import host.plas.bou.sql.DbArg;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.events.EventClassifier;
import singularity.database.modules.DBKeeper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RouteKeeper extends DBKeeper<Route> {
    public RouteKeeper() {
        super("route-keeper", Route::new);
    }

    @Override
    public void ensureMysqlTables() {
        String s1 = "CREATE TABLE IF NOT EXISTS %table_prefix%discord_routes (" +
                "Uuid VARCHAR(36) NOT NULL," +
                "InputUuid VARCHAR(36) NOT NULL," +
                "OutputUuid VARCHAR(36) NOT NULL," +
                "EnabledEvents TEXT NOT NULL," +
                "PRIMARY KEY (Uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {});

        String table = "%table_prefix%discord_routes".replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        addColumnIfNotExistsMySQL(table, "InputUuid", "VARCHAR(36) NOT NULL");
        addColumnIfNotExistsMySQL(table, "OutputUuid", "VARCHAR(36) NOT NULL");
        addColumnIfNotExistsMySQL(table, "EnabledEvents", "TEXT NOT NULL");
    }

    @Override
    public void ensureSqliteTables() {
        String s1 = "CREATE TABLE IF NOT EXISTS %table_prefix%discord_routes (" +
                "Uuid VARCHAR(36) NOT NULL," +
                "InputUuid VARCHAR(36) NOT NULL," +
                "OutputUuid VARCHAR(36) NOT NULL," +
                "EnabledEvents TEXT NOT NULL," +
                "PRIMARY KEY (Uuid)" +
                ");";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {});

        String table = "%table_prefix%discord_routes".replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        // SQLite rejects ADD COLUMN ... NOT NULL without a default on a table that already has rows.
        addColumnIfNotExistsSQLite(table, "InputUuid", "VARCHAR(36) NOT NULL DEFAULT ''");
        addColumnIfNotExistsSQLite(table, "OutputUuid", "VARCHAR(36) NOT NULL DEFAULT ''");
        addColumnIfNotExistsSQLite(table, "EnabledEvents", "TEXT NOT NULL DEFAULT ''");
    }

    // For MySQL - checks if column exists before adding
    private boolean columnExistsMySQL(String table, String column) {
        String sql =
                "SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() " +
                        "  AND TABLE_NAME = ? " +
                        "  AND COLUMN_NAME = ? " +
                        "LIMIT 1";

        AtomicBoolean exists = new AtomicBoolean(false);
        getDatabase().executeQuery(sql, ps -> {
            try {
                ps.setString(1, table);
                ps.setString(2, column);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, rs -> {
            try {
                exists.set(rs.next());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return exists.get();
    }

    private void addColumnIfNotExistsMySQL(String table, String column, String definition) {
        addColumnIfNotExistsMySQL(table, column, definition, null);
    }

    private void addColumnIfNotExistsMySQL(String table, String column, String definition, @Nullable String afterColumn) {
        if (! columnExistsMySQL(table, column)) {
            String alter = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + (afterColumn != null ? " AFTER " + afterColumn : "");
            getDatabase().execute(alter, stmt -> {});
        }
    }

    public void addColumnIfNotExistsSQLite(String table, String column, String definition) {
        addColumnIfNotExistsSQLite(table, column, definition, null);
    }

    // DBOperator logs and swallows SQL errors itself, so the column has to be checked
    // up front rather than by catching a "duplicate column" failure.
    private boolean columnExistsSQLite(String table, String column) {
        AtomicBoolean exists = new AtomicBoolean(false);
        getDatabase().executeQuery("PRAGMA table_info(" + table + ");", ps -> {}, rs -> {
            try {
                while (rs.next()) {
                    if (column.equalsIgnoreCase(rs.getString("name"))) {
                        exists.set(true);
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return exists.get();
    }

    // SQLite has no AFTER clause; afterColumn is ignored.
    private void addColumnIfNotExistsSQLite(String table, String column, String definition, @Nullable String afterColumn) {
        if (columnExistsSQLite(table, column)) return;

        getDatabase().execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + ";", stmt -> {});
    }

    @Override
    public void saveMysql(Route route) {
        ensureTables();

        String s1 = "INSERT INTO %table_prefix%discord_routes (" +
                "Uuid, InputUuid, OutputUuid, EnabledEvents" +
                ") VALUES (" +
                "?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "InputUuid = ?, " +
                "OutputUuid = ?, " +
                "EnabledEvents = ?" +
                ";";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", route.getIdentifier());
        s1 = s1.replace("%input_uuid%", route.getInput().getIdentifier());
        s1 = s1.replace("%output_uuid%", route.getOutput().getIdentifier());
        s1 = s1.replace("%enabled_events%", route.getEnabledEventsAsString());
        
        getDatabase().execute(s1, stmt -> {
            try {
                DbArg arg = new DbArg();

                stmt.setString(arg.next(), route.getIdentifier());

                stmt.setString(arg.next(), route.getInput().getIdentifier());
                stmt.setString(arg.next(), route.getOutput().getIdentifier());
                stmt.setString(arg.next(), route.getEnabledEventsAsString());

                stmt.setString(arg.next(), route.getInput().getIdentifier());
                stmt.setString(arg.next(), route.getOutput().getIdentifier());
                stmt.setString(arg.next(), route.getEnabledEventsAsString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        EndPoint input = route.getInput();
        if (input != null) StreamlineDiscord.getEndPointKeeper().saveMysql(input);
        EndPoint output = route.getOutput();
        if (output != null) StreamlineDiscord.getEndPointKeeper().saveMysql(output);
    }

    @Override
    public void saveSqlite(Route route) {
        ensureTables();

        String s1 = "INSERT OR REPLACE INTO %table_prefix%discord_routes (" +
                "Uuid, InputUuid, OutputUuid, EnabledEvents" +
                ") VALUES (" +
                "?, ?, ?, ?" +
                ");";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", route.getIdentifier());
        s1 = s1.replace("%input_uuid%", route.getInput().getIdentifier());
        s1 = s1.replace("%output_uuid%", route.getOutput().getIdentifier());
        s1 = s1.replace("%enabled_events%", route.getEnabledEventsAsString());
        
        getDatabase().execute(s1, stmt -> {
            try {
                DbArg arg = new DbArg();

                stmt.setString(arg.next(), route.getIdentifier());

                stmt.setString(arg.next(), route.getInput().getIdentifier());
                stmt.setString(arg.next(), route.getOutput().getIdentifier());
                stmt.setString(arg.next(), route.getEnabledEventsAsString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        EndPoint input = route.getInput();
        if (input != null) StreamlineDiscord.getEndPointKeeper().saveSqlite(input);
        EndPoint output = route.getOutput();
        if (output != null) StreamlineDiscord.getEndPointKeeper().saveSqlite(output);
    }

    @Override
    public Optional<Route> loadMysql(String s) {
        return loadRoute(s);
    }

    @Override
    public Optional<Route> loadSqlite(String s) {
        return loadRoute(s);
    }

    /**
     * One row of the routes table, read out of its {@link java.sql.ResultSet} so the
     * endpoints can be loaded after the query has released its connection.
     */
    private static class RouteRow {
        final String uuid, inputUuid, outputUuid, enabledEvents;

        RouteRow(java.sql.ResultSet rs) throws java.sql.SQLException {
            uuid = rs.getString("Uuid");
            inputUuid = rs.getString("InputUuid");
            outputUuid = rs.getString("OutputUuid");
            enabledEvents = rs.getString("EnabledEvents");
        }
    }

    // Both dialects share this SELECT.
    private Optional<Route> loadRoute(String s) {
        ensureTables();

        List<RouteRow> rows = queryRows("SELECT * FROM %table_prefix%discord_routes WHERE Uuid = ?;", s);
        if (rows.isEmpty()) return Optional.empty();

        return buildRoute(rows.get(0));
    }

    /**
     * Runs a routes query and copies every row out before returning. Endpoints must not be
     * loaded while the result set is open: SQLite's pool holds a single connection, so a
     * nested query would wait for the connection its own caller is holding.
     */
    private List<RouteRow> queryRows(String sql, @Nullable String uuid) {
        List<RouteRow> rows = new ArrayList<>();

        getDatabase().executeQuery(injectTablePrefix(sql), stmt -> {
            if (uuid == null) return;
            try {
                stmt.setString(1, uuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, rs -> {
            try {
                while (rs.next()) rows.add(new RouteRow(rs));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return rows;
    }

    private Optional<Route> buildRoute(RouteRow row) {
        Optional<EndPoint> input = StreamlineDiscord.getEndPointKeeper().loadRaw(row.inputUuid);
        Optional<EndPoint> output = StreamlineDiscord.getEndPointKeeper().loadRaw(row.outputUuid);
        if (input.isEmpty() || output.isEmpty()) {
            StreamlineDiscord.getInstance().logWarning("Skipping route '" + row.uuid + "': its "
                    + (input.isEmpty() ? "input endpoint '" + row.inputUuid + "'" : "output endpoint '" + row.outputUuid + "'")
                    + " is missing from the database.");
            return Optional.empty();
        }

        Route route = new Route(row.uuid);
        route.setInput(input.get());
        route.setOutput(output.get());
        route.setEnabledEventsFromString(row.enabledEvents);

        return Optional.of(route);
    }

    @Override
    public boolean existsMysql(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_routes WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Boolean> atomicReference = new AtomicReference<>(false);
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                atomicReference.set(resultSet.next());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return atomicReference.get();
    }

    @Override
    public boolean existsSqlite(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_routes WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Boolean> atomicReference = new AtomicReference<>(false);
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                atomicReference.set(resultSet.next());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return atomicReference.get();
    }

    public void drop(String id) {
        ensureTables();

        String s1 = "DELETE FROM %table_prefix%discord_routes WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {
            try {
                stmt.setString(1, id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void drop(Route route) {
        ensureTables();

        if (route.getInput() != null) route.getInput().drop();
        if (route.getOutput() != null) route.getOutput().drop();

        drop(route.getIdentifier());
    }

    public void loadAllRoutes() {
        ensureTables();

        for (RouteRow row : queryRows("SELECT * FROM %table_prefix%discord_routes;", null)) {
            buildRoute(row).ifPresent(RouteLoader::registerRoute);
        }
    }
}
