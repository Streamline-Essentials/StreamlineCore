package net.streamline.api.base.listeners;

import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.configs.given.CachedUUIDsHandler;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.events.server.LoginCompletedEvent;
import net.streamline.api.messages.builders.PlayerLocationMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.events.CreatePlayerEvent;
import net.streamline.api.savables.events.CreateSavableResourceEvent;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.utils.MessageUtils;
import net.streamline.api.utils.UserUtils;
import tv.quaint.events.BaseEventHandler;
import tv.quaint.events.BaseEventListener;
import tv.quaint.events.processing.BaseProcessor;
import tv.quaint.storage.resources.databases.events.SQLResourceStatementEvent;

public class BaseListener implements BaseEventListener {
    public BaseListener() {
        BaseModule.getInstance().logInfo("Loaded " + getClass().getSimpleName());
        BaseEventHandler.bake(this, SLAPI.getInstance());
    }

    @BaseProcessor
    public void onPlayerJoin(LoginCompletedEvent event) {
        StreamlineUser user = event.getResource();
        if (user.getUuid() == null) {
            MessageUtils.logWarning("Could not pass a player to the UUID handler! This is very serious! Please let the Quaint (the author) know immediately.");
            return;
        }
        CachedUUIDsHandler.cachePlayer(user.getUuid(), user.getLatestName());
        UserUtils.getUserFromDatabase(user);
    }

    @BaseProcessor
    public void onPlayerLevelChange(LevelChangePlayerEvent event) {
        if (GivenConfigs.getMainConfig().announceLevelChangeChat()) {
            for (String message : MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_CHAT.getStringList()) {
                SLAPI.getInstance().getMessenger().sendMessage(event.getResource(), MessageUtils.replaceAllPlayerBungee(event.getResource(), message));
            }
        }

        if (GivenConfigs.getMainConfig().announceLevelChangeTitle()) {
            StreamlineTitle title = new StreamlineTitle(
                    MessageUtils.replaceAllPlayerBungee(event.getResource(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_MAIN.get()),
                    MessageUtils.replaceAllPlayerBungee(event.getResource(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_SUBTITLE.get())
            );
            title.setFadeIn(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_IN.getInt());
            title.setStay(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_STAY.getInt());
            title.setFadeOut(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_OUT.getInt());

            SLAPI.getInstance().getMessenger().sendTitle(event.getResource(), title);
        }
    }

    @BaseProcessor
    public void onProxyMessage(ProxyMessageInEvent event) {
        if (event.getMessage() == null) return;
        if (event.getSubChannel() == null) return;

        if (SLAPI.isProxy()) {
            if (event.getSubChannel().equals(PlayerLocationMessageBuilder.getSubChannel())) {
                PlayerLocationMessageBuilder.handle(event.getMessage());
            }
        }
    }

    @BaseProcessor
    public void onSQLStatement(SQLResourceStatementEvent event) {
        MessageUtils.logDebug("&fSQL Statement&7: &2" + event.getStatement());
    }
}
