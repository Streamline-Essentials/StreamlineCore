package net.streamline.api.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.messages.ProxyMessageHelper;
import net.streamline.api.objects.SingleSet;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MathUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentSkipListMap;

public class OneSecondTimer extends BaseRunnable {

    public OneSecondTimer() {
        super(20, 20);
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

        for (Date date : ProxyMessageHelper.getCachedQueries().keySet()) {
            if (MathUtils.isDateOlderThan(date, 1, ChronoUnit.MINUTES)) {
                ProxyMessageHelper.removeQuery(date);
            }
        }
    }
}
