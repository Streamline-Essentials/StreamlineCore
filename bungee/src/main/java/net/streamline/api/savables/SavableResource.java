package net.streamline.api.savables;

import net.streamline.api.configs.StorageResource;

public abstract class SavableResource {
    public StorageResource<?> storageResource;
    public String uuid;
    public boolean enabled;

    public SavableResource(String uuid, StorageResource<?> storageResource) {
        this.uuid = uuid;
        this.storageResource = storageResource;

        try {
            reload();
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
        return "SavableResource()[ KEY: " + this.storageResource.discriminatorKey + " , VALUE: " + this.storageResource.discriminator + " ]";
    }

    public void dispose() throws Throwable {
        this.uuid = null;
        this.finalize();
    }
}
