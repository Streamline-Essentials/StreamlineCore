package net.streamline.base.runnables;

import net.streamline.base.StreamlineVelocity;
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
        StreamlineVelocity.getPlayersByUUID().forEach((uuid, player) -> {
            if (UserUtils.isLoaded(player.getUniqueId().toString())) return;

            CosmicPlayer streamPlayer = UserUtils.getOrCreatePlayer(player.getUniqueId().toString());

            streamPlayer.setCurrentIp(UserManager.getInstance().parsePlayerIP(player.getUniqueId().toString()));
            streamPlayer.setCurrentName(player.getUsername());
            player.getCurrentServer().ifPresent(serverConnection -> streamPlayer.setServerName(serverConnection.getServerInfo().getName()));

            streamPlayer.ensureLoaded();
        });
    }
}
