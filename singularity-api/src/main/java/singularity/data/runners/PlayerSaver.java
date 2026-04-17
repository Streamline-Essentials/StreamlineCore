package singularity.data.runners;

import lombok.Getter;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.data.update.defaults.CosmicPlayerUpdater;
import singularity.data.update.defaults.DefaultUpdaters;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

/**
 * A periodic task that persists all currently loaded players to the database
 * and removes offline players from the in-memory cache.
 *
 * <p>The save interval is read from the main configuration each tick so that
 * changes to the configuration are reflected at runtime without a restart.
 * Players that were recently updated by the {@link CosmicPlayerUpdater} are
 * skipped to avoid redundant writes.</p>
 */
@Getter @Setter
public class PlayerSaver extends BaseRunnable {

    /**
     * Constructs a {@code PlayerSaver} that starts immediately (delay {@code 0}) and
     * repeats at the interval defined by the main configuration.
     */
    public PlayerSaver() {
        super(0, GivenConfigs.getMainConfig().getPlayerDataSaveInterval());
    }

    /**
     * {@inheritDoc}
     *
     * <p>On each execution this method:
     * <ol>
     *   <li>Re-reads the configured save interval and adjusts the task period if it changed.</li>
     *   <li>Iterates every loaded player and, if the {@link CosmicPlayerUpdater} has not
     *       already pushed a recent update, saves the player to the database.</li>
     *   <li>Unloads any player that is no longer online after saving.</li>
     * </ol>
     */
    @Override
    public void run() {
        long p = getPeriod();
        if (p != GivenConfigs.getMainConfig().getPlayerDataSaveInterval()) {
            setPeriod(GivenConfigs.getMainConfig().getPlayerDataSaveInterval());
        }

        CosmicPlayerUpdater updater = DefaultUpdaters.getPlayerUpdater();

        UserUtils.getLoadedPlayers().forEach((string, cosmicPlayer) -> {
            boolean updated = updater.checkAndPut(cosmicPlayer.getIdentifier());
            if (updated) return;

            cosmicPlayer.save();

            if (! cosmicPlayer.isOnline()) {
                UserUtils.unloadSender(cosmicPlayer);
            }
        });
    }
}
