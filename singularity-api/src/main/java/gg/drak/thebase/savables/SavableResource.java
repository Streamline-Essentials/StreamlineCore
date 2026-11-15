package gg.drak.thebase.savables;

import lombok.Getter;
import lombok.Setter;
import gg.drak.thebase.events.BaseEventHandler;
import gg.drak.thebase.objects.Identifiable;
import gg.drak.thebase.savables.events.CreateSavableResourceEvent;
import gg.drak.thebase.savables.events.DeleteSavableResourceEvent;
import gg.drak.thebase.storage.resources.StorageResource;

public abstract class SavableResource implements Identifiable {
    @Getter @Setter
    private StorageResource<?> storageResource;
    @Getter @Setter
    private String uuid;
    @Getter @Setter
    private boolean enabled;
    @Getter @Setter
    private boolean isFirstLoad = false;

    @Override
    public String getIdentifier() {
        return uuid;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.uuid = identifier;
    }

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
            setFirstLoad(true);
            CreateSavableResourceEvent<SavableResource> event = new CreateSavableResourceEvent<>(this);
            BaseEventHandler.fireEvent(event);
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
        BaseEventHandler.fireEvent(event);
        if (event.isCancelled()) return;
        this.uuid = null;
        try {
            finalize();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

