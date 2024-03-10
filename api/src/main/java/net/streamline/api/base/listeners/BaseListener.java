package net.streamline.api.base.listeners;

import net.streamline.api.SLAPI;
import net.streamline.api.base.module.BaseModule;
import net.streamline.api.configs.given.GivenConfigs;
import net.streamline.api.configs.given.MainMessagesHandler;
import net.streamline.api.data.console.StreamSender;
import net.streamline.api.data.players.StreamPlayer;
import net.streamline.api.data.players.events.LevelChangeEvent;
import net.streamline.api.data.uuid.UuidInfo;
import net.streamline.api.data.uuid.UuidManager;
import net.streamline.api.events.server.LoginCompletedEvent;
import net.streamline.api.messages.builders.PlayerLocationMessageBuilder;
import net.streamline.api.messages.events.ProxyMessageInEvent;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.utils.MessageUtils;
import tv.quaint.events.BaseEventHandler;
import tv.quaint.events.BaseEventListener;
import tv.quaint.events.processing.BaseProcessor;

import java.util.Optional;

public class BaseListener implements BaseEventListener {
    public BaseListener() {
        BaseModule.getInstance().logInfo("Loaded " + getClass().getSimpleName());
        BaseEventHandler.bake(this, SLAPI.getInstance());
    }

    @BaseProcessor
    public void onPlayerJoin(LoginCompletedEvent event) {
        StreamSender user = event.getSender();
        if (user.getUuid() == null) {
            MessageUtils.logWarning("Could not pass a player to the UUID handler! This is very serious! Please let the Quaint (the author) know immediately.");
            return;
        }
        if (! (user instanceof StreamPlayer)) return;
        StreamPlayer player = (StreamPlayer) user;

        Optional<UuidInfo> info = UuidManager.getUuid(user.getUuid());
        if (info.isEmpty()) {
            SLAPI.getMainDatabase().loadUuidInfo(user.getUuid()).whenComplete((uuidInfo, throwable) -> {
                if (throwable != null) {
                    MessageUtils.logWarning("Could not load UUID info for " + user.getUuid() + "!", throwable);
                    return;
                }
                if (uuidInfo.isEmpty()) {
                    UuidInfo u = new UuidInfo(player.getUuid(), player.getCurrentName(), player.getCurrentIP());
                    u.register();
                    u.save();
                } else {

                    UuidInfo u = uuidInfo.get();
                    u.register();

                    u.addName(player.getCurrentName());
                    u.addIp(player.getCurrentIP());
                }
            });
        } else {
            UuidInfo u = info.get();
            u.register();

            u.addName(player.getCurrentName());
            u.addIp(player.getCurrentIP());
        }
    }

    @BaseProcessor
    public void onPlayerLevelChange(LevelChangeEvent event) {
        if (GivenConfigs.getMainConfig().announceLevelChangeChat()) {
            for (String message : MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_CHAT.getStringList()) {
                SLAPI.getInstance().getMessenger().sendMessage(event.getSender(), MessageUtils.replaceAllPlayerBungee(event.getSender(), message));
            }
        }

        if (GivenConfigs.getMainConfig().announceLevelChangeTitle()) {
            StreamlineTitle title = new StreamlineTitle(
                    MessageUtils.replaceAllPlayerBungee(event.getSender(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_MAIN.get()),
                    MessageUtils.replaceAllPlayerBungee(event.getSender(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_SUBTITLE.get())
            );
            title.setFadeIn(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_IN.getInt());
            title.setStay(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_STAY.getInt());
            title.setFadeOut(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_OUT.getInt());

            SLAPI.getInstance().getMessenger().sendTitle(event.getSender(), title);
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
}
