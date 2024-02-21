package net.streamline.api.base.timers;


import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.data.players.events.UserNameUpdateEvent;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.UserUtils;

public class OneSecondTimer extends BaseRunnable {

    public OneSecondTimer() {
        super(20, 20);
    }

    @Override
    public void run() {
        UserUtils.getLoadedSendersSet().forEach(user -> {
            if (! user.isOnline()) return;

            if (user instanceof StreamPlayer) {
                StreamPlayer player = ((StreamPlayer) user);
                player.addPlaySecond(1);
            }
        });
    }
}
