package net.streamline.api.savables.users;

import net.streamline.api.savables.StreamlineResource;

import java.util.List;
import java.util.UUID;

public interface StreamlineUser extends StreamlineResource {
    String getUUID();

    UUID getUUIDReal();

    StreamlineUser getStreamlineUser();

    String getLatestName();

    String getDisplayName();

    List<String> getTagList();

    double getPoints();

    String getLastMessage();

    boolean isOnline();

    String getLatestServer();

    boolean isBypassPermissions();

    void setTagList(List<String> tagList);

    void setLastMessage(String message);

    void setOnline(boolean online);

    void setBypassPermissions(boolean bypassPermissions);

    StreamlineUser asStreamlineUser();

    boolean updateOnline();

    List<String> getTagsFromConfig();

    void populateMoreDefaults();

    void loadMoreValues();

    void saveAll();

    void saveMore();

    void addTag(String tag);

    void removeTag(String tag);

    void setPoints(double amount);

    void addPoints(double amount);

    void removePoints(double amount);

    void updateLastMessage(String message);

    void setLatestServer(String server);

    void setLatestName(String name);

    void setDisplayName(String name);

    String getName();

    void dispose() throws Throwable;

    void reload();
}
