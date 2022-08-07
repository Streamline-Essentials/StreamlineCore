package net.streamline.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.platform.savables.UserManager;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
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
            this.cooldown = SLAPI.getInstance().getPlatform().getMainConfig().playerPayoutExperienceEvery();
            done();
        }

        this.cooldown --;
    }

    public void done() {
        for (StreamlineUser user : SLAPI.getInstance().getUserManager().getLoadedUsers()) {
            if (user instanceof StreamlinePlayer player) {
                player.addTotalXP(SLAPI.getInstance().getPlatform().getMainConfig().playerPayoutExperienceAmount());
            }
        }
    }
}
