package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamlineUtilities;
import host.plas.configs.obj.PermissionGroup;

import java.util.concurrent.ConcurrentSkipListMap;

public class GroupedPermissionConfig extends SimpleConfiguration {
    public GroupedPermissionConfig() {
        super("grouped-permissions.yml", StreamlineUtilities.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {

    }

    public ConcurrentSkipListMap<String, PermissionGroup> getPermissionGroups() {
        ConcurrentSkipListMap<String, PermissionGroup> r = new ConcurrentSkipListMap<>();
        for (String key : getResource().singleLayerKeySet()) {
            try {
                String name = getResource().getString(key + ".name");
                String permission = getResource().getString(key + ".permission");
                r.put(key, new PermissionGroup(key, name, permission));
            } catch (Exception e) {
                StreamlineUtilities.getInstance().logWarning("Could not load placeholder value for '" + key + "' due to: " + e.getMessage());
            }
        }
        return r;
    }
}
