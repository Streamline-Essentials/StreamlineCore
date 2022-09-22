package net.streamline.api.configs;

import de.leonhard.storage.Config;
import de.leonhard.storage.Json;
import de.leonhard.storage.Toml;
import de.leonhard.storage.Yaml;
import de.leonhard.storage.internal.FlatFile;
import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.modules.StreamlineModule;
import net.streamline.api.savables.SavableResource;
import org.bson.Document;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class StorageUtils {
    public static boolean copy(File updateFile, File file) {
        try {
            Files.copy(updateFile.toPath(), file.toPath());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public enum StorageType {
        YAML,
        JSON,
        TOML,
        MONGO,
        MYSQL,
        ;
    }
    public enum DatabaseType {
        MONGO,
        MYSQL,
        ;
    }

    public static StorageType getStorageTypeFromLeonhard(Class<? extends FlatFile> leonhard) {
        if (leonhard.equals(Yaml.class)) return StorageType.YAML;
        if (leonhard.equals(Config.class)) return StorageType.YAML;
        if (leonhard.equals(Json.class)) return StorageType.JSON;
        if (leonhard.equals(Toml.class)) return StorageType.TOML;

        return null;
    }

    public static StorageType getStorageType(Class<?> clazz) {
        if (clazz.equals(Yaml.class)) return StorageType.YAML;
        if (clazz.equals(Config.class)) return StorageType.YAML;
        if (clazz.equals(Json.class)) return StorageType.JSON;
        if (clazz.equals(Toml.class)) return StorageType.TOML;
        if (clazz.equals(Document.class)) return StorageType.MONGO;

        return null;
    }

    public static Document getWhere(String key, Object value) {
        return new Document(key, value);
    }

    public static String parseDotsMongo(String key) {
        return key
                .replace("-", "")
                .replace(".", "_")
                ;
    }

    public static boolean areUsersFlatFiles() {
        switch (GivenConfigs.getMainConfig().userUseType()) {
            default -> {
                return false;
            }
            case YAML, JSON, TOML -> {
                return true;
            }
        }
    }

    public static StorageResource<?> newStorageResource(String uuid, Class<? extends SavableResource> clazz, StorageType type, File folder, DatabaseConfig databaseConfig) {
        switch (type) {
            case YAML -> {
                return new FlatFileResource<>(Config.class, uuid + ".yml", folder, false);
            }
            case JSON -> {
                return new FlatFileResource<>(Json.class, uuid + ".json", folder, false);
            }
            case TOML -> {
                return new FlatFileResource<>(Toml.class, uuid + ".toml", folder, false);
            }
            case MONGO -> {
                return new MongoResource(databaseConfig, clazz.getSimpleName(), "uuid", uuid);
            }
            case MYSQL -> {
                return new MySQLResource(databaseConfig, new SQLCollection(clazz.getSimpleName(), "uuid", uuid));
            }
        }

        return null;
    }

    public static void ensureFileFromSelf(File parentDirectory, File toEnsure, String fileName) {
        if (! toEnsure.exists()) {
            try {
                parentDirectory.mkdirs();
                try (InputStream in = SLAPI.getInstance().getResourceAsStream(fileName)) {
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

    public static void ensureFileFromSelfModule(StreamlineModule module, File parentDirectory, File toEnsure, String fileName) {
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

    public static void ensureFileNoDefault(File parentDirectory, File toEnsure) {
        if (! toEnsure.exists()) {
            try {
                parentDirectory.mkdirs();
                toEnsure.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
