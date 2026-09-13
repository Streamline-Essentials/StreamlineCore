package host.plas.config;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.data.StreamerSetup;
import host.plas.StreamersUnite;

import java.util.*;

public class StreamerConfig extends SimpleConfiguration {
    public StreamerConfig() {
        super("streamers.yml", StreamersUnite.getInstance(), false);
    }

    @Override
    public void init() {
        // Nothing to do here
    }

    public void saveSetup(StreamerSetup setup) {
        write(setup.getStreamerUuid().toString() + ".go-live", setup.getGoLiveCommands());
        write(setup.getStreamerUuid().toString() + ".go-offline", setup.getGoOfflineCommands());
        write(setup.getStreamerUuid().toString() + ".stream-link", setup.getStreamLink());
    }

    public void deleteSetup(String uuid) {
        getResource().remove(uuid + ".go-live");
        getResource().remove(uuid + ".go-offline");
        getResource().remove(uuid + ".stream-link");
    }

    public List<StreamerSetup> getSetups() {
        List<StreamerSetup> setups = new ArrayList<>();

        for (String key : getResource().singleLayerKeySet()) {
            Optional<StreamerSetup> setup = getSetup(key);
            setup.ifPresent(setups::add);
        }

        return setups;
    }

    public Optional<StreamerSetup> getSetup(String uuid) {
        try {
            UUID u = UUID.fromString(uuid);

            Set<String> singleKeySet = getResource().singleLayerKeySet();
            if (! singleKeySet.contains(uuid)) return Optional.empty();

            StreamerSetup s = new StreamerSetup(
                    u,
                    getOrSetDefault(uuid + ".go-live", new ArrayList<>()),
                    getOrSetDefault(uuid + ".go-offline", new ArrayList<>()),
                    getOrSetDefault(uuid + ".stream-link", "")
            );

            return Optional.of(s);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
