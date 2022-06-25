package net.streamline.base.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
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
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        SavablePlayer savablePlayer = UserManager.getOrGetPlayer(uuid);
        if (savablePlayer == null) return;

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
    public void onPlayerLevel(LevelChangePlayerEvent event) {
        if (Streamline.getMainConfig().announceLevelChangeChat()) {
            for (String message : MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_CHAT.getStringList()) {
                MessagingUtils.sendMessage(event.user, message);
            }
        }

        if (Streamline.getMainConfig().announceLevelChangeTitle()) {
            Title title = ProxyServer.getInstance().createTitle();
            title = title.title(MessagingUtils.codedText(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_MAIN.get()));
            title = title.subTitle(MessagingUtils.codedText(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_SUBTITLE.get()));
            title = title.fadeIn(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_IN.getInt());
            title = title.stay(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_STAY.getInt());
            title = title.fadeOut(MainMessagesHandler.MESSAGES.EXPERIENCE.ONCHANGE_TITLE_OUT.getInt());

            MessagingUtils.sendTitle(event.player(), title);
        }
    }
}
