package singularity.holders;

import lombok.Getter;
import lombok.Setter;
import singularity.holders.builtin.CosmicGeyserHolder;

import java.util.concurrent.ConcurrentSkipListSet;

public class HoldersHolder {
    public static String GEYSER_IDENTIFIER = "geyser";

    @Getter @Setter
    private static ConcurrentSkipListSet<HolderInit<?>> holders = new ConcurrentSkipListSet<>();

    public static void load(HolderInit<?> holderInit) {
        unload(holderInit);

        holders.add(holderInit);
    }

    public static void unload(HolderInit<?> holderInit) {
        getHolders().removeIf(h -> h.getIdentifier().equals(holderInit.getIdentifier()));
    }

    public static HolderInit<?> get(String identifier) {
        return getHolders().stream().filter(h -> h.getIdentifier().equals(identifier)).findFirst().orElse(null);
    }

    public static boolean has(String identifier) {
        return getHolders().stream().anyMatch(h -> h.getIdentifier().equals(identifier));
    }

    public static CosmicGeyserHolder getGeyserHolder() {
        HolderInit<?> holderInit = get(GEYSER_IDENTIFIER);
        if (holderInit == null) return null;
        if (! (holderInit.getHolder() instanceof CosmicGeyserHolder)) return null;
        return (CosmicGeyserHolder) holderInit.getHolder();
    }
}
