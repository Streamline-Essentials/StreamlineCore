package net.streamline.api.base.timers;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.savables.users.StreamlineConsole;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

public class UserSyncTimer extends BaseRunnable {
    public UserSyncTimer() {
        super(0, 20 * 60 * 3); // 3 minutes
    }

    @Override
    public void run() {
        UserUtils.getLoadedUsersSet().forEach(user -> {
            if (user.isOnline()) {
                if (user instanceof StreamlineConsole) return;
                user.setLatestName(SLAPI.getInstance().getUserManager().getUsername(user.getUuid()));
                if (user.getDisplayName().equals("null")) {
                    if (user instanceof StreamlinePlayer) {
                        user.setDisplayName(GivenConfigs.getMainConfig().userCombinedNicknameDefault());
                    }
                }
            }
        });
        UserUtils.syncAllUsers();
    }
}
