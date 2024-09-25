package net.streamline.api.base.timers;

import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

public class UserSyncTimer extends BaseRunnable {
    public UserSyncTimer() {
        super(0, 20 * 60 * 3); // 3 minutes
    }

    @Override
    public void run() {
        UserUtils.syncAllUsers();
    }
}
