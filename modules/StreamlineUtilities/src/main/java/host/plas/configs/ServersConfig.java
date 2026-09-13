package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import lombok.Getter;
import singularity.modules.ModuleUtils;
import host.plas.StreamlineUtilities;
import host.plas.configs.obj.ConfiguredServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class ServersConfig extends SimpleConfiguration {
    private ConcurrentSkipListMap<String, ConfiguredServer> loadedServers = new ConcurrentSkipListMap<>();
    private Boolean hasLoaded = false;

    public ServersConfig() {
        super("server-aliases.yml", StreamlineUtilities.getInstance().getDataFolder(), false);
        hasLoaded = true;
    }

    @Override
    public void init() {
        loadedServers = new ConcurrentSkipListMap<>();
    }

    @Override
    public void reloadResource(boolean force) {
        if (hasLoaded == null) hasLoaded = false;

        this.loadedServers = new ConcurrentSkipListMap<>();
        super.reloadResource(force);

        if (hasLoaded) this.loadedServers = getServers();
    }

    private ConcurrentSkipListMap<String, ConfiguredServer> getServers() {
        ModuleUtils.getServerNames().forEach(s -> {
            if (getResource().contains(s)) return;
            new ConfiguredServer(s, new ConcurrentSkipListSet<>(), s).save();
        });

        ConcurrentSkipListMap<String, ConfiguredServer> r = new ConcurrentSkipListMap<>();
        for (String key : getResource().singleLayerKeySet()) {
            try {
                List<String> list = getResource().getStringList(key);
                if (list == null) throw new Exception("List is null");
                getResource().set(key + ".aliases", list);
            } catch (Exception e) {
                // ignore
            }

            if (! ModuleUtils.getServerNames().contains(key)) {
                StreamlineUtilities.getInstance().logWarning("Supposed server '" + key + "' in your 'server-aliases.yml' is not actually a server... Skipping...");
                continue;
            }

            try {
                ConfiguredServer configuredServer = ConfiguredServer.buildFrom(key, getResource().getSection(key));
                if (configuredServer == null) throw new Exception("ConfiguredServer is null");
                r.put(configuredServer.getActualServer(), configuredServer);
            } catch (Exception e) {
                StreamlineUtilities.getInstance().logWarning("Could not load server alias value for '" + key + "' due to: " + e.getMessage());
            }
        }
        return r;
    }

    public ConfiguredServer getServer(String aliasOrServer) {
        AtomicReference<ConfiguredServer> alias = new AtomicReference<>(null);
        StreamlineUtilities.getServersConfig().getLoadedServers().forEach((strng, configuredServer) -> {
            if (configuredServer.hasAliasOrActualName(aliasOrServer)) alias.set(configuredServer);
        });
        return alias.get();
    }

    public boolean hasServerAlias(String aliasOrServer) {
        return getServer(aliasOrServer) != null;
    }

    public String getActualName(String aliasOrServer) {
        if (! hasServerAlias(aliasOrServer)) return null;
        return getServer(aliasOrServer).getActualServer();
    }

    public ConcurrentSkipListSet<String> getPossibleNames() {
        ConcurrentSkipListSet<String> r = new ConcurrentSkipListSet<>();
        getLoadedServers().forEach((s, configuredServer) -> {
            r.add(configuredServer.getActualServer());
            r.addAll(configuredServer.getAliases());
        });
        return r;
    }

    public String getPrettyName(String aliasOrServer) {
        if (! hasServerAlias(aliasOrServer)) return null;
        return getServer(aliasOrServer).getPrettyName();
    }

    public void save(ConfiguredServer server) {
        write(server.getActualServer() + ".aliases", new ArrayList<>(server.getAliases()));
        write(server.getActualServer() + ".pretty-name", server.getPrettyName());
        getLoadedServers().put(server.getActualServer(), server);
    }

    public void remove(ConfiguredServer server) {
        getResource().remove(server.getActualServer());
        getLoadedServers().remove(server.getActualServer());
    }
}
