package net.streamline.api.configs;

import de.leonhard.storage.*;
import de.leonhard.storage.internal.FlatFile;
import net.streamline.api.modules.StreamlineModule;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class FlatFileResource<T extends FlatFile> extends StorageResource<T> {
    public T resource;
    public Class<T> resourceType;
    public String name;
    public File parentDirectory;
    public File file;
    public boolean selfContained;
    public StreamlineModule module;

    public FlatFileResource(Class<T> resourceType, String fileName, File parentDirectory, boolean selfContained) {
        super(resourceType, "name", fileName);
        this.resourceType = resourceType;
        this.name = fileName;
        this.parentDirectory = parentDirectory;
        this.file = new File(parentDirectory, fileName);
        this.selfContained = selfContained;
        this.module = null;

        reloadResource();
    }

    public FlatFileResource(StreamlineModule module, Class<T> resourceType, String fileName, File parentDirectory, boolean selfContained) {
        super(resourceType, "name", fileName);
        this.resourceType = resourceType;
        this.name = fileName;
        this.parentDirectory = parentDirectory;
        this.file = new File(parentDirectory, fileName);
        this.selfContained = selfContained;
        this.module = module;

        reloadResource();
    }

    public FlatFileResource(StreamlineModule module, Class<T> resourceType, String fileName, boolean selfContained) {
        super(resourceType, "name", fileName);
        this.resourceType = resourceType;
        this.name = fileName;
        this.parentDirectory = module.getDataFolder();
        this.file = new File(module.getDataFolder(), fileName);
        this.selfContained = selfContained;
        this.module = module;

        reloadResource();
    }

    public boolean isFromModule() {
        return this.module != null;
    }

    public T load(boolean selfContained) {
        if (selfContained) {
            return loadConfigFromSelf(this.file, this.name);
        } else {
            return loadConfigNoDefault(this.file);
        }
    }

    public void reload(boolean selfContained) {
        this.resource = load(selfContained);
    }

    public void syncMap() {
        for (String key : this.resource.keySet()) {
            this.getMap().put(key, this.resource.get(key));
        }
    }

    @Override
    public <O> O get(String key, Class<O> def) {
        try {
            O object = this.resource.get(key, def.newInstance());

            if (! def.isInstance(object)) return null;

            return object;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void continueReloadResource() {
        reload(this.selfContained);
        syncMap();
    }

    @Override
    public void write(String key, Object value) {
        this.resource.set(key, value);
    }

    @Override
    public <O> O getOrSetDefault(String key, O value) {
        return this.resource.getOrSetDefault(key, value);
    }

    @Override
    public void push() {
//        this.resource.write();
    }

    public boolean exists() {
        return this.file.exists();
    }

    public boolean empty() {
        return lineCount() <= 0;
    }

    public TreeMap<Integer, String> lines() {
        try {
            Scanner reader = new Scanner(this.file);

            TreeMap<Integer, String> lines = new TreeMap<>();
            while (reader.hasNext()) {
                String s = reader.nextLine();
                lines.put(lines.size() + 1, s);
            }
            return lines;
        } catch (Exception e) {
            e.printStackTrace();
            return new TreeMap<>();
        }
    }

    public int lineCount() {
        return lines().size();
    }

    public T loadConfigFromSelf(File file, String fileString) {
        return loadConfigFromSelf(file, fileString, isFromModule());
    }

    public T loadConfigFromSelf(File file, String fileString, boolean isFromModule) {
        if (! file.exists()) {
            try {
                this.parentDirectory.mkdirs();
                if (! isFromModule) {
                    try (InputStream in = getResourceAsStream(fileString)) {
                        assert in != null;
                        Files.copy(in, file.toPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try (InputStream in = module.getResourceAsStream(fileString)) {
                        assert in != null;
                        Files.copy(in, file.toPath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (this.resourceType.equals(Config.class)) {
            return (T) SimplixBuilder.fromFile(file).createConfig();
        }
        if (this.resourceType.equals(Yaml.class)) {
            return (T) SimplixBuilder.fromFile(file).createYaml();
        }
        if (this.resourceType.equals(Json.class)) {
            return (T) SimplixBuilder.fromFile(file).createJson();
        }
        if (this.resourceType.equals(Toml.class)) {
            return (T) SimplixBuilder.fromFile(file).createToml();
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

        if (this.resourceType.equals(Config.class)) {
            return (T) SimplixBuilder.fromFile(file).createConfig();
        }
        if (this.resourceType.equals(Yaml.class)) {
            return (T) SimplixBuilder.fromFile(file).createYaml();
        }
        if (this.resourceType.equals(Json.class)) {
            return (T) SimplixBuilder.fromFile(file).createJson();
        }
        if (this.resourceType.equals(Toml.class)) {
            return (T) SimplixBuilder.fromFile(file).createToml();
        }
        return null;
    }

    @Override
    public void delete() {
        this.file.delete();
    }

    @Override
    public ConcurrentSkipListSet<String> singleLayerKeySet() {
        return new ConcurrentSkipListSet<>(resource.singleLayerKeySet());
    }

    @Override
    public ConcurrentSkipListSet<String> singleLayerKeySet(String key) {
        return new ConcurrentSkipListSet<>(resource.singleLayerKeySet(key));
    }
}
