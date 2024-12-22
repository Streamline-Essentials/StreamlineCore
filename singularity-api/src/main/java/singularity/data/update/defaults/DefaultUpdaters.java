package singularity.data.update.defaults;

import lombok.Getter;
import lombok.Setter;

public class DefaultUpdaters {
    @Getter @Setter
    private static CosmicPlayerUpdater playerUpdater;

    public static void init() {
        playerUpdater = new CosmicPlayerUpdater();
        playerUpdater.load();
    }
}
