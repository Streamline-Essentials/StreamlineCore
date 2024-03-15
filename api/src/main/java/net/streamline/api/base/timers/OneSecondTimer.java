package net.streamline.api.base.timers;

import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

public class OneSecondTimer extends BaseRunnable {

    public OneSecondTimer() {
        super(20, 20);
    }

    @Override
    public void run() {
        UserUtils.getLoadedSenders().forEach((s, user) -> {
            if (! user.isOnline()) return;

            user.addPlaySecond(1);
        });
    }
}
