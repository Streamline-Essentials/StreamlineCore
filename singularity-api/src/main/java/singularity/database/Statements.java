package singularity.database;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Statements {
    @Getter
    public enum MySQL {
        CREATE_DATABASE("CREATE DATABASE IF NOT EXISTS `%database%`;"),
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
        PUSH_PLAYER_MAIN("INSERT INTO `%table_prefix%players` (" +
                "Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, ProxyTouched" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "FirstJoin = ?, LastJoin = ?, CurrentName = ?, CurrentIP = ?, " +
                "PlaySeconds = ?, ProxyTouched = ?;"),
        PUSH_PLAYER_META("INSERT INTO `%table_prefix%player_meta` (" +
                "Uuid, Nickname, Prefix, Suffix" +
                ") VALUES (" +
                "?, ?, ?, ? " +
                ") ON DUPLICATE KEY UPDATE  " +
                "Nickname = ?, Prefix = ?, Suffix = ?;"),
        PUSH_PLAYER_LOCATION("INSERT INTO `%table_prefix%player_location` (" +
                "Uuid, Server, World, X, Y, Z, Yaw, Pitch" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "Server = ?, World = ?, X = ?, Y = ?, Z = ?, Yaw = ?, Pitch = ?;"),
        PUSH_PLAYER_PERMISSIONS("INSERT INTO `%table_prefix%player_permissions` (" +
                "Uuid, BypassingPermissions" +
                ") VALUES (" +
                "?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "BypassingPermissions = ?;"),
        PUSH_UUID_INFO("INSERT INTO `%table_prefix%uuid_info` (" +
                "Uuid, Usernames, Ips " +
                ") VALUES (" +
                "?, ?, ? " +
                ") ON DUPLICATE KEY UPDATE  " +
                "Usernames = ?, Ips = ?;"),
        PUSH_UPKEEP("INSERT INTO `%table_prefix%saves` (" +
                "SavedAt, Uuid, ServerUuid" +
                ") VALUES (" +
                "?, ?, ?" +
                ");"),
        PUT_SERVER("INSERT INTO `%table_prefix%servers` (" +
                "Uuid, Name, Type" +
                ") VALUES (" +
                "?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "Name = ?, Type = ?;"),
        PUT_UPDATE("INSERT INTO `%table_prefix%update` (" +
                "Type, Identifier, ServerUuid, PostDate" +
                ") VALUES (" +
                "?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "ServerUuid = ?, PostDate = ?;"),
        PUT_TP_TICKET("INSERT INTO `%table_prefix%tp_tickets` (" +
                "Uuid, ServerName, WorldName, X, Y, Z, Yaw, Pitch, PostDate" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE " +
                "ServerName = ?, WorldName = ?, X = ?, Y = ?, Z = ?, Yaw = ?, Pitch = ?, PostDate = ?;"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PULL_UPKEEP("SELECT MAX(SavedAt) AS \"SavedAt\", Uuid, ServerUuid FROM `%table_prefix%saves` WHERE Uuid = ? GROUP BY Uuid, ServerUuid;"),
        PULL_SERVER("SELECT * FROM `%table_prefix%servers` WHERE Uuid = ?;"),
        PULL_ALL_SERVERS("SELECT * FROM `%table_prefix%servers`;"),
        PULL_TP_TICKET("SELECT * FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),
        PULL_ALL_TP_TICKETS("SELECT * FROM `%table_prefix%tp_tickets`;"),
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = ?);"),
        PLAYER_IS_TOUCHED("SELECT ProxyTouched FROM `%table_prefix%players` WHERE Uuid = ?;"),
        CHECK_UPDATE("SELECT ServerUuid, PostDate FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),
        CLEAR_UPDATE("DELETE FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),
        CLEAR_TP_TICKET("DELETE FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),
        ;

        private final String statement;

        MySQL(String statement) {
            this.statement = statement;
        }
    }

    @Getter
    public enum SQLite {
        CREATE_DATABASE(""),
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
        PUSH_PLAYER_MAIN("INSERT OR REPLACE INTO `%table_prefix%players` (" +
                "    Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, ProxyTouched" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?" +
                ");"),
        PUSH_PLAYER_META("INSERT OR REPLACE INTO `%table_prefix%player_meta` (" +
                "    Uuid, Nickname, Prefix, Suffix" +
                ") VALUES (" +
                "    ?, ?, ?, ? " +
                ");"),
        PUSH_PLAYER_LOCATION("INSERT OR REPLACE INTO `%table_prefix%player_location` (" +
                "    Uuid, Server, World, X, Y, Z, Yaw, Pitch" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?, ?" +
                ");"),
        PUSH_PLAYER_PERMISSIONS("INSERT OR REPLACE INTO `%table_prefix%player_permissions` (" +
                "    Uuid, BypassingPermissions" +
                ") VALUES (" +
                "    ?, ?" +
                ");"),
        PUSH_UUID_INFO("INSERT OR REPLACE INTO `%table_prefix%uuid_info` (" +
                "    Uuid, Usernames, Ips " +
                ") VALUES (" +
                "    ?, ?, ? " +
                ");"),
        PUSH_UPKEEP("INSERT OR REPLACE INTO `%table_prefix%saves` (" +
                "    SavedAt, Uuid, ServerUuid" +
                ") VALUES (" +
                "    ?, ?, ?" +
                ");"),
        PUT_SERVER("INSERT OR REPLACE INTO `%table_prefix%servers` (" +
                "    Uuid, Name, Type" +
                ") VALUES (" +
                "    ?, ?, ?" +
                ");"),
        PUT_UPDATE("INSERT OR REPLACE INTO `%table_prefix%update` (" +
                "    Type, Identifier, ServerUuid, PostDate" +
                ") VALUES (" +
                "    ?, ?, ?, ?" +
                ");"),
        PUT_TP_TICKET("INSERT OR REPLACE INTO `%table_prefix%tp_tickets` (" +
                "    Uuid, ServerName, WorldName, X, Y, Z, Yaw, Pitch, PostDate" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                ");"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PULL_UPKEEP("SELECT MAX(SavedAt) AS \"SavedAt\", Uuid, ServerUuid FROM `%table_prefix%saves` WHERE Uuid = ? GROUP BY Uuid, ServerUuid;"),
        PULL_SERVER("SELECT * FROM `%table_prefix%servers` WHERE Uuid = ?;"),
        PULL_ALL_SERVERS("SELECT * FROM `%table_prefix%servers`;"),
        PULL_TP_TICKET("SELECT * FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),
        PULL_ALL_TP_TICKETS("SELECT * FROM `%table_prefix%tp_tickets`;"),
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = ?);"),
        PLAYER_IS_TOUCHED("SELECT ProxyTouched FROM `%table_prefix%players` WHERE Uuid = ?;"),
        CHECK_UPDATE("SELECT ServerUuid, PostDate FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),
        CLEAR_UPDATE("DELETE FROM `%table_prefix%update` WHERE Type = ? AND Identifier = ?;"),
        CLEAR_TP_TICKET("DELETE FROM `%table_prefix%tp_tickets` WHERE Uuid = ?;"),
        ;

        private final String statement;

        SQLite(String statement) {
            this.statement = statement;
        }
    }

    public enum StatementType {
        CREATE_DATABASE,
        CREATE_TABLES,
        PUSH_PLAYER_MAIN,
        PUSH_PLAYER_META,
        PUSH_PLAYER_LEVELING,
        PUSH_PLAYER_LOCATION,
        PUSH_PLAYER_PERMISSIONS,
        PUSH_UUID_INFO,
        PUSH_UPKEEP,
        PUT_SERVER,
        PUT_UPDATE,
        PUT_TP_TICKET,
        PULL_PLAYER_MAIN,
        PULL_PLAYER_META,
        PULL_PLAYER_LOCATION,
        PULL_PLAYER_PERMISSIONS,
        PULL_UUID_INFO,
        PULL_ALL_UUID_INFO,
        PULL_UPKEEP,
        PULL_SERVER,
        PULL_ALL_SERVERS,
        PULL_TP_TICKET,
        PULL_ALL_TP_TICKETS,
        PLAYER_EXISTS,
        PLAYER_IS_TOUCHED,
        CHECK_UPDATE,
        CLEAR_UPDATE,
        CLEAR_TP_TICKET,
        ;
    }

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
