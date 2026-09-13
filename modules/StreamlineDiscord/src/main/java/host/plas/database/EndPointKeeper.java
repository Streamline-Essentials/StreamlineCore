package host.plas.database;

import host.plas.discord.data.channeling.EndPoint;
import host.plas.discord.data.channeling.EndPointType;
import singularity.database.modules.DBKeeper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class EndPointKeeper extends DBKeeper<EndPoint> {
    public EndPointKeeper() {
        super("endpoint-keeper", EndPoint::new);
    }

    @Override
    public void ensureMysqlTables() {
        String s1 = "CREATE TABLE IF NOT EXISTS %table_prefix%discord_endpoints (" +
                "Uuid VARCHAR(36) NOT NULL," +
                "Type VARCHAR(255) NOT NULL," +
                "Identifier VARCHAR(36) NOT NULL," +
                "Format TEXT NOT NULL," +
                "PRIMARY KEY (Uuid)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {});
    }

    @Override
    public void ensureSqliteTables() {
        String s1 = "CREATE TABLE IF NOT EXISTS %table_prefix%discord_endpoints (" +
                "Uuid VARCHAR(36) NOT NULL," +
                "Type VARCHAR(255) NOT NULL," +
                "Identifier VARCHAR(36) NOT NULL," +
                "Format TEXT NOT NULL," +
                "PRIMARY KEY (Uuid)" +
                ");";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {});
    }

    @Override
    public void saveMysql(EndPoint route) {
        ensureTables();

        String s1 = "INSERT INTO %table_prefix%discord_endpoints (" +
                "Uuid, Type, Identifier, Format" +
                ") VALUES (" +
                "?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "Type = ?, Identifier = ?, Format = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", route.getIdentifier());
        s1 = s1.replace("%type%", route.getType().name());
        s1 = s1.replace("%identifier%", route.getEndPointIdentifier());
        s1 = s1.replace("%format%", route.getToFormat());

        getDatabase().execute(s1, stmt -> {
            try {
                stmt.setString(1, route.getIdentifier());
                stmt.setString(2, route.getType().name());
                stmt.setString(3, route.getEndPointIdentifier());
                stmt.setString(4, route.getToFormat());

                stmt.setString(5, route.getType().name());
                stmt.setString(6, route.getEndPointIdentifier());
                stmt.setString(7, route.getToFormat());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void saveSqlite(EndPoint route) {
        ensureTables();

        String s1 = "INSERT OR REPLACE INTO %table_prefix%discord_endpoints (" +
                "Uuid, Type, Identifier, Format" +
                ") VALUES (" +
                "?, ?, ?, ?" +
                ");";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", route.getIdentifier());
        s1 = s1.replace("%type%", route.getType().name());
        s1 = s1.replace("%identifier%", route.getEndPointIdentifier());
        s1 = s1.replace("%format%", route.getToFormat());

        getDatabase().execute(s1, stmt -> {
            try {
                stmt.setString(1, route.getIdentifier());
                stmt.setString(2, route.getType().name());
                stmt.setString(3, route.getEndPointIdentifier());
                stmt.setString(4, route.getToFormat());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public Optional<EndPoint> loadMysql(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_endpoints WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Optional<EndPoint>> atomicReference = new AtomicReference<>(Optional.empty());
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
                    String type = resultSet.getString("Type");
                    String identifier = resultSet.getString("Identifier");
                    String format = resultSet.getString("Format");

                    EndPoint endPoint = new EndPoint(uuid);
                    endPoint.setType(EndPointType.valueOf(type));
                    endPoint.setEndPointIdentifier(identifier);
                    endPoint.setToFormat(format);

                    atomicReference.set(Optional.of(endPoint));
                } else {
                    atomicReference.set(Optional.empty());
                }
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(Optional.empty());
            }
        });

        return atomicReference.get();
    }

    @Override
    public Optional<EndPoint> loadSqlite(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_endpoints WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());
        s1 = s1.replace("%uuid%", s);

        AtomicReference<Optional<EndPoint>> atomicReference = new AtomicReference<>(Optional.empty());
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
                    String type = resultSet.getString("Type");
                    String identifier = resultSet.getString("Identifier");
                    String format = resultSet.getString("Format");

                    EndPoint endPoint = new EndPoint(uuid);
                    endPoint.setType(EndPointType.valueOf(type));
                    endPoint.setEndPointIdentifier(identifier);
                    endPoint.setToFormat(format);

                    atomicReference.set(Optional.of(endPoint));
                } else {
                    atomicReference.set(Optional.empty());
                }
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(Optional.empty());
            }
        });

        return atomicReference.get();
    }

    @Override
    public boolean existsMysql(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_endpoints WHERE Uuid = ?;";

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
                boolean b = resultSet.next();

                atomicReference.set(b);
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(false);
            }
        });

        return atomicReference.get();
    }

    @Override
    public boolean existsSqlite(String s) {
        ensureTables();

        String s1 = "SELECT * FROM %table_prefix%discord_endpoints WHERE Uuid = ?;";

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
                boolean b = resultSet.next();

                atomicReference.set(b);
            } catch (Exception e) {
                e.printStackTrace();
                atomicReference.set(false);
            }
        });

        return atomicReference.get();
    }

    public void drop(String id) {
        ensureTables();

        String s1 = "DELETE FROM %table_prefix%discord_endpoints WHERE Uuid = ?;";

        s1 = s1.replace("%table_prefix%", getDatabase().getConnectorSet().getTablePrefix());

        getDatabase().execute(s1, stmt -> {
            try {
                stmt.setString(1, id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void drop(EndPoint endPoint) {
        drop(endPoint.getIdentifier());
    }
}
