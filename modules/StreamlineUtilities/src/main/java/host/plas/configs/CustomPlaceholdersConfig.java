package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import singularity.placeholders.RATRegistry;
import singularity.placeholders.replaceables.GenericReplaceable;
import host.plas.StreamlineUtilities;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class CustomPlaceholdersConfig extends SimpleConfiguration {
    private ConcurrentSkipListMap<String, String> loadedPlaceholders = new ConcurrentSkipListMap<>();

    public CustomPlaceholdersConfig() {
        super("custom-placeholders.yml", StreamlineUtilities.getInstance().getDataFolder(), true);
        reloadResource(true);
    }

    @Override
    public void init() {
        loadedPlaceholders = new ConcurrentSkipListMap<>();
    }

    @Override
    public void reloadResource(boolean force) {
        getAsObjects().forEach(RATRegistry::unregister);
        super.reloadResource(force);
        this.loadedPlaceholders = getCustomPlaceholders();
        getAsObjects().forEach(RATRegistry::register);
    }

    private ConcurrentSkipListMap<String, String> getCustomPlaceholders() {
        ConcurrentSkipListMap<String, String> r = new ConcurrentSkipListMap<>();
        for (String key : getResource().singleLayerKeySet()) {
            try {
                r.put(key, getResource().getString(key));
            } catch (Exception e) {
                StreamlineUtilities.getInstance().logWarning("Could not load placeholder value for '" + key + "' due to: " + e.getMessage());
            }
        }
        return r;
    }

    public ConcurrentSkipListSet<GenericReplaceable> getAsObjects() {
        ConcurrentSkipListSet<GenericReplaceable> r = new ConcurrentSkipListSet<>();

        if (getLoadedPlaceholders() == null) return r;

        for (String key : getLoadedPlaceholders().keySet()) {
            r.add(new GenericReplaceable(key, (s) -> getLoadedPlaceholders().get(key)));
        }

        return r;
    }
}
