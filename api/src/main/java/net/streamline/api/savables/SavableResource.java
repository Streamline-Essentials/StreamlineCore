package net.streamline.api.savables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.configs.StorageResource;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public abstract class SavableResource implements StreamlineResource, Comparable<Date> {
    @Getter @Setter
    private StorageResource<?> storageResource;
    @Getter @Setter
    private String uuid;
    @Getter @Setter
    private boolean enabled;

    public SavableResource(String uuid, StorageResource<?> storageResource) {
        this.uuid = uuid;
        this.storageResource = storageResource;

        try {
            this.enabled = true;
        } catch (Exception e) {
            e.printStackTrace();
            this.enabled = false;
        }

        this.populateDefaults();

        this.loadValues();
    }

    public void reload() {
        this.storageResource.reloadResource();
    }

    abstract public void populateDefaults();

    public <T> T getOrSetDefault(String key, T def) {
        return this.storageResource.getOrSetDefault(key, def);
    }

    abstract public void loadValues();

    abstract public void saveAll();

    public void set(final String key, final Object value) {
        this.storageResource.write(key, value);
    }

    public String toString() {
        return "SavableResource()[ KEY: " + this.storageResource.getDiscriminatorKey() + " , VALUE: " + this.storageResource.getDiscriminator() + " ]";
    }

    public void dispose() throws Throwable {
        this.uuid = null;
        this.finalize();
    }

    @Override
    public int compareTo(@NotNull Date other) {
        return getStorageResource().compareTo(other);
    }
}
