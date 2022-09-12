package net.streamline.api.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

import java.util.ArrayList;

public class UserSaveTimer extends BaseRunnable {
    public int coolDown;

    public UserSaveTimer() {
        super(0, 1);
        this.coolDown = 0;
    }

    // LEAVE HERE for when we finally implement custom time on this.
    @Override
    public void run() {
        if (this.coolDown <= 0) {
            this.coolDown = 200;
            done();
        }

        this.coolDown--;
    }

    public void done() {
        for (StreamlineUser user : UserUtils.getLoadedUsersSet()) {
            user.saveAll();
            UserUtils.unloadUser(user);
        }
    }
}
