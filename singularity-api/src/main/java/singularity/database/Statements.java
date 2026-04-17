package singularity.database;

import lombok.Getter;
import lombok.Setter;

/**
 * Central repository of parameterised SQL statement templates used throughout
 * the Streamline persistence layer.
 *
 * <p>Two inner enums, {@link MySQL} and {@link SQLite}, hold the dialect-specific
 * SQL for each operation.  The {@link StatementType} enum enumerates every
 * logical operation so callers can request statements in a type-safe,
 * backend-agnostic manner via {@link #getStatement(StatementType, ConnectorSet)}.</p>
 *
 * <p>Templates use the following placeholders that are resolved at runtime:
 * <ul>
 *   <li>{@code %database%} — the configured database/schema name</li>
 *   <li>{@code %table_prefix%} — the configured table name prefix</li>
 * </ul>
 */
@Getter @Setter
public class Statements {

    /**
     * MySQL/MariaDB dialect SQL statement templates.
     *
     * <p>Each constant's {@link #getStatement()} value is a ready-to-use
     * parameterised SQL string that may still contain the
     * {@code %database%} and {@code %table_prefix%} placeholder tokens.</p>
     */
    @Getter
    public enum MySQL {
        /** Creates the database schema if it does not already exist. */
        CREATE_DATABASE("CREATE DATABASE IF NOT EXISTS `%database%`;"),

        /**
         * Creates all required tables and their indexes in a single
         * multi-statement batch (statements separated by {@code ;;}).
         */
        CREATE_TABLES("CREATE TABLE IF NOT EXISTS `%table_prefix%players` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "FirstJoin BIGINT, " +
                "LastJoin BIGINT, " +
                "CurrentName VARCHAR(64), " +
                "CurrentIP VARCHAR(15), " +
                "PlaySeconds INT, " +
                "ProxyTouched BOOLEAN " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_meta` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Nickname VARCHAR(255), " +
                "Prefix VARCHAR(255), " +
                "Suffix VARCHAR(255)" +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_location` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Server VARCHAR(255), " +
                "World VARCHAR(255), " +
                "X DOUBLE, " +
                "Y DOUBLE, " +
                "Z DOUBLE, " +
                "Yaw FLOAT, " +
                "Pitch FLOAT " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_permissions` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "BypassingPermissions BOOLEAN " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%uuid_info` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Usernames TEXT, " +
                "Ips TEXT " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%saves` (" +
                "SavedAt BIGINT PRIMARY KEY, " +
                "Uuid VARCHAR(36), " +
                "ServerUuid VARCHAR(36) " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%servers` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Name VARCHAR(255), " +
                "Type VARCHAR(255) " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%update` (" +
                "Type VARCHAR(255), " +
                "Identifier VARCHAR(36), " +
                "ServerUuid VARCHAR(36), " +
                "PostDate BIGINT " +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%tp_tickets` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "ServerName VARCHAR(255), " +
                "WorldName VARCHAR(36), " +
                "X DOUBLE, " +
                "Y DOUBLE, " +
                "Z DOUBLE, " +
                "Yaw FLOAT, " +
                "Pitch FLOAT, " +
                "PostDate BIGINT " +
                ");;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%players_Uuid` ON `%table_prefix%players` (Uuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%player_meta_Uuid` ON `%table_prefix%player_meta` (Uuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%player_location_Uuid` ON `%table_prefix%player_location` (Uuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%player_permissions_Uuid` ON `%table_prefix%player_permissions` (Uuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%uuid_info_Uuid` ON `%table_prefix%uuid_info` (Uuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%saves_SavedAt` ON `%table_prefix%saves` (SavedAt);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%servers_Uuid` ON `%table_prefix%servers` (Uuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%update_Type` ON `%table_prefix%update` (Type);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%update_Identifier` ON `%table_prefix%update` (Identifier);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%update_ServerUuid` ON `%table_prefix%update` (ServerUuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%update_PostDate` ON `%table_prefix%update` (PostDate);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%tp_tickets_Uuid` ON `%table_prefix%tp_tickets` (Uuid);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%tp_tickets_ServerName` ON `%table_prefix%tp_tickets` (ServerName);;" +
                "CREATE INDEX IF NOT EXISTS `%table_prefix%tp_tickets_PostDate` ON `%table_prefix%tp_tickets` (PostDate);;"
        ),

        /** Inserts or updates the core player row (join times, name, IP, play seconds, proxy flag). */
        PUSH_PLAYER_MAIN("INSERT INTO `%table_prefix%players` (" +
                "Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, ProxyTouched" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "FirstJoin = ?, LastJoin = ?, CurrentName = ?, CurrentIP = ?, " +
                "PlaySeconds = ?, ProxyTouched = ?;"),

