package host.plas.customplaceholders;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import singularity.placeholders.RATRegistry;
import singularity.placeholders.replaceables.GenericReplaceable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * {@code custom-placeholders.yml}: every top-level key is a placeholder, its value the
 * replacement text. Each key is registered twice: as written, and as
 * {@code %streamline_<name>%}, where {@code <name>} is the key without a surrounding
 * {@code %...%} pair ({@code '%place_1st%'} → {@code %streamline_place_1st%},
 * {@code ':shrug:'} → {@code %streamline_:shrug:%}).
 */
@Getter
public class CustomPlaceholdersConfig extends SimpleConfiguration {
    public static final String FILE_NAME = "custom-placeholders.yml";
    public static final String STREAMLINE_PREFIX = "%streamline_";

    private ConcurrentSkipListMap<String, String> loadedPlaceholders = new ConcurrentSkipListMap<>();

    private final List<GenericReplaceable> registered = new ArrayList<>();

    public CustomPlaceholdersConfig() {
        super(FILE_NAME, CustomPlaceholders.getInstance().getDataFolder(), true);
        reloadResource(true);
    }

    @Override
    public void init() {
        loadedPlaceholders = new ConcurrentSkipListMap<>();
    }

    @Override
    public void reloadResource(boolean force) {
        super.reloadResource(force);
        unregisterAll();
        this.loadedPlaceholders = readPlaceholders();
        registerAll();
    }

    private ConcurrentSkipListMap<String, String> readPlaceholders() {
        ConcurrentSkipListMap<String, String> r = new ConcurrentSkipListMap<>();
        for (String key : getResource().singleLayerKeySet()) {
            try {
                String value = getResource().getString(key);
                if (value != null) r.put(key, value);
            } catch (Exception e) {
                CustomPlaceholders.getInstance().logWarning("Could not load placeholder value for '" + key + "' due to: " + e.getMessage());
            }
        }
        return r;
    }

    private synchronized void registerAll() {
        loadedPlaceholders.forEach((key, value) -> {
            register(key, value);
            String alias = streamlineAlias(key);
            if (alias != null && ! loadedPlaceholders.containsKey(alias)) register(alias, value);
        });
    }

    private void register(String key, String value) {
        GenericReplaceable replaceable = new GenericReplaceable(key, (s) -> value);
        RATRegistry.register(replaceable);
        registered.add(replaceable);
    }

    public synchronized void unregisterAll() {
        registered.forEach(RATRegistry::unregister);
        registered.clear();
    }

    /**
     * The {@code %streamline_<name>%} form of a key, or {@code null} when the key
     * already is one or is empty.
     */
    static String streamlineAlias(String key) {
        if (key == null || key.isEmpty() || key.startsWith(STREAMLINE_PREFIX)) return null;

        String name = key;
        if (name.length() > 2 && name.startsWith("%") && name.endsWith("%")) {
            name = name.substring(1, name.length() - 1);
        }
        return STREAMLINE_PREFIX + name + "%";
    }
}
