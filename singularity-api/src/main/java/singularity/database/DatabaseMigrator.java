package singularity.database;

import singularity.configs.given.DatabaseConfigHandler;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.utils.MessageUtils;
import singularity.utils.UserUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Copies Streamline's data from one {@link DatabaseType} to another and switches the
 * server over to the target.
 *
 * <p>Both sides are built from {@code database-config.yml}: the same host, credentials,
 * database name, table prefix and SQLite file name, each with its own type. Every table
 * whose name starts with the table prefix is copied (every table when the prefix is empty),
 * so module tables move along with the core's. The core schema is created on the target
 * first, with its own keys and types; other tables are created from the source's columns
 * and primary key. Rows already in a copied target table are replaced.</p>
 *
 * <p>When every table copied, the configured type is set to the target and the main
 * {@link CoreDBOperator} reconnects to it in place, so code holding the operator keeps
 * working.</p>
 */
public final class DatabaseMigrator {

    /** Rows sent to the target per batch. */
    private static final int BATCH_SIZE = 500;

    private DatabaseMigrator() {
    }

    /**
     * What a migration did, or why it did not run: whether every table copied and the
     * server now uses the target, how many tables and rows copied, a short reason when it
     * failed, and the target tables already holding rows when it was refused for them.
     */
    public static final class Result {
        private final boolean success;
        private final int tables;
        private final long rows;
        private final String message;
        private final List<String> occupied;

        private Result(boolean success, int tables, long rows, String message, List<String> occupied) {
            this.success = success;
            this.tables = tables;
            this.rows = rows;
            this.message = message;
            this.occupied = occupied;
        }

        static Result failed(String message) {
            return new Result(false, 0, 0, message, List.of());
        }

        public boolean isSuccess() {
            return success;
        }

        public int getTables() {
            return tables;
        }

        public long getRows() {
            return rows;
        }

        public String getMessage() {
            return message;
        }

        /**
         * @return target tables holding rows that the copy would replace; non-empty only
         *         when the migration was refused for that reason
         */
        public List<String> getOccupied() {
            return occupied;
        }
    }

    /**
     * Builds the connector for {@code type} from {@code database-config.yml}.
     *
     * @param type the database type to connect to
     * @return the configured connection values with that type
     */
    public static ConnectorSet configured(DatabaseType type) {
        ConnectorSet set = GivenConfigs.getDatabaseConfig().getConnectorSet();
        set.setType(type);
        return set;
    }

    /**
     * Copies every Streamline table from {@code from} to {@code to}, then makes {@code to}
     * the configured and live database. Blocks; run it off the server thread.
     *
     * @param from      the database to copy from
     * @param to        the database to copy into and switch to
     * @param overwrite whether to replace rows already in the target's tables; without it,
     *                  the migration is refused when any copied target table holds rows
     * @param progress  receives a line per step
     * @return what happened
     */
    public static synchronized Result migrate(DatabaseType from, DatabaseType to, boolean overwrite, Consumer<String> progress) {
        if (from == to) return Result.failed("the source and target are the same type");

        // Flush what players have in memory, so the source holds their latest data.
        CoreDBOperator main = GivenConfigs.getMainDatabase();
        if (main != null && main.getType() == from) {
            progress.accept("Saving loaded players...");
            for (CosmicSender sender : UserUtils.getLoadedSenders().values()) {
                try {
                    main.saveSender(sender, false);
                } catch (Exception e) {
                    MessageUtils.logWarning("Failed to save " + sender.getCurrentName() + " before migrating", e);
                }
            }
        }

        CoreDBOperator source = new CoreDBOperator(configured(from));
        CoreDBOperator target = new CoreDBOperator(configured(to));
        try {
            String prefix = source.getConnectorSet().getTablePrefix();
            if (prefix == null) prefix = "";

            // The core schema on both sides: the source may predate a column, and the target
            // gets the core's own keys and types rather than ones derived from the source.
            source.ensureUsable();
            target.ensureUsable();

            try (Connection src = source.getDataSource().getConnection();
                 Connection dst = target.getDataSource().getConnection()) {
                List<String> tables = listTables(src, from, prefix);
                if (tables.isEmpty()) return Result.failed("the " + from + " database has no tables starting with '" + prefix + "'");

                Set<String> existing = new LinkedHashSet<>(listTables(dst, to, prefix));

                if (! overwrite) {
                    List<String> occupied = new ArrayList<>();
                    for (String table : tables) {
                        if (existing.contains(table) && hasRows(dst, table)) occupied.add(table);
                    }
                    if (! occupied.isEmpty()) return new Result(false, 0, 0, "the " + to + " database already holds data", occupied);
                }

                long rows = 0;
                for (String table : tables) {
                    if (! existing.contains(table)) {
                        createTable(src, dst, from, to, table);
                    }
                    long copied = copyTable(src, dst, table);
                    rows += copied;
                    progress.accept("Copied " + table + " (" + copied + " rows).");
                }

                switchTo(to);
                return new Result(true, tables.size(), rows, null, List.of());
            }
        } catch (Exception e) {
            MessageUtils.logWarning("Failed to migrate the database from " + from + " to " + to, e);
            return Result.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            source.close();
            target.close();
        }
    }

