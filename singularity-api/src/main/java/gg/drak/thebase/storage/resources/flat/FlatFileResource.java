package gg.drak.thebase.storage.resources.flat;

import de.leonhard.storage.*;
import de.leonhard.storage.internal.FlatFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import gg.drak.thebase.storage.StorageUtils;
import gg.drak.thebase.storage.resources.StorageResource;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

@Getter @Setter
public class FlatFileResource<T extends FlatFile> extends StorageResource<T> {
    T resource;
    final String fileName;
    final File parentDirectory;
    final File selfFile;
    final boolean selfContained;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE)
    private final AtomicLong lastChangeCheck = new AtomicLong();

    public FlatFileResource(Class<T> resourceType, String fileName, File parentDirectory, boolean selfContained) {
        super(resourceType, "name", fileName);
        
        this.fileName = fileName;
        this.parentDirectory = parentDirectory;
        this.selfFile = new File(parentDirectory, fileName);
        this.selfContained = selfContained;

        reloadResource(true);
    }

    public T load(boolean selfContained) {
        if (selfContained) {
            return loadConfigFromSelf(this.selfFile, this.fileName);
        } else {
            return loadConfigNoDefault(this.selfFile);
        }
    }

    public void reload(boolean selfContained) {
        this.resource = load(selfContained);
    }

    public void syncMap() {
        for (String key : this.resource.keySet()) {
            Object obj = this.resource.get(key);
            if (obj == null) continue;
            this.getMap().put(key, obj);
        }
    }

    /**
     * Reloads the underlying file in place if it changed on disk, checking at most once per
     * {@code hangingMillis}. Simplix runs with {@code ReloadSettings.MANUAL} (see
     * {@link StorageUtils#getDefaultReloadSettings()}), so this is what keeps reads through this
     * resource in step with external edits without a file stat on every read.
     */
    public void refreshIfChanged() {
        T current = this.resource;
        if (current == null) return;

        long now = System.currentTimeMillis();
        long last = this.lastChangeCheck.get();
        if (now - last < getHangingMillis()) return;
        if (! this.lastChangeCheck.compareAndSet(last, now)) return;

        if (current.hasChanged()) {
            current.forceReload();
            syncMap();
        }
    }

    /**
     * Returns the underlying Simplix file, first reloading it if it changed on disk (see
     * {@link #refreshIfChanged()}), so callers reading through it directly see file edits too.
     *
     * @return the underlying Simplix file
     */
    public T getResource() {
        refreshIfChanged();
        return this.resource;
    }

    @Override
    public <O> O get(String key, Class<O> def) {
        refreshIfChanged();
        try {
            O object = this.resource.get(key, def.newInstance());

            if (! def.isInstance(object)) return null;

            return object;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Re-reads the file, unless it is already loaded and unchanged on disk. Simplix records its own
     * writes as a load, so values set through this resource do not count as a change.
     */
    @Override
    public void continueReloadResource() {
        T current = this.resource;
        if (current != null && ! current.hasChanged()) return;

        reload(this.selfContained);
        syncMap();
    }

    @Override
    public <V> void write(String key, V value) {
        this.resource.set(key, value);
    }

    @Override
    public <O> O getOrSetDefault(String key, O value) {
        refreshIfChanged();
        return this.resource.getOrSetDefault(key, value);
    }

    @Override
    public void push() {
//        this.resource.write();
    }

    public boolean exists() {
        return this.selfFile.exists();
    }

    public boolean empty() {
        return lineCount() <= 0;
    }

    public ConcurrentSkipListMap<Integer, String> lines() {
        try {
            Scanner reader = new Scanner(this.selfFile);

            ConcurrentSkipListMap<Integer, String> lines = new ConcurrentSkipListMap<>();
            while (reader.hasNext()) {
                String s = reader.nextLine();
                lines.put(lines.size() + 1, s);
            }
            return lines;
        } catch (Exception e) {
            e.printStackTrace();
            return new ConcurrentSkipListMap<>();
        }
    }

    public int lineCount() {
        return lines().size();
    }

    public T loadConfigFromSelf(File file, String fileString) {
        if (! file.exists()) {
            try {
                this.parentDirectory.mkdirs();
                try (InputStream in = getResourceAsStream(fileString)) {
                    assert in != null;
                    Files.copy(in, file.toPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (getResourceType().equals(Config.class)) {
            return (T) StorageUtils.fromFile(file).createConfig();
        }
        if (getResourceType().equals(Yaml.class)) {
            return (T) StorageUtils.fromFile(file).createYaml();
        }
        if (getResourceType().equals(Json.class)) {
            return (T) StorageUtils.fromFile(file).createJson();
        }
        if (getResourceType().equals(Toml.class)) {
            return (T) StorageUtils.fromFile(file).createToml();
        }
        return null;
    }

    public T loadConfigNoDefault(File file) {
        if (! file.exists()) {
            try {
                this.parentDirectory.mkdirs();
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SimplixBuilder builder = SimplixBuilder.fromFile(file).setReloadSettings(StorageUtils.getDefaultReloadSettings());
        if (getResourceType().equals(Config.class)) {
            return (T) builder.createConfig();
        }
        if (getResourceType().equals(Yaml.class)) {
            return (T) builder.createYaml();
        }
        if (getResourceType().equals(Json.class)) {
            return (T) builder.createJson();
        }
        if (getResourceType().equals(Toml.class)) {
            return (T) builder.createToml();
        }
        return null;
    }

    @Override
    public void delete() {
        this.selfFile.delete();
    }

    @Override
    public ConcurrentSkipListSet<String> singleLayerKeySet() {
        return new ConcurrentSkipListSet<>(resource.singleLayerKeySet());
    }

    @Override
    public ConcurrentSkipListSet<String> singleLayerKeySet(String key) {
        return new ConcurrentSkipListSet<>(resource.singleLayerKeySet(key));
    }

    @Override
    public <V> void updateSingle(String key, V value) {
        write(key, value);
    }

    @Override
    public <V> void updateMultiple(ConcurrentSkipListMap<String, V> values) {
        values.forEach(this::updateSingle);
    }
}
