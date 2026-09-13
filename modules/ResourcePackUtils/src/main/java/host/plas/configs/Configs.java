package host.plas.configs;

import gg.drak.thebase.storage.resources.flat.simple.SimpleConfiguration;
import host.plas.ResourcePackUtils;
import org.apache.commons.codec.binary.Hex;
import singularity.objects.CosmicResourcePack;

import java.util.ArrayList;
import java.util.List;

public class Configs extends SimpleConfiguration {
    public Configs() {
        super("config.yml", ResourcePackUtils.getInstance(), true);
        init();
    }

    @Override
    public void init() {
        getResourcePack();
        isNetworkHandled();
        connectWait();
    }

    public CosmicResourcePack getResourcePack() {
        reloadResource();

        String url = getResource().getOrSetDefault("pack.url", "https://linktopack.com");
        String prompt = getResource().getOrSetDefault("pack.prompt", "&eWe recommend using our pack.");
        String hashString = getResource().getOrSetDefault("pack.hash", "hashForPack");
        boolean force = getResource().getOrSetDefault("pack.force", false);
        byte[] hash;
        try {
            if (hashString.equals("")) {
                hash = new byte[0];
            } else {
                hash = Hex.decodeHex(hashString.toCharArray());
            }
        } catch (Exception e) {
            e.printStackTrace();
            hash = new byte[0];
        }

        return new CosmicResourcePack(url, hash, prompt, force);
    }

    public boolean isNetworkHandled() {
        reloadResource();

        return getResource().getOrSetDefault("pack.network-handled", true);
    }

    public int connectWait() {
        reloadResource();

        return getResource().getOrSetDefault("pack.wait", 20);
    }

    public List<String> getExemptStartsWith() {
        reloadResource();

        return getOrSetDefault("exempt.name-starts-with", new ArrayList<>(List.of(".")));
    }
}
