package net.streamline.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;

public class OneSecondTimer extends BaseRunnable {

    public OneSecondTimer() {
        super(0, 20);
    }

    @Override
    public void run() {
        for (StreamlineUser user : SLAPI.getInstance().getUserManager().getLoadedUsers()) {
            if (user instanceof StreamlinePlayer player) {
                player.addPlaySecond(1);

                if (GivenConfigs.getMainConfig().updatePlayerFormattedNames()) {
                    player.setDisplayName(SLAPI.getInstance().getUserManager().getDisplayName(player.getLatestName(), player.getLatestName()));
                }
            }
        }
    }
}