    /**
     * Sets the configured type to {@code to} and reconnects the main operator to it.
     */
    private static void switchTo(DatabaseType to) {
        DatabaseConfigHandler config = GivenConfigs.getDatabaseConfig();
        config.getResource().set("type", to.name());

        CoreDBOperator main = GivenConfigs.getMainDatabase();
        if (main == null) return;

        // close() drops the pool and re-arms the schema check, so the next query connects
        // to the new connector.
        main.setConnectorSet(config.getConnectorSet());
        main.close();
        main.ensureUsable();
    }

    private static List<String> listTables(Connection connection, DatabaseType type, String prefix) throws Exception {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = connection.getMetaData();
        String catalog = type == DatabaseType.MYSQL ? connection.getCatalog() : null;
        try (ResultSet rs = meta.getTables(catalog, null, "%", new String[] { "TABLE" })) {
            while (rs.next()) {
                String name = rs.getString("TABLE_NAME");
                if (name == null || name.startsWith("sqlite_")) continue;
                if (! prefix.isEmpty() && ! name.startsWith(prefix)) continue;
                tables.add(name);
            }
        }
        return tables;
    }

    private static boolean hasRows(Connection connection, String table) throws Exception {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM " + quote(table) + " LIMIT 1")) {
            return rs.next();
        }
    }

    /**
     * Creates {@code table} on the target from the source's columns and primary key.
     */
    private static void createTable(Connection src, Connection dst, DatabaseType from, DatabaseType to, String table) throws Exception {
        DatabaseMetaData meta = src.getMetaData();
        String catalog = from == DatabaseType.MYSQL ? src.getCatalog() : null;

        Set<String> keys = new LinkedHashSet<>();
        try (ResultSet rs = meta.getPrimaryKeys(catalog, null, table)) {
            Map<Integer, String> ordered = new java.util.TreeMap<>();
            while (rs.next()) ordered.put(rs.getInt("KEY_SEQ"), rs.getString("COLUMN_NAME"));
            keys.addAll(ordered.values());
        }

        Map<String, String> columns = new LinkedHashMap<>();
        try (ResultSet rs = meta.getColumns(catalog, null, table, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME");
                int size = rs.getInt("COLUMN_SIZE");
                boolean notNull = rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
                String definition = columnType(type, size, keys.contains(name), to) + (notNull ? " NOT NULL" : "");
                columns.put(name, definition);
            }
        }
        if (columns.isEmpty()) throw new IllegalStateException("Could not read the columns of " + table);

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(quote(table)).append(" (");
        boolean first = true;
        for (Map.Entry<String, String> column : columns.entrySet()) {
            if (! first) sql.append(", ");
            sql.append(quote(column.getKey())).append(' ').append(column.getValue());
            first = false;
        }
        if (! keys.isEmpty()) {
            sql.append(", PRIMARY KEY (");
            first = true;
            for (String key : keys) {
                if (! first) sql.append(", ");
                sql.append(quote(key));
                first = false;
            }
            sql.append(')');
        }
        sql.append(')');

        try (Statement stmt = dst.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    /**
     * Maps a source column type onto the target. SQLite accepts any declared type, so only
     * MySQL needs translating: SQLite's types are affinities, and MySQL cannot key a
     * {@code TEXT} column without a length.
     */
    static String columnType(String sourceType, int size, boolean key, DatabaseType to) {
        String type = sourceType == null ? "" : sourceType.toUpperCase(Locale.ROOT).trim();

        if (to == DatabaseType.SQLITE) {
            return type.isEmpty() ? "TEXT" : type;
        }

        if (type.contains("BOOL")) return "BOOLEAN";
        if (type.contains("INT")) return "BIGINT";
        if (type.contains("REAL") || type.contains("DOUB") || type.contains("FLOA")) return "DOUBLE";
        if (type.contains("DEC") || type.contains("NUMERIC")) return "DECIMAL(65,10)";
        if (type.contains("BLOB")) return "LONGBLOB";
        if (type.contains("DATETIME") || type.contains("TIMESTAMP")) return "DATETIME";
        if (type.contains("CHAR") && size > 0 && size <= 16383) return "VARCHAR(" + size + ")";
        return key ? "VARCHAR(255)" : "LONGTEXT";
    }

    /**
     * Replaces the target table's rows with the source's, in one transaction, copying the
     * columns both sides have.
     *
     * @return rows copied
     */
    private static long copyTable(Connection src, Connection dst, String table) throws Exception {
        Set<String> targetColumns = new LinkedHashSet<>();
        try (Statement stmt = dst.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + quote(table) + " WHERE 1 = 0")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) targetColumns.add(meta.getColumnName(i).toLowerCase(Locale.ROOT));
        }

        boolean autoCommit = dst.getAutoCommit();
        dst.setAutoCommit(false);
        long rows = 0;
        try (Statement read = src.createStatement();
             ResultSet rs = read.executeQuery("SELECT * FROM " + quote(table))) {
            ResultSetMetaData meta = rs.getMetaData();
            List<Integer> indexes = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String name = meta.getColumnName(i);
                if (targetColumns.contains(name.toLowerCase(Locale.ROOT))) {
                    indexes.add(i);
                    names.add(name);
                }
            }
            if (names.isEmpty()) throw new IllegalStateException("No columns of " + table + " exist on the target");

            try (Statement clear = dst.createStatement()) {
                clear.executeUpdate("DELETE FROM " + quote(table));
            }

            StringBuilder insert = new StringBuilder("INSERT INTO ").append(quote(table)).append(" (");
            for (int i = 0; i < names.size(); i++) insert.append(i == 0 ? "" : ", ").append(quote(names.get(i)));
            insert.append(") VALUES (");
            for (int i = 0; i < names.size(); i++) insert.append(i == 0 ? "?" : ", ?");
            insert.append(')');

            try (PreparedStatement write = dst.prepareStatement(insert.toString())) {
                int pending = 0;
                while (rs.next()) {
                    for (int i = 0; i < indexes.size(); i++) {
                        write.setObject(i + 1, rs.getObject(indexes.get(i)));
                    }
                    write.addBatch();
                    rows++;
                    if (++pending >= BATCH_SIZE) {
                        write.executeBatch();
                        pending = 0;
                    }
                }
                if (pending > 0) write.executeBatch();
            }
            dst.commit();
        } catch (Exception e) {
            dst.rollback();
            throw e;
        } finally {
            dst.setAutoCommit(autoCommit);
        }
        return rows;
    }

    /** Backticks quote identifiers on MySQL and SQLite alike. */
    private static String quote(String identifier) {
        return '`' + identifier.replace("`", "``") + '`';
    }
}
