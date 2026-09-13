package host.plas.configs;

import singularity.configs.ModularizedConfig;
import host.plas.StreamlineGroups;
import host.plas.configs.roles.ConfiguredDefaultRole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class DefaultRoles extends ModularizedConfig {
    public DefaultRoles() {
        super(StreamlineGroups.getInstance(), "default-roles.yml", true);
    }

    @Override
    public void init() {

    }

    public List<ConfiguredDefaultRole> getDefaultRoles() {
        List<ConfiguredDefaultRole> r = new ArrayList<>();

        for (String key : getResource().singleLayerKeySet("Party")) {
            int priority = getResource().getInt("Party" + "." + key + ".priority");
            String name = getResource().getString("Party" + "." + key + ".name");
            int max = getResource().getInt("Party" + "." + key + ".max");
            ConcurrentSkipListSet<String> flags = new ConcurrentSkipListSet<>(getResource().getStringList("Party" + "." + key + ".flags"));
            r.add(new ConfiguredDefaultRole(key, priority, name, max, flags));
        }

        return r;
    }
}
