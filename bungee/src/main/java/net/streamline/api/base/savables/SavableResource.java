package net.streamline.api.base.savables;

import de.leonhard.storage.LightningBuilder;
import de.leonhard.storage.Toml;
import de.leonhard.storage.internal.FlatFile;
import net.streamline.api.base.configs.FlatFileResource;
import net.streamline.api.base.configs.StorageResource;
import net.streamline.api.base.savables.users.SavableConsole;
import net.streamline.api.base.savables.users.SavablePlayer;
import net.streamline.api.utils.MessagingUtils;

import java.io.File;

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
