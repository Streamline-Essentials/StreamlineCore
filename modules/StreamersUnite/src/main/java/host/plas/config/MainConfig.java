package host.plas.config;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamersUnite;

public class MainConfig extends SimpleConfiguration {
    public MainConfig() {
        super("config.yml", StreamersUnite.getInstance(), false);
    }

    @Override
    public void init() {
        // Nothing to do here
        announceGoLive();
        announceGoOffline();

        getLivePrefix();
        getLiveSuffix();

        getOfflinePrefix();
        getOfflineSuffix();

        getLiveMessage();
        getGoLiveMessage();
    }

    public boolean announceGoLive() {
        reloadResource();

        return getOrSetDefault("announce.go-live", true);
    }

    public boolean announceGoOffline() {
        reloadResource();

        return getOrSetDefault("announce.go-offline", false);
    }

    public String getLivePrefix() {
        reloadResource();

        return getOrSetDefault("prefix.live", "&f[&c&lLIVE&f] &r");
    }

    public String getLiveSuffix() {
        reloadResource();

        return getOrSetDefault("suffix.live", "&r &f[&c&lLIVE&f]");
    }

    public String getOfflinePrefix() {
        reloadResource();

        return getOrSetDefault("prefix.offline", "&f[&7&lOFFLINW&f] &r");
    }

    public String getOfflineSuffix() {
        reloadResource();

        return getOrSetDefault("suffix.offline", "&r &f[&7&lOFFLINW&f]");
    }

    public String getLiveMessage() {
        reloadResource();

        return getOrSetDefault("messages.live-now.live", "%display_name% &eis currently &alive &eat &b&o%link%");
    }

    public String getGoLiveMessage() {
        reloadResource();

        return getOrSetDefault("messages.go-live.main", "&5&k!! &4\u23fa &c&lLIVE NOW &4\u23fa &5&k!! &b%display_name% &ehas just gone &alive &eat&8: &d&o%link%");
    }
}
