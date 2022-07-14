package net.streamline.base.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.streamline.api.events.ProperEvent;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.events.StreamlineEventBus;
import net.streamline.api.events.server.LoginEvent;
import net.streamline.api.events.server.LogoutEvent;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.utils.MessagingUtils;

public class BaseListener implements Listener {
    public BaseListener() {
        MessagingUtils.logInfo("BaseListener registered!");
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);

        ModuleUtils.fireEvent(new LoginEvent(savablePlayer));
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(uuid);
        if (savablePlayer == null) return;

        ModuleUtils.fireEvent(new LogoutEvent(savablePlayer));

        savablePlayer.storageResource.sync();
        UserManager.unloadUser(savablePlayer);
    }

    @EventHandler
    public void onServerSwitch(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);
        savablePlayer.setLatestServer(event.getServer().getInfo().getName());
    }

    public static class Observer extends StreamlineEventBus.StreamlineObserver {
        @Override
        public void update(StreamlineEvent<?> e) {
            if (e instanceof LevelChangePlayerEvent event) {
                if (Streamline.getMainConfig().announceLevelChangeChat()) {
                    for (String message : MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_CHAT.getStringList()) {
                        MessagingUtils.sendMessage(event.getResource(), MessagingUtils.replaceAllPlayerBungee(event.getResource(), message));
                    }
                }

                if (Streamline.getMainConfig().announceLevelChangeTitle()) {
                    StreamlineTitle title = new StreamlineTitle(
                            MessagingUtils.replaceAllPlayerBungee(event.getResource(), MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_MAIN.get()),
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

    @EventHandler
    public void onStreamlineEvent(ProperEvent<?> event) {
        Streamline.getStreamlineEventBus().notifyObservers(event.getEvent());
    }
}