        /** Inserts or updates the player metadata row (nickname, prefix, suffix). */
        PUSH_PLAYER_META("INSERT INTO `%table_prefix%player_meta` (" +
                "Uuid, Nickname, Prefix, Suffix" +
                ") VALUES (" +
                "?, ?, ?, ? " +
                ") ON DUPLICATE KEY UPDATE  " +
                "Nickname = ?, Prefix = ?, Suffix = ?;"),

        /** Inserts or updates the player's last known location (server, world, coordinates, angles). */
        PUSH_PLAYER_LOCATION("INSERT INTO `%table_prefix%player_location` (" +
                "Uuid, Server, World, X, Y, Z, Yaw, Pitch" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "Server = ?, World = ?, X = ?, Y = ?, Z = ?, Yaw = ?, Pitch = ?;"),

        /** Inserts or updates the player permissions row (bypass flag). */
        PUSH_PLAYER_PERMISSIONS("INSERT INTO `%table_prefix%player_permissions` (" +
                "Uuid, BypassingPermissions" +
                ") VALUES (" +
                "?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "BypassingPermissions = ?;"),

        /** Inserts or updates the UUID-info row (username history and IP history). */
        PUSH_UUID_INFO("INSERT INTO `%table_prefix%uuid_info` (" +
                "Uuid, Usernames, Ips " +
                ") VALUES (" +
                "?, ?, ? " +
                ") ON DUPLICATE KEY UPDATE  " +
                "Usernames = ?, Ips = ?;"),

        /** Inserts an upkeep/save-heartbeat record for a player on a specific server. */
        PUSH_UPKEEP("INSERT INTO `%table_prefix%saves` (" +
                "SavedAt, Uuid, ServerUuid" +
                ") VALUES (" +
                "?, ?, ?" +
                ");"),

        /** Inserts or updates a server record (UUID, name, type). */
        PUT_SERVER("INSERT INTO `%table_prefix%servers` (" +
                "Uuid, Name, Type" +
                ") VALUES (" +
                "?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "Name = ?, Type = ?;"),

        /** Inserts or updates a cross-server update notification record. */
        PUT_UPDATE("INSERT INTO `%table_prefix%update` (" +
                "Type, Identifier, ServerUuid, PostDate" +
                ") VALUES (" +
                "?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "ServerUuid = ?, PostDate = ?;"),

        /** Inserts or updates a teleport-ticket row for deferred cross-server teleportation. */
        PUT_TP_TICKET("INSERT INTO `%table_prefix%tp_tickets` (" +
                "Uuid, ServerName, WorldName, X, Y, Z, Yaw, Pitch, PostDate" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "ServerName = ?, WorldName = ?, X = ?, Y = ?, Z = ?, Yaw = ?, Pitch = ?, PostDate = ?;"),

        /** Selects the core player row by UUID. */
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),

        /** Selects the player metadata row by UUID. */
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),

        /** Selects the player location row by UUID. */
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),

        /** Selects the player permissions row by UUID. */
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),

