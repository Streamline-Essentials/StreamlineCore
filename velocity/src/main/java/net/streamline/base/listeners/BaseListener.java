package net.streamline.base.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import net.kyori.adventure.title.Title;
import com.velocitypowered.api.proxy.Player;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
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
    }

    @Subscribe
    public void onLeave(DisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(uuid);
        if (savablePlayer == null) return;

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
    public void onPlayerLevel(LevelChangePlayerEvent event) {
        if (Streamline.getMainConfig().announceLevelChangeChat()) {
            for (String message : MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_CHAT.getStringList()) {
                MessagingUtils.sendMessage(event.getResource(), MessagingUtils.replaceAllPlayerBungee(event.getResource(),message));
            }
        }

        if (Streamline.getMainConfig().announceLevelChangeTitle()) {
            Title title = Title.title(
                    MessagingUtils.codedText(
                            MessagingUtils.replaceAllPlayerBungee(event.getResource(),MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_MAIN.get())
                    ),
                            MessagingUtils.codedText(
                                    MessagingUtils.replaceAllPlayerBungee(event.getResource(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_SUBTITLE.get()))
                    , Title.Times.of(
                            Duration.of(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_IN.getInt() * 50L, ChronoUnit.MILLIS),
                            Duration.of(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_STAY.getInt() * 50L, ChronoUnit.MILLIS),
                            Duration.of(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_OUT.getInt() * 50L, ChronoUnit.MILLIS)
                            )
                    );

            MessagingUtils.sendTitle(event.getResource(), title);
        }
    }

    @Subscribe
    public void onStreamlineEvent(StreamlineEvent<?> event) {
        Streamline.getStreamlineEventBus().notifyObservers(event);
    }
}
