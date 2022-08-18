package net.streamline.api.punishments;

import lombok.Getter;
import lombok.Setter;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.StorageResource;
import net.streamline.api.configs.StorageUtils;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.savables.SavableResource;

import java.io.File;
import java.util.Date;

public abstract class AbstractPunishment extends SavableResource {
    @Getter
    private final long id;
    @Getter @Setter
    private String convict;
    @Getter @Setter
    private String executioner;
    @Getter @Setter
    private Date convictedAt;
    @Getter @Setter
    private String reason;
    @Getter @Setter
    private boolean active;

    public abstract String type();

    public AbstractPunishment(long id, String convict, String executioner, Date convictedAt) {
        super(String.valueOf(id), StorageUtils.newStorageResource(String.valueOf(id),
                AbstractPunishment.class, GivenConfigs.getPunishmentConfig().getUseType(),
                GivenConfigs.getPunishmentFolder(), GivenConfigs.getPunishmentConfig().getConfiguredDatabase()));

        this.id = id;
        this.convict = convict;
        this.executioner = executioner;
        this.convictedAt = convictedAt;
    }

    public AbstractPunishment(String convict, String executioner) {
        this(PunishmentManager.nextId(), convict, executioner, new Date());
    }

    @Override
    public void populateDefaults() {
        storageResource.getOrSetDefault("id", 0);
        storageResource.getOrSetDefault("type", "");
        storageResource.getOrSetDefault("convict", "");
        storageResource.getOrSetDefault("executioner", "");
        storageResource.getOrSetDefault("reason", "");
        storageResource.getOrSetDefault("time", 0L);
        storageResource.getOrSetDefault("active", true);

        populateMore();
    }

    public abstract void populateMore();

    @Override
    public void loadValues() {
        convict = storageResource.get("convict", String.class);
        executioner = storageResource.get("executioner", String.class);
        reason = storageResource.get("reason", String.class);
        convictedAt = new Date(storageResource.get("time", Long.class));
        active = storageResource.get("active", Boolean.class);

        loadMore();
    }

    public abstract void loadMore();

    @Override
    public void saveAll() {
        storageResource.write("id", getId());
        storageResource.write("type", type());
        storageResource.write("convict", getConvict());
        storageResource.write("executioner", getExecutioner());
        storageResource.write("reason", getReason());
        storageResource.write("time", getConvictedAt().getTime());
        storageResource.write("active", isActive());

        saveMore();
    }

    public abstract void saveMore();

    @Override
    public StorageResource<?> getStorageResource() {
        return null;
    }
}
