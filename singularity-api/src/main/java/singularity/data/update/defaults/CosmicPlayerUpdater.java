package singularity.data.update.defaults;

import singularity.Singularity;
import singularity.configs.given.GivenConfigs;
import singularity.data.console.CosmicSender;
import singularity.data.players.CosmicPlayer;
import singularity.data.update.UpdateType;

import java.util.Optional;

/**
 * An {@link UpdateType} implementation that handles loading and saving {@link CosmicPlayer}
 * instances through the main database.
 *
 * <p>The pull function synchronously loads a player from the database by UUID,
 * filtering out non-{@code CosmicPlayer} senders. The put function saves the
 * player back to the database. The update cadence is derived from the main
 * configuration's player data save interval (converted from ticks to milliseconds).</p>
 */
public class CosmicPlayerUpdater extends UpdateType<CosmicPlayer> {

    /**
     * Constructs a {@code CosmicPlayerUpdater} wired to the main database, using
     * the configured player data save interval as the update cadence.
     */
    public CosmicPlayerUpdater() {
        super("cosmic_players", CosmicPlayer.class, (identifier) -> {
            Optional<CosmicSender> optional = Singularity.getMainDatabase().loadPlayer(identifier).join();
            return (CosmicPlayer) optional.filter(s -> s instanceof CosmicPlayer).orElse(null);
        }, (player) -> {
            Singularity.getMainDatabase().savePlayer(player);
        }, GivenConfigs.getMainConfig().getPlayerDataSaveInterval() * 20L);
    }
}
