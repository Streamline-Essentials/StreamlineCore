package net.streamline.base.runnables;

import net.streamline.base.Streamline;
import net.streamline.platform.savables.UserManager;
import singularity.data.players.CosmicPlayer;
import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

public class PlayerChecker extends BaseRunnable {
    public PlayerChecker() {
        super(0, 1);
    }

    @Override
    public void run() {
        Streamline.getPlayersByUUID().forEach((uuid, player) -> {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) return;

            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player));
            streamPlayer.setCurrentName(player.getName());

            streamPlayer.ensureLoaded();
        });
    }
}
