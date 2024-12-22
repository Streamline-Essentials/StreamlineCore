package singularity.data.runners;

import lombok.Getter;
import lombok.Setter;
import singularity.configs.given.GivenConfigs;
import singularity.data.update.defaults.CosmicPlayerUpdater;
import singularity.data.update.defaults.DefaultUpdaters;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

@Getter @Setter
public class PlayerSaver extends BaseRunnable {
    public PlayerSaver() {
        super(0, GivenConfigs.getMainConfig().getPlayerDataSaveInterval());
    }

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
