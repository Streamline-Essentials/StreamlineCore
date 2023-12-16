package net.streamline.api.savables.users;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import tv.quaint.storage.resources.StorageResource;
import tv.quaint.utils.StringUtils;

import java.util.ArrayList;
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
    @Setter
    private String latestName;
    @Setter
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
    }

    @Override
    public void populateDefaults() {
        // Profile.
        String username = SLAPI.getInstance().getUserManager().getUsername(this.getUuid());
        if (getUuid() == null) {
            SLAPI.getBaseModule().logDebug("UUID is null for user " + username + "!");
        }
        latestName = getOrSetDefault("profile.latest.name", username == null ? "null" : username);

        if (! isOnline()) {
            latestServer = getOrSetDefault("profile.latest.server", "null");
            displayName = getOrSetDefault("profile.display-name", latestName);
        } else {
            latestServer = SLAPI.getInstance().getUserManager().getServerPlayerIsOn(this.getUuid());
            displayName = SLAPI.getInstance().getUserManager().getDisplayName(this.getUuid());
        }
        tagList = new ConcurrentSkipListSet<>(getTagsFromResource());
        points = getOrSetDefault("profile.points", GivenConfigs.getMainConfig().userCombinedPointsDefault());

        populateMoreDefaults();
    }

    public List<String> getTagsFromResource(){
        return getStringListFromResource("profile.tags", new ArrayList<>(getTagsFromConfig()));
    }

    public List<String> getStringListFromResource(String key, List<String> def){
        String defString = StringUtils.listToString(def, ",");
        try {
            String s = getStorageResource().getOrSetDefault(key, defString);
            return StringUtils.stringToList(s, ",");
        } catch (ClassCastException e) {
            List<String> list = getStorageResource().getOrSetDefault(key, def);
            if (list == null) {
                list = new ArrayList<>();
            }

            return list;
        }
    }

    abstract public List<String> getTagsFromConfig();

    abstract public void populateMoreDefaults();

    @Override
    public void loadValues() {
        // Profile.
        latestName = getOrSetDefault("profile.latest.name", latestName);

        if (! isOnline()) {
            latestServer = getOrSetDefault("profile.latest.server", latestServer);
            displayName = getOrSetDefault("profile.display-name", displayName);
        } else {
            latestServer = SLAPI.getInstance().getUserManager().getServerPlayerIsOn(this.getUuid());
            displayName = SLAPI.getInstance().getUserManager().getDisplayName(this.getUuid());
        }

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
        String tags = StringUtils.listToString(new ArrayList<>(tagList), ",");
        set("profile.tags", tags);
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

    public String getLatestName() {
        return getLatest(latestName);
    }

    private String getLatest(String latestName) {
        if (online) {
            setLatestName(SLAPI.getInstance().getUserManager().getUsername(getUuid()));
            if (displayName == null) {
                if (this instanceof StreamlinePlayer) {
                    StreamlinePlayer player = (StreamlinePlayer) this;
                    player.setDisplayName(GivenConfigs.getMainConfig().userCombinedNicknameDefault());
                }
                return latestName;
            }
            if (displayName.equals("null")) {
                if (this instanceof StreamlinePlayer) {
                    StreamlinePlayer player = (StreamlinePlayer) this;
                    player.setDisplayName(GivenConfigs.getMainConfig().userCombinedNicknameDefault());
                }
            }
        }
        return latestName;
    }

    public String getDisplayName() {
        return getLatest(displayName);
    }

    public String getName() {
        return latestName;
    }

    public void sendMessage(String message, boolean format) {
        if (format) message = MessageUtils.codedString(message);

        ModuleUtils.sendMessage(this, message);
    }

    public void sendMessage(String message) {
        sendMessage(message, true);
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

    public void set(String key, Object value) {
        getStorageResource().write(key, value);
    }

    public <T> T getOrSetDefault(String key, T def) {
        return getStorageResource().getOrSetDefault(key, def);
    }

    public boolean hasPermission(String permission) {
        return ModuleUtils.hasPermission(this, permission);
    }

    public boolean isConsole() {
        return getUuid().equals(GivenConfigs.getMainConfig().userConsoleDiscriminator());
    }

    public boolean isPlayer() {
        return ! isConsole();
    }

    public void kick(String message) {
        ModuleUtils.kick(this, message);
    }
}
