package singularity.client.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * The settings pages client modules registered, by module identifier. A page is removed
 * when its module stops.
 */
public final class ClientSettingsRegistry {

    private static final Map<String, ModuleSettings> PAGES = new ConcurrentSkipListMap<>();

    private ClientSettingsRegistry() {}

    /** Adds or replaces a module's page and loads its saved values. */
    public static ModuleSettings register(ModuleSettings settings) {
        settings.load();
        PAGES.put(settings.getModuleId(), settings);
        return settings;
    }

    public static void unregister(String moduleId) {
        PAGES.remove(moduleId);
    }

    public static ModuleSettings get(String moduleId) {
        return PAGES.get(moduleId);
    }

    public static List<ModuleSettings> all() {
        return new ArrayList<>(PAGES.values());
    }
}
