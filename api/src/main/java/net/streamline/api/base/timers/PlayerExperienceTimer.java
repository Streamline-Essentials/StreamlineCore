package net.streamline.api.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;

public class PlayerExperienceTimer extends BaseRunnable {
    public int coolDown;

    public PlayerExperienceTimer() {
        super(0, 1);
        this.coolDown = 0;
    }

    @Override
    public void run() {
        if (this.coolDown <= 0) {
            this.coolDown = GivenConfigs.getMainConfig().playerPayoutExperienceEvery();
            done();
        }

        this.coolDown--;
    }

    public void done() {
        for (StreamlineUser user : SLAPI.getInstance().getUserManager().getLoadedUsers()) {
            if (user instanceof StreamlinePlayer player) {
                player.addTotalXP(GivenConfigs.getMainConfig().playerPayoutExperienceAmount());
            }
        }
    }
}
