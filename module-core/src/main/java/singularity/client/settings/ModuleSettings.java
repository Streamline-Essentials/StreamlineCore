package singularity.client.settings;

import lombok.Getter;
import singularity.client.ClientEnvironment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A client module's page in the {@code /slclient menu} menu: its settings, in the order they
 * were added. Values are kept in {@code client/settings/<module-id>.properties} in
 * Streamline's data folder; {@link ClientSettingsRegistry#register} reads them and the menu
 * calls {@link #save()} after a change.
 */
@Getter
public class ModuleSettings {

    private final String moduleId;
    private final String displayName;
    private final String description;
    private final List<Setting<?>> settings = new CopyOnWriteArrayList<>();

    public ModuleSettings(String moduleId, String displayName, String description) {
        this.moduleId = moduleId;
        this.displayName = displayName;
        this.description = description == null ? "" : description;
    }

    /** Adds a setting and returns it, for chaining {@link Setting#onChange}. */
    public <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public Setting<?> get(String key) {
        for (Setting<?> setting : settings) {
            if (setting.getKey().equals(key)) return setting;
        }
        return null;
    }

    /** @return where the values are kept, or {@code null} off a game client */
    protected File file() {
        return ClientEnvironment.file("settings", moduleId + ".properties");
    }

    /** Reads saved values over the defaults; settings without a saved value keep theirs. */
    public ModuleSettings load() {
        File file = file();
        if (file == null || ! file.isFile()) return this;
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (IOException e) {
            ClientEnvironment.logWarning("Could not read " + file + ": " + e.getMessage());
            return this;
        }
        for (Setting<?> setting : settings) {
            String saved = properties.getProperty(setting.getKey());
            if (saved != null) setting.setFromText(saved);
        }
        return this;
    }

    public synchronized void save() {
        File file = file();
        if (file == null) return;
        Properties properties = new Properties();
        for (Setting<?> setting : settings) {
            properties.setProperty(setting.getKey(), setting.formatValue());
        }
        file.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(file)) {
            properties.store(out, displayName + " client settings");
        } catch (IOException e) {
            ClientEnvironment.logWarning("Could not write " + file + ": " + e.getMessage());
        }
    }
}
