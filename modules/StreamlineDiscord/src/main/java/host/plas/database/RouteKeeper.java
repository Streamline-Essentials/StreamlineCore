package host.plas.database;

import host.plas.StreamlineDiscord;
import host.plas.bou.sql.DbArg;
import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.Route;
import host.plas.discord.data.channeling.RouteLoader;
import host.plas.discord.data.events.EventClassifier;
import singularity.database.modules.DBKeeper;

import javax.annotation.Nullable;
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

        addColumnIfNotExistsSQLite(table, "InputUuid", "VARCHAR(36) NOT NULL");
        addColumnIfNotExistsSQLite(table, "OutputUuid", "VARCHAR(36) NOT NULL");
        addColumnIfNotExistsSQLite(table, "EnabledEvents", "TEXT NOT NULL");
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

    private void addColumnIfNotExistsSQLite(String table, String column, String definition, @Nullable String afterColumn) {
        String alter = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition + (afterColumn != null ? " AFTER " + afterColumn : "");

        try {
            getDatabase().execute(alter, stmt -> {});
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("duplicate") || msg.contains("already exists") || msg.contains("duplicate column name")) {
                // expected - column already exists → ignore silently
            } else {
                // unexpected error → at least log it
                e.printStackTrace();
            }
        }
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
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_routes WHERE Uuid = ?;";
        
        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Optional<Route>> optionalRoute = new AtomicReference<>(Optional.empty());
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                if (resultSet.next()) {
                    String uuid = resultSet.getString("Uuid");
                    String inputUuid = resultSet.getString("InputUuid");
                    String outputUuid = resultSet.getString("OutputUuid");
                    String enabledEventsStr = resultSet.getString("EnabledEvents");

                    Optional<EndPoint> input = StreamlineDiscord.getEndPointKeeper().load(inputUuid).join();
                    if (input.isEmpty()) {
                        optionalRoute.set(Optional.empty());
                        return;
                    }
                    
                    Optional<EndPoint> output = StreamlineDiscord.getEndPointKeeper().load(outputUuid).join();
                    if (output.isEmpty()) {
                        optionalRoute.set(Optional.empty());
                        return;
                    }
                    
                    EndPoint in = input.get();
                    EndPoint out = output.get();

                    Route route = new Route(uuid);
                    route.setInput(in);
                    route.setOutput(out);
                    route.setEnabledEventsFromString(enabledEventsStr);
                    
                    optionalRoute.set(Optional.of(route));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        return optionalRoute.get();
    }

    @Override
    public Optional<Route> loadSqlite(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_routes WHERE Uuid = ?;";
        
        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Optional<Route>> optionalRoute = new AtomicReference<>(Optional.empty());
        getDatabase().executeQuery(s1, stmt -> {
            try {
                stmt.setString(1, s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resultSet -> {
            try {
                if (resultSet.next()) {
                    String uuid = resultSet.getString("Uuid");
                    String inputUuid = resultSet.getString("InputUuid");
                    String outputUuid = resultSet.getString("OutputUuid");
                    String enabledEventsStr = resultSet.getString("EnabledEvents");

                    Optional<EndPoint> input = StreamlineDiscord.getEndPointKeeper().load(inputUuid).join();
                    if (input.isEmpty()) {
                        optionalRoute.set(Optional.empty());
                        return;
                    }
                    
                    Optional<EndPoint> output = StreamlineDiscord.getEndPointKeeper().load(outputUuid).join();
                    if (output.isEmpty()) {
                        optionalRoute.set(Optional.empty());
                        return;
                    }
                    
                    EndPoint in = input.get();
                    EndPoint out = output.get();

                    Route route = new Route(uuid);
                    route.setInput(in);
                    route.setOutput(out);
                    route.setEnabledEventsFromString(enabledEventsStr);
                    
                    optionalRoute.set(Optional.of(route));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        return optionalRoute.get();
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

        String s1 = "SELECT * FROM %table_prefix%discord_routes;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().executeQuery(s1, stmt -> {}, resultSet -> {
            try {
                while (resultSet.next()) {
                    String uuid = resultSet.getString("Uuid");
                    String inputUuid = resultSet.getString("InputUuid");
                    String outputUuid = resultSet.getString("OutputUuid");
                    String enabledEventsStr = resultSet.getString("EnabledEvents");

                    Optional<EndPoint> input = StreamlineDiscord.getEndPointKeeper().load(inputUuid).join();
                    if (input.isEmpty()) continue;

                    Optional<EndPoint> output = StreamlineDiscord.getEndPointKeeper().load(outputUuid).join();
                    if (output.isEmpty()) continue;

                    EndPoint in = input.get();
                    EndPoint out = output.get();

                    Route route = new Route(uuid);
                    route.setInput(in);
                    route.setOutput(out);
                    route.setEnabledEventsFromString(enabledEventsStr);

                    RouteLoader.registerRoute(route);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
