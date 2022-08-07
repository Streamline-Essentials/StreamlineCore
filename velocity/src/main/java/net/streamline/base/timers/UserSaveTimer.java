package net.streamline.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;

public class UserSaveTimer extends BaseRunnable {
    public int cooldown;

    public UserSaveTimer() {
        super(0, 1);
        this.cooldown = 0;
    }

    @Override
    public void run() {
        if (this.cooldown <= 0) {
            this.cooldown = 200;
            done();
        }

        this.cooldown --;
    }

    public void done() {
        for (StreamlineUser user : SLAPI.getInstance().getUserManager().getLoadedUsers()) {
            user.saveAll();
        }
    }
}
