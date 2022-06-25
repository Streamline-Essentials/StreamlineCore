package net.streamline.base.timers;


import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.api.scheduler.StreamlineRunnable;
import net.streamline.base.Streamline;

public class OneSecondTimer extends StreamlineRunnable {
    public int cooldown;
    public int reset;

    public OneSecondTimer() {
        this.cooldown = 0;
        this.reset = 1;
    }

    @Override
    public void run() {
        if (this.cooldown <= 0) {
            this.cooldown = reset;
            done();
        }

        this.cooldown --;
    }

    public void done() {
        for (SavableUser user : UserManager.getLoadedUsers()) {
            if (user instanceof SavablePlayer player) {
                player.addPlaySecond(1);
            }

            if (Streamline.getMainConfig().updatePlayerFormattedNames()) {
                user.setDisplayName(UserManager.getDisplayName(user.latestName, user.latestName));
            }
        }
    }
}
