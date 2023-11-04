package net.streamline.api.savables;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.registries.Identifiable;
import net.streamline.api.registries.RegistryKeys;
import net.streamline.api.registries.SavablesRegistry;
import net.streamline.api.savables.events.CreateSavableResourceEvent;
import net.streamline.api.savables.events.DeleteSavableResourceEvent;
import net.streamline.api.utils.MessageUtils;
import tv.quaint.storage.resources.StorageResource;

@Getter
public abstract class SavableResource implements StreamlineResource, Identifiable {
    @Setter
    private StorageResource<?> storageResource;
    @Setter
    private String uuid;
    @Setter
    private boolean enabled;
    @Setter
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

        try {
            SavablesRegistry registry = RegistryKeys.SAVABLES.getRegistry();
            if (registry == null) {
                registry = SLAPI.getSavablesRegistry();
                if (registry == null) {
                    MessageUtils.logWarning("SavablesRegistry is null! Did you try using it before it was initialized?");
                }
            }

            if (registry == null) {
                MessageUtils.logWarning("SavablesRegistry is still null! Did you try using it before it was initialized?");
                return;
            }

            registry.register(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}

