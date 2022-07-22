package net.streamline.base.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.streamline.api.events.EventProcessor;
import net.streamline.api.events.ProperEvent;
import net.streamline.api.events.StreamlineEvent;
import net.streamline.api.events.StreamlineListener;
import net.streamline.api.events.server.LoginEvent;
import net.streamline.api.events.server.LogoutEvent;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.api.objects.StreamlineTitle;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.events.LevelChangePlayerEvent;
import net.streamline.api.savables.users.SavablePlayer;
import net.streamline.base.Streamline;
import net.streamline.base.configs.MainMessagesHandler;
import net.streamline.base.events.StreamlineChatEvent;
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

    @EventHandler
    public void onChat(ChatEvent event) {
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(savablePlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);
        if (chatEvent.isCanceled()) {
            event.setCancelled(true);
        }
        event.setMessage(chatEvent.getMessage());
    }

    @EventHandler
    public void onProperEvent(ProperEvent<?> event) {
        ModuleManager.fireEvent(event.getEvent());
    }

    public static class Observer implements StreamlineListener {
        @EventProcessor
        public void onPlayerLevelChange(LevelChangePlayerEvent event) {
            MessagingUtils.logInfo("Is of LevelChange!");
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
