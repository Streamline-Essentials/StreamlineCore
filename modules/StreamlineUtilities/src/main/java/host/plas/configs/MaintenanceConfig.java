package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamlineUtilities;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

public class MaintenanceConfig extends SimpleConfiguration {
    public MaintenanceConfig() {
        super("maintenance-config.yml", StreamlineUtilities.getInstance().getDataFolder(), false);
    }

    @Override
    public void init() {
        isModeEnabled();
        getModeKickMessage();
        isModeKickOnline();
        getAllowedUUIDs();
    }

    public boolean isModeEnabled() {
        reloadResource();

        return getResource().getOrSetDefault("mode.enabled", false);
    }

    public void setModeEnabled(boolean bool) {
        getResource().set("mode.enabled", bool);
    }

    public String getModeKickMessage() {
        reloadResource();

        return getResource().getOrSetDefault("mode.kick.message", "&cThis server is currently being maintenanced&8!%newline%&cPlease feel free to come back in a little bit&8!%newline%%newline%&aJoin our &9&lDiscord &afor updates&8: {{discord_link}}");
    }

    public boolean isModeKickOnline() {
        reloadResource();

        return getResource().getOrSetDefault("mode.kick.online", true);
    }

    public ConcurrentSkipListSet<String> getAllowedUUIDs() {
        reloadResource();

        return new ConcurrentSkipListSet<>(getResource().getOrSetDefault("allowed-to-join.uuids", new ArrayList<>()));
    }

    public boolean containsAllowed(String uuid) {
        return getAllowedUUIDs().contains(uuid);
    }

    public void addAllowed(String uuid) {
        if (containsAllowed(uuid)) return;

        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>(getAllowedUUIDs());
        r.add(uuid);

        getResource().set("allowed-to-join.uuids", new ArrayList<>(r));
    }

    public void removeAllowed(String uuid) {
        if (! containsAllowed(uuid)) return;

        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>(getAllowedUUIDs());
        r.remove(uuid);

        getResource().set("allowed-to-join.uuids", new ArrayList<>(r));
    }
}
