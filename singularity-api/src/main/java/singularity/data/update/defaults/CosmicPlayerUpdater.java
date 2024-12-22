package singularity.data.update.defaults;

import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.players.CosmicPlayer;
import singularity.data.update.UpdateType;

import java.util.Optional;

public class CosmicPlayerUpdater extends UpdateType<CosmicPlayer> {
    public CosmicPlayerUpdater() {
        super("cosmic_players", CosmicPlayer.class, (identifier) -> {
            Optional<CosmicPlayer> optional = Singularity.getMainDatabase().loadPlayer(identifier).join();
            return optional.orElse(null);
        }, (player) -> {
            Singularity.getMainDatabase().savePlayer(player);
        }, GivenConfigs.getMainConfig().getPlayerDataSaveInterval() * 20L);
    }
}
