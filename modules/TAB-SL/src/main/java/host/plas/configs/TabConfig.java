package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.TABSL;

import java.util.ArrayList;
import java.util.List;

public class TabConfig extends SimpleConfiguration {
    public TabConfig() {
        super("config.yml", TABSL.getInstance(), true);
    }

    @Override
    public void init() {
        getLuckPermsMetaKeys();
    }

    public List<String> getLuckPermsMetaKeys() {
        reloadResource();

        return getOrSetDefault("placeholders.luckperms.meta-keys", new ArrayList<>(List.of("name_color", "rank_value")));
    }
}
