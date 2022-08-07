package net.streamline.api.savables;

import net.streamline.api.configs.StorageResource;

public interface StreamlineResource {
    StorageResource<?> getStorageResource();
}
