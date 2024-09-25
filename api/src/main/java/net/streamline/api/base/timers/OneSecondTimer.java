package net.streamline.api.base.timers;

import singularity.scheduler.BaseRunnable;
import singularity.utils.UserUtils;

public class OneSecondTimer extends BaseRunnable {

    public OneSecondTimer() {
        super(0, 20);
    }

    @Override
    public void run() {
        UserUtils.getLoadedSenders().forEach((s, user) -> {
            if (! user.isOnline()) return;

            user.addPlaySeconds(1);
        });
    }
}
