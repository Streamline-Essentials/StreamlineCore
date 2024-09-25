package net.streamline.api.base.timers;

import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

public class UserEnsureTimer extends BaseRunnable {
    public UserEnsureTimer() {
        super(20 * 30 // 30 seconds
                , 20 * 60); // 1 minute
    }

    @Override
    public void run() {
        UserUtils.ensureLoadedUsers();
    }
}
