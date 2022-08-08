package net.streamline.platform.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.streamline.api.SLAPI;
import net.streamline.api.messages.ProxyMessageEvent;
import net.streamline.api.messages.ProxyMessageIn;
import net.streamline.platform.events.ProperEvent;
import net.streamline.api.events.server.LogoutEvent;
import net.streamline.api.modules.ModuleManager;
import net.streamline.api.modules.ModuleUtils;
import net.streamline.platform.savables.UserManager;
import net.streamline.api.savables.users.StreamlinePlayer;
import net.streamline.api.savables.users.StreamlineUser;
import net.streamline.api.events.server.LoginReceivedEvent;
import net.streamline.api.events.server.StreamlineChatEvent;
import net.streamline.platform.Messenger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class PlatformListener implements Listener {
    public PlatformListener() {
        Messenger.getInstance().logInfo("BaseListener registered!");
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        StreamlineUser user = SLAPI.getInstance().getUserManager().getOrGetUserByName(event.getName());
        if (! (user instanceof StreamlinePlayer player)) return;

        LoginReceivedEvent loginReceivedEvent = new LoginReceivedEvent(player);
        ModuleUtils.fireEvent(loginReceivedEvent);

        if (loginReceivedEvent.getResult().isCancelled()) {
            loginReceivedEvent.getResult().validate();

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Messenger.getInstance().codedString(loginReceivedEvent.getResult().getDisconnectMessage()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

//        if (UserManager.getInstance().userExists(player.getUniqueId().toString())) {
//            StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
//            StreamlinePlayer.setLatestIP(UserManager.getInstance().parsePlayerIP(player));
//
//            LoginCompletedEvent loginCompletedEvent = new LoginCompletedEvent(StreamlinePlayer);
//            ModuleUtils.fireEvent(loginCompletedEvent);
//        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(uuid);
        if (StreamlinePlayer == null) return;

        LogoutEvent logoutEvent = new LogoutEvent(StreamlinePlayer);
        ModuleUtils.fireEvent(logoutEvent);

        StreamlinePlayer.getStorageResource().sync();
        UserManager.getInstance().unloadUser(StreamlinePlayer);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        StreamlinePlayer StreamlinePlayer = UserManager.getInstance().getOrGetPlayer(player);
        StreamlineChatEvent chatEvent = new StreamlineChatEvent(StreamlinePlayer, event.getMessage());
        ModuleManager.fireEvent(chatEvent);
        chatEvent.complete();
        if (chatEvent.isCanceled()) {
            event.setCancelled(true);
            return;
        }
        event.setMessage(chatEvent.getMessage());
        String newMessage = event.getMessage();
        event.setCancelled(true);
        player.chat(newMessage);

//        if (player.getProtocolVersion().getProtocol() > 759) {
//            if (chatEvent.isCanceled()) {
//                event.setResult(PlayerChatEvent.ChatResult.denied());
////                return;
//            }
//        } else {
//            if (chatEvent.isCanceled()) {
//                event.setResult(PlayerChatEvent.ChatResult.denied());
//                return;
//            }
//            // TODO: Change back once Velocity fixes it.
//            event.setResult(PlayerChatEvent.ChatResult.message(chatEvent.getMessage()));
//            if (event.getResult().getMessage().isPresent()) {
//                String newMessage = event.getResult().getMessage().get();
//                event.setResult(PlayerChatEvent.ChatResult.denied());
//                player.spoofChatInput(newMessage);
//            }
//        }
    }

    @EventHandler
    public void onProperEvent(ProperEvent event) {
        ModuleManager.fireEvent(event.getStreamlineEvent());
    }

    public static class ProxyMessagingListener implements PluginMessageListener {
        public ProxyMessagingListener() {
            Messenger.getInstance().logInfo("Registered " + getClass().getSimpleName() + "!");
        }

        @Override
        public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subChannel = input.readUTF();

            ProxyMessageIn messageIn = new ProxyMessageIn(channel, subChannel, message);
            SLAPI.getInstance().getProxyMessenger().receiveMessage(new ProxyMessageEvent(messageIn));
        }
    }
}
