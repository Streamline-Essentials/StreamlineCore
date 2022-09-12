package net.streamline.platform.config;

import de.leonhard.storage.Json;
import de.leonhard.storage.sections.FlatFileSection;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.api.objects.StreamlineServerInfo;
import net.streamline.api.profile.APIProfile;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;
import net.streamline.base.Streamline;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

public class SavedProfileConfig extends FlatFileResource<Json> {
    public static class Ticker extends BaseRunnable {
        SavedProfileConfig parent;

        public Ticker(SavedProfileConfig parent) {
            super(0, 6000);
            this.parent = parent;
        }

        @Override
        public void run() {
            parent.saveProfile();
        }
    }

    @Getter @Setter
    private APIProfile cachedProfile;
    @Getter @Setter
    private Ticker ticker;

    public SavedProfileConfig() {
        super(Json.class, "saved-profile.json", Streamline.getInstance().getDataFolder(), false);
        setCachedProfile(getProfile());
        ticker = new Ticker(this);
    }

    private APIProfile getProfile() {
        APIProfile r = new APIProfile();
        r.setServers(getServerInfosFromConfig());
        r.setToken(resource.getOrSetDefault("token", UUID.randomUUID().toString()));
        r.verifyToken();
        return r;
    }

    public void saveProfile() {
        saveProfile(getCachedProfile());
    }

    public void saveProfile(APIProfile profile) {
        resource.set("token", profile.getToken());
        profile.getServers().forEach((s, serverInfo) -> {
            resource.set("servers." + s + ".name", serverInfo.getName());
            resource.set("servers." + s + ".motd", serverInfo.getMotd());
            resource.set("servers." + s + ".address", serverInfo.getAddress());
            resource.set("servers." + s + ".online", new ArrayList<>(serverInfo.getOnlineUsers().values()));
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
        FlatFileSection section = resource.getSection("servers." + key);

        String name = section.getString("name");
        String motd = section.getString("motd");
        String address = section.getString("address");
        List<String> onlineUUIDs = section.getStringList("online");

        ConcurrentSkipListMap<String, StreamlineUser> users = new ConcurrentSkipListMap<>();
        onlineUUIDs.forEach(a -> {
            StreamlineUser user = UserUtils.getOrGetUser(a);
            if (user == null) return;
            users.put(user.getUuid(), user);
        });

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
}
