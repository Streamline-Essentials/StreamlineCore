package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.StreamlineRedirect;
import host.plas.configs.bits.ConfiguredReasonCheck;
import host.plas.configs.bits.ConfiguredRedirect;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public class MainConfig extends SimpleConfiguration {
    public MainConfig() {
        super("config.yml", StreamlineRedirect.getInstance().getDataFolder(), true);
    }

    @Override
    public void init() {
        getRedirects();

        getChecks();
    }

    public ConcurrentSkipListSet<ConfiguredRedirect> getRedirects() {
        reloadResource();

        ConcurrentSkipListSet<ConfiguredRedirect> redirects = new ConcurrentSkipListSet<>();

        getResource().singleLayerKeySet("redirects").forEach(key -> {
            ConcurrentSkipListSet<String> fromServers = new ConcurrentSkipListSet<>(getResource().getStringList("redirects." + key + ".from"));
            List<String> toServer = getResource().getStringList("redirects." + key + ".to");

            ConfiguredRedirect redirect = new ConfiguredRedirect(key, fromServers, toServer);

            redirects.add(redirect);
        });

        return redirects;
    }

    public boolean getEnabledOf(String identifier) {
        reloadResource();

        return getOrSetDefault("reasons." + identifier + ".enabled", true);
    }

    public String getActionOf(String identifier) {
        reloadResource();

        return getOrSetDefault("reasons." + identifier + ".action", "deny");
    }

    public boolean getSayKickMessage(String identifier) {
        reloadResource();

        return getOrSetDefault("reasons." + identifier + ".say-kick-message", true);
    }

    public boolean getCaseInsensitiveOf(String identifier) {
        reloadResource();

        return getOrSetDefault("reasons." + identifier + ".case-insensitive", true);
    }

    public List<String> getListOf(String identifier) {
        reloadResource();

        return getOrSetDefault("reasons." + identifier + ".list", new ArrayList<>(List.of("whitelisted")));
    }

    public ConcurrentSkipListSet<ConfiguredReasonCheck> getChecks() {
        reloadResource();

        ConcurrentSkipListSet<ConfiguredReasonCheck> checks = new ConcurrentSkipListSet<>();
        singleLayerKeySet("reasons").forEach(key -> {
            boolean enabled = getOrSetDefault("reasons." + key + ".enabled", true);
            String type = getOrSetDefault("reasons." + key + ".type", "contains");
            String action = getOrSetDefault("reasons." + key + ".action", "deny");
            boolean sayKickMessage = getOrSetDefault("reasons." + key + ".say-kick-message", true);
            boolean caseInsensitive = getOrSetDefault("reasons." + key + ".case-insensitive", true);
            List<String> list = getOrSetDefault("reasons." + key + ".list", new ArrayList<>(List.of("whitelisted")));

            checks.add(ConfiguredReasonCheck.of(key, type, enabled, action, sayKickMessage, caseInsensitive, list));
        });

        return checks;
    }
}
