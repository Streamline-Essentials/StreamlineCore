package net.streamline.api.configs.given;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.profile.APIProfile;
import net.streamline.api.scheduler.BaseRunnable;
import tv.quaint.storage.documents.SimpleJsonDocument;
import tv.quaint.thebase.lib.leonhard.storage.sections.FlatFileSection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class SavedProfileConfig extends SimpleJsonDocument {
    @Override
    public void onInit() {

    }

    @Override
    public void onSave() {

    }

    @Override
    public void onDelete() {
        getSelfFile().delete();
    }

    @Override
    public boolean onExists() {
        return getSelfFile().exists();
    }

    public static class Ticker extends BaseRunnable {
        @Getter @Setter
        private SavedProfileConfig parent;

        public Ticker(SavedProfileConfig parent) {
            super(0, 600);
            setParent(parent);
        }

        @Override
        public void run() {
            getParent().saveProfile();
            getParent().saveServers();
        }
    }

    @Getter @Setter
    private APIProfile cachedProfile;
    @Getter @Setter
    private Ticker ticker;

    public SavedProfileConfig() {
        super("saved-profile.json", SLAPI.getInstance().getDataFolder(), false);
        setCachedProfile(getProfile());
        ticker = new Ticker(this);
    }

    private APIProfile getProfile() {
        APIProfile r = new APIProfile();
        r.setServers(getServerInfosFromConfig());
        r.setToken(getResource().getOrSetDefault("token", UUID.randomUUID().toString()));
        r.verifyToken();
        return r;
    }

    public void saveProfile() {
        saveProfile(getCachedProfile());
    }

    public void saveProfile(APIProfile profile) {
        getResource().set("token", profile.getToken());
        profile.getServers().forEach((s, serverInfo) -> {
            getResource().set("servers." + s + ".name", serverInfo.getName());
            getResource().set("servers." + s + ".motd", serverInfo.getMotd());
            getResource().set("servers." + s + ".address", serverInfo.getAddress());
            getResource().set("servers." + s + ".online", serverInfo.getOnlineUsers().stream().toList());
        });
    }

    public ConcurrentSkipListMap<String, StreamlineServerInfo> getServerInfosFromConfig() {
        ConcurrentSkipListMap<String, StreamlineServerInfo> r = new ConcurrentSkipListMap<>();

        singleLayerKeySet("servers").forEach(a -> {
            StreamlineServerInfo serverInfo = getServerInfoFromConfig(a);
            r.put(serverInfo.getIdentifier(), serverInfo);
        });

        return r;
    }

    public StreamlineServerInfo getServerInfoFromConfig(String key) {
        FlatFileSection section = getResource().getSection("servers." + key);

        String name = section.getString("name");
        String motd = section.getString("motd");
        String address = section.getString("address");
        List<String> onlineUUIDs = section.getStringList("online");

        ConcurrentSkipListSet<String> users = new ConcurrentSkipListSet<>(onlineUUIDs);

        return new StreamlineServerInfo(key, name, motd, address, users);
    }

    public void updateServerInfo(StreamlineServerInfo serverInfo) {
        StreamlineServerInfo si = getServerInfo(serverInfo.getIdentifier());
        if (si == null) {
            getCachedProfile().getServers().put(serverInfo.getIdentifier(), serverInfo);
        } else {
            si.setName(serverInfo.getName());
            si.setMotd(serverInfo.getMotd());
            si.setAddress(serverInfo.getAddress());
            si.setOnlineUsers(serverInfo.getOnlineUsers());
        }
    }

    public StreamlineServerInfo getServerInfo(String identifier) {
        return getCachedProfile().getServers().get(identifier);
    }

    public void ensureServers(List<String> identifiers) {
        for (String identifier : getCachedProfile().getServers().keySet()) {
            if (identifiers.contains(identifier)) continue;

            getCachedProfile().getServers().remove(identifier);
        }

        saveProfile();
    }

    public void saveServers() {
        FlatFileSection serverSection = getResource().getSection("servers");

        getServerInfosFromConfig().forEach((s, streamlineServerInfo) -> {
            serverSection.set(streamlineServerInfo.getIdentifier() + ".name", streamlineServerInfo.getName());
            serverSection.set(streamlineServerInfo.getIdentifier() + ".motd", streamlineServerInfo.getMotd());
            serverSection.set(streamlineServerInfo.getIdentifier() + ".address", streamlineServerInfo.getAddress());
            serverSection.set(streamlineServerInfo.getIdentifier() + ".online-users", streamlineServerInfo.getOnlineUsers().stream().toList());
        });
    }
}

