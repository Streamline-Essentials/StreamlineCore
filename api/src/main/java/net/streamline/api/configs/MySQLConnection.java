package net.streamline.api.configs;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.streamline.api.SLAPI;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MySQLConnection {
    public HikariConfig config;
    public HikariDataSource dataSource;
    public Connection connection;

    public MySQLConnection(String connectionUri, String database) {
        this.config = new HikariConfig();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();

            this.config.setJdbcUrl("jdbc:" + connectionUri + "/" + database);
            this.config.addDataSourceProperty("cachePrepStmts", "true");
            this.config.addDataSourceProperty("prepStmtCacheSize", "250");
            this.config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            this.config.addDataSourceProperty("allowMultiQueries", "true");
            this.dataSource = new HikariDataSource(this.config);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    public Connection getOrGetConnection() throws SQLException {
        if (this.connection == null) {
            this.connection = getConnection();
            return this.connection;
        }
        if (this.connection.isClosed()) {
            this.connection = this.getConnection();
            return this.connection;
        } else {
            return this.connection;
        }
    }

    public List<String> listCollections() {
        List<String> collections = new ArrayList<>();

        try {
            PreparedStatement statement = getOrGetConnection().prepareStatement("SHOW TABLES;");
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                collections.add(set.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return collections;
    }

    public boolean hasCollection(String name) {
        return listCollections().contains(name);
    }

    public Connection getCollection(SQLCollection sqlCollection) {
        try {
            if (! hasCollection(sqlCollection.collectionName)) {
                PreparedStatement statement = getOrGetConnection().prepareStatement(getCreateStatement(sqlCollection));
                statement.execute();
            }
            return getOrGetConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getCreateStatement(SQLCollection sqlCollection) {
        StringBuilder builder = new StringBuilder("CREATE TABLE ");
        builder.append("`").append(sqlCollection.collectionName).append("` ( ");

        for (String key : sqlCollection.document.keySet()) {
            if (key.equals(sqlCollection.document.lastKey())) {
                builder.append("`").append(key).append("` ").append(getSQLType(sqlCollection.document.get(key))).append(" ");
                continue;
            }
            builder.append("`").append(key).append("` ").append(getSQLType(sqlCollection.document.get(key))).append(", ");
        }

        builder.append(");");

        SLAPI.getInstance().getMessenger().logInfo("Prepared a create statement: " + builder);

        return builder.toString();
    }

    public String getSQLType(Object object) {
        if (object instanceof String string) return "VARCHAR(512)";
        if (object instanceof List<?> list) return "VARCHAR(512)";
        if (object instanceof Integer integer) return "INTEGER(128)";
        if (object instanceof Float flo) return "FLOAT(128)";
        if (object instanceof Double doub) return "DOUBLE(128,7)";
        if (object instanceof Long lon) return "BIGINT(128)";
        if (object instanceof Date date) return "DATETIME";
        return "VARCHAR(512)";
    }

    public enum SQLDataType {
        VARCHAR,
        INTEGER,
        FLOAT,
        DOUBLE,
        BIGINT,
        DATETIME,
        ;
    }

    public SQLDataType getSQLDataType(Object from) {
        String thing = getSQLType(from);
        if (thing.startsWith("VARCHAR")) return SQLDataType.VARCHAR;
        if (thing.startsWith("INTEGER")) return SQLDataType.INTEGER;
        if (thing.startsWith("FLOAT")) return SQLDataType.FLOAT;
        if (thing.startsWith("DOUBLE")) return SQLDataType.DOUBLE;
        if (thing.startsWith("BIGINT")) return SQLDataType.BIGINT;
        if (thing.startsWith("DATETIME")) return SQLDataType.DATETIME;
        return SQLDataType.VARCHAR;
    }

    public boolean exists(SQLCollection collection) {
        try {
            PreparedStatement statement = getCollection(collection).prepareStatement("SELECT COUNT(*) FROM " + collection.collectionName + " WHERE " + collection.discriminatorKey + " = ?;");
            switch (getSQLDataType(collection.discriminator)) {
                case VARCHAR -> {
                    statement.setString(1, (String) collection.discriminator);
                }
                case INTEGER -> {
                    statement.setInt(1, (int) collection.discriminator);
                }
                case FLOAT -> {
                    statement.setFloat(1, (float) collection.discriminator);
                }
                case DOUBLE -> {
                    statement.setDouble(1, (double) collection.discriminator);
                }
                case BIGINT -> {
                    statement.setLong(1, (long) collection.discriminator);
                }
                case DATETIME -> {
                    statement.setDate(1, (java.sql.Date) collection.discriminator);
                }
            }
            ResultSet set = statement.executeQuery();

            boolean returnValue = false;

            if (set.next()) {
                returnValue = set.getBoolean(1);
            }
            return returnValue;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void push(SQLCollection collection) {
        if (exists(collection)) {
            update(collection);
        } else {
            insert(collection);
        }
    }

    public void update(SQLCollection collection) {
        try {
            PreparedStatement statement = getCollection(collection).prepareStatement("UPDATE " + collection.collectionName + " " +
                    getUpdateString(collection) + ";");
            statement.execute();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insert(SQLCollection collection) {
        try {
            PreparedStatement statement = getCollection(collection).prepareStatement("INSERT INTO " + collection.collectionName + " " +
                    getColumnsString(collection) + " VALUES " + getValuesString(collection) + ";");
            statement.execute();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUpdateString(SQLCollection collection) {
        StringBuilder builder = new StringBuilder();

        for (String key : collection.document.keySet()) {
            builder.append("`");
            Object object = collection.document.get(key);
            builder.append(key).append("` = ");
            if (object instanceof String string) builder.append("'").append(string).append("'");
            else builder.append(object);
            if (key.equals(collection.document.lastKey())) continue;

            builder.append(", ");
        }

        return builder.toString();
    }

    public String getColumnsString(SQLCollection collection) {
        StringBuilder builder = new StringBuilder("( ");

        for (String key : collection.document.keySet()) {
            if (key.equals(collection.document.lastKey())) {
                builder.append("`").append(key).append("`");
                continue;
            }
            builder.append("`").append(key).append("`, ");
        }

        builder.append(")");

        return builder.toString();
    }

    public String getValuesString(SQLCollection collection) {
        StringBuilder builder = new StringBuilder("( ");

        for (Object object : collection.document.values()) {
            if (object.equals(collection.document.lastKey())) {
                if (object instanceof String string) builder.append("'").append(string).append("'");
                else builder.append(object);
                continue;
            }
            if (object instanceof String string) builder.append("'").append(string).append("', ");
            else builder.append(object).append(", ");
        }

        builder.append(")");

        return builder.toString();
    }

    public void delete(SQLCollection collection) {
        try {
            PreparedStatement statement = getCollection(collection).prepareStatement("DELETE FROM " + collection.collectionName + " WHERE " + collection.discriminatorKey + " = ?;");
            switch (getSQLDataType(collection.discriminator)) {
                case VARCHAR -> {
                    statement.setString(1, (String) collection.discriminator);
                }
                case INTEGER -> {
                    statement.setInt(1, (int) collection.discriminator);
                }
                case FLOAT -> {
                    statement.setFloat(1, (float) collection.discriminator);
                }
                case DOUBLE -> {
                    statement.setDouble(1, (double) collection.discriminator);
                }
                case BIGINT -> {
                    statement.setLong(1, (long) collection.discriminator);
                }
                case DATETIME -> {
                    statement.setDate(1, (java.sql.Date) collection.discriminator);
                }
            }
            statement.execute();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SQLCollection get(String collectionName, String discriminatorKey, Object discriminator) {
        SQLCollection collection = new SQLCollection(collectionName, discriminatorKey, discriminator);

        try {
            PreparedStatement statement = getOrGetConnection().prepareStatement("SELECT * FROM " + collection.collectionName + " WHERE " + collection.discriminatorKey + " = ?;");
            switch (getSQLDataType(collection.discriminator)) {
                case VARCHAR -> {
                    statement.setString(1, (String) collection.discriminator);
                }
                case INTEGER -> {
                    statement.setInt(1, (int) collection.discriminator);
                }
                case FLOAT -> {
                    statement.setFloat(1, (float) collection.discriminator);
                }
                case DOUBLE -> {
                    statement.setDouble(1, (double) collection.discriminator);
                }
                case BIGINT -> {
                    statement.setLong(1, (long) collection.discriminator);
                }
                case DATETIME -> {
                    statement.setDate(1, (java.sql.Date) collection.discriminator);
                }
            }

            ResultSet set = statement.executeQuery();
            ResultSetMetaData metaData = set.getMetaData();

            for (int i = 1; i <= metaData.getColumnCount(); i ++) {
                collection.putSet(metaData.getColumnLabel(i), set.getObject(i));
            }

            statement.close();

            return collection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
