package host.plas.configs;

import host.plas.MVInvExtras;
import host.plas.data.WorldAction;
import lombok.Getter;
import lombok.Setter;
import tv.quaint.storage.resources.flat.simple.SimpleConfiguration;

import java.util.concurrent.ConcurrentSkipListSet;

public class MainConfig extends SimpleConfiguration {
    @Getter
    @Setter
    private static ConcurrentSkipListSet<WorldAction> worldActions;

    public MainConfig() {
        super("config.yml", // The name of the file.
                MVInvExtras.getInstance(), // The module instance.
                true); // Whether to copy the file from the jar. (Would have to be placed in the resources folder.)
    }

    @Override
    public void init() {
        setWorldActions(getWorldActionsFromConfig());
    }

    public void reloadConfig() {
        init();
    }

    public ConcurrentSkipListSet<WorldAction> getWorldActionsFromConfig() {
        reloadResource();

        ConcurrentSkipListSet<WorldAction> r = new ConcurrentSkipListSet<>();

        singleLayerKeySet("world-actions").forEach(key1 -> {
            String path = "world-actions." + key1;

            String identifier = key1;
            String regex = getOrSetDefault(path + ".regex", "(.*)");
            boolean createOwn = getOrSetDefault(path + ".createOwn", true);

            r.add(new WorldAction(identifier, regex, createOwn));
        });

        return r;
    }
}
