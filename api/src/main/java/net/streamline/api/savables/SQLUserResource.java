package net.streamline.api.savables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.StringUtil;
import net.streamline.api.utils.UserUtils;
import tv.quaint.storage.resources.databases.MySQLResource;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.thebase.lib.hikari.HikariConfig;
import tv.quaint.thebase.lib.hikari.HikariDataSource;

import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class SQLUserResource<T extends StreamlineUser> extends MySQLResource {
    HikariDataSource dataSource;
    final Class<T> userClass;
    @Getter @Setter
    T user;

    public SQLUserResource(String uuid, DatabaseConfig config, Class<T> userClass) {
        super("uuid", uuid, config.getTablePrefix() + UserUtils.getTableNameByUserType(userClass), config);
        this.userClass = userClass;
    }

    @Override
    public HikariDataSource getProvider() {
        if (dataSource == null) {
            this.dataSource = createNewDataSource();
        } else {
            if (dataSource.isClosed()) {
                this.dataSource = createNewDataSource();
            }
        }
        return dataSource;
    }

    private HikariDataSource createNewDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + getConfig().getHost() + ":" + getConfig().getPort() + "/" + getConfig().getDatabase());
        hikariConfig.setUsername(getConfig().getUsername());
        hikariConfig.setPassword(getConfig().getPassword());
        hikariConfig.setDriverClassName("com.mysql.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setValidationTimeout(10000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setPoolName("Streamline-User-Pool");

        return new HikariDataSource(hikariConfig);
    }

    public boolean tableExists() {
        try {
            PreparedStatement statement = getProvider().getConnection().prepareStatement("SELECT count(*) FROM information_schema.tables WHERE table_name = ? LIMIT 1;");
            statement.setString(1, getTable());

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) != 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void createTable() {
        UserUtils.ensureTable(user, this);
    }

    @Override
    public void ensureTableExists() {
        if (! tableExists()) {
            createTable();
        }
    }

    @Override
    public void insert() {
        if (user instanceof StreamlinePlayer) {
            StreamlinePlayer streamlinePlayer = (StreamlinePlayer) user;
            try {
                PreparedStatement statement = getProvider().getConnection().prepareStatement("INSERT INTO " + getTable() + " " +
                        "(uuid, latestName, displayName, tags, points, lastMessage, latestServer, totalXP, currentXP, level, playSeconds, latestIP, ips, names)" +
                        " VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
                statement.setString(1, streamlinePlayer.getUuid());
                statement.setString(2, streamlinePlayer.getLatestName());
                statement.setString(3, streamlinePlayer.getDisplayName());
                statement.setString(4, StringUtil.concat(streamlinePlayer.getTagList(), ","));
                statement.setDouble(5, streamlinePlayer.getPoints());
                statement.setString(6, streamlinePlayer.getLastMessage());
                statement.setString(7, streamlinePlayer.getLatestServer());
                statement.setDouble(8, streamlinePlayer.getTotalXP());
                statement.setDouble(9, streamlinePlayer.getCurrentXP());
                statement.setInt(10, streamlinePlayer.getLevel());
                statement.setInt(11, streamlinePlayer.getPlaySeconds());
                statement.setString(12, streamlinePlayer.getLatestIP());
                statement.setString(13, StringUtil.concat(streamlinePlayer.getIpList(), ","));
                statement.setString(14, StringUtil.concat(streamlinePlayer.getNameList(), ","));

                statement.executeUpdate();

                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                PreparedStatement statement = getProvider().getConnection().prepareStatement("INSERT INTO " + getTable() + " " +
                        "(uuid, latestName, displayName, tags, points, lastMessage, latestServer)" +
                        " VALUES " +
                        "(?, ?, ?, ?, ?, ?, ?);");
                statement.setString(1, user.getUuid());
                statement.setString(2, user.getLatestName());
                statement.setString(3, user.getDisplayName());
                statement.setString(4, StringUtil.concat(user.getTagList(), ","));
                statement.setDouble(5, user.getPoints());
                statement.setString(6, user.getLastMessage());
                statement.setString(7, user.getLatestServer());

                statement.executeUpdate();

                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update() {
        if (user instanceof StreamlinePlayer) {
            StreamlinePlayer streamlinePlayer = (StreamlinePlayer) user;
            try {
                PreparedStatement statement = getProvider().getConnection().prepareStatement("UPDATE " + getTable() + " SET " +
                        "latestName = ?, displayName = ?, tags = ?, points = ?, lastMessage = ?, latestServer = ?, totalXP = ?, currentXP = ?, level = ?, playSeconds = ?, latestIP = ?, ips = ?, names = ? " +
                        "WHERE uuid = ?;");
                statement.setString(1, streamlinePlayer.getLatestName());
                statement.setString(2, streamlinePlayer.getDisplayName());
                statement.setString(3, StringUtil.concat(streamlinePlayer.getTagList(), ","));
                statement.setDouble(4, streamlinePlayer.getPoints());
                statement.setString(5, streamlinePlayer.getLastMessage());
                statement.setString(6, streamlinePlayer.getLatestServer());
                statement.setDouble(7, streamlinePlayer.getTotalXP());
                statement.setDouble(8, streamlinePlayer.getCurrentXP());
                statement.setInt(9, streamlinePlayer.getLevel());
                statement.setInt(10, streamlinePlayer.getPlaySeconds());
                statement.setString(11, streamlinePlayer.getLatestIP());
                statement.setString(12, StringUtil.concat(streamlinePlayer.getIpList(), ","));
                statement.setString(13, StringUtil.concat(streamlinePlayer.getNameList(), ","));
                statement.setString(14, streamlinePlayer.getUuid());

                statement.executeUpdate();

                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                PreparedStatement statement = getProvider().getConnection().prepareStatement("UPDATE " + getTable() + " SET " +
                        "latestName = ?, displayName = ?, tags = ?, points = ?, lastMessage = ?, latestServer = ? " +
                        "WHERE uuid = ?;");
                statement.setString(1, user.getLatestName());
                statement.setString(2, user.getDisplayName());
                statement.setString(3, StringUtil.concat(user.getTagList(), ","));
                statement.setDouble(4, user.getPoints());
                statement.setString(5, user.getLastMessage());
                statement.setString(6, user.getLatestServer());
                statement.setString(7, user.getUuid());

                statement.executeUpdate();

                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void continueReloadResource() {
        if (user instanceof StreamlinePlayer) {
            StreamlinePlayer streamlinePlayer = (StreamlinePlayer) user;
            try {
                PreparedStatement statement = getProvider().getConnection().prepareStatement("SELECT * FROM " + getTable() + " WHERE uuid = ?;");
                statement.setString(1, streamlinePlayer.getUuid());

                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    streamlinePlayer.setLatestName(resultSet.getString("latestName"));
                    streamlinePlayer.setDisplayName(resultSet.getString("displayName"));
                    streamlinePlayer.setTagList(StringUtil.splitToConcurrentSet(resultSet.getString("tags"), ","));
                    streamlinePlayer.setPoints(resultSet.getDouble("points"));
                    streamlinePlayer.setLastMessage(resultSet.getString("lastMessage"));
                    streamlinePlayer.setLatestServer(resultSet.getString("latestServer"));
                    streamlinePlayer.setTotalXP(resultSet.getDouble("totalXP"));
                    streamlinePlayer.setCurrentXP(resultSet.getDouble("currentXP"));
                    streamlinePlayer.setLevel(resultSet.getInt("level"));
                    streamlinePlayer.setPlaySeconds(resultSet.getInt("playSeconds"));
                    streamlinePlayer.setLatestIP(resultSet.getString("latestIP"));
                    streamlinePlayer.setIpList(StringUtil.splitToConcurrentSet(resultSet.getString("ips"), ","));
                    streamlinePlayer.setNameList(StringUtil.splitToConcurrentSet(resultSet.getString("names"), ","));
                }

                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                PreparedStatement statement = getProvider().getConnection().prepareStatement("SELECT * FROM " + getTable() + " WHERE uuid = ?;");
                statement.setString(1, user.getUuid());

                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    user.setLatestName(resultSet.getString("latestName"));
                    user.setDisplayName(resultSet.getString("displayName"));
                    user.setTagList(StringUtil.splitToConcurrentSet(resultSet.getString("tags"), ","));
                    user.setPoints(resultSet.getDouble("points"));
                    user.setLastMessage(resultSet.getString("lastMessage"));
                    user.setLatestServer(resultSet.getString("latestServer"));
                }

                statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public <V> void write(String s, V v) {
        updateSingle(s, v);
    }

    @Override
    public void delete() {
        try {
            PreparedStatement statement = getProvider().getConnection().prepareStatement("DELETE FROM " + getTable() + " WHERE uuid = ?;");
            statement.setString(1, user.getUuid());

            statement.executeUpdate();

            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists() {
        try {
            PreparedStatement statement = getProvider().getConnection().prepareStatement("SELECT count(*) FROM ? WHERE uuid = ?;");
            statement.setString(1, getTable());

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) != 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public <V> void updateSingle(String s, V v) {
        try {
            PreparedStatement statement = getProvider().getConnection().prepareStatement("UPDATE " + getTable() + " SET " + s + " = ? WHERE uuid = ?;");
            if (v instanceof String) statement.setString(1, v.toString());
            else if (v instanceof Integer) statement.setInt(1, (Integer) v);
            else if (v instanceof Double) statement.setDouble(1, (Double) v);
            else if (v instanceof Float) statement.setFloat(1, (Float) v);
            else if (v instanceof Long) statement.setLong(1, (Long) v);
            else if (v instanceof Boolean) statement.setBoolean(1, (Boolean) v);
            else if (v instanceof Byte) statement.setByte(1, (Byte) v);
            else if (v instanceof Short) statement.setShort(1, (Short) v);
            else if (v instanceof Character) statement.setString(1, v.toString());
            else if (v instanceof byte[]) statement.setBytes(1, (byte[]) v);
            else if (v instanceof Date) statement.setDate(1, (Date) v);
            else if (v instanceof Time) statement.setTime(1, (Time) v);
            else if (v instanceof Timestamp) statement.setTimestamp(1, (Timestamp) v);
            else if (v instanceof Blob) statement.setBlob(1, (Blob) v);
            else if (v instanceof Clob) statement.setClob(1, (Clob) v);
            else if (v instanceof Array) statement.setArray(1, (Array) v);
            else if (v instanceof Ref) statement.setRef(1, (Ref) v);
            else if (v instanceof URL) statement.setURL(1, (URL) v);
            else if (v instanceof SQLXML) statement.setSQLXML(1, (SQLXML) v);
            else if (v instanceof RowId) statement.setRowId(1, (RowId) v);
            else if (v instanceof Reader) statement.setCharacterStream(1, (Reader) v);
            else if (v instanceof InputStream) statement.setBinaryStream(1, (InputStream) v);
            else if (v instanceof Object) statement.setObject(1, v);
            else throw new IllegalArgumentException("Unknown type: " + v.getClass().getName());

            statement.setString(2, user.getUuid());

            statement.executeUpdate();

            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public <V> void updateMultiple(ConcurrentSkipListMap<String, V> concurrentSkipListMap) {
        concurrentSkipListMap.forEach(this::updateSingle); // todo: make better.
    }
}
