package net.streamline.api.database;

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
                "Points INT" +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_meta` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Nickname VARCHAR(255), " +
                "Prefix VARCHAR(255), " +
                "Suffix VARCHAR(255)" +
                ");;" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_leveling` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Level INT, " +
                "TotalExperience DOUBLE, " +
                "CurrentExperience DOUBLE, " +
                "EquationString NVARCHAR(255), " +
                "StartedLevel INT, " +
                "StartedExperience DOUBLE " +
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
                "SavedAt BIGINT, " +
                "Uuid VARCHAR(36), " +
                "ServerUuid VARCHAR(36), " +
                "PRIMARY KEY (SavedAt, Uuid) " +
                ");;"
        ),
        PUSH_PLAYER_MAIN("INSERT INTO `%table_prefix%players` (" +
                "Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, Points, ProxyTouched" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "FirstJoin = ?, LastJoin = ?, CurrentName = ?, CurrentIP = ?, " +
                "PlaySeconds = ?, Points = ?;"),
        PUSH_PLAYER_META("INSERT INTO `%table_prefix%player_meta` (" +
                "Uuid, Nickname, Prefix, Suffix" +
                ") VALUES (" +
                "?, ?, ?, ? " +
                ") ON DUPLICATE KEY UPDATE  " +
                "Nickname = ?, Prefix = ?, Suffix = ?;"),
        PUSH_PLAYER_LEVELING("INSERT INTO `%table_prefix%player_leveling` (" +
                "Uuid, Level, TotalExperience, CurrentExperience, EquationString, StartedLevel, StartedExperience" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?" +
                ") ON DUPLICATE KEY UPDATE  " +
                "Level = ?, TotalExperience = ?, CurrentExperience = ?, " +
                "EquationString = ?, StartedLevel = ?, StartedExperience = ?;"),
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
        PUSH_UPKEEP("INSERT INTO `%table_prefix%upkeep` (" +
                "SavedAt, Uuid, ServerUuid" +
                ") VALUES (" +
                "?, ?, ?" +
                ");"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),
        PULL_PLAYER_LEVELING("SELECT * FROM `%table_prefix%player_leveling` WHERE Uuid = ?;"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PULL_UPKEEP("SELECT MAX(SavedAt), Uuid, ServerUuid FROM `%table_prefix%upkeep` GROUP BY Uuid, ServerUuid;"),
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
                        "    Points INTEGER" +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_meta` (" +
                        "    Uuid TEXT, " +
                        "    Nickname TEXT, " +
                        "    Prefix TEXT, " +
                        "    Suffix TEXT, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");;" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_leveling` (" +
                        "    Uuid TEXT, " +
                        "    Level INTEGER, " +
                        "    TotalExperience REAL, " +
                        "    CurrentExperience REAL, " +
                        "    EquationString TEXT, " +
                        "    StartedLevel INTEGER, " +
                        "    StartedExperience REAL, " +
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
                        "    PRIMARY KEY (SavedAt, Uuid)" +
                        ");;"
        ),
        PUSH_PLAYER_MAIN("INSERT INTO `%table_prefix%players` (" +
                "    Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, Points" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?" +
                ");"),
        PUSH_PLAYER_META("INSERT INTO `%table_prefix%player_meta` (" +
                "    Uuid, Nickname, Prefix, Suffix" +
                ") VALUES (" +
                "    ?, ?, ?, '%suffix' " +
                ");"),
        PUSH_PLAYER_LEVELING("INSERT INTO `%table_prefix%player_leveling` (" +
                "    Uuid, Level, TotalExperience, CurrentExperience, EquationString, StartedLevel, StartedExperience" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?" +
                ");"),
        PUSH_PLAYER_LOCATION("INSERT INTO `%table_prefix%player_location` (" +
                "    Uuid, Server, World, X, Y, Z, Yaw, Pitch" +
                ") VALUES (" +
                "    ?, ?, ?, ?, ?, ?, ?, ?" +
                ");"),
        PUSH_PLAYER_PERMISSIONS("INSERT INTO `%table_prefix%player_permissions` (" +
                "    Uuid, BypassingPermissions" +
                ") VALUES (" +
                "    ?, ?" +
                ");"),
        PUSH_UUID_INFO("INSERT OR REPLACE INTO `%table_prefix%uuid_info` (" +
                "    Uuid, Usernames, Ips " +
                ") VALUES (" +
                "    ?, ?, ? " +
                ");"),
        PUSH_UPKEEP("INSERT INTO `%table_prefix%upkeep` (" +
                "    SavedAt, Uuid, ServerUuid" +
                ") VALUES (" +
                "    ?, ?, ?" +
                ");"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = ?;"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = ?;"),
        PULL_PLAYER_LEVELING("SELECT * FROM `%table_prefix%player_leveling` WHERE Uuid = ?;"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = ?;"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = ?;"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = ?;"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PULL_UPKEEP("SELECT MAX(SavedAt), Uuid, ServerUuid FROM `%table_prefix%upkeep` GROUP BY Uuid, ServerUuid;"),
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
        PULL_PLAYER_MAIN,
        PULL_PLAYER_META,
        PULL_PLAYER_LEVELING,
        PULL_PLAYER_LOCATION,
        PULL_PLAYER_PERMISSIONS,
        PULL_UUID_INFO,
        PULL_ALL_UUID_INFO,
        PULL_UPKEEP,
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
