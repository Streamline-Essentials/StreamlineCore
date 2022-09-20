package net.streamline.api.configs.given;

import de.leonhard.storage.Json;
import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.FlatFileResource;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class CachedUUIDsHandler extends FlatFileResource<Json> {
    public static class Runner extends BaseRunnable {
        public Runner() {
            super(1200, 1200);
        }

        @Override
        public void run() {
            setCachedTotalNames(new ConcurrentSkipListMap<>());
            setCachedUUIDs(new ConcurrentSkipListMap<>());
        }
    }

    @Getter @Setter
    private static ConcurrentSkipListMap<String, String> cachedUUIDs = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, String> cachedCurrentNames = new ConcurrentSkipListMap<>();
    @Getter @Setter
    private static ConcurrentSkipListMap<String, ConcurrentSkipListMap<Integer, String>> cachedTotalNames = new ConcurrentSkipListMap<>();

    @Getter @Setter
    private static CachedUUIDsHandler instance;

    @Getter @Setter
    private static Runner runner;

    public CachedUUIDsHandler() {
        super(Json.class, "cached-uuids.json", SLAPI.getDataFolder(), false);
        setInstance(this);
        setRunner(new Runner());
    }

    public static void cachePlayer(String uuid, String... allNames) {
        ConcurrentSkipListMap<Integer, String> names = getOrSetDefaultNames(uuid);

        ConcurrentSkipListMap<Integer, String> a = new ConcurrentSkipListMap<>();
        int actual = 1;
        for (int i = 0; i < allNames.length; i ++) {
            String s = allNames[i];
            if (names.containsValue(s)) continue;
            a.put(names.size() + actual, s);
            actual ++;
        }

        names.putAll(a);

        for (int i : names.keySet()) {
            getInstance().write(uuid + ".total." + i, names.get(i));
        }

        String currentName = allNames[allNames.length - 1];
        getInstance().write(uuid + ".current", currentName);

        getCachedTotalNames().put(uuid, names);
        getCachedUUIDs().put(uuid, currentName);
        getCachedCurrentNames().put(currentName, uuid);
    }

    public static ConcurrentSkipListMap<Integer, String> getOrSetDefaultNames(String uuid) {
        ConcurrentSkipListMap<Integer, String> r = new ConcurrentSkipListMap<>();
        for (String key : getInstance().singleLayerKeySet(uuid + ".total")) {
            try {
                int i = Integer.parseInt(key);
                r.put(i, getInstance().get(uuid + ".total." + key, String.class));
            } catch (Exception e) {
                e.printStackTrace();
                // do nothing...?
            }
        }

        return r;
    }

    public static ConcurrentSkipListMap<Integer, String> getCachedNames(String uuid) {
        if (getCachedTotalNames().containsKey(uuid)) {
            if (getCachedTotalNames().get(uuid).size() > 0) {
                return getCachedTotalNames().get(uuid);
            }
        }
        return getOrSetDefaultNames(uuid);
    }

    public static String getCachedUUID(String username) {
        if (username.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return username;
        if (username.contains("-")) return username;

        if (getCachedUUIDs().containsKey(username)) {
            return getCachedUUIDs().get(username);
        } else {
            String uuid = getUUID(username);
            if (uuid == null) {
                return null;
            }
            getCachedUUIDs().put(username, uuid);
            return uuid;
        }
    }

    public static String getCachedName(String uuid) {
        if (uuid.equals(GivenConfigs.getMainConfig().userConsoleDiscriminator())) return GivenConfigs.getMainConfig().userConsoleNameRegular();
        if (! uuid.contains("-")) return uuid;

        if (getCachedCurrentNames().containsKey(uuid)) {
            return getCachedCurrentNames().get(uuid);
        } else {
            String name = getName(uuid);
            if (name == null) return null;
            getCachedCurrentNames().put(uuid, name);
            return name;
        }
    }

    private static String getUUID(String username) {
        if (SLAPI.getGeyserHolder().isPresent()) {
            String r = SLAPI.getGeyserHolder().getUUID(username);
            if (r != null) return r;
        }

        for (String key : getInstance().singleLayerKeySet()) {
            if (getInstance().get(key + ".current", String.class).equals(username)) return key;
        }

        return null;
    }

    private static String getName(String uuid) {
        if (SLAPI.getGeyserHolder().isPresent()) {
            String r = SLAPI.getGeyserHolder().getName(uuid);
            if (r != null) return r;
        }

        try {
            return getInstance().get(uuid + ".current", String.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String makeDashedUUID(String unformatted){
        StringBuilder formatted = new StringBuilder();
        int i = 1;
        for (Character character : unformatted.toCharArray()){
            if (i == 9 || i == 13 || i == 17 || i == 21){
                formatted.append("-").append(character);
            } else {
                formatted.append(character);
            }
            i++;
        }

        return formatted.toString();
    }

    public static String stripUUID(String uuid) {
        return uuid.replace("-", "");
    }

    public static boolean isCached(String uuid) {
        return getInstance().resource.singleLayerKeySet().contains(uuid);
    }
}
