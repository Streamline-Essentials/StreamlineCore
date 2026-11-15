package gg.drak.thebase.storage;

import de.leonhard.storage.*;
import de.leonhard.storage.internal.FlatFile;
import de.leonhard.storage.internal.settings.ConfigSettings;
import de.leonhard.storage.internal.settings.ReloadSettings;
import lombok.Getter;
import gg.drak.thebase.objects.handling.derived.IModifierEventable;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class StorageUtils {
    @Getter
    private static final File environmentFolder = new File(System.getProperty("user.dir"));

    public enum SupportedStorageType {
        YAML,
        JSON,
        TOML,
        MONGO,
        MYSQL,
        SQLITE,
        ;
    }
    public enum SupportedDatabaseType {
        MONGO,
        MYSQL,
        SQLITE,
        ;
    }
    public enum SupportedSQLType {
        MYSQL,
        SQLITE,
        ;
    }

    public static File initializeModifierEventableFolder(IModifierEventable eventable) {
        File parentDir = getEnvironmentFolder();
        if (eventable.isMod()) parentDir = new File(getEnvironmentFolder(), "mods" + File.separator);
        if (eventable.isPlugin()) parentDir = new File(getEnvironmentFolder(), "plugins" + File.separator);
        return new File(parentDir, eventable.getIdentifier() + File.separator);
    }

    public static boolean copy(File updateFile, File file) {
        try {
            Files.copy(updateFile.toPath(), file.toPath());
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static SupportedStorageType getStorageTypeFromLeonhard(Class<? extends FlatFile> leonhard) {
        return getStorageType(leonhard);
    }

    public static SupportedStorageType getStorageType(Class<?> clazz) {
        if (clazz.equals(Yaml.class)) return SupportedStorageType.YAML;
        if (clazz.equals(Config.class)) return SupportedStorageType.YAML;
        if (clazz.equals(Json.class)) return SupportedStorageType.JSON;
        if (clazz.equals(Toml.class)) return SupportedStorageType.TOML;

        return null;
    }

    public static String parseDotsMongo(String key) {
        return key
                .replace("-", "")
                .replace(".", "_")
                ;
    }

    public static void ensureFileFromSelf(ClassLoader loader, File parentDirectory, File toEnsure, String fileName) {
        if (! toEnsure.exists()) {
            try {
                parentDirectory.mkdirs();
                try (InputStream in = loader.getResourceAsStream(fileName)) {
                    assert in != null;
                    Files.copy(in, toEnsure.toPath());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void ensureFileFromSelf(ClassLoader loader, File parentDirectory, File toEnsure) {
        ensureFileFromSelf(loader, parentDirectory, toEnsure, toEnsure.getName());
    }

    public static void ensureFileNoDefault(File parentDirectory, File toEnsure) {
        if (! toEnsure.exists()) {
            try {
                parentDirectory.mkdirs();
                toEnsure.createNewFile();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static void ensureFileExists(String path) {
        File file = new File(path);
        if (! file.exists()) {
            try {
                file.createNewFile();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static SimplixBuilder fromFile(File file) {
        return SimplixBuilder.fromFile(file)
                .setReloadSettings(getDefaultReloadSettings())
                .setConfigSettings(getDefaultConfigSettings())
                ;
    }

    /**
     * Simplix's own reload is disabled because {@code INTELLIGENT} stats the file on every single
     * read, which is a syscall per config getter on hot paths. File edits are picked up instead by
     * {@link gg.drak.thebase.storage.resources.flat.FlatFileResource}, which checks the file at most
     * once per {@code hangingMillis}.
     */
    public static ReloadSettings getDefaultReloadSettings() {
        return ReloadSettings.MANUALLY;
    }

    public static ConfigSettings getDefaultConfigSettings() {
        return ConfigSettings.PRESERVE_COMMENTS;
    }
}