        /** Selects the UUID-info row by UUID. */
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),

        /** Selects every UUID-info row in the table. */
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),

        /** Selects the most recent upkeep record for a given player UUID, grouped per server. */
        PULL_UPKEEP("SELECT MAX(SavedAt) AS \"SavedAt\", Uuid, ServerUuid FROM `%table_prefix%saves` WHERE Uuid = ? GROUP BY Uuid, ServerUuid;"),

        /** Selects a server record by UUID. */
        PULL_SERVER("SELECT * FROM `%table_prefix%servers` WHERE Uuid = ?;"),

        /** Selects every server record in the table. */
        PULL_ALL_SERVERS("SELECT * FROM `%table_prefix%servers`;"),

        /** Selects a teleport-ticket by player UUID. */
        PULL_TP_TICKET("SELECT * FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),

        /** Selects every teleport-ticket in the table. */
        PULL_ALL_TP_TICKETS("SELECT * FROM `%table_prefix%tp_tickets`;"),

        /** Returns {@code 1} if a player row with the given UUID exists, {@code 0} otherwise. */
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = ?);"),

        /** Selects the {@code ProxyTouched} flag for a given player UUID. */
        PLAYER_IS_TOUCHED("SELECT ProxyTouched FROM `%table_prefix%players` WHERE Uuid = ?;"),

        /** Selects the server UUID and post-date of a pending update for a given type and identifier. */
        CHECK_UPDATE("SELECT ServerUuid, PostDate FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),

        /** Deletes the update record matching the given type and identifier. */
        CLEAR_UPDATE("DELETE FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),

        /** Deletes the teleport-ticket for the given player UUID. */
        CLEAR_TP_TICKET("DELETE FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),

        /**
         * Removes all persisted data for a player across every player-related table
         * (players, player_meta, player_location, player_permissions, uuid_info).
         */
        DROP_PLAYER(
                "DELETE FROM `%table_prefix%players` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%player_meta` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%player_location` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%player_permissions` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%uuid_info` WHERE Uuid = ?;;"
        ),
        ;

        /**
         * The raw SQL template string for this statement, possibly still containing
         * {@code %database%} and {@code %table_prefix%} placeholder tokens.
         */
        private final String statement;

        /**
         * Creates a MySQL statement constant with the given SQL template.
         *
         * @param statement the SQL template string
         */
        MySQL(String statement) {
            this.statement = statement;
        }
    }

    /**
     * SQLite dialect SQL statement templates.
     *
     * <p>Each constant's {@link #getStatement()} value is a ready-to-use
     * parameterised SQL string that may still contain the
     * {@code %database%} and {@code %table_prefix%} placeholder tokens.</p>
     */
    @Getter
    public enum SQLite {
        /** No-op for SQLite — database creation is implicit in the file path. */
        CREATE_DATABASE(""),

        /**
         * Creates all required tables and their indexes in a single
         * multi-statement batch (statements separated by {@code ;;}).
         */
        CREATE_TABLES(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%players` (" +
                        "    Uuid TEXT, " +
                        "    FirstJoin REAL, " +
                        "    LastJoin REAL, " +
                        "    CurrentName TEXT, " +
                        "    CurrentIP TEXT, " +
                        "    PlaySeconds INTEGER, " +
                        "    ProxyTouched BOOLEAN, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_meta` (" +
                        "    Uuid TEXT, " +
                        "    Nickname TEXT, " +
                        "    Prefix TEXT, " +
                        "    Suffix TEXT, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_location` (" +
                        "    Uuid TEXT, " +
                        "    Server TEXT, " +
                        "    World TEXT, " +
                        "    X REAL, " +
                        "    Y REAL, " +
                        "    Z REAL, " +
                        "    Yaw REAL, " +
                        "    Pitch REAL, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_permissions` (" +
                        "    Uuid TEXT, " +
                        "    BypassingPermissions INTEGER, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%uuid_info` (" +
                        "    Uuid TEXT, " +
                        "    Usernames TEXT, " +
                        "    Ips TEXT, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%saves` (" +
                        "    SavedAt REAL, " +
                        "    Uuid TEXT, " +
                        "    ServerUuid TEXT, " +
                        "    PRIMARY KEY (SavedAt)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%servers` (" +
                        "    Uuid TEXT, " +
                        "    Name TEXT, " +
                        "    Type TEXT, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%update` (" +
                        "    Type TEXT, " +
                        "    Identifier TEXT, " +
                        "    ServerUuid TEXT, " +
                        "    PostDate REAL, " +
                        "    PRIMARY KEY (Type, Identifier) " +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%tp_tickets` (" +
                        "    Uuid TEXT, " +
                        "    ServerName TEXT, " +
                        "    WorldName TEXT, " +
                        "    X REAL, " +
                        "    Y REAL, " +
                        "    Z REAL, " +
                        "    Yaw REAL, " +
                        "    Pitch REAL, " +
                        "    PostDate REAL, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%players_Uuid` ON `%table_prefix%players` (Uuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%player_meta_Uuid` ON `%table_prefix%player_meta` (Uuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%player_location_Uuid` ON `%table_prefix%player_location` (Uuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%player_permissions_Uuid` ON `%table_prefix%player_permissions` (Uuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%uuid_info_Uuid` ON `%table_prefix%uuid_info` (Uuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%saves_SavedAt` ON `%table_prefix%saves` (SavedAt);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%servers_Uuid` ON `%table_prefix%servers` (Uuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%update_Type` ON `%table_prefix%update` (Type);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%update_Identifier` ON `%table_prefix%update` (Identifier);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%update_ServerUuid` ON `%table_prefix%update` (ServerUuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%update_PostDate` ON `%table_prefix%update` (PostDate);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%tp_tickets_Uuid` ON `%table_prefix%tp_tickets` (Uuid);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%tp_tickets_ServerName` ON `%table_prefix%tp_tickets` (ServerName);;" +
                        "CREATE INDEX IF NOT EXISTS `%table_prefix%tp_tickets_PostDate` ON `%table_prefix%tp_tickets` (PostDate);;"
        ),

        /** Inserts or replaces the core player row (join times, name, IP, play seconds, proxy flag). */
        PUSH_PLAYER_MAIN("INSERT OR REPLACE INTO `%table_prefix%players` (" +
                "    Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, ProxyTouched" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?" +
                ");"),

        /** Inserts or replaces the player metadata row (nickname, prefix, suffix). */
        PUSH_PLAYER_META("INSERT OR REPLACE INTO `%table_prefix%player_meta` (" +
                "    Uuid, Nickname, Prefix, Suffix" +
                ") VALUES (" +
                "    ?, ?, ?, ? " +
                ");"),

        /** Inserts or replaces the player's last known location (server, world, coordinates, angles). */
        PUSH_PLAYER_LOCATION("INSERT OR REPLACE INTO `%table_prefix%player_location` (" +
                "    Uuid, Server, World, X, Y, Z, Yaw, Pitch" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?, ?" +
                ");"),

        /** Inserts or replaces the player permissions row (bypass flag). */
        PUSH_PLAYER_PERMISSIONS("INSERT OR REPLACE INTO `%table_prefix%player_permissions` (" +
                "    Uuid, BypassingPermissions" +
                ") VALUES (" +
                "    ?, ?" +
                ");"),

        /** Inserts or replaces the UUID-info row (username history and IP history). */
        PUSH_UUID_INFO("INSERT OR REPLACE INTO `%table_prefix%uuid_info` (" +
                "    Uuid, Usernames, Ips " +
                ") VALUES (" +
                "    ?, ?, ? " +
                ");"),

        /** Inserts or replaces an upkeep/save-heartbeat record for a player on a specific server. */
        PUSH_UPKEEP("INSERT OR REPLACE INTO `%table_prefix%saves` (" +
                "    SavedAt, Uuid, ServerUuid" +
                ") VALUES (" +
                "    ?, ?, ?" +
                ");"),

        /** Inserts or replaces a server record (UUID, name, type). */
        PUT_SERVER("INSERT OR REPLACE INTO `%table_prefix%servers` (" +
                "    Uuid, Name, Type" +
                ") VALUES (" +
                "    ?, ?, ?" +
                ");"),

        /** Inserts or replaces a cross-server update notification record. */
        PUT_UPDATE("INSERT OR REPLACE INTO `%table_prefix%update` (" +
                "    Type, Identifier, ServerUuid, PostDate" +
                ") VALUES (" +
                "    ?, ?, ?, ?" +
                ");"),

        /** Inserts or replaces a teleport-ticket row for deferred cross-server teleportation. */
        PUT_TP_TICKET("INSERT OR REPLACE INTO `%table_prefix%tp_tickets` (" +
                "    Uuid, ServerName, WorldName, X, Y, Z, Yaw, Pitch, PostDate" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ");"),

        /** Selects the core player row by UUID. */
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),

        /** Selects the player metadata row by UUID. */
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),

        /** Selects the player location row by UUID. */
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),

        /** Selects the player permissions row by UUID. */
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),

        /** Selects the UUID-info row by UUID. */
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),

        /** Selects every UUID-info row in the table. */
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),

        /** Selects the most recent upkeep record for a given player UUID, grouped per server. */
        PULL_UPKEEP("SELECT MAX(SavedAt) AS \"SavedAt\", Uuid, ServerUuid FROM `%table_prefix%saves` WHERE Uuid = ? GROUP BY Uuid, ServerUuid;"),

        /** Selects a server record by UUID. */
        PULL_SERVER("SELECT * FROM `%table_prefix%servers` WHERE Uuid = ?;"),

        /** Selects every server record in the table. */
        PULL_ALL_SERVERS("SELECT * FROM `%table_prefix%servers`;"),

        /** Selects a teleport-ticket by player UUID. */
        PULL_TP_TICKET("SELECT * FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),

        /** Selects every teleport-ticket in the table. */
        PULL_ALL_TP_TICKETS("SELECT * FROM `%table_prefix%tp_tickets`;"),

        /** Returns {@code 1} if a player row with the given UUID exists, {@code 0} otherwise. */
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = ?);"),

        /** Selects the {@code ProxyTouched} flag for a given player UUID. */
        PLAYER_IS_TOUCHED("SELECT ProxyTouched FROM `%table_prefix%players` WHERE Uuid = ?;"),

        /** Selects the server UUID and post-date of a pending update for a given type and identifier. */
        CHECK_UPDATE("SELECT ServerUuid, PostDate FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),

        /** Deletes the update record matching the given type and identifier. */
        CLEAR_UPDATE("DELETE FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),

        /** Deletes the teleport-ticket for the given player UUID. */
        CLEAR_TP_TICKET("DELETE FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),

        /**
         * Removes all persisted data for a player across every player-related table
         * (players, player_meta, player_location, player_permissions, uuid_info).
         */
        DROP_PLAYER(
                "DELETE FROM `%table_prefix%players` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%player_meta` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%player_location` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%player_permissions` WHERE Uuid = ?;;" +
                        "DELETE FROM `%table_prefix%uuid_info` WHERE Uuid = ?;;"
        ),
        ;

        /**
         * The raw SQL template string for this statement, possibly still containing
         * {@code %database%} and {@code %table_prefix%} placeholder tokens.
         */
        private final String statement;

        /**
         * Creates a SQLite statement constant with the given SQL template.
         *
         * @param statement the SQL template string
         */
        SQLite(String statement) {
            this.statement = statement;
        }
    }

    /**
     * Enumerates every logical database operation supported by Streamline.
     *
     * <p>Values are shared between the {@link MySQL} and {@link SQLite} enums;
     * use {@link #name()} to look up the dialect-specific constant via
     * {@link Enum#valueOf(Class, String)}.</p>
     */
    public enum StatementType {
        /** Creates the database/schema. */
        CREATE_DATABASE,
        /** Creates all required tables and indexes. */
        CREATE_TABLES,
        /** Upserts the core player row. */
        PUSH_PLAYER_MAIN,
        /** Upserts the player metadata row. */
        PUSH_PLAYER_META,
        /** Upserts the player leveling row. */
        PUSH_PLAYER_LEVELING,
        /** Upserts the player location row. */
        PUSH_PLAYER_LOCATION,
        /** Upserts the player permissions row. */
        PUSH_PLAYER_PERMISSIONS,
        /** Upserts the UUID-info row. */
        PUSH_UUID_INFO,
        /** Inserts an upkeep/save-heartbeat record. */
        PUSH_UPKEEP,
        /** Upserts a server record. */
        PUT_SERVER,
        /** Upserts a cross-server update notification. */
        PUT_UPDATE,
        /** Upserts a teleport-ticket. */
        PUT_TP_TICKET,
        /** Fetches the core player row. */
        PULL_PLAYER_MAIN,
        /** Fetches the player metadata row. */
        PULL_PLAYER_META,
        /** Fetches the player location row. */
        PULL_PLAYER_LOCATION,
        /** Fetches the player permissions row. */
        PULL_PLAYER_PERMISSIONS,
        /** Fetches the UUID-info row for one player. */
        PULL_UUID_INFO,
        /** Fetches all UUID-info rows. */
        PULL_ALL_UUID_INFO,
        /** Fetches the most recent upkeep record for a player. */
        PULL_UPKEEP,
        /** Fetches a server record by UUID. */
        PULL_SERVER,
        /** Fetches all server records. */
        PULL_ALL_SERVERS,
        /** Fetches a teleport-ticket by player UUID. */
        PULL_TP_TICKET,
        /** Fetches all teleport-tickets. */
        PULL_ALL_TP_TICKETS,
        /** Checks whether a player row exists. */
        PLAYER_EXISTS,
        /** Checks whether a player has been touched by a proxy. */
        PLAYER_IS_TOUCHED,
        /** Checks for a pending cross-server update. */
        CHECK_UPDATE,
        /** Removes a cross-server update record. */
        CLEAR_UPDATE,
        /** Removes a teleport-ticket. */
        CLEAR_TP_TICKET,
        /** Removes all persisted data for a player. */
        DROP_PLAYER,
        ;
    }

    /**
     * Resolves the SQL string for the given logical operation and database backend,
     * substituting the {@code %database%} and {@code %table_prefix%} placeholders
     * with values from the supplied {@link ConnectorSet}.
     *
     * @param type         the logical statement to retrieve
     * @param connectorSet the connector configuration that specifies the backend type,
     *                     database name, and table prefix
     * @return the ready-to-prepare SQL string, or an empty string if the backend
     *         type is not recognised
     */
    public static String getStatement(StatementType type, ConnectorSet connectorSet) {
        switch (connectorSet.getType()) {
            case MYSQL:
                return MySQL.valueOf(type.name()).getStatement()
                        .replace("%database%", connectorSet.getDatabase())
                        .replace("%table_prefix%", connectorSet.getTablePrefix());
            case SQLITE:
                return SQLite.valueOf(type.name()).getStatement()
                        .replace("%database%", connectorSet.getDatabase())
                        .replace("%table_prefix%", connectorSet.getTablePrefix());
            default:
                return "";
        }
    }
}
