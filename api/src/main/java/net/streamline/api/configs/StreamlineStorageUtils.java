package net.streamline.api.configs;

import net.streamline.api.interfaces.ModuleLike;
import net.streamline.api.savables.SavableResource;
import tv.quaint.storage.StorageUtils;
import tv.quaint.storage.resources.StorageResource;
import tv.quaint.storage.resources.databases.configurations.DatabaseConfig;
import tv.quaint.storage.resources.flat.FlatFileResource;
import tv.quaint.thebase.lib.leonhard.storage.Config;
import tv.quaint.thebase.lib.leonhard.storage.Json;
import tv.quaint.thebase.lib.leonhard.storage.Toml;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class StreamlineStorageUtils {
    public static StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz, StorageUtils.SupportedStorageType type, File folder, DatabaseConfig databaseConfig) {
        switch (type) {
            case YAML:
                return new FlatFileResource<>(Config.class, uuid + ".yml", folder, false);
            case JSON:
                return new FlatFileResource<>(Json.class, uuid + ".json", folder, false);
            case TOML:
                return new FlatFileResource<>(Toml.class, uuid + ".toml", folder, false);
            case SQLITE:
            case MYSQL:
            case MONGO:
                return null;
        }

        return null;
    }

    public static void ensureFileFromSelfModule(ModuleLike module, File parentDirectory, File toEnsure, String fileName) {
        if (! toEnsure.exists()) {
            try {
                parentDirectory.mkdirs();
                try (InputStream in = module.getResourceAsStream(fileName)) {
                    assert in != null;
                    Files.copy(in, toEnsure.toPath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
