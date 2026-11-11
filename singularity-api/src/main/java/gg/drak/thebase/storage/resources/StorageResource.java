package gg.drak.thebase.storage.resources;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import gg.drak.thebase.objects.Classifiable;
import gg.drak.thebase.storage.StorageUtils;
import gg.drak.thebase.utils.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public abstract class StorageResource<T> implements Comparable<StorageResource<?>>, Classifiable<T> {
    private final Date initializeDate;
    @Setter
    private StorageUtils.SupportedStorageType type;
    @Setter
    private Class<T> resourceType;
    @Setter
    private String discriminatorKey;
    @Setter
    private Object discriminator;
    @Setter
    private int hangingMillis;
    @Setter
    private Date lastReload;
    @Setter
    private ConcurrentSkipListMap<String, Object> map;

    public StorageResource(Class<T> resourceType, String discriminatorKey, Object discriminator) {
        initializeDate = new Date();
        this.resourceType = resourceType;
        this.discriminatorKey = discriminatorKey;
        this.discriminator = discriminator;
        this.type = StorageUtils.getStorageType(resourceType);
        this.hangingMillis = 5000;
        this.map = new ConcurrentSkipListMap<>();
    }

    public void reloadResource() {
        reloadResource(false);
    }

    public abstract <O> O get(String key, Class<O> def);

    public void reloadResource(boolean force) {
        if (! force) {
            Date last = this.lastReload;
            if (last != null && System.currentTimeMillis() - last.getTime() <= this.hangingMillis) {
                return;
            }
        }

        this.continueReloadResource();
        this.lastReload = new Date();
    }

    public abstract void continueReloadResource();

    public abstract <V> void write(String key, V value);

    public abstract <O> O getOrSetDefault(String key, O value);

    public void sync() {
        this.push();
        this.reloadResource();
    }

    public void sync(boolean force) {
        this.push();
        this.reloadResource(force);
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

    public abstract <V> void updateSingle(String key, V value);

    public abstract <V> void updateMultiple(ConcurrentSkipListMap<String, V> values);

    public boolean isEmpty() {
        sync(true);

        return getMap().isEmpty();
    }

    public InputStream getResourceAsStream(String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    @Override
    public int compareTo(@NotNull StorageResource<?> other) {
        return Long.compare(getInitializeDate().getTime(), other.getInitializeDate().getTime());
    }

    @Override
    public Class<T> getClassifier() {
        return getResourceType();
    }

    public String getDiscriminatorAsString() {
        return discriminator + "";
    }

    public String getStringFromStringList(List<String> list, String prefix, String suffix) {
        return prefix + StringUtils.listToString(list, ",") + suffix;
    }

    public String getStringFromStringList(ConcurrentSkipListSet<String> list, String prefix, String suffix) {
        return getStringFromStringList(new ArrayList<>(list), prefix, suffix);
    }
}
