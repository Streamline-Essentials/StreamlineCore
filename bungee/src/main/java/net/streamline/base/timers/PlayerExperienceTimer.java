package net.streamline.base.timers;


import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.base.Streamline;

public class PlayerExperienceTimer extends BaseRunnable {
    public int cooldown;

    public PlayerExperienceTimer() {
        super(0, 1);
        this.cooldown = 0;
    }

    @Override
    public void run() {
        if (this.cooldown <= 0) {
            this.cooldown = Streamline.getMainConfig().playerPayoutExperienceEvery();
            done();
        }

        this.cooldown --;
    }

    public void done() {
        for (SavableUser user : UserManager.getLoadedUsers()) {
            if (user instanceof SavablePlayer player) {
                player.addTotalXP(Streamline.getMainConfig().playerPayoutExperienceAmount());
            }
        }
    }
}
