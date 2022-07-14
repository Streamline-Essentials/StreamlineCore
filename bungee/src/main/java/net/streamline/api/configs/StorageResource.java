package net.streamline.api.configs;

import net.streamline.utils.MathUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;

public abstract class StorageResource<T> {
    public StorageUtils.StorageType type;
    public Class<T> resourceType;
    public String discriminatorKey;
    public Object discriminator;
    public int hangingMillis;
    public Date lastReload;

    public StorageResource(Class<T> resourceType, String discriminatorKey, Object discriminator) {
        this.resourceType = resourceType;
        this.discriminatorKey = discriminatorKey;
        this.discriminator = discriminator;
        this.type = StorageUtils.getStorageType(resourceType);
        this.hangingMillis = 5000;
    }

    public void reloadResource() {
        reloadResource(false);
    }

    public void reloadResource(boolean force) {
        if (! force) {
            if (this.lastReload != null) {
                if (!MathUtils.isDateOlderThan(this.lastReload, this.hangingMillis, ChronoUnit.MILLIS)) {
                    return;
                }
            }
        }

        this.continueReloadResource();
        this.lastReload = new Date();
    }

    public abstract void continueReloadResource();

    public abstract void write(String key, Object value);

    public abstract <O> O getOrSetDefault(String key, O value);

    public void sync() {
        this.push();
        this.reloadResource();
    }

    public abstract void push();

    public abstract void delete();

    public abstract boolean exists();

    public void setHangingMillis(int setAs) {
        this.hangingMillis = setAs;
    }
}
