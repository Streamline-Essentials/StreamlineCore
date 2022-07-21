package net.streamline.base.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.kyori.adventure.title.Title;
import com.velocitypowered.api.proxy.Player;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.events.StreamlineEventBus;
import net.streamline.api.events.server.LoginEvent;
import net.streamline.api.events.server.LogoutEvent;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.base.events.StreamlineChatEvent;
import net.streamline.utils.MessagingUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class BaseListener {
    public BaseListener() {
        MessagingUtils.logInfo("BaseListener registered!");
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);

        Streamline.fireEvent(new LoginEvent(savablePlayer));
    }

    @Subscribe
    public void onLeave(DisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(uuid);
        if (savablePlayer == null) return;

        Streamline.fireEvent(new LogoutEvent(savablePlayer));

        savablePlayer.storageResource.sync();
        UserManager.unloadUser(savablePlayer);
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);
        savablePlayer.setLatestServer(event.getServer().getServerInfo().getName());
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(savablePlayer, event.getMessage());
        Streamline.getStreamlineEventBus().notifyObservers(chatEvent);
        if (chatEvent.isCanceled()) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }
        event.setResult(PlayerChatEvent.ChatResult.message(chatEvent.getMessage()));
    }

    public static class Observer extends StreamlineEventBus.StreamlineObserver {
        @Override
        public void update(StreamlineEvent<?> e) {
            MessagingUtils.logInfo("Received ping!");
            if (e instanceof LevelChangePlayerEvent event) {
                MessagingUtils.logInfo("Is of LevelChange!");
                if (Streamline.getMainConfig().announceLevelChangeChat()) {
                    for (String message : MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_CHAT.getStringList()) {
                        MessagingUtils.sendMessage(event.getResource(), MessagingUtils.replaceAllPlayerBungee(event.getResource(),message));
                    }
                }

                if (Streamline.getMainConfig().announceLevelChangeTitle()) {
                    StreamlineTitle title = new StreamlineTitle(
                            MessagingUtils.replaceAllPlayerBungee(event.getResource(),MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_MAIN.get()),
                            MessagingUtils.replaceAllPlayerBungee(event.getResource(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_SUBTITLE.get())
                    );
                    title.setFadeIn(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_IN.getInt());
                    title.setStay(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_STAY.getInt());
                    title.setFadeOut(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_OUT.getInt());

                    MessagingUtils.sendTitle(event.getResource(), title);
                }
            }
        }
    }

    @Subscribe
    public void onStreamlineEvent(StreamlineEvent<?> event) {
        Streamline.getStreamlineEventBus().notifyObservers(event);
    }
}
