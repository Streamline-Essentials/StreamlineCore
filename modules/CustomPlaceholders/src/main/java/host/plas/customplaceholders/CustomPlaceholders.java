package host.plas.customplaceholders;

import lombok.Getter;
import lombok.Setter;
import org.pf4j.PluginWrapper;
import singularity.modules.SimpleModule;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Text placeholders defined by the server owner in {@code custom-placeholders.yml}.
 * Each key is replaced as written in Streamline messages, and is also reachable as
 * {@code %streamline_<key>%} (with {@code %...%} around the key left out), which
 * PlaceholderAPI forwards to Streamline.
 */
public class CustomPlaceholders extends SimpleModule {
    /** Where StreamlineUtilities kept the file before this module existed. */
    private static final String LEGACY_FOLDER = "streamline-utilities";

    @Getter @Setter
    private static CustomPlaceholders instance;

    @Getter @Setter
    private static CustomPlaceholdersConfig placeholdersConfig;

    public CustomPlaceholders(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void registerCommands() {
        setCommands(new ArrayList<>());
    }

    @Override
    public void onEnable() {
        instance = this;
        adoptLegacyFile();
        placeholdersConfig = new CustomPlaceholdersConfig();
    }

    @Override
    public void onDisable() {
        if (placeholdersConfig != null) placeholdersConfig.unregisterAll();
        placeholdersConfig = null;
        instance = null;
    }

    /**
     * Copies StreamlineUtilities' {@code custom-placeholders.yml} into this module's
     * folder when this module has none yet, so existing placeholders keep working.
     */
    private void adoptLegacyFile() {
        File target = new File(getDataFolder(), CustomPlaceholdersConfig.FILE_NAME);
        if (target.exists()) return;

        File legacy = new File(new File(getDataFolder().getParentFile(), LEGACY_FOLDER), CustomPlaceholdersConfig.FILE_NAME);
        if (! legacy.isFile()) return;

        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.copy(legacy.toPath(), target.toPath());
            logInfo("Copied " + CustomPlaceholdersConfig.FILE_NAME + " from StreamlineUtilities' folder.");
        } catch (Exception e) {
            logWarning("Could not copy " + legacy.getPath() + ": " + e.getMessage());
        }
    }
}
