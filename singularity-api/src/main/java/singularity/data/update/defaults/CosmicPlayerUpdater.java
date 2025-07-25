package singularity.data.update.defaults;

import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.update.UpdateType;

import java.util.Optional;

public class CosmicPlayerUpdater extends UpdateType<CosmicPlayer> {
    public CosmicPlayerUpdater() {
        super("cosmic_players", CosmicPlayer.class, (identifier) -> {
            Optional<CosmicSender> optional = Singularity.getMainDatabase().loadPlayer(identifier).join();
            return (CosmicPlayer) optional.filter(s -> s instanceof CosmicPlayer).orElse(null);
        }, (player) -> {
            Singularity.getMainDatabase().savePlayer(player);
        }, GivenConfigs.getMainConfig().getPlayerDataSaveInterval() * 20L);
    }
}
