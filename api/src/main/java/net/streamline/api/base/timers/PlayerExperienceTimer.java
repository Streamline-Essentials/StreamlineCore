package net.streamline.api.base.timers;


import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

public class PlayerExperienceTimer extends BaseRunnable {
    public int coolDown;

    public PlayerExperienceTimer() {
        super(0, 1);
        this.coolDown = 0;
    }

    @Override
    public void run() {
        if (this.coolDown >= GivenConfigs.getMainConfig().playerPayoutExperienceEvery()) {
            this.coolDown = 0;
            done();
        }

        this.coolDown ++;
    }

    public void done() {
        UserUtils.getLoadedSendersSet().forEach(sender -> sender.addExperience(GivenConfigs.getMainConfig().playerPayoutExperienceAmount()));
    }
}
