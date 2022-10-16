package net.streamline.api.savables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.events.CreateSavableResourceEvent;
import net.streamline.api.savables.events.DeleteSavableResourceEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Cleaner;

public abstract class SavableResource implements StreamlineResource, Comparable<SavableResource> {

    @Getter
    @Setter
    private StorageResource<?> storageResource;
    @Getter
    @Setter
    private String uuid;
    @Getter
    @Setter
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

        if (getStorageResource().isEmpty()) {
            CreateSavableResourceEvent<SavableResource> event = new CreateSavableResourceEvent<>(this);
            ModuleUtils.fireEvent(event);
            if (event.isCancelled()) {
                try {
                    dispose();
                    return;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
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
        DeleteSavableResourceEvent<SavableResource> event = new DeleteSavableResourceEvent<>(this);
        ModuleUtils.fireEvent(event);
        if (event.isCancelled()) return;
        this.uuid = null;
        try {
            finalize();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public int compareTo(@NotNull SavableResource other) {
        return Long.compare(getStorageResource().getInitializeDate().getTime(), other.getStorageResource().getInitializeDate().getTime());
    }
}

