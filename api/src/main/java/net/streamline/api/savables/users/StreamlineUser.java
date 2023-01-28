package net.streamline.api.savables.users;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.utils.UserUtils;
import tv.quaint.storage.StorageUtils;
import tv.quaint.storage.resources.StorageResource;
import tv.quaint.storage.resources.cache.CachedResource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class StreamlineUser extends SavableResource {
    @Override
    public StorageResource<?> getStorageResource() {
        return super.getStorageResource();
    }

    public UUID getUUIDReal() {
        return UUID.fromString(this.getUuid());
    }

    @Getter
    private final StreamlineUser savableUser;
    @Getter
    private String latestName;
    @Getter
    private String displayName;
    @Getter @Setter
    private ConcurrentSkipListSet<String> tagList;
    @Getter
    private double points;
    @Getter @Setter
    private String lastMessage;
    @Setter
    private boolean online;
    public boolean isOnline() {
        updateOnline();
        return this.online;
    }
    @Getter
    private String latestServer;
    @Getter @Setter
    private boolean bypassPermissions;

    public StreamlineUser getStreamlineUser() {
        return savableUser;
    }

    public StreamlineUser asStreamlineUser() {
        return this.savableUser;
    }

    public boolean updateOnline() {
        if (getUuid().equals("%")) this.online = false;

        this.online = SLAPI.getInstance().getUserManager().isOnline(this.getUuid());
        return this.online;
    }

    public StreamlineUser(String uuid, StorageResource<?> storageResource) {
        super(uuid, storageResource == null ? UserUtils.newUserStorageResource(uuid, StreamlineUser.class) : storageResource);
        this.savableUser = this;

        if (GivenConfigs.getMainConfig().savingUseType().equals(StorageUtils.SupportedStorageType.MONGO)) getStorageResource().sync();
        if (GivenConfigs.getMainConfig().savingUseType().equals(StorageUtils.SupportedStorageType.MYSQL)) getStorageResource().sync();
        if (GivenConfigs.getMainConfig().savingUseType().equals(StorageUtils.SupportedStorageType.SQLITE)) getStorageResource().sync();
    }

    @Override
    public void populateDefaults() {
        // Profile.
        String username = SLAPI.getInstance().getUserManager().getUsername(this.getUuid());
        latestName = getOrSetDefault("profile.latest.name", username == null ? "null" : username);
        latestServer = getOrSetDefault("profile.latest.server", MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get());
        displayName = getOrSetDefault("profile.display-name", latestName);
        tagList = new ConcurrentSkipListSet<>(getTagsFromResource());
        points = getOrSetDefault("profile.points", GivenConfigs.getMainConfig().userCombinedPointsDefault());

        populateMoreDefaults();
    }

    public List<String> getTagsFromResource(){
        return getStringListFromResource("profile.tags", getTagsFromConfig().stream().toList());
    }

    public List<String> getStringListFromResource(String key, List<String> def){
        if (getStorageResource() instanceof CachedResource<?>) {
            try {
                String s = getStorageResource().get(key, String.class);
                if (s == null) {
                    set(key, def);
                    Object o = getStorageResource().get(key, Object.class);
                    if (o instanceof List<?>) {
                        return (List<String>) o;
                    } else {
                        if (o instanceof String) {
                            s = (String) o;
                        } else {
                            return def;
                        }
                    }
                }
                return List.of(s.split(", "));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getOrSetDefault(key, def);
    }

    abstract public List<String> getTagsFromConfig();

    abstract public void populateMoreDefaults();

    @Override
    public void loadValues() {
        // Profile.
        latestName = getOrSetDefault("profile.latest.name", latestName);
        latestServer = getOrSetDefault("profile.latest.server", latestServer);
        displayName = getOrSetDefault("profile.display-name", displayName);
        tagList = new ConcurrentSkipListSet<>(getTagsFromResource());
        points = getOrSetDefault("profile.points", points);
        // Online.
        online = updateOnline();
        // More.
        loadMoreValues();
    }

    abstract public void loadMoreValues();

    public void saveAll() {
        // Profile.
        set("profile.latest.name", latestName);
        set("profile.latest.server", latestServer);
        set("profile.display-name", displayName);
        set("profile.tags", tagList);
        set("profile.points", points);
        // More.
        saveMore();
        getStorageResource().push();
        sync();
    }

    public void sync() {
        UserUtils.syncUser(this);
    }

    abstract public void saveMore();

    public void addTag(String tag) {
        //        loadValues();
        if (tagList.contains(tag)) return;

        tagList.add(tag);
        //        saveAll();
    }

    public void removeTag(String tag) {
        //        loadValues();
        if (! tagList.contains(tag)) return;

        tagList.remove(tag);
        //        saveAll();
    }

    public void setPoints(double amount) {
        points = amount;
    }

    public void addPoints(double amount) {
        setPoints(points + amount);
    }

    public void removePoints(double amount) {
        setPoints(points - amount);
    }

    public void updateLastMessage(String message) {
        lastMessage = message;
    }

    public void setLatestServer(String server) {
        latestServer = server;
    }

    public void setLatestName(String name) {
        latestName = name;
    }

    public void setDisplayName(String name) {
        displayName = name;
    }

    public String getName() {
        return latestName;
    }

    public void dispose() throws Throwable {
        try {
            UserUtils.unloadUser(this);
            this.setUuid(null);
            getStorageResource().delete();
        } finally {
            super.finalize();
        }
    }
}
