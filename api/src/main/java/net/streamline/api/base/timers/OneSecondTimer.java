package net.streamline.api.base.timers;


import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.events.UserNameUpdateEvent;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

public class OneSecondTimer extends BaseRunnable {

    public OneSecondTimer() {
        super(20, 20);
    }

    @Override
    public void run() {
        UserUtils.getLoadedUsersSet().forEach(user -> {
            if (! user.updateOnline()) return;

            if (user instanceof StreamlinePlayer player) {
                player.addPlaySecond(1);
            }

            if (GivenConfigs.getMainConfig().updatePlayerFormattedNames()) {
                UserNameUpdateEvent updateEvent = new UserNameUpdateEvent(user, UserUtils.getFormattedDefaultNickname(user), user.getDisplayName());
                ModuleUtils.fireEvent(updateEvent);
                if (! updateEvent.isCancelled()) {
                    user.setDisplayName(updateEvent.getChangeTo());
                }
            }
        });
    }
}
