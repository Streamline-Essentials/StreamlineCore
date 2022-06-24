package net.streamline.base.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.streamline.api.savables.UserManager;
import net.streamline.api.savables.users.SavablePlayer;
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
}
