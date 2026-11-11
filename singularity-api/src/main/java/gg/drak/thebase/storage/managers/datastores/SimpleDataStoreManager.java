package gg.drak.thebase.storage.managers.datastores;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import gg.drak.thebase.storage.datastores.SimpleFlatDatastore;

import java.io.File;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class SimpleDataStoreManager<T extends SimpleFlatDatastore<?, ?>> implements IDataStoreManager<T> {
    @Getter
    final Class<T> classifier;
    @Getter
    final File parentDirectory;

    @Getter @Setter
    ConcurrentSkipListMap<String, T> loadedResources = new ConcurrentSkipListMap<>();

    public SimpleDataStoreManager(Class<T> classifier, File parentDirectory) {
        this.classifier = classifier;
        this.parentDirectory = parentDirectory;
    }

    public ConcurrentSkipListSet<T> getLoadedResourcesAsSet() {
        return new ConcurrentSkipListSet<>(getLoadedResources().values());
    }

    @Override
    public boolean isAlreadyLoaded(String identifier) {
        return getLoadedResources().containsKey(identifier);
    }

    @Override
    public void load(T resource) {
        if (isAlreadyLoaded((String) resource.getDiscriminator())) return;

        getLoadedResources().put((String) resource.getDiscriminator(), resource);
    }

    @Override
    public void instantiateAndLoad(String identifier) {
        if (isAlreadyLoaded(identifier)) return;

        T resource = instantiate(identifier);
    }

    @Override
    public void unload(String identifier) {
        if (! isAlreadyLoaded(identifier)) return;

        getLoadedResources().remove(identifier);
    }

    @Override
    public @Nullable T get(String identifier) {
        if (! isAlreadyLoaded(identifier)) return null;

        return getLoadedResources().get(identifier);
    }

    @Override
    public @NotNull T getOrLoad(String identifier) {
        T resource = get(identifier);
        if (resource != null) return resource;

        resource = instantiate(identifier);
        load(resource);
        return resource;
    }

    @Override
    public void saveAll() {
        getLoadedResourcesAsSet().forEach(SimpleFlatDatastore::save);
    }
}
