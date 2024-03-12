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
                "FirstJoin DATETIME, " +
                "LastJoin DATETIME, " +
                "CurrentName VARCHAR(64), " +
                "CurrentIP VARCHAR(15), " +
                "PlaySeconds INT, " +
                "Points INT, " +
                "ProxyTouched BIT" +
                ");" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_meta` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Nickname VARCHAR(255), " +
                "Prefix VARCHAR(255), " +
                "Suffix VARCHAR(255)" +
                ");" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_leveling` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Level INT, " +
                "TotalExperience DOUBLE, " +
                "CurrentExperience DOUBLE, " +
                "EquationString NVARCHAR(255), " +
                "StartedLevel INT, " +
                "StartedExperience DOUBLE " +
                ");" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_location` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Server VARCHAR(255), " +
                "World VARCHAR(255), " +
                "X DOUBLE, " +
                "Y DOUBLE, " +
                "Z DOUBLE, " +
                "Yaw FLOAT, " +
                "Pitch FLOAT " +
                ");" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%player_permissions` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "BypassingPermissions BOOLEAN " +
                ");" +
                "CREATE TABLE IF NOT EXISTS `%table_prefix%uuid_info` (" +
                "Uuid VARCHAR(36) PRIMARY KEY, " +
                "Usernames TEXT, " +
                "Ips TEXT " +
                ");"
        ),
        PUSH_PLAYER_MAIN("IF (%is_proxy% = 0) THEN" +
                "    IF NOT EXISTS (SELECT * FROM `%table_prefix%players` WHERE Uuid = '%uuid%' AND ProxyTouched <> 1) THEN" +
                "        INSERT INTO `%table_prefix%players` (Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, Points, ProxyTouched)" +
                "        VALUES ('%uuid%', '%first_join%', '%last_join%', '%current_name%', '%current_ip%', %play_seconds%, %points%, %proxy_touched%);" +
                "    ELSE" +
                "        UPDATE `%table_prefix%players`" +
                "        SET FirstJoin = '%first_join%'," +
                "            LastJoin = '%last_join%'," +
                "            CurrentName = '%current_name%'," +
                "            CurrentIP = '%current_ip%'," +
                "            PlaySeconds = %play_seconds%," +
                "            Points = %points%," +
                "            ProxyTouched = %proxy_touched%" +
                "        WHERE Uuid = '%uuid%';" +
                "    END IF;" +
                "END IF;"),
        PUSH_PLAYER_META("IF (%is_proxy% = 0) THEN" +
                "    IF NOT EXISTS (SELECT * FROM `%table_prefix%players` WHERE Uuid = '%uuid%' AND ProxyTouched <> 1) THEN" +
                "        INSERT INTO `%table_prefix%player_meta` (Uuid, Nickname, Prefix, Suffix)" +
                "        VALUES ('%uuid%', '%nickname%', '%prefix%', '%suffix%');" +
                "    ELSE" +
                "        UPDATE `%table_prefix%player_meta`" +
                "        SET Nickname = '%nickname%'," +
                "            Prefix = '%prefix%'," +
                "            Suffix = '%suffix%'" +
                "        WHERE Uuid = '%uuid';" +
                "    END IF;" +
                "END IF;"),
        PUSH_PLAYER_LEVELING("IF (%is_proxy% = 0) THEN" +
                "    IF NOT EXISTS (SELECT * FROM `%table_prefix%players` WHERE Uuid = '%uuid%' AND ProxyTouched <> 1) THEN" +
                "        INSERT INTO `%table_prefix%player_leveling` (Uuid, Level, TotalExperience, CurrentExperience, EquationString, StartedLevel, StartedExperience)" +
                "        VALUES ('%uuid%', %level%, %total_experience%, %current_experience%, '%equation_string%', %started_level%, %started_experience%);" +
                "    ELSE" +
                "        UPDATE `%table_prefix%player_leveling`" +
                "        SET Level = %level%," +
                "            TotalExperience = %total_experience%," +
                "            CurrentExperience = %current_experience%," +
                "            EquationString = '%equation_string%'," +
                "            StartedLevel = %started_level%," +
                "            StartedExperience = %started_experience%" +
                "        WHERE Uuid = '%uuid';" +
                "    END IF;" +
                "END IF;"),
        PUSH_PLAYER_LOCATION("IF (%is_proxy% = 0) THEN" +
                "    IF NOT EXISTS (SELECT * FROM `%table_prefix%players` WHERE Uuid = '%uuid%' AND ProxyTouched <> 1) THEN" +
                "        INSERT INTO `%table_prefix%player_location` (Uuid, Server, World, X, Y, Z, Yaw, Pitch)" +
                "        VALUES ('%uuid%', '%server%', '%world%', %x%, %y%, %z%, %yaw%, %pitch%);" +
                "    ELSE" +
                "        UPDATE `%table_prefix%player_location`" +
                "        SET Server = '%server%'," +
                "            World = '%world%'," +
                "            X = %x%," +
                "            Y = %y%," +
                "            Z = %z%," +
                "            Yaw = %yaw%," +
                "            Pitch = %pitch%" +
                "        WHERE Uuid = '%uuid';" +
                "    END IF;" +
                "END IF;"),
        PUSH_PLAYER_PERMISSIONS("IF (%is_proxy% = 0) THEN" +
                "    IF NOT EXISTS (SELECT * FROM `%table_prefix%players` WHERE Uuid = '%uuid%' AND ProxyTouched <> 1) THEN" +
                "        INSERT INTO `%table_prefix%player_permissions` (Uuid, BypassingPermissions)" +
                "        VALUES ('%uuid%', %bypassing_permissions%);" +
                "    ELSE" +
                "        UPDATE `%table_prefix%player_permissions`" +
                "        SET BypassingPermissions = %bypassing_permissions%" +
                "        WHERE Uuid = '%uuid';" +
                "    END IF;" +
                "END IF;"),
        PUSH_UUID_INFO("INSERT INTO `%table_prefix%uuid_info` (" +
                "Uuid, Usernames, Ips " +
                ") VALUES (" +
                "'%uuid%', '%usernames%', '%ips%' " +
                ") ON DUPLICATE KEY UPDATE  " +
                "Usernames = '%usernames%', Ips = '%ips%';"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_LEVELING("SELECT * FROM `%table_prefix%player_leveling` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = '%uuid%';"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = '%uuid%';"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = '%uuid');"),
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
                        "    FirstJoin TEXT, " +
                        "    LastJoin TEXT, " +
                        "    CurrentName TEXT, " +
                        "    CurrentIP TEXT, " +
                        "    PlaySeconds INTEGER, " +
                        "    Points INTEGER, " +
                        "    ProxyTouched BIT, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_meta` (" +
                        "    Uuid TEXT, " +
                        "    Nickname TEXT, " +
                        "    Prefix TEXT, " +
                        "    Suffix TEXT, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_leveling` (" +
                        "    Uuid TEXT, " +
                        "    Level INTEGER, " +
                        "    TotalExperience REAL, " +
                        "    CurrentExperience REAL, " +
                        "    EquationString TEXT, " +
                        "    StartedLevel INTEGER, " +
                        "    StartedExperience REAL, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");" +
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
                        ");" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%player_permissions` (" +
                        "    Uuid TEXT, " +
                        "    BypassingPermissions INTEGER, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%uuid_info` (" +
                        "    Uuid TEXT, " +
                        "    Usernames TEXT, " +
                        "    Ips TEXT, " +
                        "    PRIMARY KEY (Uuid)" +
                        ");"
        ),
        PUSH_PLAYER_MAIN("IF (%is_proxy% = 0) THEN" +
                "    INSERT OR IGNORE INTO `%table_prefix%players` (Uuid, FirstJoin, LastJoin, CurrentName, CurrentIP, PlaySeconds, Points, ProxyTouched)" +
                "    VALUES ('%uuid%', '%first_join%', '%last_join%', '%current_name%', '%current_ip%', %play_seconds%, %points%, %proxy_touched%);" +
                "ELSE" +
                "    UPDATE `%table_prefix%players`" +
                "    SET FirstJoin = '%first_join%'," +
                "        LastJoin = '%last_join%'," +
                "        CurrentName = '%current_name%'," +
                "        CurrentIP = '%current_ip%'," +
                "        PlaySeconds = %play_seconds%," +
                "        Points = %points%," +
                "        ProxyTouched = %proxy_touched%" +
                "    WHERE Uuid = '%uuid' AND ProxyTouched <> 1;" +
                "END IF;"),
        PUSH_PLAYER_META("IF (%is_proxy% = 0) THEN" +
                "    INSERT OR IGNORE INTO `%table_prefix%player_meta` (Uuid, Nickname, Prefix, Suffix)" +
                "    VALUES ('%uuid%', '%nickname%', '%prefix%', '%suffix%');" +
                "ELSE" +
                "    UPDATE `%table_prefix%player_meta`" +
                "    SET Nickname = '%nickname%'," +
                "        Prefix = '%prefix%'," +
                "        Suffix = '%suffix%'" +
                "    WHERE Uuid = '%uuid';" +
                "END IF;"),
        PUSH_PLAYER_LEVELING("IF (%is_proxy% = 0) THEN" +
                "    INSERT OR IGNORE INTO `%table_prefix%player_leveling` (Uuid, Level, TotalExperience, CurrentExperience, EquationString, StartedLevel, StartedExperience)" +
                "    VALUES ('%uuid%', %level%, %total_experience%, %current_experience%, '%equation_string%', %started_level%, %started_experience%);" +
                "ELSE" +
                "    UPDATE `%table_prefix%player_leveling`" +
                "    SET Level = %level%," +
                "        TotalExperience = %total_experience%," +
                "        CurrentExperience = %current_experience%," +
                "        EquationString = '%equation_string%'," +
                "        StartedLevel = %started_level%," +
                "        StartedExperience = %started_experience%" +
                "    WHERE Uuid = '%uuid';" +
                "END IF;"),
        PUSH_PLAYER_LOCATION("IF (%is_proxy% = 0) THEN" +
                "    INSERT OR IGNORE INTO `%table_prefix%player_location` (Uuid, Server, World, X, Y, Z, Yaw, Pitch)" +
                "    VALUES ('%uuid%', '%server%', '%world%', %x%, %y%, %z%, %yaw%, %pitch%);" +
                "ELSE" +
                "    UPDATE `%table_prefix%player_location`" +
                "    SET Server = '%server%'," +
                "        World = '%world%'," +
                "        X = %x%," +
                "        Y = %y%," +
                "        Z = %z%," +
                "        Yaw = %yaw%," +
                "        Pitch = %pitch%" +
                "    WHERE Uuid = '%uuid';" +
                "END IF;"),
        PUSH_PLAYER_PERMISSIONS("IF (%is_proxy% = 0) THEN" +
                "    INSERT OR IGNORE INTO `%table_prefix%player_permissions` (Uuid, BypassingPermissions)" +
                "    VALUES ('%uuid%', %bypassing_permissions%);" +
                "ELSE" +
                "    UPDATE `%table_prefix%player_permissions`" +
                "    SET BypassingPermissions = %bypassing_permissions%" +
                "    WHERE Uuid = '%uuid';" +
                "END IF;"),
        PUSH_UUID_INFO("INSERT OR REPLACE INTO `%table_prefix%uuid_info` (" +
                "    Uuid, Usernames, Ips " +
                ") VALUES (" +
                "    '%uuid%', '%usernames%', '%ips%' " +
                ");"),
        PULL_PLAYER_MAIN("SELECT * FROM `%table_prefix%players` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_META("SELECT * FROM `%table_prefix%player_meta` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_LEVELING("SELECT * FROM `%table_prefix%player_leveling` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_LOCATION("SELECT * FROM `%table_prefix%player_location` WHERE Uuid = '%uuid%';"),
        PULL_PLAYER_PERMISSIONS("SELECT * FROM `%table_prefix%player_permissions` WHERE Uuid = '%uuid%';"),
        PULL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info` WHERE Uuid = '%uuid%';"),
        PULL_ALL_UUID_INFO("SELECT * FROM `%table_prefix%uuid_info`;"),
        PLAYER_EXISTS("SELECT EXISTS(SELECT 1 FROM `%table_prefix%players` WHERE Uuid = '%uuid');"),
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
        PULL_PLAYER_MAIN,
        PULL_PLAYER_META,
        PULL_PLAYER_LEVELING,
        PULL_PLAYER_LOCATION,
        PULL_PLAYER_PERMISSIONS,
        PULL_UUID_INFO,
        PULL_ALL_UUID_INFO,
        PLAYER_EXISTS,
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
                        .replace("%table_prefix%", connectorSet.getTablePrefix());
            default:
                return "";
        }
    }
}
