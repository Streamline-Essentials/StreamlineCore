package net.streamline.api.base.timers;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

public class UserSyncTimer extends BaseRunnable {
    public UserSyncTimer() {
        super(0, 20 * 60 * 3); // 3 minutes
    }

    @Override
    public void run() {
        UserUtils.syncAllUsers();
    }
}
