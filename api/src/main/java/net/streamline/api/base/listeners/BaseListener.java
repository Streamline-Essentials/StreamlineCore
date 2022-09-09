package net.streamline.api.base.listeners;

import net.streamline.api.SLAPI;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.events.EventProcessor;
import net.streamline.api.events.StreamlineListener;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.events.LevelChangePlayerEvent;

public class BaseListener implements StreamlineListener {
    @EventProcessor
    public void onPlayerLevelChange(LevelChangePlayerEvent event) {
        if (GivenConfigs.getMainConfig().announceLevelChangeChat()) {
            for (String message : MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_CHAT.getStringList()) {
                SLAPI.getInstance().getMessenger().sendMessage(event.getResource(), SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(event.getResource(), message));
            }
        }

        if (GivenConfigs.getMainConfig().announceLevelChangeTitle()) {
            StreamlineTitle title = new StreamlineTitle(
                    SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(event.getResource(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_MAIN.get()),
                    SLAPI.getInstance().getMessenger().replaceAllPlayerBungee(event.getResource(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_SUBTITLE.get())
            );
            title.setFadeIn(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_IN.getInt());
            title.setStay(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_STAY.getInt());
            title.setFadeOut(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_OUT.getInt());

            SLAPI.getInstance().getMessenger().sendTitle(event.getResource(), title);
        }
    }
}
