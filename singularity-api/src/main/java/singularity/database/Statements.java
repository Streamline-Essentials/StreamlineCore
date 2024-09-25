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
                ");;"
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
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PULL_UPKEEP("SELECT MAX(SavedAt) AS \"SavedAt\", Uuid, ServerUuid FROM `%table_prefix%saves` WHERE Uuid = ? GROUP BY Uuid, ServerUuid;"),
        PULL_ALL_SERVERS("SELECT * FROM `%table_prefix%servers`;"),
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = ?);"),
        PLAYER_IS_TOUCHED("SELECT ProxyTouched FROM `%table_prefix%players` WHERE Uuid = ?;"),
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
                        ");;"
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
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PULL_UPKEEP("SELECT MAX(SavedAt) AS \"SavedAt\", Uuid, ServerUuid FROM `%table_prefix%saves` WHERE Uuid = ? GROUP BY Uuid, ServerUuid;"),
        PULL_ALL_SERVERS("SELECT * FROM `%table_prefix%servers`;"),
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = ?);"),
        PLAYER_IS_TOUCHED("SELECT ProxyTouched FROM `%table_prefix%players` WHERE Uuid = ?;"),
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
        PULL_PLAYER_MAIN,
        PULL_PLAYER_META,
        PULL_PLAYER_LOCATION,
        PULL_PLAYER_PERMISSIONS,
        PULL_UUID_INFO,
        PULL_ALL_UUID_INFO,
        PULL_UPKEEP,
        PULL_ALL_SERVERS,
        PLAYER_EXISTS,
        PLAYER_IS_TOUCHED,
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
