package net.streamline.api.base.timers;


import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.messages.builders.SavablePlayerMessageBuilder;
import net.streamline.api.messages.builders.UserNameMessageBuilder;
import net.streamline.api.messages.proxied.ProxiedMessage;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.savables.events.UserNameUpdateEvent;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.scheduler.BaseRunnable;
import net.streamline.api.utils.MathUtils;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;

import java.time.temporal.ChronoUnit;
import java.util.Date;

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
                UserNameUpdateEvent updateEvent = new UserNameUpdateEvent(user, UserUtils.getDisplayName(user.getLatestName(), user.getLatestName()), user.getDisplayName());
                ModuleUtils.fireEvent(updateEvent);
                if (! updateEvent.isCancelled()) {
                    user.setDisplayName(updateEvent.getChangeTo());
                }

                if (user instanceof StreamlinePlayer player && SLAPI.isProxy()) {
                    UserNameMessageBuilder.build(player, user.getDisplayName(), player).send();
                }
            }
        });
    }
}
