package net.streamline.base.timers;


import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.api.savables.users.SavableUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.base.Streamline;

public class OneSecondTimer extends BaseRunnable {

    public OneSecondTimer() {
        super(0, 20);
    }

    @Override
    public void run() {
        for (SavableUser user : UserManager.getLoadedUsers()) {
            if (user instanceof SavablePlayer player) {
                player.addPlaySecond(1);

                if (Streamline.getMainConfig().updatePlayerFormattedNames()) {
                    player.setDisplayName(UserManager.getDisplayName(player.latestName, player.latestName));
                }
            }
        }
    }
}
