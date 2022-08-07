package net.streamline.api.configs;

import net.streamline.api.utils.MathUtils;

import java.io.InputStream;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class StorageResource<T> {
    public StorageUtils.StorageType type;
    public Class<T> resourceType;
    public String discriminatorKey;
    public Object discriminator;
    public int hangingMillis;
    public Date lastReload;
    public TreeMap<String, Object> map;

    public StorageResource(Class<T> resourceType, String discriminatorKey, Object discriminator) {
        this.resourceType = resourceType;
        this.discriminatorKey = discriminatorKey;
        this.discriminator = discriminator;
        this.type = StorageUtils.getStorageType(resourceType);
        this.hangingMillis = 5000;
        this.map = new TreeMap<>();
    }

    public void reloadResource() {
        reloadResource(false);
    }

    public abstract <O> O get(String key, Class<O> def);

    public void reloadResource(boolean force) {
        if (! force) {
            if (this.lastReload != null) {
                if (!MathUtils.isDateOlderThan(this.lastReload, this.hangingMillis, ChronoUnit.MILLIS)) {
                    return;
                }
            }
        }

        this.continueReloadResource();
        this.lastReload = new Date();
    }

    public abstract void continueReloadResource();

    public abstract void write(String key, Object value);

    public abstract <O> O getOrSetDefault(String key, O value);

    public void sync() {
        this.push();
        this.reloadResource();
    }

    public abstract void push();

    public abstract void delete();

    public abstract boolean exists();

    public ConcurrentSkipListSet<String> singleLayerKeySet() {
        return singleLayerKeySet("");
    }

    public ConcurrentSkipListSet<String> singleLayerKeySet(String section) {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();

        this.map.keySet().forEach(a -> {
            if (a.startsWith(section)) {
                int start = a.substring(section.length()).lastIndexOf(".") + 1;
                String k = a.substring(start);
                int end = k.indexOf(".");
                if (end == -1) end = k.length();
                k = k.substring(0, end);
                r.add(k);
            }
        });

        return r;
    }

    public void setHangingMillis(int setAs) {
        this.hangingMillis = setAs;
    }

    public InputStream getResourceAsStream(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }
}
