package net.streamline.api.savables.users;

import net.streamline.api.configs.given.GivenConfigs;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.databases.processing.mongo.MongoSchematic;
import tv.quaint.storage.resources.databases.processing.sql.SQLSchematic;

public class DatabaseRealization {
    public static MongoSchematic MONGO_PLAYER_TABLE_SCHEMATIC() {
        DatabaseConfig config = GivenConfigs.getMainConfig().getConfiguredDatabase();
        MongoSchematic schematic = new MongoSchematic(config.getTablePrefix() + "users");
        schematic.addColumn("uuid", MongoSchematic.MongoType.STRING);
        schematic.addColumn("latestName", MongoSchematic.MongoType.STRING);
        schematic.addColumn("displayName", MongoSchematic.MongoType.STRING);
        schematic.addColumn("tagList", MongoSchematic.MongoType.ARRAY);
        schematic.addColumn("points", MongoSchematic.MongoType.DOUBLE);
        schematic.addColumn("lastMessage", MongoSchematic.MongoType.STRING);
        schematic.addColumn("online", MongoSchematic.MongoType.BOOLEAN);
        schematic.addColumn("latestServer", MongoSchematic.MongoType.STRING);
        schematic.addColumn("bypassPermissions", MongoSchematic.MongoType.BOOLEAN);
        schematic.addColumn("level", MongoSchematic.MongoType.INT);
        schematic.addColumn("totalXp", MongoSchematic.MongoType.DOUBLE);
        schematic.addColumn("currentXp", MongoSchematic.MongoType.DOUBLE);
        schematic.addColumn("playSeconds", MongoSchematic.MongoType.INT);
        schematic.addColumn("latestIP", MongoSchematic.MongoType.STRING);
        schematic.addColumn("ipList", MongoSchematic.MongoType.ARRAY);
        schematic.addColumn("nameList", MongoSchematic.MongoType.ARRAY);
        return schematic;
    }

    public static SQLSchematic SQL_PLAYER_TABLE_SCHEMATIC() {
        DatabaseConfig config = GivenConfigs.getMainConfig().getConfiguredDatabase();
        SQLSchematic schematic = new SQLSchematic(config.getTablePrefix() + "users");
        schematic.addColumn("uuid", SQLSchematic.SQLType.VARCHAR, 0);
        schematic.addColumn("latestName", SQLSchematic.SQLType.VARCHAR, 1);
        schematic.addColumn("displayName", SQLSchematic.SQLType.VARCHAR, 2);
        schematic.addColumn("tagList", SQLSchematic.SQLType.VARCHAR, 3);
        schematic.addColumn("points", SQLSchematic.SQLType.DOUBLE, 4);
        schematic.addColumn("lastMessage", SQLSchematic.SQLType.VARCHAR, 5);
        schematic.addColumn("online", SQLSchematic.SQLType.INT, 6);
        schematic.addColumn("latestServer", SQLSchematic.SQLType.VARCHAR, 7);
        schematic.addColumn("bypassPermissions", SQLSchematic.SQLType.INT, 8);
        schematic.addColumn("level", SQLSchematic.SQLType.INT, 9);
        schematic.addColumn("totalXp", SQLSchematic.SQLType.DOUBLE, 10);
        schematic.addColumn("currentXp", SQLSchematic.SQLType.DOUBLE, 11);
        schematic.addColumn("playSeconds", SQLSchematic.SQLType.INT, 12);
        schematic.addColumn("latestIP", SQLSchematic.SQLType.VARCHAR, 13);
        schematic.addColumn("ipList", SQLSchematic.SQLType.VARCHAR, 14);
        schematic.addColumn("nameList", SQLSchematic.SQLType.VARCHAR, 15);
        return schematic;
    }
}
