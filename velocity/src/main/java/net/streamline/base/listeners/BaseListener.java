package net.streamline.base.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
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

public class BaseListener {
    public BaseListener() {
        MessagingUtils.logInfo("BaseListener registered!");
    }

    @Subscribe
    public void onJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);
        savablePlayer.setLatestIP(UserManager.parsePlayerIP(player));

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
        if (! event.getResult().isAllowed()) return;

        Player player = event.getPlayer();

        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(savablePlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);
        chatEvent.complete();

        if (player.getProtocolVersion().getProtocol() > 759) {
            if (chatEvent.isCanceled()) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
//                return;
            }
        } else {
            if (chatEvent.isCanceled()) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                return;
            }
            // TODO: Change back once Velocity fixes it.
            event.setResult(PlayerChatEvent.ChatResult.message(chatEvent.getMessage()));
            if (event.getResult().getMessage().isPresent()) {
                String newMessage = event.getResult().getMessage().get();
                event.setResult(PlayerChatEvent.ChatResult.denied());
                player.spoofChatInput(newMessage);
            }
        }
    }

    @Subscribe
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getEvent());
    }

    public static class Observer implements StreamlineListener {
        @EventProcessor
        public void onPlayerLevelChange(LevelChangePlayerEvent event) {
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
