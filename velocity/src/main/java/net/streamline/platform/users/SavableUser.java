package net.streamline.platform.users;

import de.leonhard.storage.internal.FlatFile;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.configs.StorageUtils;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.savables.SavableResource;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.UUIDUtils;

import java.util.List;
import java.util.UUID;

public abstract class SavableUser extends SavableResource implements StreamlineUser {
    @Override
    public StorageResource<?> getStorageResource() {
        return storageResource;
    }

    @Override
    public String getUUID() {
        return this.uuid;
    }

    @Override
    public UUID getUUIDReal() {
        return UUID.fromString(this.uuid);
    }

    @Getter
    private final SavableUser savableUser;
    @Getter
    private String latestName;
    @Getter
    private String displayName;
    @Getter @Setter
    private List<String> tagList;
    @Getter
    private double points;
    @Getter @Setter
    private String lastMessage;
    @Getter @Setter
    private boolean online;
    @Getter
    private String latestServer;
    @Getter @Setter
    private boolean bypassPermissions;

    @Override
    public StreamlineUser getStreamlineUser() {
        return savableUser;
    }

    public SavableUser asStreamlineUser() {
        return this.savableUser;
    }

    public boolean updateOnline() {
        if (uuid.equals("%")) this.online = false;

        this.online = SLAPI.getInstance().getUserManager().isOnline(this.uuid);
        return this.online;
    }

    public SavableUser(String uuid) {
        super(uuid, SLAPI.getInstance().getUserManager().newStorageResource(uuid, uuid.equals("%") ? SavableConsole.class : SavablePlayer.class));

        this.savableUser = this;
    }

    @Override
    public void populateDefaults() {
        // Profile.
        String username = UUIDUtils.getCachedName(this.uuid);
        latestName = getOrSetDefault("profile.latest.name", username == null ? "null" : username);
        latestServer = getOrSetDefault("profile.latest.server", MainMessagesHandler.MESSAGES.DEFAULTS.IS_NULL.get());
        displayName = getOrSetDefault("profile.display-name", latestName);
        tagList = getOrSetDefault("profile.tags", getTagsFromConfig());
        points = getOrSetDefault("profile.points", SLAPI.getInstance().getPlatform().getMainConfig().userCombinedPointsDefault());

        populateMoreDefaults();
    }

    abstract public List<String> getTagsFromConfig();

    abstract public void populateMoreDefaults();

    @Override
    public void loadValues() {
        // Profile.
        latestName = getOrSetDefault("profile.latest.name", latestName);
        latestServer = getOrSetDefault("profile.latest.server", latestServer);
        displayName = getOrSetDefault("profile.display-name", displayName);
        tagList = getOrSetDefault("profile.tags", tagList);
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
        set("profile.display-name", latestName);
        set("profile.tags", tagList);
        set("profile.points", points);
        // More.
        saveMore();
        storageResource.push();
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
            SLAPI.getInstance().getUserManager().unloadUser(this);
            this.uuid = null;
            if (StorageUtils.areUsersFlatFiles()) {
                FlatFileResource<? extends FlatFile> resource = (FlatFileResource<? extends FlatFile>) this.storageResource;
                resource.delete();
            }
        } finally {
            super.finalize();
        }
    }
}
