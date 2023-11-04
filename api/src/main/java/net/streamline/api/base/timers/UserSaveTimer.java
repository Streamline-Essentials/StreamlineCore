package net.streamline.api.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

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
            this.coolDown = 48000;
            done();
        }

        this.coolDown--;
    }

    public void done() {
        UserUtils.syncAllUsers();
        UserUtils.getLoadedUsersSet().forEach(UserUtils::unloadUser);

        SLAPI.getInstance().getPlatform().getOnlinePlayerNames().forEach(UserUtils::getOrGetUserByName);
        UserUtils.getAllUsersFromDatabase();
    }
}
