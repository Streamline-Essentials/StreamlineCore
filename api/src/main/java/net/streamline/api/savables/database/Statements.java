package net.streamline.api.savables.database;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Statements {
    public interface StatementHolder {
        String getStatement();
    }

    @Getter
    public enum MySQL implements StatementHolder {
        CREATE_DATABASE(
                "CREATE DATABASE IF NOT EXISTS `%database%`;"
        ),
        CREATE_TABLES(
                "CREATE TABLE IF NOT EXISTS `%table_prefix%users` (\n" +
                        "  `Id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                        "  `Uuid` nvarchar(36) NOT NULL,\n" +
                        "  PRIMARY KEY (`Id`),\n" +
                        "  UNIQUE KEY `Uuid` (`Uuid`)\n" +
                        ");\n" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%stats` (\n" +
                        "  `Id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                        "  `Uuid` nvarchar(36) NOT NULL,\n" +
                        "  `PlaySeconds` int(11) NOT NULL,\n" +
                        "  `TotalXP` int(11) NOT NULL,\n" +
                        "  `CurrentXP` int(11) NOT NULL,\n" +
                        "  `Level` int(11) NOT NULL,\n" +
                        "  PRIMARY KEY (`Id`),\n" +
                        "  UNIQUE KEY `Uuid` (`Uuid`)\n" +
                        ");\n" +
                        "CREATE TABLE IF NOT EXISTS `%table_prefix%uniqueness` (\n" +
                        "  `Id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                        "  `Uuid` nvarchar(36) NOT NULL,\n" +
                        "  `DisplayName` nvarchar(255) NOT NULL,\n" +
                        "  `LatestName` nvarchar(255) NOT NULL,\n" +
                        "  `TagList` nvarchar(255) NOT NULL,\n" +
                        "  `NameList` nvarchar(255) NOT NULL,\n" +
                        "  `LatestIP` nvarchar(255) NOT NULL,\n" +
                        "  `IPList` nvarchar(255) NOT NULL,\n" +
                        "  `FirstJoin` datetime NOT NULL,\n" +
                        "  `LastJoin` datetime NOT NULL,\n" +
                        "  `Location` nvarchar(255) NOT NULL,\n" +
                        "  PRIMARY KEY (`Id`),\n" +
                        "  UNIQUE KEY `Uuid` (`Uuid`)\n" +
                        ");"
        ),

        ;

        private final String statement;

        MySQL(String statement) {
            this.statement = statement;
        }
    }
}
