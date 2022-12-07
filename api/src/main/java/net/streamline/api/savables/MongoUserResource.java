package net.streamline.api.savables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.StringUtil;
import net.streamline.api.utils.UserUtils;
import tv.quaint.storage.resources.databases.MongoResource;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.thebase.lib.bson.Document;
import tv.quaint.thebase.lib.mongodb.Block;
import tv.quaint.thebase.lib.mongodb.MongoClient;
import tv.quaint.thebase.lib.mongodb.MongoClientURI;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class MongoUserResource<T extends StreamlineUser> extends MongoResource {
    MongoClient client;
    final Class<T> userClass;
    @Getter @Setter
    T user;

    public MongoUserResource(String uuid, DatabaseConfig config, Class<T> userClass) {
        super("uuid", uuid, config.getTablePrefix() + UserUtils.getTableNameByUserType(userClass), config);
        this.userClass = userClass;
    }

    @Override
    public MongoClient getProvider() {
        if (client == null) {
            client = createNewClient();
        }
        return client;
    }

    public MongoClient createNewClient() {
        String link = getConfig().getLink();

        link.replace("{{user}}", getConfig().getUsername());
        link.replace("{{pass}}", getConfig().getPassword());
        link.replace("{{host}}", getConfig().getHost());
        link.replace("{{port}}", String.valueOf(getConfig().getPort()));
        link.replace("{{database}}", getConfig().getDatabase());
        MongoClientURI uri = new MongoClientURI(link);

        return new MongoClient(uri);
    }

    @Override
    public void createTable() {
        getProvider().getDatabase(getConfig().getDatabase()).createCollection(getTable());
    }

    public ConcurrentSkipListSet<String> collectionNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        getProvider().getDatabase(getConfig().getDatabase()).listCollectionNames().forEach((Block<? super String>) r::add);

        return r;
    }

    @Override
    public void ensureTableExists() {
        if (! collectionNames().contains(getTable())) {
            createTable();
        }
    }

    @Override
    public void insert() {
        if (user instanceof StreamlinePlayer) {
            StreamlinePlayer streamlinePlayer = (StreamlinePlayer) user;
            Document document = new Document();
            document.put("uuid", streamlinePlayer.getUuid());
            document.put("latestName", streamlinePlayer.getLatestName());
            document.put("displayName", streamlinePlayer.getDisplayName());
            document.put("tags", StringUtil.concat(streamlinePlayer.getTagList(), ","));
            document.put("points", streamlinePlayer.getPoints());
            document.put("lastMessage", streamlinePlayer.getLastMessage());
            document.put("latestServer", streamlinePlayer.getLatestServer());
            document.put("totalXP", streamlinePlayer.getTotalXP());
            document.put("currentXP", streamlinePlayer.getCurrentXP());
            document.put("level", streamlinePlayer.getLevel());
            document.put("playSeconds", streamlinePlayer.getPlaySeconds());
            document.put("latestIP", streamlinePlayer.getLatestIP());
            document.put("ips", StringUtil.concat(streamlinePlayer.getIpList(), ","));
            document.put("names", StringUtil.concat(streamlinePlayer.getNameList(), ","));

            getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).insertOne(document);
        } else {
            Document document = new Document();
            document.put("uuid", user.getUuid());
            document.put("latestName", user.getLatestName());
            document.put("displayName", user.getDisplayName());
            document.put("tags", StringUtil.concat(user.getTagList(), ","));
            document.put("points", user.getPoints());
            document.put("lastMessage", user.getLastMessage());
            document.put("latestServer", user.getLatestServer());

            getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).insertOne(document);
        }
    }

    @Override
    public void update() {
        if (user instanceof StreamlinePlayer) {
            StreamlinePlayer streamlinePlayer = (StreamlinePlayer) user;
            Document document = new Document();
            document.put("uuid", streamlinePlayer.getUuid());
            document.put("latestName", streamlinePlayer.getLatestName());
            document.put("displayName", streamlinePlayer.getDisplayName());
            document.put("tags", StringUtil.concat(streamlinePlayer.getTagList(), ","));
            document.put("points", streamlinePlayer.getPoints());
            document.put("lastMessage", streamlinePlayer.getLastMessage());
            document.put("latestServer", streamlinePlayer.getLatestServer());
            document.put("totalXP", streamlinePlayer.getTotalXP());
            document.put("currentXP", streamlinePlayer.getCurrentXP());
            document.put("level", streamlinePlayer.getLevel());
            document.put("playSeconds", streamlinePlayer.getPlaySeconds());
            document.put("latestIP", streamlinePlayer.getLatestIP());
            document.put("ips", StringUtil.concat(streamlinePlayer.getIpList(), ","));
            document.put("names", StringUtil.concat(streamlinePlayer.getNameList(), ","));

            getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).replaceOne(new Document("uuid", streamlinePlayer.getUuid()), document);
        } else {
            Document document = new Document();
            document.put("uuid", user.getUuid());
            document.put("latestName", user.getLatestName());
            document.put("displayName", user.getDisplayName());
            document.put("tags", StringUtil.concat(user.getTagList(), ","));
            document.put("points", user.getPoints());
            document.put("lastMessage", user.getLastMessage());
            document.put("latestServer", user.getLatestServer());

            getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).replaceOne(new Document("uuid", user.getUuid()), document);
        }
    }

    @Override
    public void continueReloadResource() {
        Document document = getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).find(new Document("uuid", user.getUuid())).first();

        if (document == null) {
            insert();
            return;
        }

        if (user instanceof StreamlinePlayer) {
            StreamlinePlayer streamlinePlayer = (StreamlinePlayer) user;
            streamlinePlayer.setLatestName(document.getString("latestName"));
            streamlinePlayer.setDisplayName(document.getString("displayName"));
            streamlinePlayer.setTagList(StringUtil.splitToConcurrentSet(document.getString("tags"), ","));
            streamlinePlayer.setPoints(document.getInteger("points"));
            streamlinePlayer.setLastMessage(document.getString("lastMessage"));
            streamlinePlayer.setLatestServer(document.getString("latestServer"));
            streamlinePlayer.setTotalXP(document.getInteger("totalXP"));
            streamlinePlayer.setCurrentXP(document.getInteger("currentXP"));
            streamlinePlayer.setLevel(document.getInteger("level"));
            streamlinePlayer.setPlaySeconds(document.getInteger("playSeconds"));
            streamlinePlayer.setLatestIP(document.getString("latestIP"));
            streamlinePlayer.setIpList(StringUtil.splitToConcurrentSet(document.getString("ips"), ","));
            streamlinePlayer.setNameList(StringUtil.splitToConcurrentSet(document.getString("names"), ","));
        } else {
            user.setLatestName(document.getString("latestName"));
            user.setDisplayName(document.getString("displayName"));
            user.setTagList(StringUtil.splitToConcurrentSet(document.getString("tags"), ","));
            user.setPoints(document.getInteger("points"));
            user.setLastMessage(document.getString("lastMessage"));
            user.setLatestServer(document.getString("latestServer"));
        }
    }

    @Override
    public <V> void write(String s, V v) {
        updateSingle(s, v);
    }

    @Override
    public void delete() {
        getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).deleteOne(new Document("uuid", user.getUuid()));
    }

    @Override
    public boolean exists() {
        return getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).find(new Document("uuid", user.getUuid())).first() != null;
    }

    @Override
    public <V> void updateSingle(String s, V v) {
        Document document = new Document();
        document.put(s, v);
        getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).updateOne(new Document("uuid", user.getUuid()), new Document("$set", document));
    }

    @Override
    public <V> void updateMultiple(ConcurrentSkipListMap<String, V> concurrentSkipListMap) {
        Document document = new Document();
        document.putAll(concurrentSkipListMap);
        getProvider().getDatabase(getConfig().getDatabase()).getCollection(getTable()).updateOne(new Document("uuid", user.getUuid()), new Document("$set", document));
    }
}
