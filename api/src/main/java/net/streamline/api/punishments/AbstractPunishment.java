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
        getStorageResource().getOrSetDefault("id", 0);
        getStorageResource().getOrSetDefault("type", "");
        getStorageResource().getOrSetDefault("convict", "");
        getStorageResource().getOrSetDefault("executioner", "");
        getStorageResource().getOrSetDefault("reason", "");
        getStorageResource().getOrSetDefault("time", 0L);
        getStorageResource().getOrSetDefault("active", true);

        populateMore();
    }

    public abstract void populateMore();

    @Override
    public void loadValues() {
        convict = getStorageResource().get("convict", String.class);
        executioner = getStorageResource().get("executioner", String.class);
        reason = getStorageResource().get("reason", String.class);
        convictedAt = new Date(getStorageResource().get("time", Long.class));
        active = getStorageResource().get("active", Boolean.class);

        loadMore();
    }

    public abstract void loadMore();

    @Override
    public void saveAll() {
        getStorageResource().write("id", getId());
        getStorageResource().write("type", type());
        getStorageResource().write("convict", getConvict());
        getStorageResource().write("executioner", getExecutioner());
        getStorageResource().write("reason", getReason());
        getStorageResource().write("time", getConvictedAt().getTime());
        getStorageResource().write("active", isActive());

        saveMore();
    }

    public abstract void saveMore();

    @Override
    public StorageResource<?> getStorageResource() {
        return getStorageResource();
    }
}
